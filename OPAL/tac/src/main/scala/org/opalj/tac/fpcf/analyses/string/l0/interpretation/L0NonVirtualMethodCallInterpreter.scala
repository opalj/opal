/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.IdentityFlow
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
object L0NonVirtualMethodCallInterpreter extends StringInterpreter {

    override type T = NonVirtualMethodCall[V]

    override def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        instr.name match {
            case "<init>" => interpretInit(instr)
            case _        => computeFinalResult(IdentityFlow)
        }
    }

    private def interpretInit(init: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        init.params.size match {
            case 0 =>
                computeFinalResult(IdentityFlow)
            case _ =>
                val targetVar = StringInterpreter.findUVarForDVar(init.receiver.asVar).toPersistentForm(state.tac.stmts)
                // Only StringBuffer and StringBuilder are interpreted which have constructors with <= 1 parameters
                val paramVar = init.params.head.asVar.toPersistentForm(state.tac.stmts)

                computeFinalResult((env: StringTreeEnvironment) => env.update(targetVar, env(paramVar)))
        }
    }
}
