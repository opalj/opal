/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package finalizer

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation

/**
 * @author Maximilian Rüsch
 */
case class NonVirtualMethodCallFinalizer[State <: L1ComputationState[State]]() extends L1Finalizer[State] {

    override type T = NonVirtualMethodCall[V]

    /**
     * Finalizes [[NonVirtualMethodCall]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int)(implicit state: State): Unit = {
        val toAppend = if (instr.params.nonEmpty) {
            val defSitesByPC = instr.params.head.asVar.definedBy.map(ds => (pcOfDefSite(ds)(state.tac.stmts), ds)).toMap
            defSitesByPC.keys.foreach { pc =>
                if (!state.fpe2sci.contains(pc)) {
                    state.iHandler.finalizeDefSite(defSitesByPC(pc))
                }
            }
            StringConstancyInformation.reduceMultiple(defSitesByPC.keys.toList.sorted.flatMap(state.fpe2sci))
        } else {
            StringConstancyInformation.lb
        }
        state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), toAppend, reset = true)
    }
}
