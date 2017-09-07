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
import org.opalj.br.ObjectAllocationSite
import org.opalj.br.ArrayAllocationSite
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.GlobalEscapeViaStaticFieldAssignment
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturn
import org.opalj.fpcf.properties.MaybeArgEscape
import org.opalj.fpcf.properties.MethodEscapeViaReturnAssignment
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

import scala.annotation.switch

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
    var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
    var leastRestrictiveProperty: EscapeProperty = NoEscape

    def doDetermineEscape(): PropertyComputationResult = {
        for (use ← uses)
            checkStmtForEscape(code(use))

        // Every entity that is not identified as escaping is not escaping
        if (dependees.isEmpty || leastRestrictiveProperty.isBottom)
            return Result(e, leastRestrictiveProperty)

        IntermediateResult(e, MaybeNoEscape, dependees, c)
    }

    /**
     * Sets leastRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline def calcLeastRestrictive(prop: EscapeProperty): Unit = {
        leastRestrictiveProperty = leastRestrictiveProperty meet prop
    }

    /**
     * Checks whether the given expression is a [[UVar]] and if so if it is a use of the defSite.
     */
    def usesDefSite(expr: Expr[V]): Boolean = {
        if (expr.isVar)
            if (expr.asVar.definedBy contains defSite) true
            else false
        else false
    }

    /**
     * If there exists a [[UVar]] in params that is a use of the defSite, return true.
     */
    def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        if (params.exists { case UVar(_, defSites) ⇒ defSites contains defSite })
            true
        else false
    }

    /**
     * Checks how the given statements effects the most possible restrictiveness of the entity e
     * with definition site defSite.
     * It might set the leastRestrictiveProperty.
     */
    def checkStmtForEscape(stmt: Stmt[V]): Unit = {
        (stmt.astID: @switch) match {
            case PutStatic.ASTID ⇒
                val value = stmt.asPutStatic.value
                if (usesDefSite(value)) calcLeastRestrictive(GlobalEscapeViaStaticFieldAssignment)
            // we are field insensitive, so we have to consider a field (and array) write as
            // GlobalEscape
            case PutField.ASTID ⇒
                visitPutField(stmt.asPutField)
            case ArrayStore.ASTID ⇒
                visitArrayStore(stmt.asArrayStore)
            case Throw.ASTID ⇒
                visitThrow(stmt.asThrow)
            case ReturnValue.ASTID ⇒
                visitReturnValue(stmt.asReturnValue)
            case StaticMethodCall.ASTID ⇒
                visitStaticMethodCall(stmt.asStaticMethodCall)
            case VirtualMethodCall.ASTID ⇒
                visitVirtualMethodCall(stmt.asVirtualMethodCall)
            case NonVirtualMethodCall.ASTID ⇒
                visitNonVirtualCall(stmt.asNonVirtualMethodCall)
            case ExprStmt.ASTID ⇒
                visitExprStmt(stmt.asExprStmt)
            case Assignment.ASTID ⇒
                visitAssignment(stmt.asAssignment)
            case _ ⇒
        }
    }

    /**
     * Sets leastRestrictiveProperty to the lower bound of p and the current worst and remove entity
     * other from dependees. If this entity does not depend on any more results it has
     * associated property of leastRestrictiveProperty, otherwise build a continuation.
     */
    def meetAndFilter(other: Entity, p: EscapeProperty): PropertyComputationResult = {
        calcLeastRestrictive(p)
        dependees = dependees filter (_.e ne other)
        if (dependees.isEmpty)
            Result(e, leastRestrictiveProperty)
        else
            IntermediateResult(e, MaybeNoEscape, dependees, c)
    }

    def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = other match {

        // this entity is written into a field of the other entity
        case ObjectAllocationSite(_, _) | ArrayAllocationSite(_, _) ⇒ p match {
            case MethodEscapeViaReturn                  ⇒ meetAndFilter(other, MethodEscapeViaReturnAssignment)
            case state: EscapeProperty if state.isFinal ⇒ meetAndFilter(other, state)
            case state: EscapeProperty ⇒ u match {
                case IntermediateUpdate ⇒
                    val newEP = EP(other, state)
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, state meet leastRestrictiveProperty, dependees, c)
                case _ ⇒ meetAndFilter(other, state)
            }
        }
        // this local of constructor constructor
        case FormalParameter(method, -1) if method.name == "<init>" ⇒ p match {
            case state: GlobalEscape ⇒ Result(e, state)
            case NoEscape            ⇒ meetAndFilter(other, NoEscape)
            case ArgEscape           ⇒ meetAndFilter(other, ArgEscape)
            case _: MethodEscape     ⇒ meetAndFilter(other, NoEscape)
            case MaybeNoEscape | MaybeArgEscape ⇒
                u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, MaybeNoEscape)
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, p.asInstanceOf[EscapeProperty] meet leastRestrictiveProperty, dependees, c)
                    case _ ⇒ meetAndFilter(other, p.asInstanceOf[EscapeProperty])
                }
            case MaybeMethodEscape ⇒ u match {
                case IntermediateUpdate ⇒
                    val newEP = EP(other, MaybeMethodEscape)
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, MaybeNoEscape meet leastRestrictiveProperty, dependees, c)
                case _ ⇒ meetAndFilter(other, MaybeNoEscape)
            }
        }

        // this entity is passed as parameter (or this local) to a method
        case FormalParameter(_, _) ⇒ p match {
            case state: GlobalEscape  ⇒ Result(e, state)
            case NoEscape | ArgEscape ⇒ meetAndFilter(other, ArgEscape)
            case MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape ⇒ u match {
                case IntermediateUpdate ⇒
                    val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(e, MaybeArgEscape meet leastRestrictiveProperty, dependees, c)
                case _ ⇒ meetAndFilter(other, MaybeArgEscape)
            }
        }

    }

    def visitPutField(putField: PutField[V]): Unit = if (usesDefSite(putField.value))
        calcLeastRestrictive(MaybeNoEscape)
    def visitArrayStore(arrayStore: ArrayStore[V]): Unit = if (usesDefSite(arrayStore.value))
        calcLeastRestrictive(MaybeNoEscape)
    def visitThrow(aThrow: Throw[V]): Unit = if (usesDefSite(aThrow.exception))
        calcLeastRestrictive(MaybeNoEscape)
    def visitReturnValue(returnValue: ReturnValue[V]): Unit = if (usesDefSite(returnValue.expr))
        calcLeastRestrictive(MethodEscapeViaReturn)
    def visitStaticMethodCall(call: StaticMethodCall[V]): Unit =
        if (anyParameterUsesDefSite(call.params)) calcLeastRestrictive(MaybeArgEscape)
    def visitVirtualMethodCall(call: VirtualMethodCall[V]): Unit =
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcLeastRestrictive(MaybeArgEscape)
    def visitNonVirtualCall(call: NonVirtualMethodCall[V]): Unit =
        if (usesDefSite(call.receiver) ||
            anyParameterUsesDefSite(call.params)) calcLeastRestrictive(MaybeArgEscape)
    def visitExprStmt(asExprStmt: ExprStmt[V]): Unit
    def visitAssignment(asAssignment: Assignment[V]): Unit
}
