/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.StringConstancyProperty
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
     * Finalizes [[VirtualFunctionCall]]s. Currently, this finalizer supports only the "append" and
     * "toString" function.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        instr.name match {
            case "append"   ⇒ finalizeAppend(instr, defSite)
            case "toString" ⇒ finalizeToString(instr, defSite)
            case _ ⇒ state.appendToFpe2Sci(
                defSite, StringConstancyProperty.lb.stringConstancyInformation, reset = true
            )
        }
    }

    /**
     * This function actually finalizes append calls by mimicking the behavior of the corresponding
     * interpretation function of
     * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.VirtualFunctionCallPreparationInterpreter]].
     */
    private def finalizeAppend(instr: T, defSite: Int): Unit = {
        val receiverDefSites = instr.receiver.asVar.definedBy.toArray.sorted
        receiverDefSites.foreach { ds ⇒
            if (!state.fpe2sci.contains(ds)) {
                state.iHandler.finalizeDefSite(ds, state)
            }
        }
        val receiverSci = StringConstancyInformation.reduceMultiple(
            receiverDefSites.flatMap { s ⇒
                // As the receiver value is used already here, we do not want it to be used a
                // second time (during the final traversing of the path); thus, reset it to have it
                // only once in the result, i.e., final tree
                val sci = state.fpe2sci(s)
                state.appendToFpe2Sci(s, StringConstancyInformation.getNeutralElement, reset = true)
                sci
            }
        )

        val paramDefSites = instr.params.head.asVar.definedBy.toArray.sorted
        paramDefSites.foreach { ds ⇒
            if (!state.fpe2sci.contains(ds)) {
                state.iHandler.finalizeDefSite(ds, state)
            }
        }
        val appendSci = if (paramDefSites.forall(state.fpe2sci.contains)) {
            StringConstancyInformation.reduceMultiple(
                paramDefSites.flatMap(state.fpe2sci(_))
            )
        } else StringConstancyInformation.lb

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

    private def finalizeToString(instr: T, defSite: Int): Unit = {
        val dependeeSites = instr.receiver.asVar.definedBy
        dependeeSites.foreach { nextDependeeSite ⇒
            if (!state.fpe2sci.contains(nextDependeeSite)) {
                state.iHandler.finalizeDefSite(nextDependeeSite, state)
            }
        }
        val finalSci = StringConstancyInformation.reduceMultiple(
            dependeeSites.toArray.flatMap { ds ⇒ state.fpe2sci(ds) }
        )
        // Remove the dependees, such as calls to "toString"; the reason being is that we do not
        // duplications (arising from an "append" and a "toString" call)
        dependeeSites.foreach {
            state.appendToFpe2Sci(_, StringConstancyInformation.getNeutralElement, reset = true)
        }
        state.appendToFpe2Sci(defSite, finalSci)
    }

}

object VirtualFunctionCallFinalizer {

    def apply(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
    ): VirtualFunctionCallFinalizer = new VirtualFunctionCallFinalizer(state, cfg)

}
