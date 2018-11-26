/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * The `NonVirtualFunctionCallInterpreter` is responsible for processing
 * [[NonVirtualFunctionCall]]s.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class NonVirtualFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: ExprHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualFunctionCall[V]

    /**
     * Currently, [[NonVirtualFunctionCall]] are not supported. Thus, this function always returns a
     * list with a single element consisting of [[StringConstancyLevel.DYNAMIC]] and
     * [[StringConstancyInformation.UnknownWordSymbol]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] = {
        List(StringConstancyInformation(
            StringConstancyLevel.DYNAMIC, StringConstancyInformation.UnknownWordSymbol
        ))
    }

}
