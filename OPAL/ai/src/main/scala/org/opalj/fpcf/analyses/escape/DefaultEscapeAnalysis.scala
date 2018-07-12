/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.Invokedynamic
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutField
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Throw
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

/**
 * A safe default implementation of the [[AbstractEscapeAnalysis]] that uses fallback values.
 *
 * @author Florian Kuebler
 */
trait DefaultEscapeAnalysis extends AbstractEscapeAnalysis {

    protected[this] override def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(putField.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(arrayStore.value))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(aThrow.exception))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(call.receiver) || context.anyParameterUsesDefSite(call.params))
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
        if (context.usesDefSite(call.receiver))
            state.meetMostRestrictive(AtMost(NoEscape))
    }

    protected[this] override def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name != "<init>")
        if (context.usesDefSite(call.receiver) || context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(call.receiver) || context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(call.receiver) || context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleInvokeDynamic(
        call: Invokedynamic[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.anyParameterUsesDefSite(call.params))
            state.meetMostRestrictive(AtMost(EscapeInCallee))
    }

    protected[this] override def handleOtherKindsOfExpressions(
        expr: Expr[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {}

    protected[this] override def continuation(
        someEPS: SomeEPS
    )(implicit context: AnalysisContext, state: AnalysisState): PropertyComputationResult = {
        throw new UnknownError(s"unhandled escape property (${someEPS.ub} for ${someEPS.e}")
    }
}
