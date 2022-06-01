/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * @author Patrick Mell
 */
class NonVirtualMethodCallFinalizer(
        state: InterproceduralComputationState
) extends AbstractFinalizer(state) {

    override type T = NonVirtualMethodCall[V]

    /**
     * Finalizes [[NonVirtualMethodCall]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        val toAppend = if (instr.params.nonEmpty) {
            instr.params.head.asVar.definedBy.toArray.foreach { ds ⇒
                if (!state.fpe2sci.contains(ds)) {
                    state.iHandler.finalizeDefSite(ds, state)
                }
            }
            val scis = instr.params.head.asVar.definedBy.toArray.sorted.map { state.fpe2sci }
            StringConstancyInformation.reduceMultiple(scis.flatten.toList)
        } else {
            StringConstancyProperty.lb.stringConstancyInformation
        }
        state.appendToFpe2Sci(defSite, toAppend, reset = true)
    }

}

object NonVirtualMethodCallFinalizer {

    def apply(
        state: InterproceduralComputationState
    ): NonVirtualMethodCallFinalizer = new NonVirtualMethodCallFinalizer(state)

}