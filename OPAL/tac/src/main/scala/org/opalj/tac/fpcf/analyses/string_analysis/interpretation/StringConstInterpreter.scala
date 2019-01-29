/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * The `StringConstInterpreter` is responsible for processing [[StringConst]]s.
 * <p>
 * For this interpreter, the given interpretation handler does not play any role. Consequently, any
 * implementation may be passed.
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
     * [[StringConstancyLevel.CONSTANT]] [[StringConstancyInformation]] element holding the
     * stringified value.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): ProperPropertyComputationResult =
        Result(instr, StringConstancyProperty(StringConstancyInformation(
            StringConstancyLevel.CONSTANT,
            StringConstancyType.APPEND,
            instr.value
        )))

}
