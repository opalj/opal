/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.Method
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * Encapsulates the state of a TAC-based alias analysis.
 *
 * It additionally contains the TACAI for the first and second alias source element if they are method bound.
 */
trait TacBasedAliasAnalysisState extends AliasAnalysisState {

    private[this] var _tacai1: Option[TACode[TACMethodParameter, V]] = None
    private[this] var _tacai2: Option[TACode[TACMethodParameter, V]] = None

    private[this] var _tacEPSToMethod: Map[SomeEPS, Method] = Map()

    /**
     * Updates the TACAI for the given method.
     */
    private[alias] def updateTACAI(
        m:     Method,
        tacai: TACode[TACMethodParameter, V]
    )(implicit context: AliasAnalysisContext): Unit = {

        var anyMatch: Boolean = false

        if (context.element1.isMethodBound && m.equals(context.element1.method)) {
            _tacai1 = Some(tacai)
            anyMatch = true
        }

        if (context.element2.isMethodBound && m.equals(context.element2.method)) {
            _tacai2 = Some(tacai)
            anyMatch = true
        }

        if (!anyMatch) throw new IllegalArgumentException("Method not found")
    }

    /**
     * Returns the TACAI for the first alias source element.
     *
     * If the element is not method bound, `None` is returned.
     *
     * @return The TACAI for the first alias source element.
     */
    def tacai1: Option[TACode[TACMethodParameter, V]] = _tacai1

    /**
     * Returns the TACAI for the second alias source element.
     *
     * If the element is not method bound, `None` is returned.
     *
     * @return The TACAI for the second alias source element.
     */
    def tacai2: Option[TACode[TACMethodParameter, V]] = _tacai2

    /**
     * Associates the given TAC EPS with the given method.
     */
    def addTacEPSToMethod(eps: SomeEPS, m: Method): Unit = {
        _tacEPSToMethod += (eps -> m)
    }

    /**
     * Returns the method that is represented by the given TAC EPS.
     */
    def getMethodForTacEPS(eps: SomeEPS): Method = {
        _tacEPSToMethod(eps)
    }

}
