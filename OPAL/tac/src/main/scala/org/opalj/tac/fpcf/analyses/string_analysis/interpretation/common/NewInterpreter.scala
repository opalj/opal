/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package common

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP

/**
 * The `NewInterpreter` is responsible for processing [[New]] expressions.
 * <p>
 * For this implementation, the concrete implementation passed for [[exprHandler]] is not relevant.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class NewInterpreter(
    cfg:         CFG[Stmt[V], TACStmts[V]],
    exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = New

    /**
     * [[New]] expressions do not carry any relevant information in this context (as the initial
     * values are not set in a [[New]] expressions but, e.g., in
     * [[org.opalj.tac.NonVirtualMethodCall]]s). Consequently, this implementation always returns a
     * Result containing [[StringConstancyProperty.getNeutralElement]].
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.getNeutralElement)
}
