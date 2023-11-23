/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural
package finalizer

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation

/**
 * @author Patrick Mell
 */
class ArrayLoadFinalizer(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
) extends AbstractFinalizer(state) {

    override type T = ArrayLoad[V]

    /**
     * Finalizes [[ArrayLoad]]s.
     * <p>
     * @inheritdoc
     */
    override def finalizeInterpretation(instr: T, defSite: Int): Unit = {
        val allDefSites = ArrayPreparationInterpreter.getStoreAndLoadDefSites(
            instr, state.tac.stmts
        )

        allDefSites.foreach { ds =>
            if (!state.fpe2sci.contains(ds)) {
                state.iHandler.finalizeDefSite(ds, state)
            }
        }

        state.fpe2sci(defSite) = ListBuffer(StringConstancyInformation.reduceMultiple(
            allDefSites.filter(state.fpe2sci.contains).sorted.flatMap { ds =>
                state.fpe2sci(ds)
            }
        ))
    }

}

object ArrayLoadFinalizer {

    def apply(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
    ): ArrayLoadFinalizer = new ArrayLoadFinalizer(state, cfg)

}
