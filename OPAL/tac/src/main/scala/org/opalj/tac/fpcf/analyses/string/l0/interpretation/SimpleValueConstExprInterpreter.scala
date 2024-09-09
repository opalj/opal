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
 * @author Maximilian Rüsch
 */
object SimpleValueConstExprInterpreter extends AssignmentBasedStringInterpreter {

    override type E = SimpleValueConst

    override def interpretExpr(target: PV, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        // IMPROVE the constants generated here could also be interpreted outside string flow interpretation, reducing
        // the overhead needed to interpret a simple string const for e.g. a call parameter.
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
