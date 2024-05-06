/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
object L0VirtualMethodCallInterpreter extends StringInterpreter {

    override type T = VirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following virtual methods:
     * <ul>
     * <li>
     * `setLength`: `setLength` is a method to reset / clear a [[StringBuilder]] / [[StringBuffer]]
     * (at least when called with the argument `0`). For simplicity, this interpreter currently
     * assumes that 0 is always passed, i.e., the `setLength` method is currently always regarded as
     * a reset mechanism.
     * </li>
     * </ul>
     *
     * For all other calls, a [[StringConstancyInformation.neutralElement]] will be returned.
     */
    override def interpret(instr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        val sci = instr.name match {
            // IMPROVE interpret argument for setLength
            case "setLength" => StringConstancyInformation.neutralElement
            case _           => StringConstancyInformation.neutralElement
        }
        computeFinalResult(pc, sci)
    }
}
