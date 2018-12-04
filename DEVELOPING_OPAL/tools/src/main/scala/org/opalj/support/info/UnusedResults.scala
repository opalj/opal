/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import org.opalj.fpcf.FinalP
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.EagerVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties.{Purity ⇒ PurityProperty}
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.VirtualMethodPurity.VCompileTimePure
import org.opalj.fpcf.properties.VirtualMethodPurity.VPure
import org.opalj.fpcf.properties.VirtualMethodPurity.VSideEffectFree
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall

/**
 * Identifies calls to pure/side-effect free methods where the results are not used subsequently.
 *
 * @author Dominik Helm
 */
object UnusedResults extends DefaultOneStepAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[ValueInformation]

    override def title: String = "Unused Results Analysis"

    override def description: String = {
        "find invokations of pure/side effect free methods where the result is not used"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val issues = new ConcurrentLinkedQueue[String]

        implicit val p: SomeProject = project
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        implicit val tacai: Method ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyVirtualMethodStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            EagerVirtualMethodPurityAnalysis,
            EagerL2PurityAnalysis
        )

        project.parForeachMethodWithBody() { methodInfo ⇒
            val method = methodInfo.method
            issues.addAll(
                analyzeMethod(method).asJava
            )
        }

        BasicReport(issues.asScala)
    }

    def analyzeMethod(
        method: Method
    )(
        implicit
        project:             SomeProject,
        propertyStore:       PropertyStore,
        tacai:               Method ⇒ TACode[_, V],
        declaredMethods:     DeclaredMethods,
        isMethodOverridable: Method ⇒ Answer
    ): Seq[String] = {
        val code = tacai(method).stmts

        val issues = code collect {
            case ExprStmt(_, call: StaticFunctionCall[V]) ⇒
                val callee = call.resolveCallTarget
                handleCall(method, callee, call.pc)
            case ExprStmt(_, call: NonVirtualFunctionCall[V]) ⇒
                val callee = call.resolveCallTarget(method.classFile.thisType)
                handleCall(method, callee, call.pc)
            case ExprStmt(_, call: VirtualFunctionCall[V]) ⇒
                handleVirtualCall(call, method)
        }

        issues collect { case Some(issue) ⇒ issue }
    }

    def handleCall(
        caller: Method,
        callee: Result[Method],
        pc:     Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Option[String] = {
        if (callee.hasValue) {
            propertyStore(declaredMethods(callee.value), PurityProperty.key) match {
                case FinalP(_, CompileTimePure | Pure | SideEffectFree) ⇒
                    createIssue(caller, callee.value, pc)
                case _ ⇒ None
            }
        } else {
            None
        }
    }

    def handleVirtualCall(
        call:   VirtualFunctionCall[V],
        caller: Method
    )(
        implicit
        project:             SomeProject,
        propertyStore:       PropertyStore,
        declaredMethods:     DeclaredMethods,
        isMethodOverridable: Method ⇒ Answer
    ): Option[String] = {
        val callerType = caller.classFile.thisType
        val VirtualFunctionCall(_, dc, _, name, descr, receiver, _) = call

        val value = receiver.asVar.value.asReferenceValue
        val receiverType = value.leastUpperType

        if (receiverType.isEmpty) {
            None // Receiver is null, call will never be executed
        } else if (receiverType.get.isArrayType) {
            val callee = project.instanceCall(callerType, ObjectType.Object, name, descr)
            handleCall(caller, callee, call.pc)
        } else if (value.isPrecise) {
            val callee = project.instanceCall(callerType, receiverType.get, name, descr)
            handleCall(caller, callee, call.pc)
        } else {
            val callee = declaredMethods(
                dc.asObjectType,
                callerType.packageName,
                receiverType.get.asObjectType,
                name,
                descr
            )

            if (!callee.hasSingleDefinedMethod || isMethodOverridable(callee.definedMethod).isNotNo) {
                None // We don't know all overrides, ignore the call (it may be impure)
            } else {
                propertyStore(callee, VirtualMethodPurity.key) match {
                    case FinalP(_, VCompileTimePure | VPure | VSideEffectFree) ⇒
                        createIssue(caller, callee.definedMethod, call.pc)
                    case _ ⇒ None
                }
            }
        }
    }

    private def createIssue(
        method: Method,
        target: Method,
        pc:     PC
    ): Some[String] = {
        Some(s"Unused result of call to ${target.toJava} from ${method.toJava} at $pc")
    }
}
