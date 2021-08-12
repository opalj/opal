/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.{Future => JFuture}
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong

/**
 * A ThreadPool that knows the `ThreadGroup` associated with its threads and that catches
 * exceptions if a thread crashes and reports them using the OPALLogger facility.
 *
 * If the root cause of the exception should be related to the OPALLogger then the error
 * is written to `System.err`.
 *
 * The pool uses demon threads to make sure that these threads never prevent the JVM from
 * regular termination.
 *
 * @author Michael Eichberg
 */
class OPALBoundedThreadPoolExecutor(
        n:         Int,
        val group: ThreadGroup
) extends ThreadPoolExecutor(
    n, n,
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable](), // this is a fixed size pool
    new ThreadFactory {

        val nextID = new AtomicLong(0L)

        def newThread(r: Runnable): Thread = {
            val id = s"${nextID.incrementAndGet()}"
            val name = group.getName + s" - Thread $id"
            val t = new Thread(group, r, name)
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
                case ce: CancellationException =>
                    e = ce
                case ee: ExecutionException =>
                    e = ee.getCause()
                case ie: InterruptedException =>
                    e = ie
                    Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (e ne null) {
            handleUncaughtException(e)
        }
    }

}
