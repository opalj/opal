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
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.EmptyIntArraySet
import org.opalj.fpcf.properties.MethodEscapeViaParameterAssignment
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturnAssignment
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturn
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.GlobalEscapeViaHeapObjectAssignment
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MaybeMethodEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.MethodEscape
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.ArrayStore
import org.opalj.tac.Expr
import org.opalj.tac.PutField
import org.opalj.tac.ExprStmt
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DUVar
import org.opalj.tac.GetField
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Const
import org.opalj.tac.GetStatic
import org.opalj.tac.New
import org.opalj.tac.FunctionCall
import org.opalj.tac.NewArray
import org.opalj.tac.UVar
import org.opalj.tac.Invokedynamic
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall

import scala.annotation.switch

trait FieldSensitiveEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {

    override def visitPutField(putField: PutField[V]): Unit = {
        if (usesDefSite(putField.value))
            handleField(putField.objRef.asVar.definedBy)
    }

    override def visitArrayStore(arrayStore: ArrayStore[V]): Unit = {
        if (usesDefSite(arrayStore.value))
            handleField(arrayStore.arrayRef.asVar.definedBy)
    }

    /**
     * TODO
     * @param defSites
     */
    def handleField(defSites: IntArraySet): Unit = {
        var worklist = defSites
        var seen: IntArraySet = EmptyIntArraySet
        while (worklist.nonEmpty) {
            val defSite1 = worklist.head
            worklist = worklist - defSite1
            seen = seen + defSite1

            if (defSite1 != defSite) //TODO think about it
                if (defSite1 >= 0) {
                    code(defSite1) match {
                        case Assignment(pc, _, New(_, _) | NewArray(_, _, _)) ⇒
                            val allocationSites = propertyStore.context[AllocationSites]
                            val allocationSite = allocationSites(m)(pc)
                            val escapeState = propertyStore(allocationSite, EscapeProperty.key)
                            escapeState match {
                                case EP(_, MethodEscapeViaReturn) ⇒ calcLeastRestrictive(MethodEscapeViaReturnAssignment)
                                case EP(_, p) if p.isFinal        ⇒ calcLeastRestrictive(p)
                                case _                            ⇒ dependees += escapeState
                            }
                        case Assignment(_, _, GetStatic(_, _, _, _)) ⇒ calcLeastRestrictive(GlobalEscapeViaHeapObjectAssignment)
                        case Assignment(_, _, GetField(_, _, _, _, objRef)) ⇒
                            objRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x)) worklist = worklist + x

                            }
                        case Assignment(_, _, ArrayLoad(_, _, arrayRef)) ⇒
                            arrayRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x)) worklist = worklist + x
                            }
                        case Assignment(_, _, _: FunctionCall[_]) ⇒ calcLeastRestrictive(MaybeNoEscape)
                        case Assignment(_, _, _: Const)           ⇒ // Nothing to do
                        case _                                    ⇒ throw new RuntimeException("not yet implemented")
                    }
                } else {
                    // assigned to field of parameter
                    calcLeastRestrictive(MethodEscapeViaParameterAssignment)
                    val formalParameters = propertyStore.context[FormalParameters]
                    val formalParameter = formalParameters(m)(-(1 + defSite1)) //TODO
                    val escapeState = propertyStore(formalParameter, EscapeProperty.key)
                    escapeState match {
                        case EP(_, p: GlobalEscape) ⇒ calcLeastRestrictive(p)
                        case EP(_, p) if p.isFinal  ⇒
                        case _                      ⇒ dependees += escapeState
                    }
                }
        }
    }
}

trait ConstructorSensitiveEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {
    override def visitNonVirtualCall(call: NonVirtualMethodCall[V]): Unit = {
        handleNonVirtualCall(call.declaringClass, call.isInterface, call.name, call.descriptor, call.receiver, call.params)
    }

    override def visitExprStmt(exprStmt: ExprStmt[V]): Unit = examineCall(exprStmt.expr)

    override def visitAssignment(assignment: Assignment[V]): Unit = examineCall(assignment.expr)

