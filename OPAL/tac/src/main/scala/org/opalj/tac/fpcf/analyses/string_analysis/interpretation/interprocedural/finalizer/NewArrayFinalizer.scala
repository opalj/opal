/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural
package finalizer

import org.opalj.br.cfg.CFG

/**
 * @author Patrick Mell
 */
class NewArrayFinalizer(
        state: InterproceduralComputationState,
        cfg:   CFG[Stmt[SEntity], TACStmts[SEntity]]
) extends AbstractFinalizer(state) {

    override type T = NewArray[SEntity]

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
        state: InterproceduralComputationState,
        cfg:   CFG[Stmt[SEntity], TACStmts[SEntity]]
    ): NewArrayFinalizer = new NewArrayFinalizer(state, cfg)

}
