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

import org.opalj.ai.DefinitionSiteLike
import org.opalj.ai.ValueOrigin
import org.opalj.br.Method
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.DefaultTACAIKey
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
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACStmts
import org.opalj.tac.TACode
import org.opalj.tac.Throw
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

import scala.annotation.switch

// TODO @Florian Replace dead link to AllocationSite
/**
 * An abstract escape analysis for a concrete [[org.opalj.br.AllocationSite]] or a
 * [[org.opalj.br.analyses.VirtualFormalParameter]].
 * The entity and all other information required by the analyses such as the defSite, uses or the
 * code correspond to this entity are given as [[AbstractEscapeAnalysisContext]].
 *
 * It is assumed that the tac code has a flat hierarchy, i.e. it is real three address code.
 *
 * The control-flow is intended to be: Client calls determineEscape. This method extracts the
 * information for the given entity and calls doDetermineEscape.
 *
 * @define JustIntraProcedural ''This analysis only uses intra-procedural knowledge and does not
 *                             take the behavior of the called method(s) into consideration.''
 * @author Florian Kuebler
 */

trait AbstractEscapeAnalysis extends FPCFAnalysis {

    type AnalysisContext <: AbstractEscapeAnalysisContext
    type AnalysisState <: AbstractEscapeAnalysisState

    def doDetermineEscape(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): PropertyComputationResult = {
        if (context.targetMethod.name == "handlingReturnGlobalEscape")
            println()
        // for every use-site, check its escape state
        for (use ← context.uses) {
            checkStmtForEscape(context.code(use))
        }
        returnResult
    }

    /**
     * Checks how the given statements effects the most possible restrictiveness of the entity e
     * with definition site defSite.
     * It might set the mostRestrictiveProperty.
     */
    private[this] def checkStmtForEscape(
        stmt: Stmt[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        (stmt.astID: @switch) match {
            case PutStatic.ASTID ⇒
                val value = stmt.asPutStatic.value
                if (context.usesDefSite(value)) {
                    state.meetMostRestrictive(EscapeViaStaticField)
                }

            case ReturnValue.ASTID ⇒
                if (context.usesDefSite(stmt.asReturnValue.expr))
                    state.meetMostRestrictive(EscapeViaReturn)

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
    protected[this] def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Same as [[handlePutField]].
     */
    protected[this] def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Thrown exceptions that are not caught would lead to a
     * [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]].
     * This analysis does not check whether the exception is caught or not.
     *
     * @see [[org.opalj.fpcf.analyses.escape.ExceptionAwareEscapeAnalysis]] which overrides
     *      this very simple behavior.
     */
    protected[this] def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualMethodCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        // we only allow special (inter-procedural) handling for constructors
        if (call.name == "<init>") {
            if (context.usesDefSite(call.receiver)) {
                handleThisLocalOfConstructor(call)
            }
            //TODO should be also correct in an else branch
            handleParameterOfConstructor(call)

        } else {
            handleNonVirtualAndNonConstructorCall(call)
        }
    }

    protected[this] def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    protected[this] def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    protected[this] def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * [[org.opalj.tac.ExprStmt]] can contain function calls, so they have to handle them.
     */
    protected[this] def handleExprStmt(
        exprStmt: ExprStmt[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(exprStmt.expr, hasAssignment = false)
    }

    /**
     * [[org.opalj.tac.Assignment]]s can contain function calls, so they have to handle them.
     */
    protected[this] def handleAssignment(
        assignment: Assignment[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(assignment.expr, hasAssignment = true)
    }

    /**
     * Currently, the only expressions that can lead to an escape are the different kinds of
     * function calls. So this method delegates to them. In the case of another expression
     * [[org.opalj.fpcf.analyses.escape.AbstractEscapeAnalysis.handleOtherKindsOfExpressions]]
     * will be called.
     */
    protected[this] def handleExpression(
        expr: Expr[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
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
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleInvokeDynamic(
        call: Invokedynamic[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * All basic analyses only care about function calls for [[org.opalj.tac.Assignment]] or
     * [[org.opalj.tac.ExprStmt]], but if a future analysis requires handling other expressions, it
     * can override this method.
     */
    protected[this] def handleOtherKindsOfExpressions(
        expr: Expr[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * This method is called, after the entity has been analyzed. If there is no dependee left or
     * the entity escapes globally, the result is returned directly.
     * Otherwise, the `maybe` version of the current escape state is returned as
     * [[IntermediateResult]].
     */
    protected[this] def returnResult(
        implicit
        context: AnalysisContext, state: AnalysisState
    ): PropertyComputationResult = {
        // if we do not depend on other entities, or are globally escaping, return the result
        // note: replace by global escape
        if (state.dependees.isEmpty || state.mostRestrictiveProperty.isBottom) {
            // that is, mostRestrictiveProperty is an AtMost
            if (state.mostRestrictiveProperty.isInstanceOf[AtMost]) {
                //TODO IntermediateResult(context.entity, GlobalEscape, state.mostRestrictiveProperty, Seq.empty, continuation)
                Result(context.entity, state.mostRestrictiveProperty)
            } else {
                Result(context.entity, state.mostRestrictiveProperty)
            }
        } else {
            IntermediateResult(
                context.entity,
                GlobalEscape, state.mostRestrictiveProperty,
                state.dependees, continuation
            )
        }
    }

    /**
     * A continuation function, that handles the updates of property values for entity `other`.
     */
    protected[this] def continuation(
        someEPS: SomeEPS
    )(implicit context: AnalysisContext, state: AnalysisState): PropertyComputationResult

    /**
     * Extracts information from the given entity and should call [[doDetermineEscape]] afterwards.
     * For some entities a result might be returned immediately.
     */
    def determineEscape(e: Entity): PropertyComputationResult = e match {
        case dsl: DefinitionSiteLike ⇒
            determineEscapeOfDS(dsl)

        case vfp: VirtualFormalParameter ⇒
            determineEscapeOfFP(vfp)

        case _ ⇒ throw new RuntimeException(s"unsupported entity $e")
    }

    def determineEscapeOfDS(dsl: DefinitionSiteLike): PropertyComputationResult = {
        val tacai = tacaiProvider(dsl.method)
        val uses = dsl.usedBy.map(tacai.pcToIndex(_))
        val defSite = tacai.pcToIndex(dsl.pc)

        // if the definition site is dead (-1), the object does not escape
        if (defSite == -1)
            Result(dsl, NoEscape)
        else {
            val ctx = createContext(dsl, defSite, dsl.method, uses, tacai.stmts, tacai.cfg)
            doDetermineEscape(ctx, createState)
        }

    }

    def determineEscapeOfFP(fp: VirtualFormalParameter): PropertyComputationResult

    def createContext(
        entity:       Entity,
        defSite:      ValueOrigin,
        targetMethod: Method,
        uses:         IntTrieSet,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    ): AnalysisContext

    def createState: AnalysisState

    protected[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, V] = {
        project.get(DefaultTACAIKey)
    }
    protected[this] lazy val virtualFormalParameters: VirtualFormalParameters = {
        project.get(VirtualFormalParametersKey)
    }
    protected[this] val declaredMethods: DeclaredMethods = {
        project.get(DeclaredMethodsKey)
    }
}
