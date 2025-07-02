/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.br.fpcf.properties.string.StringTreeEmptyConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment
import org.opalj.value.TheIntegerValue

/**
 * Processes [[VirtualMethodCall]]s without a call graph. Currently, only calls to `setLength` of string buffers and
 * string builders are interpreted. For other calls, ID is returned.
 *
 * @author Maximilian Rüsch
 */
class L1VirtualMethodCallInterpreter()(
    implicit val highSoundness: Boolean
) extends StringInterpreter {

    override type T = VirtualMethodCall[V]

    override def interpret(call: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        val pReceiver = call.receiver.asVar.toPersistentForm(state.tac.stmts)

        call.name match {
            case "setLength" if StringInterpreter.isStringBuilderBufferCall(call) =>
                call.params.head.asVar.value match {
                    case TheIntegerValue(intVal) if intVal == 0 =>
                        computeFinalResult(StringFlowFunctionProperty.constForVariableAt(
                            state.pc,
                            pReceiver,
                            StringTreeEmptyConst
                        ))

                    case TheIntegerValue(intVal) =>
                        computeFinalResult(
                            PDUWeb(state.pc, pReceiver),
                            (env: StringTreeEnvironment) => {
                                env(state.pc, pReceiver) match {
                                    case StringTreeConst(string) =>
                                        val sb = new StringBuilder(string)
                                        sb.setLength(intVal)
                                        env.update(state.pc, pReceiver, StringTreeConst(sb.toString()))
                                    case _ =>
                                        env.update(
                                            state.pc,
                                            pReceiver,
                                            failureTree
                                        )
                                }
                            }
                        )
                    case _ =>
                        computeFinalResult(StringFlowFunctionProperty.ub(state.pc, pReceiver))
                }

            case _ =>
                interpretArbitraryCall(call)
        }
    }

    protected def interpretArbitraryCall(call: T)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult =
        StringInterpreter.uninterpretedCall(call)
}
