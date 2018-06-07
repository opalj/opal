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
package concurrent

// OLD import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import scala.concurrent.ExecutionContext
// OLD import scala.concurrent.Future
// OLD import scala.util.Failure

import org.opalj.concurrent.Locking.withLock

sealed trait Tasks[T] {

    /** Returns the current tasks count; primarily intended to be used for debugging purposes! */
    def currentTasksCount: Int

    def hasExceptions: Boolean

    def currentExceptions: ConcurrentExceptions

    def abortDueToExternalException(exception: Throwable): Unit

    def submit(t: T): Unit

    def join(): Unit

}

final class SequentialTasks[T](
        val process:                               (Tasks[T], T) ⇒ Unit,
        val abortOnExceptions:                     Boolean              = false,
        @volatile private[this] var isInterrupted: () ⇒ Boolean         = () ⇒ Thread.currentThread().isInterrupted()
) extends Tasks[T] {

    private[this] val tasksQueue = scala.collection.mutable.Queue.empty[T]
    private[this] var concurrentExceptions: ConcurrentExceptions = _

    def currentTasksCount: Int = tasksQueue.size

    def hasExceptions: Boolean = concurrentExceptions != null

    def currentExceptions: ConcurrentExceptions = {
        if (concurrentExceptions == null) {
            throw new IllegalStateException("no exceptions have been thrown while processing the tasks")
        } else {
            throw concurrentExceptions
        }
    }

    def abortDueToExternalException(exception: Throwable): Unit = {
        isInterrupted = () ⇒ true
        if (concurrentExceptions == null) concurrentExceptions = new ConcurrentExceptions()
        concurrentExceptions.addSuppressed(exception)
    }

    def submit(t: T): Unit = {
        if (isInterrupted())
            return ;

        tasksQueue += t
    }

    def join(): Unit = {
        while (tasksQueue.nonEmpty) {
            try {
                process(this, tasksQueue.dequeue)
            } catch {
                case t: Throwable ⇒ {
                    if (concurrentExceptions == null) concurrentExceptions = new ConcurrentExceptions()
                    concurrentExceptions.addSuppressed(t)
                    if (abortOnExceptions) {
                        this.isInterrupted = () ⇒ true
                        throw concurrentExceptions;
                    }
                }
            }
        }

        if (concurrentExceptions != null) {
            throw concurrentExceptions;
        }
    }
}

/**
 * Executes the given function `process` for each submitted value of
 * type `T`. The `process` function can add further values that should be processed.
 *
 * @example
 * {{{
 * val tasks = new Tasks[T] { (tasks : Tasks[T], t : T) ⇒
 *          // do something with t
 *          if (<some condition>) { tasks.submit(nextT) }
 *      }
 * val exceptions = tasks.join()
 * }}}
 *
 * @param     process A function that is given a value of type `T` and this instance of `Tasks`.
 *             `Tasks` can be used to submit further values of type `T`.
 *
 * @author Michael Eichberg
 */
final class ConcurrentTasks[T](
        val process:                               (Tasks[T], T) ⇒ Unit,
        val abortOnExceptions:                     Boolean              = false,
        @volatile private[this] var isInterrupted: () ⇒ Boolean         = () ⇒ Thread.currentThread().isInterrupted()
)(
        implicit
        val executionContext: ExecutionContext
) extends Tasks[T] { self ⇒

    @volatile private[this] var tasksCount = 0
    private[this] val tasksLock: ReentrantLock = new ReentrantLock()
    private[this] val isFinished: Condition = tasksLock.newCondition()
    @volatile private[this] var exceptions: ConcurrentExceptions = _ /*null*/ // lazy initialized

    def currentTasksCount: Int = tasksCount

    def hasExceptions: Boolean = exceptions != null

    def currentExceptions: ConcurrentExceptions = exceptions

    def abortDueToExternalException(exception: Throwable): Unit = {
        isInterrupted = () ⇒ true
        self.synchronized {
            if (exceptions == null) exceptions = new ConcurrentExceptions()
            exceptions.addSuppressed(exception)
        }
    }

    def submit(t: T): Unit = {
        if (isInterrupted())
            return ;
        val runnable = new Runnable {
            def run(): Unit = {
                try {
                    if (!isInterrupted()) process(self, t)
                } catch {
                    case userException: Throwable ⇒ self.synchronized {
                        if (exceptions == null) exceptions = new ConcurrentExceptions()
                        exceptions.addSuppressed(userException)
                        if (abortOnExceptions) {
                            isInterrupted = () ⇒ true // prevent further submissions...
                        }
                    }
                } finally {
                    withLock(tasksLock) {
                        val newTasksCount = tasksCount - 1
                        tasksCount = newTasksCount
                        if (newTasksCount == 0) { isFinished.signalAll() }
                    }
                }
            }
        }
        withLock(tasksLock) { tasksCount += 1 }
        try {
            executionContext.execute(runnable)
        } catch {
            case unexpectedException: Throwable ⇒
                self.synchronized {
                    if (exceptions == null) exceptions = new ConcurrentExceptions()
                    exceptions.addSuppressed(unexpectedException)
                    if (abortOnExceptions) {
                        isInterrupted = () ⇒ true
                    }
                }
                withLock(tasksLock) {
                    val newTasksCount = tasksCount - 1
                    tasksCount = newTasksCount
                    if (newTasksCount == 0) { isFinished.signalAll() }
                }
        }
        /* OLD using synchronized
        tasksCount.incrementAndGet()
        val future = Future[Unit] { process(this, t) }(executionContext)
        future.onComplete { result ⇒
            // the workQueue may contain one to many new entries to work on
            if (result.isFailure) {
                val Failure(exception) = result
                if (exceptions eq null) {
                    Tasks.synchronized {
                        if (exceptions eq null) exceptions = new ConcurrentLinkedQueue[Throwable]()
                    }
                }
                exceptions.add(exception)
            }
            if (tasksCount.decrementAndGet() == 0) {
                tasksLock.synchronized { tasksLock.notifyAll() }
            }
        }(executionContext)
        */
    }

    /**
     * Blocks the calling thread until all sbumitted tasks as well as those tasks
     * that are created while processing tasks have been processed.
     *
     * '''`join` must not be called by a thread that actually executes a task!'''
     */
    def join(): Unit = {
        withLock(tasksLock) {
            while (tasksCount > 0) {
                // if tasksCount is zero we may already be finished or there were never any tasks...
                isFinished.await()
            }
        }
        if (exceptions != null) {
            throw exceptions;
        }
    }

    /* OLD - USING synchronized
    def join(): List[Throwable] = {
        tasksLock.synchronized {
            while (tasksCount.get != 0) {
                // if tasksCount is zero we may already be finished or there were never any tasks...
                tasksLock.wait()
            }

            if (exceptions ne null) {
                exceptions.asScala.toList
            } else {
                Nil
            }
        }
    }
    */
}

/**
 * Factory to create [[Tasks]] objects to process value oriented tasks.
 *
 * @author Michael Eichberg
 */
object Tasks {

    def apply[T](
        process:           (Tasks[T], T) ⇒ Unit,
        abortOnExceptions: Boolean              = false,
        isInterrupted:     () ⇒ Boolean         = () ⇒ Thread.currentThread().isInterrupted()
    )(
        implicit
        executionContext: ExecutionContext
    ): Tasks[T] = {
        if (executionContext eq null) {
            new SequentialTasks[T](process, abortOnExceptions, isInterrupted)
        } else {
            new ConcurrentTasks[T](process, abortOnExceptions, isInterrupted)(executionContext)
        }
    }

}
