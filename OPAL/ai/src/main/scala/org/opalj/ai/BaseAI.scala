/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * A base abstract interpreter that can be used with any domain that has
 * no special requirements on the abstract interpreter. The base
 * interpreter can be interrupted by calling the `interrupt` method of the
 * AI's thread.
 *
 * @see [[BoundedInterruptableAI]] for an abstract interpreter that can easily be
 *      interrupted and which also interrupts itself if a certain threshold is exceeded.
 *
 * @author Michael Eichberg
 */
class BaseAI(
        IdentifyDeadVariables:           Boolean = true,
        RegisterStoreMayThrowExceptions: Boolean = false
)
    extends AI[Domain](IdentifyDeadVariables, RegisterStoreMayThrowExceptions) {

    override def isInterrupted: Boolean = Thread.interrupted()

}

/**
 * Instance of the base abstract interpreter.
 */
object BaseAI extends BaseAI(true, false)
