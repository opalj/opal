/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
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
    override implicit val ps:     PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L0VirtualFunctionCallInterpreter
    with StringInterpreter
    with L0FunctionCallInterpreter {

    override type E = VirtualFunctionCall[V]

    private case class CalleeDepender(
        target:             PV,
        methodContext:      Context,
        var calleeDependee: EOptionP[DefinedMethod, Callees]
    )

    override protected def interpretArbitraryCall(target: PV, call: E)(
        implicit state: InterpretationState
    ): ProperPropertyComputationResult = {
        val depender = CalleeDepender(target, contextProvider.newContext(state.dm), ps(state.dm, Callees.key))

        continuation(state, depender)(depender.calleeDependee.asInstanceOf[SomeEPS])
    }

    private def continuation(
        state:    InterpretationState,
        depender: CalleeDepender
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(c: Callees) =>
                implicit val _state: InterpretationState = state

                val methods = getMethodsFromCallees(depender.methodContext, c)
                if (methods.isEmpty) {
                    computeFinalLBFor(depender.target)
                } else {
                    val tacDependees = methods.map(m => (m, ps(m, TACAI.key))).toMap
                    val params = getParametersForPC(state.pc).map(_.asVar.toPersistentForm(state.tac.stmts))
                    val callState =
                        FunctionCallState(state, depender.target, tacDependees.keys.toSeq, params, tacDependees)

                    interpretArbitraryCallToFunctions(callState)
                }

            case UBP(_: Callees) =>
                depender.calleeDependee = eps.asInstanceOf[EOptionP[DefinedMethod, Callees]]
                InterimResult.forUB(
                    InterpretationHandler.getEntity(state),
                    StringFlowFunctionProperty.ub,
                    Set(depender.calleeDependee),
                    continuation(state, depender)
                )

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }

    private def getMethodsFromCallees(context: Context, callees: Callees)(implicit
        state: InterpretationState
    ): Seq[Method] = {
        val methods = ListBuffer[Method]()
        // IMPROVE only process newest callees
        callees.callees(context, state.pc).map(_.method).foreach {
            case definedMethod: DefinedMethod => methods.append(definedMethod.definedMethod)
            case _                            => // IMPROVE add some uncertainty element if methods with unknown body exist
        }
        methods.sortBy(_.classFile.fqn).toList
    }
}
