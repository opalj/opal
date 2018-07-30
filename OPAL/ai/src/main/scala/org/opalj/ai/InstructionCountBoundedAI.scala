/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.Code

/**
 * An abstract interpreter that interrupts itself after the evaluation of
 * the given number of instructions.
 *
 * ==Thread Safety==
 * This class is thread-safe. I.e., one instance of the InstructionCountBoundedAI can
 * be used to run multiple abstract interpretations in parallel and to ensure that they
 * terminate (as a whole) if the threshold is exceeded.
 *
 * @param maxEvaluationCount Determines the maximum number of instructions that should
 *      be interpreted. If the interpretation did not finish before that, the abstract
 *      interpretation is aborted. An aborted abstract interpretation can be continued
 *      later on using the `continueInterpretation` method of the [[AIAborted]] object.
 *      In general, it makes sense to determine this value based on the complexity of
 *      the code as it is done by [[InstructionCountBoundedAI.calculateMaxEvaluationCount]].
 *
 * @author Michael Eichberg
 */
class InstructionCountBoundedAI[D <: Domain](
        val maxEvaluationCount: Int,
        IdentifyDeadVariables:  Boolean
) extends AI[D](IdentifyDeadVariables) {

    /**
     * @param maxEvaluationFactor Determines the maximum number of instruction evaluations
     *      before the evaluation of the method is automatically interrupted.
     */
    def this(
        code:                  Code,
        maxEvaluationFactor:   Double  = 1.5d,
        identifyDeadVariables: Boolean = true
    )(implicit logContext: LogContext) = {
        this(
            InstructionCountBoundedAI.calculateMaxEvaluationCount(code, maxEvaluationFactor),
            identifyDeadVariables
        )
    }

    assert(maxEvaluationCount > 0)

    private[this] val evaluationCount = new java.util.concurrent.atomic.AtomicInteger(0)

    def currentEvaluationCount: Int = evaluationCount.get

    override def isInterrupted = {
        var count = evaluationCount.get()
        var newCount = count + 1
        while (count < maxEvaluationCount && !evaluationCount.compareAndSet(count, newCount)) {
            count = evaluationCount.get()
            newCount = count + 1
        }
        count >= maxEvaluationCount
    }

}
/**
 * Defines common helper methods.
 *
 * @author Michael Eichberg
 */
object InstructionCountBoundedAI {

    /**
     * Calculates a meaningful upper bound for the number of instruction evaluation
     * that should suffice for evaluating the code. However, this is only a
     * heuristics which may fail for methods with a certain (hidden) complexity.
     */
    def calculateMaxEvaluationCount(
        code:                Code,
        maxEvaluationFactor: Double
    )(
        implicit
        logContext: LogContext
    ): Int = {
        if (maxEvaluationFactor == Double.PositiveInfinity)
            return Int.MaxValue

        // If this method is just a convenience wrapper we want to ensure that
        // we can still analyze the called methods if we also analyze the called
        // methods.
        val instructionsSize = Math.max(code.instructions.size, 100).toDouble
        // this is roughly the number of instructions * ~2
        var upperBound: Double = instructionsSize

        // to accommodate for the reduced complexity of long methods
        upperBound = upperBound * Math.min(48, Math.pow(65535d / upperBound, 2d / 3d))

        // exception handling usually leads to a large number of evaluations
        upperBound = upperBound * Math.log(code.exceptionHandlers.size + 2 * Math.E)

        // to accommodate for analysis specific factors
        upperBound = (
            upperBound * maxEvaluationFactor +
            // we want to guarantee a certain minimum length if we raise the
            // evaluation factor
            (maxEvaluationFactor * 250.0d)
        )
        if (upperBound == java.lang.Double.POSITIVE_INFINITY ||
            upperBound >= Int.MaxValue.toDouble) {
            upperBound = Int.MaxValue
            OPALLogger.warn(
                "analysis configuration",
                "effectively unbounded evaluation"+
                    "; instructions size="+code.instructions.size+
                    "; exception handlers="+code.exceptionHandlers.size+
                    "; maxEvaluationFactor="+maxEvaluationFactor
            )
        }

        if (upperBound > 1000000.0d) {
            OPALLogger.warn(
                "analysis configuration",
                "evaluation (up to: "+upperBound.toInt+
                    " instructions) may take execessively long"+
                    "; instructions size="+code.instructions.size+
                    "; exception handlers="+code.exceptionHandlers.size+
                    "; maxEvaluationFactor="+maxEvaluationFactor
            )
        }

        Math.max(instructionsSize, upperBound).toInt
    }

}
