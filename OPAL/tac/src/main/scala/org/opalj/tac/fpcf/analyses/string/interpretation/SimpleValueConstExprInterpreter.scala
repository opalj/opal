/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.properties.string.ConstantResultFlow
import org.opalj.tac.fpcf.properties.string.IdentityFlow

/**
 * @author Maximilian Rüsch
 */
object SimpleValueConstExprInterpreter extends AssignmentBasedStringInterpreter {

    override type E = SimpleValueConst

    override def interpretExpr(target: V, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val pt = target.toPersistentForm(state.tac.stmts)
        computeFinalResult(expr match {
            case ic: IntConst    => ConstantResultFlow.forVariable(pt, StringTreeConst(ic.value.toString))
            case fc: FloatConst  => ConstantResultFlow.forVariable(pt, StringTreeConst(fc.value.toString))
            case dc: DoubleConst => ConstantResultFlow.forVariable(pt, StringTreeConst(dc.value.toString))
            case lc: LongConst   => ConstantResultFlow.forVariable(pt, StringTreeConst(lc.value.toString))
            case sc: StringConst => ConstantResultFlow.forVariable(pt, StringTreeConst(sc.value))
            case _               => IdentityFlow
        })
    }
}
