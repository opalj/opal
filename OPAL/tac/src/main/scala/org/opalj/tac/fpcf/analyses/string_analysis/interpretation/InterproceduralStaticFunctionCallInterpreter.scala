/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * The `InterproceduralStaticFunctionCallInterpreter` is responsible for processing
 * [[StaticFunctionCall]]s in an interprocedural fashion.
 * <p>
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralStaticFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        callees:     Callees
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = StaticFunctionCall[V]

    /**
     * This function always returns a list with a single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]], and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): ProperPropertyComputationResult =
        Result(instr, StringConstancyProperty.lowerBound)

}
