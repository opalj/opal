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

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.Invokedynamic
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.Throw
import org.opalj.tac.UVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

import scala.annotation.switch

/**
 * An abstract escape analysis for a single entity.
 * These entity can be either a concrete [[org.opalj.br.AllocationSite]]s or
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s.
 * All other information such as the defSite, uses or the code correspond to this entity.
 *
 * It is assumed that the tac code has a flat hierarchy, i.e. it is real three address code.
 *
 * @define JustIntraProcedural ''This analysis only uses intra-procedural knowledge and does not
 *                             take the behavior of the called method into consideration.''
 * @author Florian Kuebler
 */
trait AbstractEntityEscapeAnalysis {

    // TODO   val domain : Domain with RecordDefUse;     type V = DUVar[domain.DomainValue]
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    //
    // STATE DEFINING THE ANALYSIS CONTEXT
    //
    val code: Array[Stmt[V]]
    val entity: Entity
    val uses: IntTrieSet
    val defSite: ValueOrigin

    //
    // STATE MUTATED WHILE ANALYZING THE METHOD
    //
    protected[this] var dependees = Set.empty[EOptionP[Entity, Property]]
    protected[this] var mostRestrictiveProperty: EscapeProperty = NoEscape

    def doDetermineEscape(): PropertyComputationResult = {
        // for every use-site, check its escape state
        for (use ← uses) {
            checkStmtForEscape(code(use))
        }
        returnResult
    }

    /**
     * Sets mostRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline protected[this] final def meetMostRestrictive(prop: EscapeProperty): Unit = {
        assert((mostRestrictiveProperty meet prop).lessOrEqualRestrictive(mostRestrictiveProperty))
        assert(!prop.isInstanceOf[Conditional])
        mostRestrictiveProperty = mostRestrictiveProperty meet prop
        assert(!mostRestrictiveProperty.isInstanceOf[Conditional])
    }

    /**
     * Checks whether the expression is a use of the defSite.
     * This method is called on expressions within tac statements. We assume a flat hierarchy, so
     * the expression is expected to be a [[org.opalj.tac.Var]].
     */
    protected[this] final def usesDefSite(expr: Expr[V]): Boolean = {
        assert(expr.isVar)
        expr.asVar.definedBy.contains(defSite)
    }

