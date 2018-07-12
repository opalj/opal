/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.util.Milliseconds
import org.opalj.util.Nanoseconds

/**
 * An abstract interpreter that interrupts itself after some configurable
 * ([[maxEffort]]) time has passed.
 *
 * @param maxEffort  The number of nanoseconds after which the abstract
 *      interpretation is aborted. The default value is 150 milliseconds.
 *
 * @author Michael Eichberg
 */
class TimeBoundedAI[D <: Domain](
        val maxEffort: Nanoseconds = new Milliseconds(150L).toNanoseconds
) extends AI[D] {

    private[this] final val CheckInterval = 100

    private[this] var evaluationCount = -1

    private[this] var startTime: Long = _

    private[this] var interrupted: Boolean = false

    private[this] var interruptTime: Long = 0

    def abortedAfter: Nanoseconds = new Nanoseconds(interruptTime - startTime)

    // This method is only intended to be called during the Abstract Interpretation as
    // each call increases the evaluation count!
    override protected def isInterrupted: Boolean = interrupted || {
        evaluationCount += 1
        if (evaluationCount == 0) {
            startTime = System.nanoTime()
            false
        } else if (evaluationCount % CheckInterval == 0 &&
            (System.nanoTime() - startTime) > maxEffort.timeSpan) {
            interrupted = true
            interruptTime = System.nanoTime()
            true
        } else {
            false
        }
    }
}
