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
case class FloatValueInterpreter[State <: ComputationState[State]]() extends SingleStepStringInterpreter[State] {

    override type T = FloatConst

    def interpret(instr: T, defSite: Int)(implicit state: State): FinalIPResult =
        FinalIPResult(
            StringConstancyInformation(
                StringConstancyLevel.CONSTANT,
                StringConstancyType.APPEND,
                instr.value.toString
            ),
            state.dm,
            pcOfDefSite(defSite)(state.tac.stmts)
        )
}

object FloatValueInterpreter {

    def interpret[State <: ComputationState[State]](instr: FloatConst, defSite: Int)(implicit
        state: State
    ): FinalIPResult =
        FloatValueInterpreter[State]().interpret(instr, defSite)
}
