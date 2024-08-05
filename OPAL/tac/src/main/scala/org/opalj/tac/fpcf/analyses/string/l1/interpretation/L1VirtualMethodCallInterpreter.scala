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
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment
import org.opalj.value.TheIntegerValue

/**
 * @author Maximilian Rüsch
 */
object L1VirtualMethodCallInterpreter extends StringInterpreter {

    override type T = VirtualMethodCall[V]

    /**
     * Currently, this function supports no method calls. However, it treats [[StringBuilder.setLength]] such that it
     * will return the lower bound for now.
     */
    override def interpret(call: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        val pReceiver = call.receiver.asVar.toPersistentForm(state.tac.stmts)

        call.name match {
            case "setLength" =>
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
                                        env.update(state.pc, pReceiver, StringTreeNode.lb)
                                }
                            }
                        )
                    case _ =>
                        computeFinalResult(StringFlowFunctionProperty.noFlow(state.pc, pReceiver))
                }

            case _ =>
                computeFinalResult(StringFlowFunctionProperty.identity)
        }
    }
}
