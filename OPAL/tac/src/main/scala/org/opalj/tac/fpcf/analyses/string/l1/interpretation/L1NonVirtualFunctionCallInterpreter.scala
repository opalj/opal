/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string.SoundnessMode
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Processes [[NonVirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L1NonVirtualFunctionCallInterpreter()(
    implicit val p:             SomeProject,
    implicit val ps:            PropertyStore,
    implicit val soundnessMode: SoundnessMode
) extends AssignmentLikeBasedStringInterpreter
    with L1FunctionCallInterpreter {

    override type T = AssignmentLikeStmt[V]
    override type E = NonVirtualFunctionCall[V]
    override type CallState = FunctionCallState

    override def interpretExpr(instr: T, expr: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        val target = expr.receiver.asVar.toPersistentForm(state.tac.stmts)
        val calleeMethod = expr.resolveCallTarget(state.dm.definedMethod.classFile.thisType)
        if (calleeMethod.isEmpty) {
            return failure(target)
        }

        val m = calleeMethod.value
        val params = getParametersForPC(state.pc).map(_.asVar.toPersistentForm(state.tac.stmts))
        val callState = new FunctionCallState(target, params, Seq(m), Map((m, ps(m, TACAI.key))))

        interpretArbitraryCallToFunctions(state, callState)
    }
}
