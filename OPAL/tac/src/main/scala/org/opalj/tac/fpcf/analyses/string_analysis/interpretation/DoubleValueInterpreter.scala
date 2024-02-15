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

/**
 * @author Maximilian Rüsch
 */
case class DoubleValueInterpreter[State <: ComputationState[State]]() extends SingleStepStringInterpreter[State] {

    override type T = DoubleConst

    def interpret(instr: T, defSite: Int)(implicit state: State): FinalIPResult =
        FinalIPResult(StringConstancyInformation(
            StringConstancyLevel.CONSTANT,
            StringConstancyType.APPEND,
            instr.value.toString
        ))
}

object DoubleValueInterpreter {

    def interpret[State <: ComputationState[State]](instr: DoubleConst, defSite: Int)(implicit
        state: State
    ): FinalIPResult =
        DoubleValueInterpreter[State]().interpret(instr, defSite)
}
