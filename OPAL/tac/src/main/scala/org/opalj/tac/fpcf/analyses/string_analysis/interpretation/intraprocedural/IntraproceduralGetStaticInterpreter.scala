/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package intraprocedural

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP

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
     * Currently, this type is not interpreted. Thus, this function always returns [[StringConstancyProperty.lb]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)
}
