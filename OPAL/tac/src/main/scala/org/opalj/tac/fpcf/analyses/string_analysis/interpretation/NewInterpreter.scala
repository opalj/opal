/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.New
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

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
     * [[org.opalj.tac.NonVirtualMethodCall]]s). Consequently, this implementation always returns an
     * empty list.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] = List()

}
