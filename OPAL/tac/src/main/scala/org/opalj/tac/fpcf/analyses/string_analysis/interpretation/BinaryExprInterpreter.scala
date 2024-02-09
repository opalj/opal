/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[BinaryExpr]]ions. A list of currently supported binary expressions can be found in the
 * documentation of [[interpret]].
 *
 * @author Maximilian Rüsch
 */
object BinaryExprInterpreter extends StringInterpreter[Nothing] {

    override type T = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary
     * expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * For all other expressions, a result containing [[StringConstancyProperty.getNeutralElement]]
     * will be returned.
     */
    def interpret(instr: T): FinalEP[T, StringConstancyProperty] = {
        val sci = instr.cTpe match {
            case ComputationalTypeInt   => InterpretationHandler.getConstancyInfoForDynamicInt
            case ComputationalTypeFloat => InterpretationHandler.getConstancyInfoForDynamicFloat
            case _                      => StringConstancyInformation.getNeutralElement
        }
        FinalEP(instr, StringConstancyProperty(sci))
    }
}
