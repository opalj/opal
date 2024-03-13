/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0FunctionCallInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0VirtualFunctionCallInterpreter
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[VirtualFunctionCall]]s similar to the [[L0VirtualFunctionCallInterpreter]] but handles arbitrary calls
 * with a call graph.
 *
 * @author Maximilian Rüsch
 */
class L1VirtualFunctionCallInterpreter(
    override implicit val ps:     PropertyStore,
    implicit val contextProvider: ContextProvider
) extends L0VirtualFunctionCallInterpreter(ps)
    with StringInterpreter
    with L0FunctionCallInterpreter {

    override type T = VirtualFunctionCall[V]

    private case class CalleeDepender(
        pc:                 Int,
        methodContext:      Context,
        var calleeDependee: EOptionP[DefinedMethod, Callees]
    )

    override protected def interpretArbitraryCall(instr: T, pc: Int)(
        implicit state: ComputationState
    ): ProperPropertyComputationResult = {
        val depender = CalleeDepender(pc, contextProvider.newContext(state.dm), ps(state.dm, Callees.key))

        depender.calleeDependee match {
            case FinalP(c: Callees) =>
                val methods = getMethodsFromCallees(depender.pc, depender.methodContext, c)
                if (methods.isEmpty) {
                    computeFinalResult(pc, StringConstancyInformation.lb)
                } else {
                    val tacDependees = methods.map(m => (m, ps(m, TACAI.key))).toMap
                    val callState = FunctionCallState(pc, tacDependees.keys.toSeq, tacDependees)
                    callState.setParamDependees(evaluateParameters(getParametersForPC(pc)))

                    interpretArbitraryCallToMethods(state, callState)
                }

            case _ =>
                InterimResult.forUB(
                    InterpretationHandler.getEntityFromDefSitePC(pc),
                    StringConstancyProperty.ub,
                    Set(depender.calleeDependee),
                    continuation(state, depender)
                )
        }
    }

    private def continuation(
        state:    ComputationState,
        depender: CalleeDepender
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(c: Callees) =>
                val methods = getMethodsFromCallees(depender.pc, depender.methodContext, c)
                if (methods.isEmpty) {
                    computeFinalResult(depender.pc, StringConstancyInformation.lb)(state)
                } else {
                    val tacDependees = methods.map(m => (m, ps(m, TACAI.key))).toMap
                    val callState = FunctionCallState(depender.pc, tacDependees.keys.toSeq, tacDependees)
                    callState.setParamDependees(evaluateParameters(getParametersForPC(depender.pc)(state))(state))

                    interpretArbitraryCallToMethods(state, callState)
                }

            case UBP(_: Callees) =>
                depender.calleeDependee = eps.asInstanceOf[EOptionP[DefinedMethod, Callees]]
                InterimResult.forUB(
                    InterpretationHandler.getEntityFromDefSitePC(depender.pc)(state),
                    StringConstancyProperty.ub,
                    Set(depender.calleeDependee),
                    continuation(state, depender)
                )

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }

    private def getMethodsFromCallees(pc: Int, context: Context, callees: Callees): Seq[Method] = {
        val methods = ListBuffer[Method]()
        // IMPROVE only process newest callees
        callees.callees(context, pc).map(_.method).foreach {
            case definedMethod: DefinedMethod => methods.append(definedMethod.definedMethod)
            case _                            => // IMPROVE add some uncertainty element if methods with unknown body exist
        }
        methods.sortBy(_.classFile.fqn).toList
    }
}
