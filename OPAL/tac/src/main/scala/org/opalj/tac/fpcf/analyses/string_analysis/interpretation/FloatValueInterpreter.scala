/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[FloatConst]]s.
 *
 * @author Maximilian Rüsch
 */
object FloatValueInterpreter extends StringInterpreter[Nothing] {

    override type T = FloatConst

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
