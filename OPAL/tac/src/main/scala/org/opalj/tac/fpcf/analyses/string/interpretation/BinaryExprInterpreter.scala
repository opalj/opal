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
import org.opalj.tac.fpcf.properties.string.ConstantResultFlow
import org.opalj.tac.fpcf.properties.string.IdentityFlow

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
     * For all other expressions, [[IdentityFlow]] will be returned.
     */
    override def interpretExpr(target: V, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val pt = target.toPersistentForm(state.tac.stmts)
        computeFinalResult(expr.cTpe match {
            case ComputationalTypeInt   => ConstantResultFlow.forVariable(pt, StringTreeDynamicInt)
            case ComputationalTypeFloat => ConstantResultFlow.forVariable(pt, StringTreeDynamicFloat)
            case _                      => IdentityFlow
        })
    }
}
