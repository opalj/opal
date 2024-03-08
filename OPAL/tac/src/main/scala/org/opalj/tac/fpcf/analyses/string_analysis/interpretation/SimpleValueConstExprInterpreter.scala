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
case class SimpleValueConstExprInterpreter[State <: ComputationState]() extends StringInterpreter[State] {

    override type T = SimpleValueConst

    def interpret(expr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult = {
        val sci = expr match {
            case ic: IntConst =>
                StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.APPEND, ic.value.toString)
            case fc: FloatConst =>
                StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.APPEND, fc.value.toString)
            case dc: DoubleConst =>
                StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.APPEND, dc.value.toString)
            case lc: LongConst =>
                StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.APPEND, lc.value.toString)
            case sc: StringConst =>
                StringConstancyInformation(StringConstancyLevel.CONSTANT, StringConstancyType.APPEND, sc.value)
            case _ =>
                StringConstancyInformation.getNeutralElement
        }
        computeFinalResult(defSite, sci)
    }
}

object SimpleValueConstExprInterpreter {

    def interpret[State <: ComputationState](instr: SimpleValueConst, defSite: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = SimpleValueConstExprInterpreter[State]().interpret(instr, defSite)
}
