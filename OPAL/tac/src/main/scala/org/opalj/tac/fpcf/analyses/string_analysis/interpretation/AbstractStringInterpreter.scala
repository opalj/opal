/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * @param cfg The control flow graph that underlies the instruction to interpret.
 * @param exprHandler In order to interpret an instruction, it might be necessary to interpret
 *                    another instruction in the first place. `exprHandler` makes this possible.
 *
 * @author Patrick Mell
 */
abstract class AbstractStringInterpreter(
        protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        protected val exprHandler: InterpretationHandler
) {

    type T <: Any

    /**
     *
     * @param instr The instruction that is to be interpreted. It is the responsibility of
     *              implementations to make sure that an instruction is properly and comprehensively
     *              evaluated.
     * @return The interpreted instruction. An empty list indicates that an instruction was not /
     *         could not be interpreted (e.g., because it is not supported or it was processed
     *         before). A list with more than one element indicates an option (only one out of the
     *         values is possible during runtime of the program); thus, some concatenations must
     *         already happen within the interpretation.
     */
    def interpret(instr: T): List[StringConstancyInformation]

}
