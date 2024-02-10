/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package finalizer

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType

/**
 * @author Maximilian Rüsch
 */
case class VirtualFunctionCallFinalizer[State <: L1ComputationState[State]]() extends L1Finalizer[State] {

    override type T = VirtualFunctionCall[V]

    /**
     * Finalizes [[VirtualFunctionCall]]s. Currently, this finalizer supports only the "append" and
     * "toString" function.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int)(implicit state: State): Unit = {
        instr.name match {
            case "append"   => finalizeAppend(instr, defSite)
            case "toString" => finalizeToString(instr, defSite)
            case _ =>
                state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb, reset = true)
        }
    }

    /**
     * This function actually finalizes append calls by mimicking the behavior of the corresponding
     * interpretation function of
     * [[org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1VirtualFunctionCallInterpreter]].
     */
    private def finalizeAppend(instr: T, defSite: Int)(implicit state: State): Unit = {
        val receiverDefSitesByPC =
            instr.receiver.asVar.definedBy.map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
        receiverDefSitesByPC.keys.foreach { pc =>
            if (!state.fpe2sci.contains(pc)) {
                state.iHandler.finalizeDefSite(receiverDefSitesByPC(pc))
            }
        }
        val receiverSci = StringConstancyInformation.reduceMultiple(
            receiverDefSitesByPC.keys.toList.sorted.flatMap { pc =>
                // As the receiver value is used already here, we do not want it to be used a
                // second time (during the final traversing of the path); thus, reset it to have it
                // only once in the result, i.e., final tree
                val sci = state.fpe2sci(pc)
                state.appendToFpe2Sci(pc, StringConstancyInformation.getNeutralElement, reset = true)
                sci
            }
        )

        val paramDefSitesByPC =
            instr.params.head.asVar.definedBy.map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
        paramDefSitesByPC.keys.foreach { pc =>
            if (!state.fpe2sci.contains(pc)) {
                state.iHandler.finalizeDefSite(paramDefSitesByPC(pc))
            }
        }
        val appendSci = if (paramDefSitesByPC.keys.forall(state.fpe2sci.contains)) {
            StringConstancyInformation.reduceMultiple(paramDefSitesByPC.keys.toList.sorted.flatMap(state.fpe2sci(_)))
        } else StringConstancyInformation.lb

        val finalSci = if (receiverSci.isTheNeutralElement && appendSci.isTheNeutralElement) {
            receiverSci
        } else if (receiverSci.isTheNeutralElement) {
            appendSci
        } else if (appendSci.isTheNeutralElement) {
            receiverSci
        } else {
            StringConstancyInformation(
                StringConstancyLevel.determineForConcat(receiverSci.constancyLevel, appendSci.constancyLevel),
                StringConstancyType.APPEND,
                receiverSci.possibleStrings + appendSci.possibleStrings
            )
        }

        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), finalSci, reset = true)
    }

    private def finalizeToString(instr: T, defSite: Int)(implicit state: State): Unit = {
        val dependeeSites = instr.receiver.asVar.definedBy.map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
        dependeeSites.keys.foreach { pc =>
            if (!state.fpe2sci.contains(pc)) {
                state.iHandler.finalizeDefSite(dependeeSites(pc))
            }
        }
        val finalSci = StringConstancyInformation.reduceMultiple(
            dependeeSites.keys.toList.flatMap { pc => state.fpe2sci(pc) }
        )
        // Remove the dependees, such as calls to "toString"; the reason being is that we do not
        // duplications (arising from an "append" and a "toString" call)
        dependeeSites.keys.foreach {
            state.appendToFpe2Sci(_, StringConstancyInformation.getNeutralElement, reset = true)
        }
        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), finalSci)
    }
}
