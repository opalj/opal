/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj.concurrent

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._

/**
 * Executes the given function `process` for each submitted task; each task can add
 * new tasks.
 * @example
 * {{{
 * val tasks = new Tasks[T] { (tasks : Tasks[T], t : T) ⇒
 * 			// do something with t
 * 			if (<some condition>) { tasks.submit(nextT) }
 * 		}
 * val exceptions = tasks.join()
 * }}}
 * @author Michael Eichberg
 */
class Tasks[T](
        val process:   (Tasks[T], T) ⇒ Unit,
        isInterrupted: () ⇒ Boolean         = () ⇒ Thread.currentThread().isInterrupted()
)(
        implicit
        val executionContext: ExecutionContext
) {

    private[this] val latch = new CountDownLatch(1)
    private[this] val tasksCount = new AtomicInteger(0)
    private[this] var exceptions = new ConcurrentLinkedQueue[Throwable]()

    def submit(t: T): Unit = {
        if (isInterrupted())
            return ;

        tasksCount.incrementAndGet()
        val future = Future[Unit] { process(this, t) }(executionContext)
        future.onComplete { result ⇒
            // the workQueue may contain one to many new entries to work on
            if (result.isFailure) {
                val Failure(exception) = result
                exceptions.add(exception)
            }
            if (tasksCount.decrementAndGet() == 0) {
                latch.countDown()
            }
        }(executionContext)
    }

    def join(): List[Throwable] = {

        if (tasksCount.get != 0) {
            // if tasksCount is zero we may already be finished or there were never any tasks...
            latch.await()
        }

        if (!exceptions.isEmpty)
            exceptions.asScala.toList
        else
            Nil
    }
}
/**
 * Factory to create [[Tasks]] objects to process value oriented tasks.
 *
 * @author Michael Eichber
 */
object Tasks {

    def apply[T](
        process:       (Tasks[T], T) ⇒ Unit,
        isInterrupted: () ⇒ Boolean         = () ⇒ Thread.currentThread().isInterrupted()
    )(
        implicit
        executionContext: ExecutionContext
    ): Tasks[T] = {
        new Tasks[T](process, isInterrupted)(executionContext)
    }

}
