/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l0
package interpretation

import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Interprets the given assignment statement containing a [[SimpleValueConst]] expression by determining the possible
 * constant values from the given expression. The result is converted to a [[StringTreeConst]] and applied to the
 * assignment target variable in the string flow function. If no applicable const is found, ID is returned for all
 * variables.
 *
 * @author Maximilian Rüsch
 */
object SimpleValueConstExprInterpreter extends AssignmentBasedStringInterpreter {

    override type E = SimpleValueConst

    override def interpretExpr(target: PV, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        computeFinalResult(expr match {
            case ic: IntConst =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeConst(ic.value.toString))
            case fc: FloatConst =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeConst(fc.value.toString))
            case dc: DoubleConst =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeConst(dc.value.toString))
            case lc: LongConst =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeConst(lc.value.toString))
            case sc: StringConst =>
                StringFlowFunctionProperty.constForVariableAt(state.pc, target, StringTreeConst(sc.value))
            case _ => StringFlowFunctionProperty.identity
        })
    }
}
