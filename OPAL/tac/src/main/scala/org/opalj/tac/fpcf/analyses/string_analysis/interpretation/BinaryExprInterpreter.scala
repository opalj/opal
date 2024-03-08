/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
case class BinaryExprInterpreter[State <: ComputationState]() extends StringInterpreter[State] {

    override type T = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * For all other expressions, a [[StringConstancyInformation.getNeutralElement]] will be returned.
     */
    def interpret(instr: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        val sci = instr.cTpe match {
            case ComputationalTypeInt   => InterpretationHandler.getConstancyInfoForDynamicInt
            case ComputationalTypeFloat => InterpretationHandler.getConstancyInfoForDynamicFloat
            case _                      => StringConstancyInformation.getNeutralElement
        }
        computeFinalResult(pc, sci)
    }
}

object BinaryExprInterpreter {

    def interpret[State <: ComputationState](instr: BinaryExpr[V], pc: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = BinaryExprInterpreter[State]().interpret(instr, pc)
}
