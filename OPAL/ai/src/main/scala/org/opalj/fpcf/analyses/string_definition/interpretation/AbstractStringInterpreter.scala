/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * @param cfg The control flow graph that underlies the instruction to interpret.
 * @param exprHandler In order to interpret an instruction, it might be necessary to interpret
 *                    another instruction in the first place. `exprHandler` makes this possible.
 *
 * @author Patrick Mell
 */
abstract class AbstractStringInterpreter(
        protected val cfg: CFG[Stmt[V], TACStmts[V]],
        protected val exprHandler: ExprHandler,
) {

    type T <: Any

    /**
     *
     * @param instr The instruction that is to be interpreted. It is the responsibility of
     *              implementations to make sure that an instruction is properly and comprehensively
     *              evaluated.
     * @return
     */
    def interpret(instr: T): List[StringConstancyInformation]

}
