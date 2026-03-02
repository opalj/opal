/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.util.elidedAssert

/**
 * A safe default implementation of the [[AbstractEscapeAnalysis]] that uses fallback values.
 *
 * @author Florian Kuebler
 */
trait DefaultEscapeAnalysis extends AbstractEscapeAnalysis {

    override protected def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(putField.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(arrayStore.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(aThrow.exception))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleExprStmt(
        exprStmt: ExprStmt[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(exprStmt.expr, hasAssignment = false)
    }

    override protected def handleAssignment(
        assignment: Assignment[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleExpression(assignment.expr, hasAssignment = true)
    }

    override protected def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        elidedAssert(call.name == "<init>")
        if (state.usesDefSite(call.receiver))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    override protected def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        elidedAssert(call.name != "<init>")
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleVirtualFunctionCall(
        call:          VirtualFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleNonVirtualFunctionCall(
        call:          NonVirtualFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(call.receiver) || state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleStaticFunctionCall(
        call:          StaticFunctionCall[V],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleInvokedynamicFunctionCall(
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

    override protected def handleInvokedynamicMethodCall(
        call: InvokedynamicMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        if (state.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    override protected def handleOtherKindsOfExpressions(
        expr: Expr[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {}
}
