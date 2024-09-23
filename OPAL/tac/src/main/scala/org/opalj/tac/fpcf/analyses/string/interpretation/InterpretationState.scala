/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.fpcf.EOptionP
import org.opalj.tac.fpcf.properties.TACAI

/**
 * The state for the FPCF analysis responsible for interpreting the statement at the given PC of the given method and
 * obtaining its string flow information.
 *
 * @see [[InterpretationHandler]], [[StringInterpreter]]
 *
 * @param pc The PC of the statement under analysis.
 * @param dm The method of the statement under analysis.
 * @param tacDependee The initial TACAI dependee of the method under analysis.
 */
case class InterpretationState(pc: Int, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }
}
