/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * The `BinaryExprInterpreter` is responsible for processing [[BinaryExpr]]ions. A list of currently
 * supported binary expressions can be found in the documentation of [[interpret]].
 * <p>
 * For this interpreter, it is of no relevance what concrete implementation of
 * [[InterpretationHandler]] is passed.
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
     * For all other expressions, a result containing [[StringConstancyProperty.getNeutralElement]]
     * will be returned.
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val sci = instr.cTpe match {
            case ComputationalTypeInt   ⇒ InterpretationHandler.getConstancyInfoForDynamicInt
            case ComputationalTypeFloat ⇒ InterpretationHandler.getConstancyInfoForDynamicFloat
            case _                      ⇒ StringConstancyInformation.getNeutralElement
        }
        FinalEP(instr, StringConstancyProperty(sci))
    }

}
