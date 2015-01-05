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
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

/**
 * Common constants, factory methods and objects used throughout OPAL when doing
 * parallelization.
 *
 * @author Michael Eichberg
 */
package object concurrent {

    /**
     * The number of threads that should be used by parallelized computations that are
     * CPU bound (which do not use IO). This number is always larger than 0. This
     * number is intended to reflect the number of physical cores (not hyperthreaded
     * ones).
     */
    final val NumberOfThreadsForCPUBoundTasks: Int = {
        val threadsCPUBoundTasks = System.getProperty("org.opalj.threads.CPUBoundTasks")
        if (threadsCPUBoundTasks ne null) {
            val t = Integer.parseInt(threadsCPUBoundTasks)
            if (t <= 0)
                throw new IllegalArgumentException(
                    s"org.opalj.threads.CPUBoundTasks must be larger than 0 (current: $t)"
                )
            t
        } else {
            println("[info] the property org.opalj.threads.CPUBoundTasks is unspecified")
            Runtime.getRuntime.availableProcessors()
        }
    }
    println(s"[info] using $NumberOfThreadsForCPUBoundTasks threads for CPU bound tasks "+
        "(can be changed by setting the system property org.opalj.threads.CPUBoundTasks; "+
        "the number should be equal to the number of physical – not hyperthreaded – cores)")

    /**
     * The size of the thread pool used by OPAL. The size should be at least as large
     * as the number of physical cores and is ideally between 1 and 3 times larger
     * than the number of (hyperthreaded) cores. This enables the efficient execution of
     * IO bound tasks.
     */
    final lazy val ThreadPoolSize: Int = {
        val threadPoolSize = System.getProperty("org.opalj.threads.ThreadPoolSize")
        if (threadPoolSize ne null) {
            val s = Integer.parseInt(threadPoolSize)
            if (s <= NumberOfThreadsForCPUBoundTasks)
                throw new IllegalArgumentException(
                    s"org.opalj.threads.ThreadPoolSize must be larger than $NumberOfThreadsForCPUBoundTasks (current: $s)"
                )
            s
        } else {
            println("[info] the property org.opalj.threads.ThreadPoolSize is unspecified")
            Runtime.getRuntime.availableProcessors() * 2
        }
    }
    println(s"[info] using at most $ThreadPoolSize threads "+
        "(can be changed by setting the system property org.opalj.threads.ThreadPoolSize; "+
        "the number should be betweeen 1 and 3 times the number of (hyperthreaded) cores)")

    @volatile private[this] var theThreadPool: ExecutorService = null

    def ThreadPool: ExecutorService = {
        if (theThreadPool ne null)
            return theThreadPool;

        // we only support Java 7 and newer; hence, the double checked locking idiom works
        this.synchronized {
            val theTP = theThreadPool
            if (theTP eq null) {
                val group = new ThreadGroup(s"org.opalj.ThreadPool ${System.nanoTime()}")
                val tp =
                    new ThreadPoolExecutor(
                        ThreadPoolSize, ThreadPoolSize,
                        60L, TimeUnit.SECONDS,
                        new LinkedBlockingQueue[Runnable](),
                        new ThreadFactory {

                            val nextID = new java.util.concurrent.atomic.AtomicInteger(0)

                            def newThread(r: Runnable): Thread = {
                                val id = s"${nextID.incrementAndGet()}"
                                val name = s"org.opalj.ThreadPool-Thread $id"
                                val t = new Thread(group, r, name)
                                // we are using demon threads to make sure that these
                                // threads never prevent the JVM from regular termination
                                t.setDaemon(true)
                                t
                            }
                        }
                    )
                tp.prestartAllCoreThreads()
                theThreadPool = tp
                tp
            } else {
                theTP
            }
        }
    }

    /**
     * The ExecutionContext used by OPAL.
     *
     * This `ExecutionContext` is not intended to be shutdown explicitly. However,
     * if it used by some program, the program – at its very end – must call this
     * package's [[shutdown()]] method. Otherwise, the program may not terminate.
     */
    implicit lazy val OPALExecutionContext: ExecutionContext =
        ExecutionContext.fromExecutorService(ThreadPool)

    lazy val OPALExecutionContextTaskSupport: ExecutionContextTaskSupport =
        new ExecutionContextTaskSupport(OPALExecutionContext)
}
