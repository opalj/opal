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
 * the given number of instructions or if the callback function `doInterrupt` returns
 * `false`.
 *
 * @param maxEvaluationCount The maximum number of instructions after which the
 *      abstract interpretation is aborted. In general this number should be calculated
 *      based on the method's/project's properties. To calculate it based on the properties
 *      of a method, it is possible to use the predefined function
 *      [[InstructionCountBoundedAI#calculateMaxEvaluationCount]].
 * @param doInterrupt This function is called by the abstract interpreter to check if
 *      the abstract interpretation should be aborted. Given that this function is called
 *      very often, it is important that it is efficient.
 *
 * @author Michael Eichberg
 */
class BoundedInterruptableAI[D <: Domain](
    maxEvaluationCount: Int,
    maxEvaluationTimeInNS: Long,
    val doInterrupt: () ⇒ Boolean)
        extends InstructionCountBoundedAI[D](maxEvaluationCount) {

    private[this] var startTime: Long = -1l;

    def this(
        code: Code,
        maxEvaluationFactor: Double,
        maxEvaluationTimeInMS: Int,
        doInterrupt: () ⇒ Boolean) =
        this(
            InstructionCountBoundedAI.calculateMaxEvaluationCount(code, maxEvaluationFactor),
            maxEvaluationTimeInMS * 1000000l,
            doInterrupt)

    override def isInterrupted: Boolean = {
        if (super.isInterrupted || doInterrupt())
            return true

        val startTime = this.startTime
        if (startTime == -1l) {
            this.startTime = System.nanoTime()
            false
        } else if (super.currentEvaluationCount % 1000 == 0) {
            val elapsedTime = System.nanoTime() - startTime
            elapsedTime > maxEvaluationTimeInNS
        } else
            false
    }

}
