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
package org
package opalj
package concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.{Future ⇒ JFuture}
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong

/**
 * A ThreadPool that knows the `ThreadGroup` associated with its threads and that
 * catches exceptions if a thread crashes and reports them using the OPALLogger
 * facility.
 *
 * @author Michael Eichberg
 */
class OPALThreadPoolExecutor(
        n:         Int,
        val group: ThreadGroup
) extends ThreadPoolExecutor(
    n, n,
    60L, TimeUnit.SECONDS, // this is a fixed size pool
    new LinkedBlockingQueue[Runnable](),
    new ThreadFactory {

        val nextID = new AtomicLong(0L)

        def newThread(r: Runnable): Thread = {
            val id = s"${nextID.incrementAndGet()}"
            val name = s"org.opalj.ThreadPool[N=$n]-Thread $id"
            val t = new Thread(group, r, name)
            // we are using demon threads to make sure that these
            // threads never prevent the JVM from regular termination
            t.setDaemon(true)
            t.setUncaughtExceptionHandler(UncaughtExceptionHandler)
            t
        }
    }
) {
    override def afterExecute(r: Runnable, t: Throwable): Unit = {
        super.afterExecute(r, t)
        var e = t
        if (e == null && r.isInstanceOf[JFuture[_]]) {
            try {
                r.asInstanceOf[JFuture[_]].get()
            } catch {
                case ce: CancellationException ⇒
                    e = ce
                case ee: ExecutionException ⇒
                    e = ee.getCause()
                case ie: InterruptedException ⇒
                    e = ie
                    Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (e ne null) {
            handleUncaughtException(e)
        }
    }
}
