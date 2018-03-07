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

import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.NonVirtualMethodCall

/**
 * Special handling for constructor calls, as the receiver of an constructor is always an
 * allocation site.
 * The constructor of Object does not escape the self reference by definition. For other
 * constructors, the inter-procedural chain will be processed until it reaches the Object
 * constructor or escapes. Is this the case, leastRestrictiveProperty will be set to the lower bound
 * of the current value and the calculated escape state.
 *
 * For non constructor calls, [[org.opalj.fpcf.properties.AtMost(EscapeInCallee)]] of `e will be `
 * returned whenever the receiver or a parameter is a use of defSite.
 */
trait ConstructorSensitiveEscapeAnalysis extends AbstractEscapeAnalysis {
    override type AnalysisContext <: AbstractEscapeAnalysisContext with ProjectContainer with PropertyStoreContainer with VirtualFormalParametersContainer with DeclaredMethodsContainer

    abstract protected[this] override def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name == "<init>")
        assert(context.usesDefSite(call.receiver))

        // the object constructor will not escape the this local
        if (call.declaringClass eq ObjectType.Object)
            return ;

        // resolve the constructor
        context.project.specialCall(
            call.declaringClass,
            call.isInterface,
            "<init>",
            call.descriptor
        ) match {
                case Success(callee) ⇒
                    // check if the this local escapes in the callee
                    val escapeState = context.propertyStore(
                        context.virtualFormalParameters(context.declaredMethods(callee))(0),
                        EscapeProperty.key
                    )
                    escapeState match {
                        case EP(_, NoEscape)                                    ⇒ //NOTHING TO DO
                        case EP(_, GlobalEscape)                                ⇒ state.meetMostRestrictive(GlobalEscape)
                        case EP(_, EscapeViaStaticField)                        ⇒ state.meetMostRestrictive(EscapeViaStaticField)
                        case EP(_, EscapeViaHeapObject)                         ⇒ state.meetMostRestrictive(EscapeViaHeapObject)
                        case EP(_, EscapeInCallee)                              ⇒ state.meetMostRestrictive(EscapeInCallee)
                        case EP(_, AtMost(EscapeInCallee))                      ⇒ state.meetMostRestrictive(AtMost(EscapeInCallee))
                        case EP(_, EscapeViaParameter)                          ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, EscapeViaAbnormalReturn)                     ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, EscapeViaParameterAndAbnormalReturn)         ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(NoEscape))                            ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaParameter))                  ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaAbnormalReturn))             ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ state.meetMostRestrictive(AtMost(NoEscape))
                        case ep @ EP(_, Conditional(NoEscape)) ⇒
                            state.dependees += ep
                        case ep @ EP(_, Conditional(EscapeInCallee)) ⇒
                            state.meetMostRestrictive(EscapeInCallee)
                            state.dependees += ep
                        case ep @ EP(_, Conditional(AtMost(EscapeInCallee))) ⇒
                            state.meetMostRestrictive(AtMost(EscapeInCallee))
                            state.dependees += ep
                        case ep @ EP(_, Conditional(_)) ⇒
                            state.meetMostRestrictive(AtMost(NoEscape))
                            state.dependees += ep
                        case EP(_, p) ⇒
                            throw new UnknownError(s"unexpected escape property ($p) for constructors")
                        // result not yet finished
                        case epk ⇒
                            state.dependees += epk
                    }
                case /* unknown method */ _ ⇒ state.meetMostRestrictive(AtMost(NoEscape))
            }
    }

    abstract override protected[this] def c(
        other: Entity, p: Property, u: UpdateType
    )(implicit context: AnalysisContext, state: AnalysisState): PropertyComputationResult = {
        other match {
            case VirtualFormalParameter(DefinedMethod(_, method), -1) if method.isConstructor ⇒ p match {

                case GlobalEscape         ⇒ Result(context.entity, GlobalEscape)

                case EscapeViaStaticField ⇒ Result(context.entity, EscapeViaStaticField)

                case EscapeViaHeapObject  ⇒ Result(context.entity, EscapeViaHeapObject)

                case NoEscape             ⇒ removeFromDependeesAndComputeResult(EP(other, p), NoEscape)

                case EscapeInCallee       ⇒ removeFromDependeesAndComputeResult(EP(other, p), EscapeInCallee)

                case EscapeViaParameter ⇒
                    // we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(NoEscape))

                case EscapeViaAbnormalReturn ⇒
                    // this could be the case if `other` is an exception and is thrown in its constructor
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(NoEscape))

                case EscapeViaParameterAndAbnormalReturn ⇒
                    // combines the two cases above
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(NoEscape))

                case AtMost(NoEscape) | AtMost(EscapeViaParameter) | AtMost(EscapeViaAbnormalReturn) |
                    AtMost(EscapeViaParameterAndAbnormalReturn) ⇒
                    //assert(u ne IntermediateUpdate)
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(NoEscape))

                case AtMost(EscapeInCallee) ⇒
                    //assert(u ne IntermediateUpdate)
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case p @ Conditional(NoEscape) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(EP(other, p), NoEscape)

                case p @ Conditional(EscapeInCallee) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ Conditional(AtMost(EscapeInCallee)) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(EP(other, p), AtMost(EscapeInCallee))

                case p @ Conditional(_) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(EP(other, p), AtMost(NoEscape))

                case PropertyIsLazilyComputed ⇒
                    IntermediateResult(
                        context.entity,
                        Conditional(state.mostRestrictiveProperty),
                        state.dependees,
                        c
                    )

                case _ ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for constructors")
            }
            case _ ⇒ super.c(other, p, u)
        }
    }
}
