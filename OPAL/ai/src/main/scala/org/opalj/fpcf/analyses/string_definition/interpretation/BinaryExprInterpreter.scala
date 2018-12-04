/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.TACStmts
import org.opalj.br.cfg.CFG
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.tac.Stmt
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.BinaryExpr

/**
 * The `BinaryExprInterpreter` is responsible for processing [[BinaryExpr]]ions. A list of currently
 * supported binary expressions can be found in the documentation of [[interpret]].
 *
 * @see [[AbstractStringInterpreter]]
 * @author Patrick Mell
 */
class BinaryExprInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = BinaryExpr[V]

    /**
     * Currently, this implementation supports the interpretation of the following binary
     * expressions:
     * <ul>
     * <li>[[ComputationalTypeInt]]
     * <li>[[ComputationalTypeFloat]]</li>
     * </li>
     * To be more precise, that means that a list with one element will be returned. In all other
     * cases, an empty list will be returned.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] =
        instr.cTpe match {
            case ComputationalTypeInt ⇒
                List(InterpretationHandler.getStringConstancyInformationForInt)
            case ComputationalTypeFloat ⇒
                List(InterpretationHandler.getStringConstancyInformationForFloat)

            case _ ⇒ List()
        }

}
