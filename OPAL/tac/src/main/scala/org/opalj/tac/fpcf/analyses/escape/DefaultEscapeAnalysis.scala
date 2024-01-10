/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.NoEscape

/**
 * A safe default implementation of the [[AbstractEscapeAnalysis]] that uses fallback values.
 *
 * @author Florian Kuebler
 */
trait DefaultEscapeAnalysis extends AbstractEscapeAnalysis {

    override protected[this] def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(putField.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected[this] def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(arrayStore.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected[this] def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(aThrow.exception))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected[this] def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleExprStmt(
        exprStmt: ExprStmt[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(exprStmt.expr, hasAssignment = false)
    }

    override protected[this] def handleAssignment(
        assignment: Assignment[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(assignment.expr, hasAssignment = true)
    }

    override protected[this] def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name == "<init>")
        if (state.usesDefSite(call.receiver))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected[this] def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name != "<init>")
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleVirtualFunctionCall(
        call:          VirtualFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleNonVirtualFunctionCall(
        call:          NonVirtualFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleStaticFunctionCall(
        call:          StaticFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleInvokedynamicFunctionCall(
        call:          InvokedynamicFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleInvokedynamicMethodCall(
        call: InvokedynamicMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected[this] def handleOtherKindsOfExpressions(
        expr: Expr[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {}
}
