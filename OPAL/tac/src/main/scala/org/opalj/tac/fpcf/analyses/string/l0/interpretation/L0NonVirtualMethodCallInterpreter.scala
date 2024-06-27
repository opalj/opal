/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.fpcf.properties.string.StringTreeEmptyConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
object L0NonVirtualMethodCallInterpreter extends StringInterpreter {

    override type T = NonVirtualMethodCall[V]

    override def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>" => interpretInit(instr)
            case _        => computeFinalResult(StringFlowFunctionProperty.identity)
        }
    }

    private def interpretInit(init: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        val pc = state.pc
        val targetVar = init.receiver.asVar.toPersistentForm(state.tac.stmts)
        init.params.size match {
            case 0 =>
                computeFinalResult(StringFlowFunctionProperty.constForVariableAt(pc, targetVar, StringTreeEmptyConst))
            case _ =>
                // Only StringBuffer and StringBuilder are interpreted which have constructors with <= 1 parameters
                val paramVar = init.params.head.asVar.toPersistentForm(state.tac.stmts)

                computeFinalResult(
                    Set(PDUWeb(pc, targetVar), PDUWeb(pc, paramVar)),
                    (env: StringTreeEnvironment) => env.update(pc, targetVar, env(pc, paramVar))
                )
        }
    }
}
