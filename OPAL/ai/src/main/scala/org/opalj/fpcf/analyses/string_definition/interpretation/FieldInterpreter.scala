/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.Stmt
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.fpcf.string_definition.properties.StringConstancyType
import org.opalj.tac.GetField
import org.opalj.tac.TACStmts

/**
 * The `FieldInterpreter` is responsible for processing [[GetField]]s. Currently, there is only
 * primitive support for fields, i.e., they are not analyzed but a constant
 * [[StringConstancyInformation]] is returned.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class FieldInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = GetField[V]

    /**
     * Currently, fields are not interpreted. Thus, this function always returns a list with a
     * single element consisting of [[StringConstancyLevel.DYNAMIC]],
     * [[StringConstancyType.APPEND]] and [[StringConstancyInformation.UnknownWordSymbol]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] =
        List(StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.APPEND,
            StringConstancyInformation.UnknownWordSymbol
        ))

}
