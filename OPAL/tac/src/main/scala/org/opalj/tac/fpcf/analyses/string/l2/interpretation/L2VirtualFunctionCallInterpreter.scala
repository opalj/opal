/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l2
package interpretation

import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
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
import org.opalj.tac.fpcf.analyses.string.SoundnessMode
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1FunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1SystemPropertiesInterpreter
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1VirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes [[VirtualFunctionCall]]s similar to the [[L1VirtualFunctionCallInterpreter]] but handles arbitrary calls
 * with a call graph.
 *
 * @author Maximilian Rüsch
 */
class L2VirtualFunctionCallInterpreter(
    implicit val ps:                     PropertyStore,
    implicit val contextProvider:        ContextProvider,
    implicit val project:                SomeProject,
    override implicit val soundnessMode: SoundnessMode
) extends L1VirtualFunctionCallInterpreter
    with StringInterpreter
    with L1SystemPropertiesInterpreter
    with L2ArbitraryVirtualFunctionCallInterpreter {

    override type E = VirtualFunctionCall[V]

    override protected def interpretArbitraryCall(target: PV, call: E)(
        implicit state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (call.name == "getProperty" && call.declaringClass == ObjectType("java/util/Properties")) {
            interpretGetSystemPropertiesCall(target)
        } else {
            interpretArbitraryCallWithCallees(target)
        }
    }
}

private[string] trait L2ArbitraryVirtualFunctionCallInterpreter extends L1FunctionCallInterpreter {

    implicit val ps: PropertyStore
    implicit val contextProvider: ContextProvider
    implicit val soundnessMode: SoundnessMode

    override type CallState = CalleeDepender

    protected[this] case class CalleeDepender(
        override val target:     PV,
        override val parameters: Seq[PV],
        methodContext:           Context,
        var calleeDependee:      EOptionP[DefinedMethod, Callees],
        var seenDirectCallees:   Int = 0,
        var seenIndirectCallees: Int = 0
    ) extends FunctionCallState(target, parameters) {

        override def hasDependees: Boolean = calleeDependee.isRefinable || super.hasDependees

        override def dependees: Iterable[SomeEOptionP] = super.dependees ++ Seq(calleeDependee).filter(_.isRefinable)
    }

    protected def interpretArbitraryCallWithCallees(target: PV)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val params = getParametersForPC(state.pc).map(_.asVar.toPersistentForm(state.tac.stmts))
        // IMPROVE pass the actual method context through the entity - needs to be differentiated from "upward" entities
        val depender = CalleeDepender(target, params, contextProvider.newContext(state.dm), ps(state.dm, Callees.key))

        if (depender.calleeDependee.isEPK) {
            InterimResult.forUB(
                InterpretationHandler.getEntity(state),
                StringFlowFunctionProperty.ub(state.pc, target),
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
                val newCallees = c.directCallees(callState.methodContext, state.pc).drop(callState.seenDirectCallees) ++
                    c.indirectCallees(callState.methodContext, state.pc).drop(callState.seenIndirectCallees)

                // IMPROVE add some uncertainty element if methods with unknown body exist
                val newMethods = newCallees
                    .filter(_.method.hasSingleDefinedMethod)
                    .map(_.method.definedMethod)
                    .filterNot(callState.calleeMethods.contains)
                    .distinct.toList.sortBy(_.classFile.fqn)

                callState.calleeDependee = eps.asInstanceOf[EOptionP[DefinedMethod, Callees]]
                if (newMethods.isEmpty && callState.calleeMethods.isEmpty && eps.isFinal) {
                    failure(callState.target)(state, soundnessMode)
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
}
