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
package org
package opalj

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import scala.concurrent.ExecutionContext
import scala.collection.parallel.ExecutionContextTaskSupport
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.{Future ⇒ JFuture}
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import scala.util.Failure

/**
 * Common constants, factory methods and objects used throughout OPAL when performing
 * concurrent computations.
 *
 * @author Michael Eichberg
 */
package object concurrent {

    private implicit def logContext = GlobalLogContext

    final def handleUncaughtException(t: Throwable): Unit = {
        OPALLogger.error("internal", "uncaught exception", t)
    }

    final def handleUncaughtException(t: Thread, e: Throwable): Unit = {
        OPALLogger.error("internal", s"uncaught exception (Thread=${t.getName})", e)
    }

    //
    // STEP 1
    //
    /**
     * The number of threads that should be used by parallelized computations that are
     * CPU bound (which do not use IO). This number is always larger than 0. This
     * number is intended to reflect the number of physical cores (not hyperthreaded
     * ones).
     */
    final val NumberOfThreadsForCPUBoundTasks: Int = {

        val maxCPUBoundTasks = System.getProperty("org.opalj.threads.CPUBoundTasks")
        if (maxCPUBoundTasks ne null) {
            val t = Integer.parseInt(maxCPUBoundTasks)
            if (t <= 0)
                throw new IllegalArgumentException(
                    s"org.opalj.threads.CPUBoundTasks must be larger than 0 (current: $t)"
                )
            t
        } else {
            OPALLogger.warn(
                "OPAL",
                "the property org.opalj.threads.CPUBoundTasks is unspecified"
            )
            Runtime.getRuntime.availableProcessors()
        }
    }
    OPALLogger.info(
        "OPAL",
        s"using $NumberOfThreadsForCPUBoundTasks thread(s) for CPU bound tasks "+
            "(can be changed by setting the system property org.opalj.threads.CPUBoundTasks; "+
            "the number should be equal to the number of physical – not hyperthreaded – cores)"
    )

    //
    // STEP 2
    //
    /**
     * The size of the thread pool used by OPAL for IO bound tasks. The size should be
     * at least as large as the number of physical cores and is ideally between 1 and 3
     * times larger than the number of (hyperthreaded) cores. This enables the efficient
     * execution of IO bound tasks.
     */
    final val NumberOfThreadsForIOBoundTasks: Int = {
        val maxIOBoundTasks = System.getProperty("org.opalj.threads.IOBoundTasks")
        if (maxIOBoundTasks ne null) {
            val s = Integer.parseInt(maxIOBoundTasks)
            if (s < NumberOfThreadsForCPUBoundTasks)
                throw new IllegalArgumentException(
                    s"org.opalj.threads.IOBoundTasks===$s must be larger than "+
                        s"org.opalj.threads.CPUBoundTasks===$NumberOfThreadsForCPUBoundTasks"
                )
            s
        } else {
            OPALLogger.warn(
                "OPAL",
                "the property org.opalj.threads.IOBoundTasks is unspecified"
            )
            Runtime.getRuntime.availableProcessors() * 2
        }
    }
    OPALLogger.info(
        "OPAL",
        s"using at most $NumberOfThreadsForIOBoundTasks thread(s) for IO bound tasks "+
            "(can be changed by setting the system property org.opalj.threads.IOBoundTasks; "+
            "the number should be betweeen 1 and 2 times the number of (hyperthreaded) cores)"
    )

    //
    // STEP 3
    //
    private[concurrent] final val UncaughtExceptionHandler = new Thread.UncaughtExceptionHandler {
        def uncaughtException(t: Thread, e: Throwable): Unit = {
            handleUncaughtException(e)
        }
    }

    def ThreadPoolN(n: Int): OPALThreadPoolExecutor = {
        val group = new ThreadGroup(s"org.opalj.ThreadPool ${System.nanoTime()}")
        val tp = new OPALThreadPoolExecutor(n, group)
        tp.allowCoreThreadTimeOut(true)
        tp.prestartAllCoreThreads()
        tp
    }

    /**
     * Returns the singleton instance of the global `ThreadPool` used throughout OPAL.
     */
    final val ThreadPool = ThreadPoolN(NumberOfThreadsForIOBoundTasks)

    //
    // STEP 4
    //
    /**
     * The ExecutionContext used by OPAL.
     *
     * This `ExecutionContext` must not be shutdown.
     */
    implicit final val OPALExecutionContext: ExecutionContext =
        ExecutionContext.fromExecutorService(ThreadPool)

    //
    // STEP 5
    //
    final val OPALExecutionContextTaskSupport: ExecutionContextTaskSupport =
        new ExecutionContextTaskSupport(OPALExecutionContext) {
            override def parallelismLevel: Int = NumberOfThreadsForCPUBoundTasks
        }

    //
    // GENERAL HELPER METHODS
    //

    /**
     * Execute the given function `f` in parallel for each element of the given array.
     * After processing an element it is checked whether the computation should be
     * aborted.
     *
     * In general – but also at most – `parallelizationLevel` many threads will be used
     * to process the elements. The core idea is that each thread processes an element
     * and after that grabs the next element from the array. Hence, this handles
     * situations gracefully where the effort necessary to analyze a specific element
     * varies widely.
     *
     * @note The OPALExecutionContext is used for getting the necessary threads.
     */
    def parForeachArrayElement[T, U](
        data:                 Array[T],
        parallelizationLevel: Int          = NumberOfThreadsForCPUBoundTasks,
        isInterrupted:        () ⇒ Boolean = () ⇒ Thread.currentThread().isInterrupted()
    )(
        f: Function[T, U]
    ): Unit = {

        if (parallelizationLevel == 1) {
            data.foreach(f)
            return ;
        }

        val max = data.length
        val index = new AtomicInteger(0)
        val futures = new Array[Future[Unit]](parallelizationLevel)

        // Start parallel execution
        {
            var t = 0
            while (t < parallelizationLevel) {
                futures(t) =
                    Future[Unit] {
                        var i: Int = -1
                        while ({ i = index.getAndIncrement; i } < max && !isInterrupted()) {
                            f(data(i))
                        }
                    }
                t += 1
            }
        }
        // Await completion
        {
            var t = 0
            while (t < parallelizationLevel) {
                Await.ready(futures(t), Duration.Inf)
                t += 1
            }
        }
    }

    /**
     * Executes a given function f for each element in the given workqueue. `f` is – of course –
     * also allowed to add elements to the workqueue; however `f` must do so concurrently. Given
     * that `f` may be executed in parallel, `f` has to be thread-safe. The exceptions that occur
     * while executing `f` are collected and returned at the end.
     *
     * @example
     * {{{
     * val workQueue = new ConcurrentLinkedQueue[T]()
     * workQueue.add(<T>)
     * val exceptions =
     * 		whileNonEmpty(workQueue) { t ⇒
     * 			// do something with t
     * 			if (<some condition>) { workQueue.add(nextT) }
     * 		}
     * }}}
     */
    def whileNonEmpty[T >: Null <: AnyRef, U](
        // the workQueue may be filled as a side effect of executing f
        workQueue:     ConcurrentLinkedQueue[T],
        isInterrupted: () ⇒ Boolean             = () ⇒ Thread.currentThread().isInterrupted()
    )(
        f: T ⇒ U
    )(
        implicit
        executionContext: ExecutionContext
    ): List[Throwable] = {

        val exceptionsMutex = new Object
        var exceptions: List[Throwable] = Nil

        val futuresCountMutex = new Object
        var futuresCount = 0

        def schedule(): Unit = {
            while (!workQueue.isEmpty()) {
                val next = workQueue.poll()
                // schedule is executed concurrently, hence some other thread
                // may have grapped the (last remaining) value in between. 
                if (next != null) {
                    futuresCountMutex.synchronized { futuresCount += 1 }
                    val future = Future[Unit] { f(next) }(executionContext)
                    future.onComplete { result ⇒
                        // the workQueue may contain one to many new entries to work on
                        result match {
                            case Failure(t) ⇒
                                exceptionsMutex.synchronized { exceptions = t :: exceptions }
                            case _ ⇒
                            /* nothing special to do */
                        }
                        schedule();
                        futuresCountMutex.synchronized {
                            futuresCount -= 1
                            futuresCountMutex.notify()
                        }
                    }(executionContext)
                }
            }
        }

        schedule();

        // let's wait until the workqueue is empty
        futuresCountMutex.synchronized {
            while (futuresCount > 0) futuresCountMutex.wait();
        }

        exceptionsMutex.synchronized { exceptions }
    }
}

