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
case class NewArrayFinalizer[State <: L1ComputationState[State]]() extends L1Finalizer[State] {

    override type T = NewArray[V]

    /**
     * Finalizes [[NewArray]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int)(implicit state: State): Unit =
        // Simply re-trigger the computation
        state.iHandler.processDefSite(defSite)(state)
}
