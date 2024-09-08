/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.string.StringTreeDynamicFloat
import org.opalj.br.fpcf.properties.string.StringTreeDynamicInt
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @author Maximilian Rüsch
 */
case class BinaryExprInterpreter()(
    implicit val soundnessMode: SoundnessMode
) extends AssignmentBasedStringInterpreter {

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
        // IMPROVE Use the underlying domain to retrieve the result of such expressions if possible in low soundness mode
        computeFinalResult(expr.cTpe match {
            case ComputationalTypeInt if soundnessMode.isHigh =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeDynamicInt)
            case ComputationalTypeInt =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeNode.ub)
            case ComputationalTypeFloat if soundnessMode.isHigh =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeDynamicFloat)
            case ComputationalTypeFloat =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeNode.ub)
            case _ => StringFlowFunctionProperty.identity
        })
    }
}
