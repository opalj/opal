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
 * The `IntraproceduralFieldInterpreter` is responsible for processing [[GetField]]s. In this
 * implementation, there is currently only primitive support for fields, i.e., they are not analyzed
 * but a constant [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation]]
 * is returned (see [[interpret]] of this class).
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralFieldInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: IntraproceduralInterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = GetField[V]

    /**
     * Fields are not supported by this implementation. Thus, this function always returns
     * [[StringConstancyProperty.lb]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)
}
