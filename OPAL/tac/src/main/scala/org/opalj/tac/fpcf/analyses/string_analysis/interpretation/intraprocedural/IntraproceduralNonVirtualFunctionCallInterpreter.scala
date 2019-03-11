/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter

/**
 * The `IntraproceduralNonVirtualFunctionCallInterpreter` is responsible for processing
 * [[NonVirtualFunctionCall]]s in an intraprocedural fashion.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralNonVirtualFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: IntraproceduralInterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualFunctionCall[V]

    /**
     * This function always returns a result that contains [[StringConstancyProperty.lb]].
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)

}
