/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[New]] expressions.
 *
 * @author Maximilian Rüsch
 */
case class NewInterpreter(
    override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
    override protected val exprHandler: InterpretationHandler
) extends StringInterpreter {

    override type T = New

    /**
     * [[New]] expressions do not carry any relevant information in this context (as the initial values are not set in
     * [[New]] expressions but, e.g., in [[org.opalj.tac.NonVirtualMethodCall]]s). Consequently, this implementation
     * always returns a [[StringConstancyProperty.getNeutralElement]].
     */
    def interpret(instr: T): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.getNeutralElement)
}
