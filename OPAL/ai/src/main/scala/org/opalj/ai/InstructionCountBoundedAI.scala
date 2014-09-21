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
 * @author Michael Eichberg
 */
class InstructionCountBoundedAI[D <: Domain](val maxEvaluationCount: Int) extends AI[D] {

    require(maxEvaluationCount > 0)

    /**
     * @param maxEvaluationFactor Determines the maximum number of instruction evaluations
     * before the evaluation of the method is automatically interrupted.
     */
    def this(code: Code, maxEvaluationFactor: Int) = {

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
        maxEvaluationFactor: Int): Int = {
        // this is roughly the number of instructions * ~2
        var max = code.instructions.size

        // to accommodate for the reduced complexity of long methods
        max = max * Math.min(48, Math.pow(65535 / max, 2d / 3d)).toInt

        // exception handling usually leads to a large number of evaluations
        max = max * Math.log(code.exceptionHandlers.size + 2 * Math.E).toInt

        // to accommodate for analysis specific factors
        max = max * maxEvaluationFactor
        if (max < 0) {
            max = Int.MaxValue
            println(Console.YELLOW+"[warn] effectively unbounded evaluation"+
                "; instructions size="+code.instructions.size+
                "; exception handlers="+code.exceptionHandlers.size+
                "; maxEvaluationFactor="+maxEvaluationFactor + Console.RESET)
        }
        if (max > 65535 /*Max Length*/ * 10) {
            println(Console.YELLOW+"[warn] evaluation (up to: "+max+" instructions) may take execessively long"+
                "; instructions size="+code.instructions.size+
                "; exception handlers="+code.exceptionHandlers.size+
                "; maxEvaluationFactor="+maxEvaluationFactor + Console.RESET)
        }
        max
    }

}
