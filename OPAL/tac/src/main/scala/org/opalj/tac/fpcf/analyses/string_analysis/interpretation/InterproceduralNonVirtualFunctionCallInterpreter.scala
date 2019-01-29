/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * The `InterproceduralNonVirtualFunctionCallInterpreter` is responsible for processing
 * [[NonVirtualFunctionCall]]s in an interprocedural fashion.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralNonVirtualFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        callees:     Callees
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualFunctionCall[V]

    /**
     * Currently, [[NonVirtualFunctionCall]]s are not supported. Thus, this function always returns
     * a list with a single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]] and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): ProperPropertyComputationResult =
        Result(instr, StringConstancyProperty.lowerBound)

}