    /**
     * If there exists a [[org.opalj.tac.UVar]] in the params of a method call that is a use of the
     * current entity's def-site return true.
     */
    protected[this] final def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        assert(params.forall(_.isVar))
        params.exists { case UVar(_, defSites) ⇒ defSites.contains(defSite) }
    }

    /**
     * Checks how the given statements effects the most possible restrictiveness of the entity e
     * with definition site defSite.
     * It might set the mostRestrictiveProperty.
     */
    private[this] def checkStmtForEscape(stmt: Stmt[V]): Unit = {
        (stmt.astID: @switch) match {
            case PutStatic.ASTID ⇒
                val value = stmt.asPutStatic.value
                if (usesDefSite(value)) meetMostRestrictive(EscapeViaStaticField)
            case ReturnValue.ASTID ⇒
                if (usesDefSite(stmt.asReturnValue.expr))
                    meetMostRestrictive(EscapeViaReturn)
            case PutField.ASTID ⇒
                handlePutField(stmt.asPutField)
            case ArrayStore.ASTID ⇒
                handleArrayStore(stmt.asArrayStore)
            case Throw.ASTID ⇒
                handleThrow(stmt.asThrow)
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

            case _ ⇒ /* The other statements are irrelevant. */
        }
    }

    /**
     * Putting an entity into a field can lead to an escape if the base of that field escapes or
     * let its field escape.
     *
     * $JustIntraProcedural
     */
    protected[this] def handlePutField(putField: PutField[V]): Unit

    /**
     * Same as [[handlePutField]].
     */
    protected[this] def handleArrayStore(arrayStore: ArrayStore[V]): Unit

    /**
     * Thrown exceptions that are not caught would lead to a
     * [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]].
     * This analysis does not check whether the exception is caught or not.
     *
     * @see [[org.opalj.fpcf.analyses.escape.ExceptionAwareEntityEscapeAnalysis]] which overrides
     *      this very simple behavior.
     */
    protected[this] def handleThrow(aThrow: Throw[V]): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticMethodCall(call: StaticMethodCall[V]): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualMethodCall(call: NonVirtualMethodCall[V]): Unit = {
        // we only allow special (inter-procedural) handling for constructors
        if (call.name == "<init>") {
            if (usesDefSite(call.receiver)) {
                handleThisLocalOfConstructor(call)
            }
            //TODO should be also correct in an else branch
            handleParameterOfConstructor(call)

        } else {
            handleNonVirtualAndNonConstructorCall(call)
        }
    }

    protected[this] def handleThisLocalOfConstructor(call: NonVirtualMethodCall[V]): Unit

    protected[this] def handleParameterOfConstructor(call: NonVirtualMethodCall[V]): Unit

    protected[this] def handleNonVirtualAndNonConstructorCall(call: NonVirtualMethodCall[V]): Unit

    /**
     * [[org.opalj.tac.ExprStmt]] can contain function calls, so they have to handle them.
     */
    protected[this] def handleExprStmt(exprStmt: ExprStmt[V]): Unit = {
        handleExpression(exprStmt.expr, hasAssignment = false)
    }

    /**
     * [[org.opalj.tac.Assignment]]s can contain function calls, so they have to handle them.
     */
    protected[this] def handleAssignment(assignment: Assignment[V]): Unit = {
        handleExpression(assignment.expr, hasAssignment = true)
    }

    /**
     * Currently, the only expressions that can lead to an escape are the different kinds of
     * function calls. So this method delegates to them. In the case of another expression
     * [[org.opalj.fpcf.analyses.escape.AbstractEntityEscapeAnalysis.handleOtherKindsOfExpressions]]
     * will be called.
     */
    protected[this] def handleExpression(expr: Expr[V], hasAssignment: Boolean): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                handleNonVirtualFunctionCall(expr.asNonVirtualFunctionCall, hasAssignment)
            case VirtualFunctionCall.ASTID ⇒
                handleVirtualFunctionCall(expr.asVirtualFunctionCall, hasAssignment)
            case StaticFunctionCall.ASTID ⇒
                handleStaticFunctionCall(expr.asStaticFunctionCall, hasAssignment)
            case Invokedynamic.ASTID ⇒
                handleInvokeDynamic(expr.asInvokedynamic, hasAssignment)

            case _ ⇒ handleOtherKindsOfExpressions(expr)
        }
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], hasAssignment: Boolean
    ): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    ): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    ): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleInvokeDynamic(
        call: Invokedynamic[V], hasAssignment: Boolean
    ): Unit

    /**
     * All basic analyses only care about function calls for [[org.opalj.tac.Assignment]] or
     * [[org.opalj.tac.ExprStmt]], but if a future analysis requires handling other expressions, it
     * can override this method.
     */
    protected[this] def handleOtherKindsOfExpressions(expr: Expr[V]): Unit

    /**
     * Sets mostRestrictiveProperty to the lower bound of p and the current most restrictive and
     * remove entity `other` from dependees. If this entity does not depend on any more results it
     * has associated property of mostRestrictiveProperty, otherwise build a continuation.
     */
    protected[this] def removeFromDependeesAndComputeResult(
        other: EP[Entity, Property], p: EscapeProperty
    ): PropertyComputationResult = {
        meetMostRestrictive(p)
        //TODO what if we depend on two properties for the same entity
        assert(dependees.count(epk ⇒ (epk.e eq other.e) && epk.pk == other.pk) <= 1)
        dependees = dependees.filter(epk ⇒ (epk.e ne other.e) || epk.pk != other.pk)
        returnResult
    }

    /**
     * This method is called, after the entity has been analyzed. If there is no dependee left or
     * the entity escapes globally, the result is returned directly.
     * Otherwise, the `maybe` version of the current escape state is returned as
     * [[IntermediateResult]].
     */
    protected[this] def returnResult: PropertyComputationResult = {
        // if we do not depend on other entities, or are globally escaping, return the result
        // note: replace by global escape
        if (dependees.isEmpty || mostRestrictiveProperty.isBottom) {
            // that is, mostRestrictiveProperty is an AtMost
            if (mostRestrictiveProperty.isRefinable) {
                RefinableResult(entity, mostRestrictiveProperty)
            } else {
                Result(entity, mostRestrictiveProperty)
            }
        } else {
            IntermediateResult(entity, Conditional(mostRestrictiveProperty), dependees, c)
        }
    }

    /**
     * In the list of dependees the result of `other` is updated with the new property `p`.
     * The current escape state is updated to the `non-maybe` version of `newProp` and
     * the intermediate result is returned.
     */
    protected[this] def performIntermediateUpdate(
        newEP: EOptionP[Entity, Property], intermediateProperty: EscapeProperty
    ): PropertyComputationResult = {
        //TODO what if we depend on two properties for the same entity
        assert(dependees.count(epk ⇒ (epk.e eq newEP.e) && epk.pk == newEP.pk) <= 1)
        dependees = dependees.filter(epk ⇒ (epk.e ne newEP.e) || epk.pk != newEP.pk) + newEP
        meetMostRestrictive(intermediateProperty)
        IntermediateResult(entity, Conditional(mostRestrictiveProperty), dependees, c)
    }

    /**
     * A continuation function, that handles the updates of property values for entity `other`.
     */
    protected[this] def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult
}
