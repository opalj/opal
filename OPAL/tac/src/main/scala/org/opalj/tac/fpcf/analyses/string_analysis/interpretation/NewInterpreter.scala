/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP

/**
 * Responsible for processing [[New]] expressions.
 *
 * @author Maximilian Rüsch
 */
object NewInterpreter extends StringInterpreter[Nothing] {

    override type T = New

    /**
     * [[New]] expressions do not carry any relevant information in this context (as the initial values are not set in
     * [[New]] expressions but, e.g., in [[org.opalj.tac.NonVirtualMethodCall]]s). Consequently, this implementation
     * always returns a [[StringConstancyProperty.getNeutralElement]].
     */
    def interpret(instr: T): FinalEP[T, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.getNeutralElement)
}