    /**
     * For a given entity with defSite, check whether the expression is a function call.
     * For function call mark parameters and receiver objects that use the defSite as
     * [[MaybeArgEscape]]. For constructor calls with receiver being a use of e,
     * interprocedurally check the constructor.
     */
    def examineCall(expr: Expr[V]): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                val NonVirtualFunctionCall(_, dc, interface, name, descr, receiver, params) = expr
                handleNonVirtualCall(dc, interface, name, descr, receiver, params)
            case VirtualFunctionCall.ASTID ⇒
                val call = expr.asVirtualFunctionCall
                if (usesDefSite(call.receiver) ||
                    anyParameterUsesDefSite(call.params)) calcLeastRestrictive(MaybeArgEscape)
            case StaticFunctionCall.ASTID ⇒
                val params = expr.asStaticFunctionCall.params
                if (anyParameterUsesDefSite(params)) calcLeastRestrictive(MaybeArgEscape)
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
     * constructors, the interprocedural chain will be processed until it reaches the Object
     * constructor or escapes. Is this the case, leastRestrictiveProperty will be set to the lower bound
     * of the current value and the calculated escape state.
     *
     * For non constructor calls, [[MaybeArgEscape]] of e will be returned whenever the receiver
     * or a parameter is a use of defSite.
     */
    def handleNonVirtualCall(dc: ReferenceType, interface: Boolean,
                             name: String, descr: MethodDescriptor,
                             receiver: Expr[V], params: Seq[Expr[V]]): Unit = {
        // we only allow special (inter-procedural) handling for constructors
        if (name == "<init>") {
            // the object constructor will not escape the this local
            if (dc != ObjectType.Object) {
                // this is safe as we assume a flat tac hierarchy
                val UVar(_, defSites) = receiver
                if (defSites.contains(defSite))
                    // resolve the constructor
                    project.specialCall(dc.asObjectType, interface, "<init>", descr) match {
                        case Success(m) ⇒
                            val fp = propertyStore.context[FormalParameters]
                            // check if the this local escapes in the callee
                            val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                            escapeState match {
                                case EP(_, NoEscape)            ⇒ //NOTHING TO DO
                                case EP(_, state: GlobalEscape) ⇒ calcLeastRestrictive(state) //TODO return
                                case EP(_, _: MethodEscape) ⇒
                                    /*println(fp.toString+" "+m.classFile); */ calcLeastRestrictive(NoEscape) //TODO make sense?
                                case EP(_, state @ (MaybeArgEscape | MaybeMethodEscape)) ⇒
                                    calcLeastRestrictive(state)
                                case EP(_, MaybeNoEscape) ⇒ dependees += escapeState
                                case EP(_, _) ⇒
                                    // other types of escape should not occur on this analysis
                                    throw new RuntimeException("not yet implemented "+escapeState)
                                // result not yet finished
                                case epk ⇒ dependees += epk
                            }
                        case /* unknown method */ _ ⇒ calcLeastRestrictive(MaybeNoEscape)
                    }
                if (anyParameterUsesDefSite(params)) calcLeastRestrictive(MaybeArgEscape)
            }
        } else {
            if (usesDefSite(receiver) || anyParameterUsesDefSite(params))
                calcLeastRestrictive(MaybeArgEscape)
        }
    }
}

class SimpleEntityEscapeAnalysis(
    val e:             Entity,
    val defSite:       ValueOrigin,
    val uses:          IntArraySet,
    val code:          Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
    val params:        Parameters[TACMethodParameter],
    val m:             Method,
    val propertyStore: PropertyStore,
    val project:       SomeProject
) extends ConstructorSensitiveEntityEscapeAnalysis with FieldSensitiveEntityEscapeAnalysis

/*def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
        other match {
            case FormalParameter(_, _) ⇒ p match {
                case state: GlobalEscape ⇒ Result(e, state)
                case NoEscape            ⇒ meetAndFilter(other, NoEscape)
                case _: MethodEscape     ⇒ meetAndFilter(other, NoEscape)
                case MaybeArgEscape      ⇒ meetAndFilter(other, MaybeArgEscape)
                case MaybeMethodEscape   ⇒ meetAndFilter(other, MaybeNoEscape)
                case MaybeNoEscape /* could be an intermediate result */ ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, MaybeNoEscape)
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, MaybeNoEscape, dependees, c)
                    case _ ⇒ meetAndFilter(other, MaybeNoEscape)
                }
                case _ ⇒ throw new RuntimeException("not yet implemented")
            }
            case ObjectAllocationSite(_, _) | ArrayAllocationSite(_, _) ⇒ p match {
                case MethodEscapeViaReturn                  ⇒ meetAndFilter(other, MethodEscapeViaReturnAssignment)
                case state: EscapeProperty if state.isFinal ⇒ meetAndFilter(other, state)
                case state: EscapeProperty ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, state)
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, state, dependees, c)
                    case _ ⇒ meetAndFilter(other, state)
                }
            }
        }
    }*/

