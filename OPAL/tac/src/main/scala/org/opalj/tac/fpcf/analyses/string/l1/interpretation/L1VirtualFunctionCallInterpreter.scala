/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0FunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string.l0.interpretation.L0VirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes [[VirtualFunctionCall]]s similar to the [[L0VirtualFunctionCallInterpreter]] but handles arbitrary calls
 * with a call graph.
 *
 * @author Maximilian Rüsch
 */
class L1VirtualFunctionCallInterpreter(
    implicit val ps:              PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L0VirtualFunctionCallInterpreter
    with StringInterpreter
    with L1ArbitraryVirtualFunctionCallInterpreter {

    override type E = VirtualFunctionCall[V]

    override protected def interpretArbitraryCall(target: PV, call: E)(
        implicit state: InterpretationState
    ): ProperPropertyComputationResult = {
        interpretArbitraryCallWithCallees(target)
    }
}

private[string] trait L1ArbitraryVirtualFunctionCallInterpreter extends L0FunctionCallInterpreter {

    implicit val ps: PropertyStore
    implicit val contextProvider: ContextProvider

    override type CallState = CalleeDepender

    protected[this] case class CalleeDepender(
        override val target:     PV,
        override val parameters: Seq[PV],
        methodContext:           Context,
        var calleeDependee:      EOptionP[DefinedMethod, Callees]
    ) extends FunctionCallState(target, parameters) {

        override def hasDependees: Boolean = calleeDependee.isRefinable || super.hasDependees

        override def dependees: Iterable[SomeEOptionP] = super.dependees ++ Seq(calleeDependee).filter(_.isRefinable)
    }

    protected def interpretArbitraryCallWithCallees(target: PV)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val params = getParametersForPC(state.pc).map(_.asVar.toPersistentForm(state.tac.stmts))
        val depender = CalleeDepender(target, params, contextProvider.newContext(state.dm), ps(state.dm, Callees.key))

        if (depender.calleeDependee.isEPK) {
            InterimResult.forUB(
                InterpretationHandler.getEntity(state),
                StringFlowFunctionProperty.ub,
                Set(depender.calleeDependee),
                continuation(state, depender)
            )
        } else {
            continuation(state, depender)(depender.calleeDependee.asInstanceOf[SomeEPS])
        }
    }

    override protected[this] def continuation(
        state:     InterpretationState,
        callState: CallState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(c: Callees) =>
                callState.calleeDependee = eps.asInstanceOf[EOptionP[DefinedMethod, Callees]]
                val newMethods = getNewMethodsFromCallees(callState.methodContext, c)(state, callState)
                if (newMethods.isEmpty && eps.isFinal) {
                    // Improve add previous results back
                    computeFinalLBFor(callState.target)(state)
                } else {
                    for {
                        method <- newMethods
                    } {
                        callState.addCalledMethod(method, ps(method, TACAI.key))
                    }

                    interpretArbitraryCallToFunctions(state, callState)
                }

            case _ => super.continuation(state, callState)(eps)
        }
    }

    private def getNewMethodsFromCallees(context: Context, callees: Callees)(implicit
        state:     InterpretationState,
        callState: CallState
    ): Seq[Method] = {
        // IMPROVE only process newest callees
        callees.callees(context, state.pc)
            // IMPROVE add some uncertainty element if methods with unknown body exist
            .filter(_.method.hasSingleDefinedMethod)
            .map(_.method.definedMethod)
            .filterNot(callState.calleeMethods.contains)
            .distinct.toList.sortBy(_.classFile.fqn)
    }
}
