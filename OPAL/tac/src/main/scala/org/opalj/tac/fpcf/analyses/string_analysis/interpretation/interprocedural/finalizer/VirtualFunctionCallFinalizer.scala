/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState

/**
 * @author Patrick Mell
 */
class VirtualFunctionCallFinalizer(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
) extends AbstractFinalizer(state) {

    override type T = VirtualFunctionCall[V]

    /**
     * Finalizes [[VirtualFunctionCall]]s. Currently, this finalizer supports only the "append"
     * function.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        if (instr.name == "append") {
            finalizeAppend(instr, defSite)
        }
    }

    /**
     * This function actually finalizes append calls by mimicking the behavior of the corresponding
     * interpretation function of
     * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.VirtualFunctionCallPreparationInterpreter]].
     */
    private def finalizeAppend(instr: T, defSite: Int): Unit = {
        val receiverSci = StringConstancyInformation.reduceMultiple(
            instr.receiver.asVar.definedBy.toArray.sorted.flatMap(state.fpe2sci(_))
        )
        val appendSci = StringConstancyInformation.reduceMultiple(
            instr.params.head.asVar.definedBy.toArray.sorted.flatMap { state.fpe2sci(_) }
        )

        val finalSci = if (receiverSci.isTheNeutralElement && appendSci.isTheNeutralElement) {
            receiverSci
        } else if (receiverSci.isTheNeutralElement) {
            appendSci
        } else if (appendSci.isTheNeutralElement) {
            receiverSci
        } else {
            StringConstancyInformation(
                StringConstancyLevel.determineForConcat(
                    receiverSci.constancyLevel, appendSci.constancyLevel
                ),
                StringConstancyType.APPEND,
                receiverSci.possibleStrings + appendSci.possibleStrings
            )
        }

        state.appendToFpe2Sci(defSite, finalSci, reset = true)
    }

}
