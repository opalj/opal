/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.util.perf

import scala.collection.mutable.Map

/**
 * Prints the result of the runtime performance evaluation of some function to
 * the command line. This trait can be used as a very simple drop-in replacement
 *
 * @author Michael Eichberg
 */
trait ToCommandLinePerformanceEvaluation extends PerformanceEvaluation {

    def time[T](s: String)(f: ⇒ T): T = {
        time(duration ⇒ println(s + ": " + nanoSecondsToMilliseconds(duration) + "msecs."))(f)
    }

    private[this] val aggregatedTimes: Map[Symbol, Long] = Map()

    def aggregateTimes[T](s: Symbol)(f: ⇒ T): T = {
        val startTime = System.nanoTime
        val result = f
        val endTime = System.nanoTime

        val old = aggregatedTimes.getOrElseUpdate(s, 0l)
        aggregatedTimes.update(s, old + (endTime - startTime))

        result
    }

    def printAggregatedTimes(sym: Symbol, s: String) {
        println(s + ": " + nanoSecondsToSeconds(aggregatedTimes.getOrElseUpdate(sym, 0l)).toString + "secs.")
    }

    def resetAggregatedTimes(s: Symbol) {
        aggregatedTimes.remove(s)
    }

    def resetAll() {
        aggregatedTimes.clear()
    }
}