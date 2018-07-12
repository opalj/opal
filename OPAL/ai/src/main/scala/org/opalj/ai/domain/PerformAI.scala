/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * A base abstract interpreter that can be used with any domain that has
 * no special requirements on the abstract interpreter and which provides some convenience
 * factory methods to run the abstract interpretation if the domain provide the necessary
 * information.
 *
 * The base interpreter can be interrupted by calling the `interrupt` method of the
 * AI's thread.
 *
 * @see [[BoundedInterruptableAI]] for an abstract interpreter that can easily be
 *      interrupted and which also interrupts itself if a certain threshold is exceeded.
 *
 * @author Michael Eichberg
 */
object PerformAI extends BaseAI {

    override def isInterrupted = Thread.interrupted()

    def apply(d: TheProject with TheMethod with Domain): AIResult { val domain: d.type } = {
        val m = d.method
        apply(m, d)
    }

}
