/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[FieldRead]]s. Currently, there is no support for fields, i.e., they are not analyzed but
 * a constant [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation]] is returned.
 *
 * @author Maximilian Rüsch
 */
case class L0FieldReadInterpreter(
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: L0InterpretationHandler
) extends L0StringInterpreter {

    override type T = GetField[V]

    /**
     * Fields are currently unsupported, thus this function always returns [[StringConstancyProperty.lb]].
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)
}
