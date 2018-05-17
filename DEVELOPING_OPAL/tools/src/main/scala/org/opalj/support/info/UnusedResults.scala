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
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.DeclaredMethods
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.DeclaredMethodsKey
import org.opalj.fpcf.properties.{Purity ⇒ PurityProperty}
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.LBSideEffectFree
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.VirtualMethodPurity.VLBPure
import org.opalj.fpcf.properties.VirtualMethodPurity.VLBSideEffectFree
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

        val propertyStore = project.get(PropertyStoreKey)
        val tacai: Method ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
        val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        project.parForeachMethodWithBody() { methodInfo ⇒
            val method = methodInfo.method
            issues.addAll(
                analyzeMethod(project, propertyStore, tacai, declaredMethods, method).asJava
            )
        }

        BasicReport(issues.asScala)
    }

    def analyzeMethod(
        project:         SomeProject,
        propertyStore:   PropertyStore,
        tacai:           Method ⇒ TACode[TACMethodParameter, V],
        declaredMethods: DeclaredMethods,
        method:          Method
    ): Seq[String] = {
        val code = tacai(method).stmts

        val issues = code collect {
            case ExprStmt(_, call @ StaticFunctionCall(_, declClass, isInterface, name, descr, _)) ⇒
                val callee = project.staticCall(declClass, isInterface, name, descr)
                if (callee.hasValue)
                    propertyStore(declaredMethods(callee.value), PurityProperty.key) match {
                        case FinalEP(_, CompileTimePure | LBPure | LBSideEffectFree) ⇒
                            createIssue(method, callee.value, call.pc)
                        case _ ⇒ None
                    }
                else None
            case ExprStmt(_, call @ NonVirtualFunctionCall(_, declClass, isInterface, name, descr, _, _)) ⇒
                val callee = project.specialCall(declClass, isInterface, name, descr)
                if (callee.hasValue)
                    propertyStore(declaredMethods(callee.value), PurityProperty.key) match {
                        case FinalEP(_, CompileTimePure | LBPure | LBSideEffectFree) ⇒
                            createIssue(method, callee.value, call.pc)
                        case _ ⇒ None
                    }
                else None
            case ExprStmt(_, call @ VirtualFunctionCall(_, declClass, isInterface, name, descr, receiver, _)) ⇒
                val receiverType =
                    if (receiver.isVar) {
                        project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                            receiver.asVar.value.asDomainReferenceValue.upperTypeBound
                        )
                    } else {
                        declClass
                    }
                val methodO =
                    project.instanceCall(declClass.asObjectType, receiverType, name, descr)
                if (methodO.hasValue) {
                    val declaredMethod = declaredMethods(DefinedMethod(receiverType, methodO.value))
                    propertyStore(declaredMethod, VirtualMethodPurity.key) match {
                        case FinalEP(_, VCompileTimePure | VLBPure | VLBSideEffectFree) ⇒
                            createIssue(method, methodO.value, call.pc)
                        case _ ⇒ None
                    }
                } else None
        }

        issues collect { case Some(issue) ⇒ issue }
    }

    private def createIssue(
        method: Method,
        target: Method,
        pc:     PC
    ): Some[String] = {
        Some(s"Unused result of call to ${target.toJava} from ${method.toJava} at $pc")
    }
}
