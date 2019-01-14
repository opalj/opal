/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.fpcf.string_definition.properties.StringConstancyType
import org.opalj.br.cfg.CFG
import org.opalj.tac.GetStatic
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * The `GetStaticInterpreter` is responsible for processing [[org.opalj.tac.GetStatic]]s. Currently,
 * there is only primitive support, i.e., they are not analyzed but a fixed
 * [[StringConstancyInformation]] is returned.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class GetStaticInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = GetStatic

    /**
     * Currently, this type is not interpreted. Thus, this function always returns a list with a
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