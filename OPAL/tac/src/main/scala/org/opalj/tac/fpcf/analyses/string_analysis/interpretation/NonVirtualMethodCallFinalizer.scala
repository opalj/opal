/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * @author Patrick Mell
 */
class NonVirtualMethodCallFinalizer(state: ComputationState) {

    type T = NonVirtualMethodCall[V]

    def interpret(
        instr: T, defSite: Int
    ): Unit = {
        val scis = instr.params.head.asVar.definedBy.toArray.sorted.map { state.fpe2sci }
        state.fpe2sci(defSite) = StringConstancyInformation.reduceMultiple(scis.toList)
    }

}
