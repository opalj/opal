/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural
package finalizer

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation

/**
 * @author Patrick Mell
 */
class StaticFunctionCallFinalizer(
        state: InterproceduralComputationState
) extends AbstractFinalizer(state) {

    override type T = StaticFunctionCall[SEntity]

    /**
     * Finalizes [[StaticFunctionCall]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        val isValueOf = instr.declaringClass.fqn == "java/lang/String" && instr.name == "valueOf"
        val toAppend = if (isValueOf) {
            // For the finalization we do not need to consider between chars and non-chars as chars
            // are only considered when they are char constants and thus a final result is already
            // computed by InterproceduralStaticFunctionCallInterpreter (which is why this method
            // will not be called for char parameters)
            val defSites = instr.params.head.asVar.definedBy.toArray.sorted
            defSites.foreach { ds =>
                if (!state.fpe2sci.contains(ds)) {
                    state.iHandler.finalizeDefSite(ds, state)
                }
            }
            val scis = defSites.map { state.fpe2sci }
            StringConstancyInformation.reduceMultiple(scis.flatten.toList)
        } else {
            StringConstancyProperty.lb.stringConstancyInformation
        }
        state.appendToFpe2Sci(defSite, toAppend, reset = true)
    }

}

object StaticFunctionCallFinalizer {

    def apply(
        state: InterproceduralComputationState
    ): StaticFunctionCallFinalizer = new StaticFunctionCallFinalizer(state)

}
