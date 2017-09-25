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
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.GlobalEscapeViaStaticFieldAssignment
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturn
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.ArgEscape
import org.opalj.fpcf.properties.MaybeMethodEscape
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MethodEscape
import org.opalj.tac.Stmt
import org.opalj.tac.DUVar
import org.opalj.tac.UVar
import org.opalj.tac.Expr
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutStatic
import org.opalj.tac.PutField
import org.opalj.tac.Assignment
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.Throw
import org.opalj.tac.ExprStmt
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.ArrayStore
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Invokedynamic
import org.opalj.tac.StaticFunctionCall

import scala.annotation.switch

/**
 * An abstract escape analysis for a single entity.
 * TODO welche entities....
 *
 * @author Florian Kuebler
 */
trait AbstractEntityEscapeAnalysis {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
    val e: Entity
    val defSite: ValueOrigin
    val uses: IntArraySet
    val code: Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]]
    val params: Parameters[TACMethodParameter]
    val m: Method
    val propertyStore: PropertyStore
    val project: SomeProject
    protected var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
    private var mostRestrictiveProperty: EscapeProperty = NoEscape

    def doDetermineEscape(): PropertyComputationResult = {
        // for every use-site, check its escape state
        for (use ← uses)
            checkStmtForEscape(code(use))

        // if we do not depend on other entities, or are already globally escaping, return the result
        if (dependees.isEmpty || mostRestrictiveProperty.isBottom)
            ImmediateResult(e, mostRestrictiveProperty)
        else
            //TODO comment on meet
            IntermediateResult(e, MaybeNoEscape meet mostRestrictiveProperty, dependees, c)
    }

    /**
     * Sets mostRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline def calcMostRestrictive(prop: EscapeProperty): Unit = {
        mostRestrictiveProperty = mostRestrictiveProperty meet prop
    }

    /**
     * Checks whether the given expression is a [[UVar]] and if so if it is a use of the defSite.
     */
    def usesDefSite(expr: Expr[V]): Boolean = {
        expr.isVar && (expr.asVar.definedBy contains defSite)
    }

    /**
     * If there exists a [[UVar]] in the params of a method call that is a use of the defSite,
     * return true.
     */
    def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        params.exists { case UVar(_, defSites) ⇒ defSites contains defSite }
    }

    /**
     * Checks how the given statements effects the most possible restrictiveness of the entity e
     * with definition site defSite.
     * It might set the mostRestrictiveProperty.
     */
    def checkStmtForEscape(stmt: Stmt[V]): Unit = {
        (stmt.astID: @switch) match {
            case PutStatic.ASTID ⇒
                val value = stmt.asPutStatic.value
                if (usesDefSite(value)) calcMostRestrictive(GlobalEscapeViaStaticFieldAssignment)
            case PutField.ASTID ⇒
                handlePutField(stmt.asPutField)
            case ArrayStore.ASTID ⇒
                handleArrayStore(stmt.asArrayStore)
            case Throw.ASTID ⇒
                handleThrow(stmt.asThrow)
            case ReturnValue.ASTID ⇒
                handelReturnValue(stmt.asReturnValue)
            case StaticMethodCall.ASTID ⇒
                handleStaticMethodCall(stmt.asStaticMethodCall)
            case VirtualMethodCall.ASTID ⇒
                handleVirtualMethodCall(stmt.asVirtualMethodCall)
            case NonVirtualMethodCall.ASTID ⇒
                handleNonVirtualMethodCall(stmt.asNonVirtualMethodCall)
            case ExprStmt.ASTID ⇒
                handleExprStmt(stmt.asExprStmt)
            case Assignment.ASTID ⇒
                handleAssignment(stmt.asAssignment)
            case _ ⇒
        }
    }

    /**
     * Sets mostRestrictiveProperty to the lower bound of p and the current best and remove entity
     * other from dependees. If this entity does not depend on any more results it has
     * associated property of mostRestrictiveProperty, otherwise build a continuation.
     */
    def removeFromDependeesAndComputeResult(other: Entity, p: EscapeProperty): PropertyComputationResult = {
        calcMostRestrictive(p)
        dependees = dependees filter (_.e ne other)
        if (dependees.isEmpty || mostRestrictiveProperty.isBottom)
            Result(e, mostRestrictiveProperty)
        else
            IntermediateResult(e, MaybeNoEscape meet mostRestrictiveProperty, dependees, c)
    }

    /**
     * A continuation function, that handles the updates of property values for entity `other`.
     */
    def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = other match {

        /* Until there is no simple may-alias analysis, this code is useless
        // this entity is written into a field of the other entity
         case AllocationSite(_, _, _) ⇒ p match {
            case MethodEscapeViaReturn                  ⇒ removeFromDependeesAndComputeResult(other, MethodEscapeViaReturnAssignment)
            case GlobalEscapeViaStaticFieldAssignment   ⇒ removeFromDependeesAndComputeResult(other, GlobalEscapeViaHeapObjectAssignment)
            case state: EscapeProperty if state.isFinal ⇒ removeFromDependeesAndComputeResult(other, state)
            case state: EscapeProperty ⇒ u match {
                case IntermediateUpdate ⇒
                    val newEP = EP(other, state)
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, state meet mostRestrictiveProperty, dependees, c)
                case _ ⇒ removeFromDependeesAndComputeResult(other, state)
            }
        }*/

        // this local of constructor constructor
        case FormalParameter(method, -1) if method.name == "<init>" ⇒ p match {
            case state: GlobalEscape ⇒ Result(e, state)
            case NoEscape            ⇒ removeFromDependeesAndComputeResult(other, NoEscape)
            case ArgEscape           ⇒ removeFromDependeesAndComputeResult(other, ArgEscape)
            case _: MethodEscape ⇒ removeFromDependeesAndComputeResult(other, MaybeNoEscape)
            case state @ (MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape) ⇒
                u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, state.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(
                            e,
                            state.asInstanceOf[EscapeProperty] meet mostRestrictiveProperty,
                            dependees,
                            c
                        )
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, state.asInstanceOf[EscapeProperty])
                }
        }

        // this entity is passed as parameter (or this local) to a method
        case FormalParameter(_, _) ⇒ p match {
            case state: GlobalEscape  ⇒ Result(e, state)
            case NoEscape | ArgEscape ⇒ removeFromDependeesAndComputeResult(other, ArgEscape)
            case _: MethodEscape      ⇒ removeFromDependeesAndComputeResult(other, MaybeArgEscape) //TODO
            case MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape ⇒ u match {
                case IntermediateUpdate ⇒
                    val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, MaybeArgEscape meet mostRestrictiveProperty, dependees, c)
                case _ ⇒ removeFromDependeesAndComputeResult(other, MaybeArgEscape)
            }
        }

    }

    def handlePutField(putField: PutField[V]): Unit = if (usesDefSite(putField.value))
        calcMostRestrictive(MaybeNoEscape)

    def handleArrayStore(arrayStore: ArrayStore[V]): Unit = if (usesDefSite(arrayStore.value))
        calcMostRestrictive(MaybeNoEscape)

    def handleThrow(aThrow: Throw[V]): Unit = if (usesDefSite(aThrow.exception))
        calcMostRestrictive(MaybeNoEscape)

    def handelReturnValue(returnValue: ReturnValue[V]): Unit = if (usesDefSite(returnValue.expr))
        calcMostRestrictive(MethodEscapeViaReturn)

    def handleStaticMethodCall(call: StaticMethodCall[V]): Unit =
        if (anyParameterUsesDefSite(call.params)) calcMostRestrictive(MaybeArgEscape)

    def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit =
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)

    def handleNonVirtualMethodCall(call: NonVirtualMethodCall[V]): Unit =
        //TODO comment
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
        else if (usesDefSite(call.receiver))
            calcMostRestrictive(MaybeNoEscape)

    def handleExprStmt(exprStmt: ExprStmt[V]): Unit = handleExpression(exprStmt.expr)

    def handleAssignment(assignment: Assignment[V]): Unit = handleExpression(assignment.expr)

    //TODO comment
    def handleExpression(expr: Expr[V]): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                handleNonVirtualFunctionCall(expr.asNonVirtualFunctionCall)
            case VirtualFunctionCall.ASTID ⇒
                handleVirtualFunctionCall(expr.asVirtualFunctionCall)
            case StaticFunctionCall.ASTID ⇒
                handleStaticFunctionCall(expr.asStaticFunctionCall)
            case Invokedynamic.ASTID ⇒
                handleInvokeDynamic(expr.asInvokedynamic)
            case _ ⇒ handleExpr(expr)
        }
    }

    def handleNonVirtualFunctionCall(call: NonVirtualFunctionCall[V]): Unit =
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)

    def handleVirtualFunctionCall(call: VirtualFunctionCall[V]): Unit =
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)

    def handleStaticFunctionCall(call: StaticFunctionCall[V]): Unit =
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)

    def handleInvokeDynamic(call: Invokedynamic[V]): Unit =
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)

    //TODO comment
    def handleExpr(expr: Expr[V]): Unit = {}
}
