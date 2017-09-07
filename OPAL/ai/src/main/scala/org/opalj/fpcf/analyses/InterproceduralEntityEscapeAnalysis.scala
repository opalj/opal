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

import org.opalj.ai.ValueOrigin
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ReferenceType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntArraySet
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Invokedynamic
import org.opalj.tac.Expr

import scala.annotation.switch
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.ArgEscape
import org.opalj.fpcf.properties.MaybeMethodEscape
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.UVar
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.DUVar
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter

trait InterproceduralEntityEscapeAnalysis1 extends ConstructorSensitiveEntityEscapeAnalysis {

    override def visitStaticMethodCall(call: StaticMethodCall[V]): Unit = {
        handleStaticCall(call.declaringClass, call.isInterface, call.name, call.descriptor, call.params)
    }

    override def visitVirtualMethodCall(call: VirtualMethodCall[V]): Unit = {
        handleVirtualCall(call.declaringClass, call.isInterface, call.name, call.descriptor, call.receiver, call.params)
    }
    /**
     * For a given entity with defSite, check whether the expression is a function call or a
     * CheckCast. For function call mark parameters and receiver objects that use the defSite as
     * GlobalEscape.
     */
    override def examineCall(expr: Expr[V]): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                val NonVirtualFunctionCall(_, dc, interface, name, descr, receiver, params) = expr
                handleNonVirtualCall(dc, interface, name, descr, receiver, params)
            case VirtualFunctionCall.ASTID ⇒
                val VirtualFunctionCall(_, dc, isI, name, descr, receiver, params) = expr
                handleVirtualCall(dc, isI, name, descr, receiver, params)
            case StaticFunctionCall.ASTID ⇒
                val StaticFunctionCall(_, dc, isI, name, descr, params) = expr
                handleStaticCall(dc, isI, name, descr, params)
            // see Java8LambdaExpressionsRewriting
            case Invokedynamic.ASTID ⇒
                val params = expr.asInvokedynamic.params
                if (anyParameterUsesDefSite(params)) calcLeastRestrictive(MaybeArgEscape)
            case _ ⇒
        }
    }

    /**
     * Special handling for constructor calls, as the receiver of an constructor is always an
     * allocation site.
     * The constructor of Object does not escape the self reference by definition. For other
     * constructor, the inter procedural chain will be processed until it reaches the Object
     * constructor or escapes.
     * For non constructor calls, [[GlobalEscape]] of e will be returned whenever the receiver
     * or a parameter is a use of defSite.
     */
    override def handleNonVirtualCall(dc: ReferenceType, interface: Boolean,
                                      name: String, descr: MethodDescriptor,
                                      receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
        val methodO = project.specialCall(dc.asObjectType, interface, name, descr)
        if (name == "<init>") {
            // the object constructor will not escape the this local
            if (dc ne ObjectType.Object) {
                // this is safe as we assume a flat tac hierarchy
                val UVar(_, defSites) = receiver
                if (defSites.contains(defSite))
                    // resolve the constructor
                    methodO match {
                        case Success(m) ⇒
                            val fp = propertyStore.context[FormalParameters]
                            // check if the this local escapes in the callee
                            val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                            escapeState match {
                                case EP(_, NoEscape)            ⇒
                                case EP(_, state: GlobalEscape) ⇒ calcLeastRestrictive(state)
                                case EP(_, ArgEscape)           ⇒ calcLeastRestrictive(ArgEscape)
                                case EP(_, MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape) ⇒
                                    dependees += escapeState
                                case EP(_, x) ⇒
                                    throw new RuntimeException("not yet implemented "+x)
                                // result not yet finished
                                case epk ⇒ dependees += epk
                            }
                        case /* unknown method */ _ ⇒
                            calcLeastRestrictive(MaybeNoEscape)
                    }
                checkParams(methodO, params)
            } else /* Object constructor does escape by def. */ NoEscape
        } else {
            checkParams(methodO, params)
            if (usesDefSite(receiver))
                handleCallO(methodO, 0)
        }
    }

    def handleStaticCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, params: Seq[Expr[V]]): Unit = {
        checkParams(project.staticCall(dc.asObjectType, isI, name, descr), params)
    }

    def handleVirtualCall(dc: ReferenceType, isI: Boolean, name: String, descr: MethodDescriptor, receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
        if (receiver.isVar) {
            val value = receiver.asVar.value.asDomainReferenceValue
            if (value.isPrecise) {
                value.valueType match {
                    case Some(valueType) ⇒
                        val methodO = project.instanceCall(m.classFile.thisType, valueType, name, descr)
                        checkParams(methodO, params)
                        if (usesDefSite(receiver)) handleCallO(methodO, 0)
                        return ;
                    case None ⇒ throw new NullPointerException()
                }
            }
        }
        val packageName = m.classFile.thisType.packageName
        val methods =
            if (isI) project.interfaceCall(dc.asObjectType, name, descr)
            else project.virtualCall(packageName, dc, name, descr)
        for (method ← methods) {
            checkParams(Success(method), params)
            if (usesDefSite(receiver)) handleCall(method, 0)
        }

    }

    def checkParams(methodO: org.opalj.Result[Method], params: Seq[Expr[V]]): Unit = {
        for (i ← params.indices) {
            if (usesDefSite(params(i)))
                handleCallO(methodO, i + 1)
        }
    }

    def handleCallO(methodO: org.opalj.Result[Method], param: Int): Unit = {
        methodO match {
            case Success(m) ⇒ handleCall(m, param)
            case _          ⇒ calcLeastRestrictive(MaybeArgEscape)
        }
    }

    def handleCall(m: Method, param: Int): Unit = {
        val fp = propertyStore.context[FormalParameters]

        try {
            val escapeState = propertyStore(fp(m)(param), EscapeProperty.key)
            escapeState match {
                case EP(_, NoEscape | ArgEscape) ⇒ calcLeastRestrictive(ArgEscape)
                case EP(_, state: GlobalEscape)  ⇒ calcLeastRestrictive(state)
                case EP(_, MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape) ⇒
                    dependees += escapeState
                    calcLeastRestrictive(ArgEscape)
                case EP(_, _) ⇒
                    throw new RuntimeException("not yet implemented")
                // result not yet finished
                case epk ⇒
                    dependees += epk
                    calcLeastRestrictive(ArgEscape)
            }
        } catch {
            case _: ArrayIndexOutOfBoundsException ⇒
                //TODO params to array
                calcLeastRestrictive(MaybeArgEscape)
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
) extends InterproceduralEntityEscapeAnalysis1 with FieldSensitiveEntityEscapeAnalysis

