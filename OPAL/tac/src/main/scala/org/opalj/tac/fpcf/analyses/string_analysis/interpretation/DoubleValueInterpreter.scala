/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
case class DoubleValueInterpreter[State <: ComputationState]() extends StringInterpreter[State] {

    override type T = DoubleConst

    def interpret(instr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult =
        computeFinalResult(
            defSite,
            StringConstancyInformation(
                StringConstancyLevel.CONSTANT,
                StringConstancyType.APPEND,
                instr.value.toString
            )
        )
}

object DoubleValueInterpreter {

    def interpret[State <: ComputationState](instr: DoubleConst, defSite: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = DoubleValueInterpreter[State]().interpret(instr, defSite)
}
