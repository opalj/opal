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
package fpcf
package analyses
package escape

import org.opalj.ai.ValueOrigin
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ReferenceType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntArraySet
import org.opalj.tac.Expr
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.ArgEscape
import org.opalj.fpcf.properties.MaybeMethodEscape
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.MethodEscape
import org.opalj.tac.Stmt
import org.opalj.tac.DUVar
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualMethodCall

trait InterproceduralEntityEscapeAnalysis1 extends ConstructorSensitiveEntityEscapeAnalysis {

    override def handleStaticMethodCall(call: StaticMethodCall[V]): Unit = {
        handleStaticCall(
            call.declaringClass, call.isInterface, call.name, call.descriptor, call.params
        )
    }

    override def handleStaticFunctionCall(call: StaticFunctionCall[V]): Unit = {
        handleStaticCall(
            call.declaringClass, call.isInterface, call.name, call.descriptor, call.params
        )
    }

    override def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params
        )
    }

    override def handleVirtualFunctionCall(call: VirtualFunctionCall[V]): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params
        )
    }

    override def handleParameterOfConstructor(call: NonVirtualMethodCall[V]): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params)
    }

    override def handleNonVirtualAndNonConstructorCall(call: NonVirtualMethodCall[V]): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params)
        if (usesDefSite(call.receiver))
            handleCall(methodO, 0)
    }

    override def handleNonVirtualFunctionCall(call: NonVirtualFunctionCall[V]): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params)
        if (usesDefSite(call.receiver))
            handleCall(methodO, 0)
    }

    def handleStaticCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, params: Seq[Expr[V]]): Unit = {
        checkParams(project.staticCall(dc.asObjectType, isI, name, descr), params)
    }

    def handleVirtualCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
        assert(receiver.isVar)
        val value = receiver.asVar.value.asDomainReferenceValue
        if (value.isPrecise) {
            if (value.isNull.isNoOrUnknown) {
                val valueType = value.valueType.get
                val methodO = project.instanceCall(m.classFile.thisType, valueType, name, descr)
                checkParams(methodO, params)
                if (usesDefSite(receiver)) handleCall(methodO, 0)

            } else {
                // todo throw new NullPointerException()
            }
        } else {
            //TODO
            val packageName = m.classFile.thisType.packageName
            val methods =
                if (isI) project.interfaceCall(dc.asObjectType, name, descr)
                else project.virtualCall(packageName, dc, name, descr)
            for (method ← methods) {
                checkParams(Success(method), params)
                if (usesDefSite(receiver))
                    handleCall(Success(method), 0)
            }
        }
    }

    def checkParams(methodO: org.opalj.Result[Method], params: Seq[Expr[V]]): Unit = {
        for (i ← params.indices) {
            if (usesDefSite(params(i)))
                handleCall(methodO, i + 1)
        }
    }

    def handleCall(methodO: org.opalj.Result[Method], param: Int): Unit = {
        methodO match {
            case Success(m) ⇒ {
                try {
                    val fps = propertyStore.context[FormalParameters]
                    val fp = fps(m)(param)
                    if (fp != e) {
                        val escapeState = propertyStore(fp, EscapeProperty.key)
                        escapeState match {
                            case EP(_, NoEscape | ArgEscape) ⇒ calcMostRestrictive(ArgEscape)
                            case EP(_, state: GlobalEscape)  ⇒ calcMostRestrictive(state)
                            case EP(_, _: MethodEscape)      ⇒ calcMostRestrictive(MaybeArgEscape) //TODO
                            case EP(_, MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape) ⇒
                                dependees += escapeState
                                calcMostRestrictive(ArgEscape)
                            case EP(_, _) ⇒
                                throw new RuntimeException("not yet implemented")
                            // result not yet finished
                            case epk ⇒
                                dependees += epk
                                calcMostRestrictive(ArgEscape)
                        }
                    }
                } catch {
                    case _: ArrayIndexOutOfBoundsException ⇒
                        //TODO params to array
                        calcMostRestrictive(MaybeArgEscape)
                }
            }
            case _ ⇒ calcMostRestrictive(MaybeArgEscape)
        }
    }

}

class InterproceduralEntityEscapeAnalysis(
        val e:             Entity,
        val defSite:       ValueOrigin,
        val uses:          IntArraySet,
        val code:          Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
        val params:        Parameters[TACMethodParameter],
        val m:             Method,
        val propertyStore: PropertyStore,
        val project:       SomeProject
) extends InterproceduralEntityEscapeAnalysis1 with SimpleFieldAwareEntityEscapeAnalysis

