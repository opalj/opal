/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt

/**
 * @author Maximilian Rüsch
 */
case class BinaryExprInterpreter[State <: ComputationState[State]]() extends StringInterpreter[State] {

    override type T = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * For all other expressions, a [[NoIPResult]] will be returned.
     */
    def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        instr.cTpe match {
            case ComputationalTypeInt   => FinalIPResult(InterpretationHandler.getConstancyInfoForDynamicInt)
            case ComputationalTypeFloat => FinalIPResult(InterpretationHandler.getConstancyInfoForDynamicFloat)
            case _                      => NoIPResult
        }
    }
}

object BinaryExprInterpreter {

    def interpret[State <: ComputationState[State]](instr: BinaryExpr[V], defSite: Int)(implicit state: State): IPResult =
        BinaryExprInterpreter[State]().interpret(instr, defSite)
}
