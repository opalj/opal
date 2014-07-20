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

/**
 * A abstract interpreter that interrupts itself after some configurable
 * ([[maxEffortInNs]]) time has passed.
 *
 * @param maxEffortInNs  The number of nanoseconds after which the abstract 
 *      interpretation is aborted. The default value is 150 milliseconds.
 *
 * @author Michael Eichberg
 */
class SelfTerminatingAI[D <: Domain](
        val maxEffortInNs: Long = 150l /*ms*/ * 1000l * 1000l) extends AI[D] {

    private[this] var evaluationCount = -1

    private[this] var startTime: Long = _

    private[this] var interrupted: Boolean = false

    private[this] var interruptTime: Long = 0

    def abortedAfter = interruptTime - startTime

    override def isInterrupted =
        interrupted || {
            evaluationCount += 1
            if (evaluationCount == 0) {
                startTime = System.nanoTime()
                false
            } else if (evaluationCount % 100 == 0 &&
                (System.nanoTime() - startTime) > maxEffortInNs) {
                interrupted = true
                interruptTime = System.nanoTime()
                true
            } else {
                false
            }
        }

}
