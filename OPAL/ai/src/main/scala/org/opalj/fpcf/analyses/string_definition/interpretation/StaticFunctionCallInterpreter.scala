/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.fpcf.string_definition.properties.StringConstancyType.APPEND
import org.opalj.tac.StaticFunctionCall

/**
 * The `StaticFunctionCallInterpreter` is responsible for processing [[StaticFunctionCall]]s.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class StaticFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = StaticFunctionCall[V]

    /**
     * Currently, [[StaticFunctionCall]]s are not supported. Thus, this function always returns a
     * list with a single element consisting of [[StringConstancyLevel.DYNAMIC]] and
     * [[StringConstancyInformation.UnknownWordSymbol]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] =
        List(StringConstancyInformation(
            StringConstancyLevel.DYNAMIC, APPEND, StringConstancyInformation.UnknownWordSymbol
        ))

}
