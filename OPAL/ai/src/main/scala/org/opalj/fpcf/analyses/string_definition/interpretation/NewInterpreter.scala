/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.TACStmts
import org.opalj.tac.Stmt
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.New

/**
 * The `NewInterpreter` is responsible for processing [[New]] expressions.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class NewInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: ExprHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = New

    /**
     * [[New]] expressions do not carry any relevant information in this context (as the initial
     * values are not set in a [[New]] expressions but, e.g., in
     * [[org.opalj.tac.NonVirtualMethodCall]]s). Consequently, thus implementation always returns an
     * empty list.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] = List()

}
