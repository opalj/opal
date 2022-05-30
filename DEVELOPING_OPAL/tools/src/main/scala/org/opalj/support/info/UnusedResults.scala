/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.properties.{Purity => PurityProperty}
import org.opalj.br.fpcf.properties.VirtualMethodPurity.VCompileTimePure
import org.opalj.br.fpcf.properties.VirtualMethodPurity.VPure
import org.opalj.br.fpcf.properties.VirtualMethodPurity.VSideEffectFree
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.VirtualMethodPurity
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.immutable.ArraySeq

/**
 * Identifies calls to pure/side-effect free methods where the results are not used subsequently.
 *
 * @author Dominik Helm
 */
object UnusedResults extends ProjectAnalysisApplication {

    /** The type of the TAC domain. */
    type V = DUVar[ValueInformation]

    override def title: String = "Unused Results Analysis"

    override def description: String = {
        "find invokations of pure/side effect free methods where the result is not used"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean
    ): BasicReport = {

        val issues = new ConcurrentLinkedQueue[String]

        implicit val p: SomeProject = project
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val isMethodOverridable: Method => Answer = project.get(IsOverridableMethodKey)

        project.get(RTACallGraphKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            EagerL2PurityAnalysis
        )

        project.parForeachMethodWithBody() { mi =>
            val method = mi.method
            val tacai = (m: Method) => { val FinalP(taCode) = ps(method, TACAI.key); taCode.tac }
            issues.addAll(analyzeMethod(method, tacai = tacai).asJava)
        }

        BasicReport(issues.asScala)
    }

    def analyzeMethod(
        method: Method,
        tacai:  Method => Option[TACode[_, V]]
    )(
        implicit
        project:             SomeProject,
        propertyStore:       PropertyStore,
        declaredMethods:     DeclaredMethods,
        isMethodOverridable: Method => Answer
    ): Seq[String] = {
        val taCodeOption = tacai(method)
        if (taCodeOption.isEmpty)
            return Nil;

        val code = taCodeOption.get.stmts

        val issues = code collect {
            case ExprStmt(_, call: StaticFunctionCall[V]) =>
                val callee = call.resolveCallTarget(method.classFile.thisType)
                handleCall(method, callee, call.pc)
            case ExprStmt(_, call: NonVirtualFunctionCall[V]) =>
                val callee = call.resolveCallTarget(method.classFile.thisType)
                handleCall(method, callee, call.pc)
            case ExprStmt(_, call: VirtualFunctionCall[V]) =>
                handleVirtualCall(call, method)
        }

        ArraySeq.unsafeWrapArray(issues) collect { case Some(issue) => issue }
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
                case FinalP(CompileTimePure | Pure | SideEffectFree) =>
                    createIssue(caller, callee.value, pc)
                case _ => None
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
        isMethodOverridable: Method => Answer
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
                    case FinalP(VCompileTimePure | VPure | VSideEffectFree) =>
                        createIssue(caller, callee.definedMethod, call.pc)
                    case _ => None
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
