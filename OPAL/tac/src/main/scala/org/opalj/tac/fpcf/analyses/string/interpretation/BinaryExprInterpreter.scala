/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
object BinaryExprInterpreter extends StringInterpreter {

    override type T = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * For all other expressions, a [[StringConstancyInformation.neutralElement]] will be returned.
     */
    def interpret(instr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        val sci = instr.cTpe match {
            case ComputationalTypeInt   => StringConstancyInformation.dynamicInt
            case ComputationalTypeFloat => StringConstancyInformation.dynamicFloat
            case _                      => StringConstancyInformation.neutralElement
        }
        computeFinalResult(pc, sci)
    }
}
