/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.FloatConst
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * The `FloatValueInterpreter` is responsible for processing [[FloatConst]]s.
 * <p>
 * For this implementation, the concrete implementation passed for [[exprHandler]] is not relevant.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class FloatValueInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = FloatConst

    /**
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty(StringConstancyInformation(
            StringConstancyLevel.CONSTANT,
            StringConstancyType.APPEND,
            instr.value.toString
        )))

}