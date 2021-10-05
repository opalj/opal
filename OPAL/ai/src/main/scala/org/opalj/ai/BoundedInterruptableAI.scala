/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.Code
import org.opalj.log.LogContext
import org.opalj.util.Nanoseconds
import org.opalj.util.Milliseconds

/**
 * An abstract interpreter that interrupts itself after the evaluation of
 * the given number of instructions or if the callback function `doInterrupt` returns
 * `false` or if the maximum allowed time is exceeded.
 *
 * @param maxEvaluationCount See [[InstructionCountBoundedAI.maxEvaluationCount]].
 *
 * @param maxEvaluationTime The maximum number of nanoseconds the abstract interpreter
 *      is allowed to run. It starts with the evaluation of the first instruction.
 *
 * @param doInterrupt This function is called by the abstract interpreter to check if
 *      the abstract interpretation should be aborted. Given that this function is called
 *      very often (before the evaluation of each instruction), it is important that it
 *      is efficient.
 *
 * @author Michael Eichberg
 */
class BoundedInterruptableAI[D <: Domain](
        maxEvaluationCount:    Int,
        val maxEvaluationTime: Nanoseconds,
        val doInterrupt:       () => Boolean,
        IdentifyDeadVariables: Boolean
) extends InstructionCountBoundedAI[D](maxEvaluationCount, IdentifyDeadVariables) {

    private[this] var startTime: Long = -1L;

    def this(
        code:                  Code,
        maxEvaluationFactor:   Double,
        maxEvaluationTime:     Milliseconds,
        doInterrupt:           () => Boolean,
        identifyDeadVariables: Boolean       = true
    )(
        implicit
        logContext: LogContext
    ) = {
        this(
            InstructionCountBoundedAI.calculateMaxEvaluationCount(code, maxEvaluationFactor),
            maxEvaluationTime.toNanoseconds,
            doInterrupt,
            identifyDeadVariables
        )
    }

    override def isInterrupted: Boolean = {
        if (super.isInterrupted || doInterrupt())
            return true;

        val startTime = this.startTime
        if (startTime == -1L) {
            this.startTime = System.nanoTime()
            false
        } else if (super.currentEvaluationCount % 1000 == 0) {
            val elapsedTime = System.nanoTime() - startTime
            elapsedTime > maxEvaluationTime.timeSpan
        } else
            false
    }

}
