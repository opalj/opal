/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[IntConst]]s.
 *
 * @author Maximilian Rüsch
 */
case class IntegerValueInterpreter(
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: InterpretationHandler
) extends StringInterpreter {

    override type T = IntConst

    def interpret(instr: T): FinalEP[T, StringConstancyProperty] =
        FinalEP(
            instr,
            StringConstancyProperty(StringConstancyInformation(
                StringConstancyLevel.CONSTANT,
                StringConstancyType.APPEND,
                instr.value.toString
            ))
        )
}
