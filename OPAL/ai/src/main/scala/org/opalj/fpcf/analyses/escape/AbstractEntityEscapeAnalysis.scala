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
import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MaybeEscapeInCallee
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaReturn
import org.opalj.fpcf.properties.MaybeEscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaParameterAndReturn
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
    val code: Array[Stmt[V]]
    val cfg: CFG
    val aiResult: AIResult

    val e: Entity
    val uses: IntArraySet

    //
    // STATE MUTATED WHILE ANALYZING THE METHOD
    //

    protected[this] var defSite: IntArraySet
    protected[this] var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]
    protected[this] var dependeeToStmt = Map.empty[Entity, Option[Assignment[V]]]
    private[this] var mostRestrictiveProperty: EscapeProperty = NoEscape

    def doDetermineEscape(): PropertyComputationResult = {
        // for every use-site, check its escape state
        for (use ← uses) {
            checkStmtForEscape(code(use))
        }

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
        assert((mostRestrictiveProperty meet prop).lessOrEqualRestrictive(mostRestrictiveProperty))
        mostRestrictiveProperty = mostRestrictiveProperty meet prop
    }

    /**
     * Checks whether the expression is a use of the defSite.
     * This method is called on expressions within tac statements. We assume a flat hierarchy, so
     * the expression is expected to be a [[org.opalj.tac.Var]].
     */
    protected final def usesDefSite(expr: Expr[V]): Boolean = {
        assert(expr.isVar)
        expr.asVar.definedBy.exists(defSite.contains)
    }

    /**
     * If there exists a [[org.opalj.tac.UVar]] in the params of a method call that is a use of the
     * current entity's defsite return true.
     */
    protected final def anyParameterUsesDefSite(params: Seq[Expr[V]]): Boolean = {
        params.exists { case UVar(_, defSites) ⇒ defSites.exists(defSite.contains) }
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
                if (usesDefSite(value)) calcMostRestrictive(EscapeViaStaticField)
            case ReturnValue.ASTID ⇒
                if (usesDefSite(stmt.asReturnValue.expr))
                    calcMostRestrictive(EscapeViaReturn)
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
     * [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]].
     * This analysis does not check whether the exception is caught or not.
     *
     * @see [[org.opalj.fpcf.analyses.escape.ExceptionAwareEntitiyEscapeAnalysis]] which overrides
     *      this very simple behavior.
     */
    protected def handleThrow(aThrow: Throw[V]): Unit = {
        if (usesDefSite(aThrow.exception))
            calcMostRestrictive(MaybeNoEscape)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected def handleStaticMethodCall(call: StaticMethodCall[V]): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]]. An exception for this are the receiver objects of a
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
            calcMostRestrictive(MaybeEscapeInCallee)
        else if (usesDefSite(call.receiver))
            calcMostRestrictive(MaybeNoEscape)

    /**
     * [[org.opalj.tac.ExprStmt]] can contain function calls, so they have to handle them.
     */
    protected def handleExprStmt(exprStmt: ExprStmt[V]): Unit = {
        handleExpression(exprStmt.expr, None)
    }

    /**
     * [[org.opalj.tac.Assignment]]s can contain function calls, so they have to handle them.
     */
    protected def handleAssignment(assignment: Assignment[V]): Unit = {
        handleExpression(assignment.expr, Some(assignment))
    }

    /**
     * Currently, the only expressions that can lead to an escape are the different kinds of
     * function calls. So this method delegates to them. In the case of another expression
     * [[org.opalj.fpcf.analyses.escape.AbstractEntityEscapeAnalysis.handleOtherKindsOfExpressions]]
     * will be called.
     */
    protected def handleExpression(expr: Expr[V], assignment: Option[Assignment[V]]): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID ⇒
                handleNonVirtualFunctionCall(expr.asNonVirtualFunctionCall, assignment)
            case VirtualFunctionCall.ASTID ⇒
                handleVirtualFunctionCall(expr.asVirtualFunctionCall, assignment)
            case StaticFunctionCall.ASTID ⇒
                handleStaticFunctionCall(expr.asStaticFunctionCall, assignment)
            case Invokedynamic.ASTID ⇒
                handleInvokeDynamic(expr.asInvokedynamic, assignment)

            case _ ⇒ handleOtherKindsOfExpressions(expr)
        }
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        if (usesDefSite(call.receiver) || anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticFunctionCall(
        call: StaticFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleInvokeDynamic(
        call: Invokedynamic[V], assignment: Option[Assignment[V]]
    ): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
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
            /*
            case AllocationSite(_, _, _) ⇒ p match {
                case GlobalEscape         ⇒ Result(e, GlobalEscape)
                case EscapeViaStaticField ⇒ Result(e, EscapeViaHeapObject)
                case EscapeViaHeapObject  ⇒ Result(e, EscapeViaHeapObject)
                case NoEscape             ⇒ removeFromDependeesAndComputeResult(other, NoEscape)
                case _                    ⇒ removeFromDependeesAndComputeResult(other, MaybeNoEscape)
            }*/

            // special handling for the this local of the constructor
            case FormalParameter(method, -1) if method.isConstructor ⇒ p match {

                case GlobalEscape         ⇒ Result(e, GlobalEscape)

                case EscapeViaStaticField ⇒ Result(e, EscapeViaStaticField)

                case EscapeViaHeapObject  ⇒ Result(e, EscapeViaHeapObject)

                case NoEscape             ⇒ removeFromDependeesAndComputeResult(other, NoEscape)

                case EscapeInCallee       ⇒ removeFromDependeesAndComputeResult(other, EscapeInCallee)

                case EscapeViaParameter ⇒
                    // we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(other, MaybeNoEscape)

                case EscapeViaAbnormalReturn ⇒
                    // this could be the case if `other` is an exception and is thrown in its constructor
                    removeFromDependeesAndComputeResult(other, MaybeNoEscape)

                case EscapeViaParameterAndAbnormalReturn ⇒
                    // combines the two cases above
                    removeFromDependeesAndComputeResult(other, MaybeNoEscape)

                case EscapeViaReturn | EscapeViaNormalAndAbnormalReturn |
                    EscapeViaParameterAndReturn | EscapeViaParameterAndNormalAndAbnormalReturn |
                    MaybeEscapeViaReturn | MaybeEscapeViaNormalAndAbnormalReturn |
                    MaybeEscapeViaParameterAndReturn |
                    MaybeEscapeViaParameterAndNormalAndAbnormalReturn ⇒
                    // the constructor has no return statement, so this should not occur
                    throw new RuntimeException("Constructor has no return statement")

                case MaybeNoEscape | MaybeEscapeViaParameter | MaybeEscapeViaAbnormalReturn |
                    MaybeEscapeViaParameterAndAbnormalReturn ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        IntermediateResult(e, MaybeNoEscape meet mostRestrictiveProperty, dependees, c)
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, MaybeNoEscape)
                }

                case MaybeEscapeInCallee ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        calcMostRestrictive(EscapeInCallee)
                        IntermediateResult(e, MaybeEscapeInCallee meet mostRestrictiveProperty, dependees, c)
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)
                }

                case p ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for constructors")
            }

            // this entity is passed as parameter (or this local) to a method
            case FormalParameter(_, _) ⇒ p match {

                case GlobalEscape         ⇒ Result(e, GlobalEscape)

                case EscapeViaStaticField ⇒ Result(e, EscapeViaStaticField)

                case EscapeViaHeapObject  ⇒ Result(e, EscapeViaHeapObject)

                case NoEscape | EscapeInCallee ⇒
                    removeFromDependeesAndComputeResult(other, EscapeInCallee)

                case EscapeViaParameter ⇒
                    // IMPROVE we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)

                case EscapeViaAbnormalReturn ⇒
                    // IMPROVE we do not further track the exception thrown in the callee
                    removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)

                case EscapeViaReturn ⇒
                    // IMPROVE we do not further track the return value of the callee
                    val stmt = dependeeToStmt(other)
                    aiResult.domain match {
                        case _: org.opalj.ai.domain.l2.PerformInvocations ⇒
                            stmt match {
                                case Some(a) ⇒
                                    //IMPROVE further track the value
                                    removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)
                                case None ⇒
                                    removeFromDependeesAndComputeResult(other, EscapeInCallee)
                            }
                        case _: org.opalj.ai.domain.l1.ReferenceValues ⇒
                            stmt match {
                                case Some(a) ⇒
                                    removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)
                                case None ⇒
                                    removeFromDependeesAndComputeResult(other, EscapeInCallee)
                            }

                    }

                case EscapeViaParameterAndAbnormalReturn | EscapeViaNormalAndAbnormalReturn |
                    EscapeViaParameterAndAbnormalReturn | EscapeViaParameterAndReturn |
                    EscapeViaParameterAndNormalAndAbnormalReturn ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)

                case MaybeNoEscape | MaybeEscapeInCallee | MaybeEscapeViaParameter |
                    MaybeEscapeViaAbnormalReturn | MaybeEscapeViaReturn |
                    MaybeEscapeViaParameterAndAbnormalReturn | MaybeEscapeViaParameterAndReturn |
                    MaybeEscapeViaNormalAndAbnormalReturn |
                    MaybeEscapeViaParameterAndNormalAndAbnormalReturn ⇒ u match {
                    case IntermediateUpdate ⇒
                        val newEP = EP(other, p.asInstanceOf[EscapeProperty])
                        dependees = dependees.filter(_.e ne other) + newEP
                        calcMostRestrictive(EscapeInCallee)
                        IntermediateResult(e, MaybeEscapeInCallee meet mostRestrictiveProperty, dependees, c)
                    case _ ⇒
                        removeFromDependeesAndComputeResult(other, MaybeEscapeInCallee)
                }

                case p ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for constructors")
            }

        }
    }
}
