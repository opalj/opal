/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural
package finalizer

class GetFieldFinalizer(
        state: InterproceduralComputationState
) extends AbstractFinalizer(state) {

    override protected type T = FieldRead[SEntity]

    /**
     * Finalizes [[FieldRead]]s.
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
