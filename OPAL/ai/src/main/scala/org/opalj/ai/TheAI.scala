/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Makes the instance of the abstract interpreter that performs the
 * abstract interpretation available to the domain.
 *
 * ==Usage==
 * It is sufficient to mixin this trait in a [[Domain]] that needs to access the abstract
 * interpreter. The abstract interpreter will then perform the initialization.
 *
 * The concrete instance of [[AI]] that performs the abstract interpretation is set
 * immediately before the abstract interpretation is started/continued.
 *
 * @author Michael Eichberg
 */
trait TheAI[D <: Domain] {

    private[this] var theAI: AI[D] = null

    private[ai] def setAI(ai: AI[D]): Unit = {
        this.theAI = ai
    }

    /**
     * Returns the instance of the abstract interpreter that performs the abstract
     * interpretation.
     */
    def ai: AI[D] = theAI

    def tracer: Option[AITracer] = ai.tracer

}
