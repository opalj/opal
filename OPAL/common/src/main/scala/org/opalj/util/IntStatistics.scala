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
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Reif
 */
class IntStatistics extends Locking {

    private[this] val count = mutable.Map.empty[Symbol, Int]

    final def increase(s: Symbol, value: Int): Unit = {
        withWriteLock { doUpdateCounts(s, value) }
    }

    protected[this] def doUpdateCounts(s: Symbol, value: Int): Unit = {
        val oldValue = count.getOrElseUpdate(s, 0)
        count.update(s, oldValue + value)
    }

    def getCount(s: Symbol): Int = withReadLock { doGetCount(s) }

    protected[this] def doGetCount(s: Symbol): Int = count.getOrElse(s, 0)

    final def reset(s: Symbol): Unit = withWriteLock { doReset(s) }

    protected[this] def doReset(s: Symbol): Unit = count.remove(s)

    final def resetAll(): Unit = withWriteLock { doResetAll() }

    protected[this] def doResetAll(): Unit = count.clear()
}

object GlobalIntStatistics extends IntStatistics