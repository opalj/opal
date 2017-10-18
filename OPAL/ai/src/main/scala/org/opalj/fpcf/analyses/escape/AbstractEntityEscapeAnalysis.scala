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

import scala.annotation.switch

import org.opalj.ai.ValueOrigin
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.cfg.CFG
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
import org.opalj.fpcf.properties.MethodEscapeViaParameterAssignment
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

/**
 * An abstract escape analysis for a single entity.
 * These entity can be either a concrete [[org.opalj.br.AllocationSite]]s or
 * [[org.opalj.br.analyses.FormalParameter]]s.
 * All other information such as the defSite, uses or the code correspond to this entity.
 *
 * It is assumed that the tac code has a flat hierarchy, i.e. it is real three address code.
 *
 * @define JustIntraProcedural ''This analysis only uses intra-procedural knowledge and does not
 *                             take the behavior of the called method into consideration.''
 *
 * @author Florian Kuebler
 */
trait AbstractEntityEscapeAnalysis {

    // TODO   val domain : Domain with RecordDefUse;     type V = DUVar[domain.DomainValue]
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    //
    // STATE DEFINING THE ANALYSIS CONTEXT
    //
    val project: SomeProject
    val propertyStore: PropertyStore
    val m: Method
    val params: Parameters[TACMethodParameter]
    val code: Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]]
    val cfg: CFG

    val e: Entity
    val defSite: ValueOrigin
    val uses: IntArraySet

    //
    // STATE MUTATED WHILE ANALYZING THE METHOD
    //
    protected[this] var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
    private[this] var mostRestrictiveProperty: EscapeProperty = NoEscape

    def doDetermineEscape(): PropertyComputationResult = {
        // for every use-site, check its escape state
        for (use ← uses) checkStmtForEscape(code(use))

        // if we do not depend on other entities, or are globally escaping, return the result
        if (dependees.isEmpty || mostRestrictiveProperty.isBottom)
            ImmediateResult(e, mostRestrictiveProperty)
        else {
            // The refineable escape properties are the `maybe` ones.
            // So a meet between the currently most restrictive property and MaybeNoEscape
            // will lead to the maybe version of it
            IntermediateResult(e, MaybeNoEscape meet mostRestrictiveProperty, dependees, c)
        }
    }

    /**
     * Sets mostRestrictiveProperty to the greatest lower bound of its current value and the
     * given one.
     */
    @inline protected final def calcMostRestrictive(prop: EscapeProperty): Unit = {
        assert((prop meet mostRestrictiveProperty).lessOrEqualRestrictive(mostRestrictiveProperty))
        mostRestrictiveProperty = mostRestrictiveProperty meet prop
    }

    /**
     * Checks whether the expression is a use of the defSite.
     * This method is called on expressions within tac statements. We assume a flat hierarchy, so
     * the expression is expected to be a [[org.opalj.tac.Var]].
     */
    protected final def usesDefSite(expr: Expr[V]): Boolean = {
        assert(expr.isVar)
        expr.asVar.definedBy contains defSite
    }

    /**
     * If there exists a [[org.opalj.tac.UVar]] in the params of a method call that is a use of the
     * current entity's defsite return true.
     */
    protected final def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        params.exists { case UVar(_, defSites) ⇒ defSites contains defSite }
    }

    /**
     * Checks how the given statements effects the most possible restrictiveness of the entity e
     * with definition site defSite.
     * It might set the mostRestrictiveProperty.
     */
    private def checkStmtForEscape(stmt: Stmt[V]): Unit = {
        (stmt.astID: @switch) match {
            case PutStatic.ASTID ⇒
                val value = stmt.asPutStatic.value
                if (usesDefSite(value)) calcMostRestrictive(GlobalEscapeViaStaticFieldAssignment)
            case ReturnValue.ASTID ⇒
                if (usesDefSite(stmt.asReturnValue.expr))
                    calcMostRestrictive(MethodEscapeViaReturn)

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
    protected def handlePutField(putField: PutField[V]): Unit = {
        if (usesDefSite(putField.value))
            calcMostRestrictive(MaybeNoEscape)
    }

    /**
     * Same as [[handlePutField]].
     */
    protected def handleArrayStore(arrayStore: ArrayStore[V]): Unit = {
        if (usesDefSite(arrayStore.value))
            calcMostRestrictive(MaybeNoEscape)
    }

    /**
     * Thrown exceptions that are not caught would lead to a
     * [[org.opalj.fpcf.properties.MethodEscapeViaReturn]].
     * This analysis does not check whether the exception is caught or not.
     *
     * @see [[org.opalj.fpcf.analyses.escape.ExceptionAwareEntitiyEscapeAnalysis]] which overrides
     *     this very simple behavior.
     */
    protected def handleThrow(aThrow: Throw[V]): Unit = {
        if (usesDefSite(aThrow.exception))
            calcMostRestrictive(MaybeNoEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected def handleStaticMethodCall(call: StaticMethodCall[V]): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]]. An exception for this are the receiver objects of a
     * constructor. Here [[org.opalj.fpcf.properties.NoEscape]] is still possible.
     *
     * $JustIntraProcedural
     */
    protected def handleNonVirtualMethodCall(call: NonVirtualMethodCall[V]): Unit =
        /*
         * In java bytecode we always have the pattern: X x = new X; x.init(...);
         * So if the receiver is a use of our def site, NoEscape is still possible.
         */
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
        else if (usesDefSite(call.receiver))
            calcMostRestrictive(MaybeNoEscape)

    /**
     * [[org.opalj.tac.ExprStmt]] can contain function calls, so they have to handle them.
     */
    protected def handleExprStmt(exprStmt: ExprStmt[V]): Unit = {
        handleExpression(exprStmt.expr)
    }

    /**
     * [[org.opalj.tac.Assignment]]s can contain function calls, so they have to handle them.
     */
    protected def handleAssignment(assignment: Assignment[V]): Unit = {
        handleExpression(assignment.expr)
    }

    /**
     * Currently, the only expressions that can lead to an escape are the different kinds of
     * function calls. So this method delegates to them. In the case of another expression
     * [[org.opalj.fpcf.analyses.escape.AbstractEntityEscapeAnalysis.handleOtherKindsOfExpressions]]
     * will be called.
     */
    protected def handleExpression(expr: Expr[V]): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                handleNonVirtualFunctionCall(expr.asNonVirtualFunctionCall)
            case VirtualFunctionCall.ASTID ⇒
                handleVirtualFunctionCall(expr.asVirtualFunctionCall)
            case StaticFunctionCall.ASTID ⇒
                handleStaticFunctionCall(expr.asStaticFunctionCall)
            case Invokedynamic.ASTID ⇒
                handleInvokeDynamic(expr.asInvokedynamic)

            case _ ⇒ handleOtherKindsOfExpressions(expr)
        }
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected def handleNonVirtualFunctionCall(call: NonVirtualFunctionCall[V]): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected def handleVirtualFunctionCall(call: VirtualFunctionCall[V]): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticFunctionCall(call: StaticFunctionCall[V]): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.ArgEscape]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleInvokeDynamic(call: Invokedynamic[V]): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeArgEscape)
    }

    /**
     * All basic analyses only care about function calls for [[org.opalj.tac.Assignment]] or
     * [[org.opalj.tac.ExprStmt]], but if a future analysis requires handling other expressions, it
     * can override this method.
     */
    protected[this] def handleOtherKindsOfExpressions(expr: Expr[V]): Unit = {}

    /**
     * Sets mostRestrictiveProperty to the lower bound of p and the current most restrictive and
     * remove entity `other` from dependees. If this entity does not depend on any more results it
     * has associated property of mostRestrictiveProperty, otherwise build a continuation.
     */
    protected[this] def removeFromDependeesAndComputeResult(
        other: Entity, p: EscapeProperty
    ): PropertyComputationResult = {
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
    protected[this] def c(other: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
        other match {

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

            case FormalParameter(method, -1) if method.isConstructor ⇒ p match {

                case state: GlobalEscape ⇒ Result(e, state)

                case NoEscape            ⇒ removeFromDependeesAndComputeResult(other, NoEscape)

                case ArgEscape           ⇒ removeFromDependeesAndComputeResult(other, ArgEscape)

                case MethodEscapeViaParameterAssignment ⇒
                    // we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(other, MaybeNoEscape)

                case MaybeNoEscape | MaybeMethodEscape ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, MaybeNoEscape meet mostRestrictiveProperty, dependees, c)
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, MaybeNoEscape)
                }

                case MaybeArgEscape ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, MaybeArgEscape meet mostRestrictiveProperty, dependees, c)
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, MaybeArgEscape)
                }

                case p ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for constructors")
            }

            // this entity is passed as parameter (or this local) to a method
            case FormalParameter(_, _) ⇒ p match {

                case state: GlobalEscape  ⇒ Result(e, state)

                case NoEscape | ArgEscape ⇒ removeFromDependeesAndComputeResult(other, ArgEscape)

                case _: MethodEscape      ⇒ removeFromDependeesAndComputeResult(other, MaybeArgEscape) //TODO What??

                case MaybeNoEscape | MaybeArgEscape | MaybeMethodEscape ⇒ u match {

                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, MaybeArgEscape meet mostRestrictiveProperty, dependees, c)

                    case _ ⇒ removeFromDependeesAndComputeResult(other, MaybeArgEscape)
                }
            }

        }
    }
}
