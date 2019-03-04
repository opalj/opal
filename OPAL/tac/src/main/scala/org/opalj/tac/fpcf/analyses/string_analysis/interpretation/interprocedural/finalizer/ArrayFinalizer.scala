/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.finalizer

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.ArrayPreparationInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState

/**
 * @author Patrick Mell
 */
class ArrayFinalizer(
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
        state.fpe2sci(defSite) = ListBuffer(StringConstancyInformation.reduceMultiple(
            allDefSites.sorted.flatMap(state.fpe2sci(_))
        ))
    }

}

object ArrayFinalizer {

    def apply(
        state: InterproceduralComputationState, cfg: CFG[Stmt[V], TACStmts[V]]
    ): ArrayFinalizer = new ArrayFinalizer(state, cfg)

}
