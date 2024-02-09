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
import org.opalj.tac.StringConst

/**
 * Responsible for processing [[StringConst]]s.
 *
 * @author Maximilian Rüsch
 */
object StringConstInterpreter extends StringInterpreter[Nothing] {

    override type T = StringConst

    /**
     * Always returns a list with one [[StringConstancyLevel.CONSTANT]] [[StringConstancyInformation]] element holding
     * the string const value.
     */
    def interpret(instr: T): FinalEP[T, StringConstancyProperty] =
        FinalEP(
            instr,
            StringConstancyProperty(StringConstancyInformation(
                StringConstancyLevel.CONSTANT,
                StringConstancyType.APPEND,
                instr.value
            ))
        )
}
