/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package finalizer

import scala.collection.mutable.ListBuffer

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0ArrayAccessInterpreter

/**
 * @author Maximilian Rüsch
 */
case class ArrayLoadFinalizer(
        override protected val state: L1ComputationState
) extends L1Finalizer {

    override type T = ArrayLoad[V]

    /**
     * Finalizes [[ArrayLoad]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        val allDefSites = L0ArrayAccessInterpreter.getStoreAndLoadDefSites(instr)(state.tac.stmts)
        val allDefSitesByPC = allDefSites.map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
        allDefSitesByPC.keys.foreach { pc =>
            if (!state.fpe2sci.contains(pc)) {
                state.iHandler.finalizeDefSite(allDefSitesByPC(pc), state)
            }
        }

        state.fpe2sci(pcOfDefSite(defSite)(state.tac.stmts)) = ListBuffer(StringConstancyInformation.reduceMultiple(
            allDefSitesByPC.keys.filter(state.fpe2sci.contains).toList.sorted.flatMap { pc => state.fpe2sci(pc) }
        ))
    }
}
