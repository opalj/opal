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
case class GetFieldFinalizer(
        override protected val state: L1ComputationState
) extends L1Finalizer {

    override protected type T = FieldRead[V]

    /**
     * Finalizes [[FieldRead]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit =
        // Processing the definition site again is enough as the finalization procedure is only
        // called after all dependencies are resolved.
        state.iHandler.processDefSite(defSite)
}
