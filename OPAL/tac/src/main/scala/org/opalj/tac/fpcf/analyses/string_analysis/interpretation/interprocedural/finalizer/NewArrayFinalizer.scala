/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import org.opalj.br.cfg.CFG
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.NewArray

/**
 * @author Patrick Mell
 */
class NewArrayFinalizer(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
) extends AbstractFinalizer(state) {

    override type T = NewArray[V]

    /**
     * Finalizes [[NewArray]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit =
        // Simply re-trigger the computation
        state.iHandler.processDefSite(defSite)

}

object NewArrayFinalizer {

    def apply(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
    ): NewArrayFinalizer = new NewArrayFinalizer(state, cfg)

}
