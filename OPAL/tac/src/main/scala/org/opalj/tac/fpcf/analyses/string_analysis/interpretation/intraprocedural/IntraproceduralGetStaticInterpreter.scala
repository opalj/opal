/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.GetStatic
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter

/**
 * The `IntraproceduralGetStaticInterpreter` is responsible for processing
 * [[org.opalj.tac.GetStatic]]s in an intraprocedural fashion. Thus, they are not analyzed but a
 * fixed [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation]] is returned
 * (see [[interpret]]).
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralGetStaticInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: IntraproceduralInterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = GetStatic

    /**
     * Currently, this type is not interpreted. Thus, this function always returns a result
     * containing [[StringConstancyProperty.lb]].
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)

}