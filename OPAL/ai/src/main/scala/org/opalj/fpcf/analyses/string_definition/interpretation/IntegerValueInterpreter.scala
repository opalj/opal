/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.fpcf.string_definition.properties.StringConstancyType
import org.opalj.br.cfg.CFG
import org.opalj.tac.IntConst
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * The `IntegerValueInterpreter` is responsible for processing [[IntConst]]s.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntegerValueInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = IntConst

    /**
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] =
        List(StringConstancyInformation(
            StringConstancyLevel.CONSTANT,
            StringConstancyType.APPEND,
            instr.value.toString
        ))

}
