/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.GetField

class GetFieldFinalizer(
        state: InterproceduralComputationState
) extends AbstractFinalizer(state) {

    override protected type T = GetField[V]

    /**
     * Finalizes [[GetField]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit =
        // Processing the definition site again is enough as the finalization procedure is only
        // called after all dependencies are resolved. Thus, processing the given def site with
        // produce a result
        state.iHandler.processDefSite(defSite)

}

object GetFieldFinalizer {

    def apply(
        state: InterproceduralComputationState
    ): GetFieldFinalizer = new GetFieldFinalizer(state)

}
