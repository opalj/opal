/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.tac.TACStmts
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst

/**
 * The `StringConstInterpreter` is responsible for processing [[StringConst]]s.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class StringConstInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = StringConst

    /**
     * The interpretation of a [[StringConst]] always results in a list with one
     * [[StringConstancyInformation]] element.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] =
        List(StringConstancyInformation(StringConstancyLevel.CONSTANT, instr.value))

}
