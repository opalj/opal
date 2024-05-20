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

    override def interpretExpr(target: PV, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        computeFinalResult(expr match {
            case ic: IntConst    => ConstantResultFlow.forVariable(target, StringTreeConst(ic.value.toString))
            case fc: FloatConst  => ConstantResultFlow.forVariable(target, StringTreeConst(fc.value.toString))
            case dc: DoubleConst => ConstantResultFlow.forVariable(target, StringTreeConst(dc.value.toString))
            case lc: LongConst   => ConstantResultFlow.forVariable(target, StringTreeConst(lc.value.toString))
            case sc: StringConst => ConstantResultFlow.forVariable(target, StringTreeConst(sc.value))
            case _               => IdentityFlow
        })
    }
}
