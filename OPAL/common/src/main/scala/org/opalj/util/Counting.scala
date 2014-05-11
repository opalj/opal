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
package util

/**
 * Counts how often some piece of code is executed. Usually it is sufficient
 * to create an instance of this object and to execute some piece of code using
 * the function `time(Symbol,=>T)`. Afterwards it is possible to query this object
 * to get detailed information: (1) how often the function given to `time` was evaluated
 * and (2) about the accumulated time.
 *
 * @author Michael Eichberg
 */
class Counting extends PerformanceEvaluation {

    import scala.collection.mutable.Map

    private[this] val count: Map[Symbol, Int] = Map.empty

    /**
     * Times and counts the execution of `f` and associates the information with the
     * given symbol `s`.
     */
    override protected[this] def doUpdateTimes(s: Symbol, duration: Long): Unit = {
        super.doUpdateTimes(s, duration)
        count.update(s, count.getOrElseUpdate(s, 0) + 1)
    }

    override protected[this] def doReset(sym: Symbol): Unit = {
        super.reset(sym)
        count.update(sym, 0)
    }

    override protected[this] def doResetAll(): Unit = {
        super.resetAll()
        count.clear()
    }

    /**
     * Returns how often some function `f` that was tagged using the given symbol
     * was executed.
     */
    def getCount(s: Symbol): Int = withReadLock { doGetCount(s) }

    protected[this] def doGetCount(s: Symbol): Int = count.getOrElse(s, 0)

}