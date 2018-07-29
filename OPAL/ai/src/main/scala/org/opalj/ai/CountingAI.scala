/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.util.concurrent.atomic.AtomicLong

/**
 * An abstract interpreter that counts the number of instruction evaluations that
 * are performed. This is particularly helpful to determine the effect of optimizations
 * or the choice of the domain.
 *
 * ==Thread Safety==
 * This class is thread-safe. I.e., one instance can be used to run multiple abstract
 * interpretations in parallel.
 *
 * @author Michael Eichberg
 */
class CountingAI[D <: Domain] extends InterruptableAI[D] {

    private[this] val evaluationCount = new AtomicLong(0)

    def currentEvaluationCount: Long = evaluationCount.get

    override def isInterrupted = {
        evaluationCount.incrementAndGet()
        super.isInterrupted
    }

}
