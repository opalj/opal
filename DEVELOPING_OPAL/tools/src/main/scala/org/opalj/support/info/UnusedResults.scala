/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package support
package info

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.EagerVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties.{Purity ⇒ PurityProperty}
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.VirtualMethodPurity.VPure
import org.opalj.fpcf.properties.VirtualMethodPurity.VSideEffectFree
import org.opalj.fpcf.properties.VirtualMethodPurity.VCompileTimePure
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.DUVar

import scala.collection.JavaConverters._

/**
 * Identifies calls to pure/side-effect free methods where the results are not used subsequently.
 *
 * @author Dominik Helm
 */
object UnusedResults extends DefaultOneStepAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

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
                val callee = call.resolveCallTarget
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
                case FinalEP(_, CompileTimePure | Pure | SideEffectFree) ⇒
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

        val value = receiver.asVar.value.asDomainReferenceValue
        val receiverType = value.valueType

        if (receiverType.isEmpty) {
            None // Receiver is null, call will never be executed
        } else if (receiverType.get.isArrayType) {
            val callee = project.instanceCall(callerType, ObjectType.Object, name, descr)
            handleCall(caller, callee, call.pc)
        } else if (receiver.asVar.value.asDomainReferenceValue.isPrecise) {
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
                    case FinalEP(_, VCompileTimePure | VPure | VSideEffectFree) ⇒
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
