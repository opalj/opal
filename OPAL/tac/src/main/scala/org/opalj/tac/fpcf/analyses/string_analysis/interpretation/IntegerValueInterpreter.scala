/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
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
case class IntegerValueInterpreter[State <: ComputationState]() extends StringInterpreter[State] {

    override type T = IntConst

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

object IntegerValueInterpreter {

    def interpret[State <: ComputationState](instr: IntConst, defSite: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = IntegerValueInterpreter[State]().interpret(instr, defSite)
}
