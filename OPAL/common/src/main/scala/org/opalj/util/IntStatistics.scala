/*
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
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

import scala.collection.mutable
import org.opalj.concurrent.Locking

/**
 *
 * Enable simple counting statistics, e.g. how often a specific case occurs during runtime.
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Reif
 */
class IntStatistics extends Locking {

    private[this] val count = mutable.Map.empty[Symbol, Int]

    /**
     * Increases or decreases the count of the current statistics which is defined over the passed `symbol`.
     * If the passed `value` is positive the count will be increases whereas it will decreases when a negative number is passed.
     *
     * @param s Symbol used to put multiple statistics into relation.
     * @param value The value that will be added to the statistics. A negative number will reduce the current count.
     */
    final def updateStatistics(s: Symbol, value: Int): Unit = {
        withWriteLock { doUpdateStatistics(s, value) }
    }

    /**
     * Called by the `updateStatistics(Symbol, Int)` method.
     *
     * ==Thread Safety==
     * The `updateStatistics` method takes care of the synchronization.
     */
    protected[this] def doUpdateStatistics(s: Symbol, value: Int): Unit = {
        val oldValue = count.getOrElseUpdate(s, 0)
        count.update(s, oldValue + value)
    }

    /**
     * Returns the overall count that has been summed up with the given symbol `s`.
     */
    def getCount(s: Symbol): Int = withReadLock { doGetCount(s) }

    /**
     * Called by the `getCount(Symbol)` method.
     *
     * ==Thread Safety==
     * The `getTime` method takes care of the synchronization.
     */
    protected[this] def doGetCount(s: Symbol): Int = count.getOrElse(s, 0)

    /**
     * Resets the overall count of the given symbol.
     */
    final def reset(s: Symbol): Unit = withWriteLock { doReset(s) }

    /**
     * Called by the `reset(Symbol)` method.
     *
     * ==Thread Safety==
     * The `reset` method takes care of the synchronization.
     */
    protected[this] def doReset(s: Symbol): Unit = count.remove(s)

    /**
     * Resets everything. The effect is comparable to creating a new
     * `IntStatistics` object, but is a bit more efficient.
     */
    final def resetAll(): Unit = withWriteLock { doResetAll() }

    /**
     * Called by the `resetAll` method.
     *
     * ==Thread Safety==
     * The `resetAll` method takes care of the synchronization.
     */
    protected[this] def doResetAll(): Unit = count.clear()
}

object GlobalIntStatistics extends IntStatistics