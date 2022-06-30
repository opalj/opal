/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import scala.annotation.switch

import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.EscapeViaStaticField
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.ai.ValueOrigin
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.TACAI

/**
 * An abstract escape analysis for a [[org.opalj.tac.common.DefinitionSiteLike]] or a
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

    /**
     * Retrieves the TAC and starts the analysis, if there is already a (intermediate) version of
     * it. Otherwise, continue when a TAC is available.
     */
    protected[this] def doDetermineEscape(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        retrieveTAC(context.targetMethod)
        if (state.tacai.isDefined) {
            analyzeTAC()
        } else {
            InterimResult(context.entity, GlobalEscape, NoEscape, state.dependees, c)
        }
    }

    /**
     * Analyzes each TAC statement of the given method. This methods assumes that there is at least
     * an intermediate result for the TAC present.
     */
    protected[this] def analyzeTAC()(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        assert(state.tacai.isDefined)
        // for every use-site, check its escape state
        for (use <- state.uses) {
            checkStmtForEscape(state.tacai.get.stmts(use))
        }
        returnResult
    }

    /**
     * For the given method, retrieve the TAC from the property store.
     * It sets the tacai option in the analysis `state`.
     * If the TAC is non-final, a dependency to it will be added to the `state`.
     */
    private[this] def retrieveTAC(
        m: Method
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        val tacai = propertyStore(m, TACAI.key)

        if (tacai.isRefinable) {
            state.addDependency(tacai)
        }

        if (tacai.hasUBP && tacai.ub.tac.isDefined) {
            state.updateTACAI(tacai.ub.tac.get)
        }
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
            case PutStatic.ASTID =>
                val value = stmt.asPutStatic.value
                if (state.usesDefSite(value)) {
                    state.meetMostRestrictive(EscapeViaStaticField)
                }

            case ReturnValue.ASTID =>
                if (state.usesDefSite(stmt.asReturnValue.expr))
                    state.meetMostRestrictive(EscapeViaReturn)

            case PutField.ASTID =>
                handlePutField(stmt.asPutField)

            case ArrayStore.ASTID =>
                handleArrayStore(stmt.asArrayStore)

            case Throw.ASTID =>
                handleThrow(stmt.asThrow)

            case StaticMethodCall.ASTID =>
                handleStaticMethodCall(stmt.asStaticMethodCall)

            case VirtualMethodCall.ASTID =>
                handleVirtualMethodCall(stmt.asVirtualMethodCall)

            case NonVirtualMethodCall.ASTID =>
                handleNonVirtualMethodCall(stmt.asNonVirtualMethodCall)

            case InvokedynamicMethodCall.ASTID =>
                handleInvokedynamicMethodCall(stmt.asInvokedynamicMethodCall)

            case ExprStmt.ASTID =>
                handleExprStmt(stmt.asExprStmt)

            case Assignment.ASTID =>
                handleAssignment(stmt.asAssignment)

            case _ => /* The other statements are irrelevant. */
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
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit

    /**
     * Same as [[handlePutField]].
     */
    protected[this] def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Thrown exceptions that are not caught would lead to a
     * [[org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn]].
     * This analysis does not check whether the exception is caught or not.
     *
     * @see [[org.opalj.tac.fpcf.analyses.escape.ExceptionAwareEscapeAnalysis]] which overrides
     *      this very simple behavior.
     */
    protected[this] def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualMethodCall(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        // we only allow special (inter-procedural) handling for constructors
        if (call.name == "<init>") {
            if (state.usesDefSite(call.receiver)) {
                handleThisLocalOfConstructor(call)
            } else {
                // an object can't be a parameter and the receiver of a constructor call
                handleParameterOfConstructor(call)
            }

        } else {
            handleNonVirtualAndNonConstructorCall(call)
        }
    }

    protected[this] def handleInvokedynamicMethodCall(
        call: InvokedynamicMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit

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
     * [[org.opalj.tac.fpcf.analyses.escape.AbstractEscapeAnalysis.handleOtherKindsOfExpressions]]
     * will be called.
     */
    protected[this] def handleExpression(
        expr: Expr[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        (expr.astID: @switch) match {
            case NonVirtualFunctionCall.ASTID =>
                handleNonVirtualFunctionCall(expr.asNonVirtualFunctionCall, hasAssignment)
            case VirtualFunctionCall.ASTID =>
                handleVirtualFunctionCall(expr.asVirtualFunctionCall, hasAssignment)
            case StaticFunctionCall.ASTID =>
                handleStaticFunctionCall(expr.asStaticFunctionCall, hasAssignment)
            case InvokedynamicFunctionCall.ASTID =>
                handleInvokedynamicFunctionCall(expr.asInvokedynamicFunctionCall, hasAssignment)

            case _ => handleOtherKindsOfExpressions(expr)
        }
    }

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit

    /**
     * Passing an entity as argument to a call, will make the entity at most
     * [[org.opalj.br.fpcf.properties.EscapeInCallee]].
     *
     * $JustIntraProcedural
     */
    protected[this] def handleInvokedynamicFunctionCall(
        call:          InvokedynamicFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit

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
     * [[org.opalj.fpcf.InterimResult]].
     */
    protected[this] def returnResult(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        // if we do not depend on other entities, or are globally escaping, return the result
        // note: replace by global escape
        if (!state.hasDependees || state.mostRestrictiveProperty.isBottom) {
            Result(context.entity, state.mostRestrictiveProperty)

        } else {
            InterimResult(
                context.entity,
                GlobalEscape, state.mostRestrictiveProperty,
                state.dependees, c
            )
        }
    }

    /**
     * A continuation function, that handles the updates of property values for entity `other`.
     */
    protected[this] def c(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        someEPS match {
            case UBP(ub: TACAI) =>
                state.removeDependency(someEPS)
                if (someEPS.isRefinable) {
                    state.addDependency(someEPS)
                }
                if (ub.tac.isDefined) {
                    state.updateTACAI(ub.tac.get)
                    analyzeTAC()
                } else {
                    InterimResult(
                        context.entity,
                        GlobalEscape,
                        state.mostRestrictiveProperty,
                        state.dependees,
                        c
                    )
                }
            case _ =>
                throw new UnknownError(s"unhandled escape property (${someEPS.ub} for ${someEPS.e}")
        }
    }

    /**
     * Extracts information from the given entity and should call [[doDetermineEscape]] afterwards.
     * For some entities a result might be returned immediately.
     */
    def determineEscape(e: Entity): ProperPropertyComputationResult =
        e.asInstanceOf[(Context, Entity)]._2 match {
            case _: DefinitionSiteLike =>
                determineEscapeOfDS(e.asInstanceOf[(Context, DefinitionSiteLike)])
            case _: VirtualFormalParameter =>
                determineEscapeOfFP(e.asInstanceOf[(Context, VirtualFormalParameter)])
            case _ =>
                throw new IllegalArgumentException(s"$e is unsupported")
        }

    protected[this] def determineEscapeOfDS(
        dsl: (Context, DefinitionSiteLike)
    ): ProperPropertyComputationResult = {
        val ctx = createContext(dsl, dsl._2.pc, dsl._2.method)
        doDetermineEscape(ctx, createState)
    }

    protected[this] def determineEscapeOfFP(
        fp: (Context, VirtualFormalParameter)
    ): ProperPropertyComputationResult

    protected[this] def createState: AnalysisState

    protected[this] lazy val virtualFormalParameters: VirtualFormalParameters = {
        project.get(VirtualFormalParametersKey)
    }

    protected[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    protected[this] implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    protected[this] def createContext(
        entity:       (Context, Entity),
        defSitePC:    ValueOrigin,
        targetMethod: Method
    ): AnalysisContext
}
