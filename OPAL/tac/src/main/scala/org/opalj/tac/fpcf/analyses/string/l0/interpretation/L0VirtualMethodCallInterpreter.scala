/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
object L0VirtualMethodCallInterpreter extends StringInterpreter {

    override type T = VirtualMethodCall[V]

    /**
     * Currently, this function supports no method calls. However, it treats [[StringBuilder.setLength]] such that it
     * will return the lower bound for now.
     */
    override def interpret(instr: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        instr.name match {
            // IMPROVE interpret argument for setLength
            case "setLength" => computeFinalLBFor(instr.receiver.asVar)
            case _           => computeFinalResult(StringFlowFunctionProperty.identity)
        }
    }
}
