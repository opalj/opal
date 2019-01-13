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

    protected[this] override def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(putField.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(arrayStore.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(aThrow.exception))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleExprStmt(
        exprStmt: ExprStmt[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(exprStmt.expr, hasAssignment = false)
    }

    protected[this] override def handleAssignment(
        assignment: Assignment[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(assignment.expr, hasAssignment = true)
    }

    protected[this] override def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name == "<init>")
        if (state.usesDefSite(call.receiver))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name != "<init>")
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleInvokedynamicFunctionCall(
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

    protected[this] override def handleInvokedynamicMethodCall(
        call: InvokedynamicMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleOtherKindsOfExpressions(
        expr: Expr[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {}
}
