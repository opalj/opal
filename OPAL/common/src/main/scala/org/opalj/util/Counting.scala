/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.concurrent.Locking

import scala.collection.mutable

/**
 * A simple class that enable the counting of something.
 *
 * Examples:
 *   - How often is a function called during runtime?
 *   - How often runs the program in a specific case?
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Reif
 */
trait Counting extends Locking {

    private[this] val count = mutable.Map.empty[Symbol, Long]

    final def incrementCount(s: Symbol): Unit = {
        withWriteLock { updateCount(s, 1L) }
    }

    /**
     * Updates the count related to the entity identified by the given symbol.
     *
     * If the passed `value` is positive the count will be increased whereas it will be
     * decreased when a negative number is passed.
     *
     * @param   s Symbol used to correlate values related to the same entity.
     * @param   value The value that will be added to the entity's current value.
     */
    final def updateCount(s: Symbol, value: Long): Unit = {
        withWriteLock { doUpdateCount(s, value) }
    }

    /**
     * Called by the `updateCount(Symbol, Int)` method.
     *
     * ==Thread Safety==
     * The `updateCount` method takes care of the synchronization.
     */
    protected[this] def doUpdateCount(s: Symbol, value: Long): Unit = {
        val oldValue = count.getOrElseUpdate(s, 0L)
        count.update(s, oldValue + value)
    }

    /**
     * Returns the overall `count` that has been summed up with the given symbol `s`.
     */
    def getCount(s: Symbol): Long = withReadLock { doGetCount(s) }

    /**
     * Called by the `getCount(Symbol)` method.
     *
     * ==Thread Safety==
     * The `getCount` method takes care of the synchronization.
     */
    protected[this] def doGetCount(s: Symbol): Long = count.getOrElse(s, 0L)

    /**
     * Resets the overall count of the given symbol.
     */
    def reset(s: Symbol): Unit = withWriteLock { doReset(s) }

    /**
     * Called by the `reset(Symbol)` method.
     *
     * ==Thread Safety==
     * The `reset` method takes care of the synchronization.
     */
    private[this] def doReset(s: Symbol): Unit = count.remove(s)

    /**
     * Resets everything. The effect is comparable to creating a new
     * instance, but is more efficient.
     */
    def resetAll(): Unit = withWriteLock { doResetAll() }

    /**
     * Called by the `resetAll` method.
     *
     * ==Thread Safety==
     * The `resetAll` method takes care of the synchronization.
     */
    private[this] def doResetAll(): Unit = count.clear()

}

class IntStatistics extends Counting
