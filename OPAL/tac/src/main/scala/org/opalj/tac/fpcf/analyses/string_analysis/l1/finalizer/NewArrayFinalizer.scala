/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package finalizer

/**
 * @author Maximilian Rüsch
 */
case class NewArrayFinalizer(
        override protected val state: L1ComputationState
) extends L1Finalizer {

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
