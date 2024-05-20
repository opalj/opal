/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.string.StringTreeDynamicFloat
import org.opalj.br.fpcf.properties.string.StringTreeDynamicInt
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
object BinaryExprInterpreter extends AssignmentBasedStringInterpreter {

    override type E = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * For all other expressions, [[StringFlowFunctionProperty.identity]] will be returned.
     */
    override def interpretExpr(target: PV, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        computeFinalResult(expr.cTpe match {
            case ComputationalTypeInt =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeDynamicInt)
            case ComputationalTypeFloat =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeDynamicFloat)
            case _ => StringFlowFunctionProperty.identity
        })
    }
}
