/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai

import org.opalj.br.Code

/**
 * An abstract interpreter that interrupts itself after the evaluation of
 * the given number of instructions.
 *
 * ==Thread Safety==
 * This class is thread-safe. I.e., one instance of the InstructionCountBoundedAI can
 * be used to run multiple abstract interpretations in parallel and to ensure that they
 * terminate (as a whole) if the set threshold is exceeded.
 *
 * @author Michael Eichberg
 */
class InstructionCountBoundedAI[D <: Domain](val maxEvaluationCount: Int) extends AI[D] {

    require(maxEvaluationCount > 0)

    /**
     * @param maxEvaluationFactor Determines the maximum number of instruction evaluations
     * before the evaluation of the method is automatically interrupted.
     */
    def this(code: Code, maxEvaluationFactor: Double = 1.5d) = {

        this(InstructionCountBoundedAI.calculateMaxEvaluationCount(code, maxEvaluationFactor))
    }

    private[this] val evaluationCount = new java.util.concurrent.atomic.AtomicInteger(0)

    def currentEvaluationCount: Int = evaluationCount.get

    override def isInterrupted = {

        var count = evaluationCount.get()
        var newCount = count + 1
        while (count < maxEvaluationCount &&
            !evaluationCount.compareAndSet(count, newCount)) {
            count = evaluationCount.get()
            newCount = count + 1
        }
        count >= maxEvaluationCount
    }

}

object InstructionCountBoundedAI {

    def calculateMaxEvaluationCount(
        code: Code,
        maxEvaluationFactor: Double): Int = {
        val min = code.instructions.size
        // this is roughly the number of instructions * ~2
        var upperBound: Double = min

        // to accommodate for the reduced complexity of long methods
        upperBound = upperBound * Math.min(48, Math.pow(65535 / upperBound, 2d / 3d))

        // exception handling usually leads to a large number of evaluations
        upperBound = upperBound * Math.log(code.exceptionHandlers.size + 2 * Math.E)

        // to accommodate for analysis specific factors
        upperBound = (upperBound * maxEvaluationFactor)
        if (upperBound < 0.0) {
            upperBound = Int.MaxValue
            println(Console.YELLOW+"[warn] effectively unbounded evaluation"+
                "; instructions size="+code.instructions.size+
                "; exception handlers="+code.exceptionHandlers.size+
                "; maxEvaluationFactor="+maxEvaluationFactor + Console.RESET)
        }

        if (upperBound > 65535.0 /*Max Length*/ * 10.0) {
            println(Console.YELLOW+
                "[warn] evaluation (up to: "+upperBound.toInt+
                " instructions) may take execessively long"+
                "; instructions size="+code.instructions.size+
                "; exception handlers="+code.exceptionHandlers.size+
                "; maxEvaluationFactor="+maxEvaluationFactor + Console.RESET)
        }

        Math.max(min, upperBound).toInt
    }

}
