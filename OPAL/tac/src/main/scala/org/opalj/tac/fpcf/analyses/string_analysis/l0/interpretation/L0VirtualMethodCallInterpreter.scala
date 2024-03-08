/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * Responsible for processing [[VirtualMethodCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualMethodCallInterpreter[State <: L0ComputationState]() extends L0StringInterpreter[State] {

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
     * For all other calls, a [[StringConstancyInformation.getNeutralElement]] will be returned.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        val sci = instr.name match {
            // IMPROVE interpret argument for setLength
            case "setLength" => StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.RESET)
            case _           => StringConstancyInformation.getNeutralElement
        }
        computeFinalResult(defSite, sci)
    }
}
