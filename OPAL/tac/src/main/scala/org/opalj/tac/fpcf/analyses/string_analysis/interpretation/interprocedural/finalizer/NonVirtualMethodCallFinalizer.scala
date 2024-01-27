/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural
package finalizer

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation

/**
 * @author Patrick Mell
 */
class NonVirtualMethodCallFinalizer(state: InterproceduralComputationState) extends AbstractFinalizer(state) {

    override type T = NonVirtualMethodCall[V]

    /**
     * Finalizes [[NonVirtualMethodCall]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        val toAppend = if (instr.params.nonEmpty) {
            instr.params.head.asVar.definedBy.toArray.foreach { ds =>
                if (!state.fpe2sci.contains(ds)) {
                    state.iHandler.finalizeDefSite(ds, state)
                }
            }
            val scis = instr.params.head.asVar.definedBy.toArray.sorted.map { state.fpe2sci }
            StringConstancyInformation.reduceMultiple(scis.flatten.toList)
        } else {
            StringConstancyInformation.lb
        }
        state.appendToFpe2Sci(defSite, toAppend, reset = true)
    }
}

object NonVirtualMethodCallFinalizer {

    def apply(state: InterproceduralComputationState): NonVirtualMethodCallFinalizer =
        new NonVirtualMethodCallFinalizer(state)
}
