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
import org.opalj.tac.fpcf.analyses.string_analysis.l1.interpretation.L1ArrayAccessInterpreter

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
        val allDefSites = L1ArrayAccessInterpreter.getStoreAndLoadDefSites(instr, state.tac.stmts)

        allDefSites.foreach { ds =>
            if (!state.fpe2sci.contains(ds)) {
                state.iHandler.finalizeDefSite(ds, state)
            }
        }

        state.fpe2sci(defSite) = ListBuffer(StringConstancyInformation.reduceMultiple(
            allDefSites.filter(state.fpe2sci.contains).sorted.flatMap { ds => state.fpe2sci(ds) }
        ))
    }
}
