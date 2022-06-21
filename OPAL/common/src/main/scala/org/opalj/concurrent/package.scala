/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong
import scala.collection.parallel.ExecutionContextTaskSupport
import scala.util.control.ControlThrowable
import scala.concurrent.ExecutionContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.warn
import org.opalj.log.OPALLogger.info

import scala.collection.immutable.ArraySeq

/**
 * Common constants, factory methods and objects used throughout OPAL when performing
 * concurrent computations.
 *
 * @author Michael Eichberg
 */
package object concurrent {

    private implicit def logContext: GlobalLogContext.type = GlobalLogContext

    final val defaultIsInterrupted = () => Thread.currentThread.isInterrupted

    final def handleUncaughtException(t: Throwable): Unit = {
        error("internal", "uncaught exception", t)
    }

    final def handleUncaughtException(t: Thread, e: Throwable): Unit = {
        error("internal", s"uncaught exception (Thread=${t.getName})", e)
    }

    final val OPALUnboundedThreadPool: ExecutorService = {
        val nextID = new AtomicLong(0L)
        Executors.newCachedThreadPool(
            new ThreadFactory {
                def newThread(r: Runnable): Thread = {
                    val name = s"[global] opalj.ThreadPool - Thread "+nextID.incrementAndGet()
                    val t = new Thread(r, name)
                    t.setDaemon(true)
                    t.setUncaughtExceptionHandler(UncaughtExceptionHandler)
                    t
                }
            }
        )
    }

    final val OPALUnboundedExecutionContext: ExecutionContext = {
        ExecutionContext.fromExecutorService(OPALUnboundedThreadPool)
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
            if (t <= 0) {
                val message = s"org.opalj.threads.CPUBoundTasks must be larger than 0 (current: $t)"
                throw new IllegalArgumentException(message)
            }
            t
        } else {
            warn("OPAL", "the property org.opalj.threads.CPUBoundTasks is unspecified")
            Runtime.getRuntime.availableProcessors()
        }
    }
    info(
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
            warn("OPAL", "the property org.opalj.threads.IOBoundTasks is unspecified")
            Runtime.getRuntime.availableProcessors() * 2
        }
    }
    info(
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
            try {
                handleUncaughtException(e)
            } catch {
                case t: Throwable =>
                    // We shouldn't use the OPALLogger here to ensure that we can report
                    // problems related to the logger!
                    Console.err.println("[fatal] internal error when reporting errors: ")
                    t.printStackTrace(Console.err)
            }
        }
    }

    def BoundedThreadPool(name: String, n: Int): OPALBoundedThreadPoolExecutor = {
        val groupName = s"[$name/${System.nanoTime().toHexString.drop(4)}] opalj.ThreadPool[N=$n]"
        val group = new ThreadGroup(groupName)
        val tp = new OPALBoundedThreadPoolExecutor(n, group)
        tp.allowCoreThreadTimeOut(true)
        tp.prestartAllCoreThreads()
        tp
    }

    def BoundedExecutionContext(name: String, n: Int): ExecutionContext = {
        ExecutionContext.fromExecutorService(BoundedThreadPool(name, n))
    }

    /**
     * Thread pool which supports at most `NumberOfThreadsForIOBoundTasks` tasks.
     *
     * @note This thread pool must not be shutdown.
     */
    final val OPALHTBoundedThreadPool = BoundedThreadPool("global", NumberOfThreadsForIOBoundTasks)

    //
    // STEP 4
    //

    /**
     * The ExecutionContext used by OPAL.
     *
     * @note This `ExecutionContext` must not be shutdown.
     */
    implicit final val OPALHTBoundedExecutionContext: ExecutionContext = {
        ExecutionContext.fromExecutorService(OPALHTBoundedThreadPool)
    }

    //
    // STEP 5
    //
    final val OPALHTBoundedExecutionContextTaskSupport: ExecutionContextTaskSupport = {
        new ExecutionContextTaskSupport(OPALHTBoundedExecutionContext) {
            override def parallelismLevel: Int = NumberOfThreadsForCPUBoundTasks
        }
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
     * @note   The given function `f` must not make use of non-local returns; such returns
     *         will be caught and reported later.
     *
     * @note   The OPALExecutionContext is used for getting the necessary threads.
     *
     * @throws ConcurrentExceptions if any exception occurs;
     *         the thrown exception stores all other exceptions (`getSuppressed`)
     */
    @throws[ConcurrentExceptions]("the set of concurrently thrown suppressed exceptions ")
    final def parForeachArrayElement[T, U](
        data:                 Array[T],
        parallelizationLevel: Int           = NumberOfThreadsForCPUBoundTasks,
        isInterrupted:        () => Boolean = defaultIsInterrupted
    )(
        f: Function[T, U]
    ): Unit = {
        parForeachSeqElement(ArraySeq.unsafeWrapArray(data), parallelizationLevel, isInterrupted)(f)
    }

    /**
     * Execute the given function `f` in parallel for each element of the given indexed seq.
     * After processing an element it is checked whether the computation should be
     * aborted.
     *
     * In general – but also at most – `parallelizationLevel` many threads will be used
     * to process the elements. The core idea is that each thread processes an element
     * and after that grabs the next element from the array. Hence, this handles
     * situations gracefully where the effort necessary to analyze a specific element
     * varies widely.
     *
     * @note   The given function `f` must not make use of non-local returns; such returns
     *         will be caught and reported later.
     *
     * @note   The OPALExecutionContext is used for getting the necessary threads.
     *
     * @throws ConcurrentExceptions if any exception occurs;
     *         the thrown exception stores all other exceptions (`getSuppressed`)
     */
    @throws[ConcurrentExceptions]("the set of concurrently thrown suppressed exceptions ")
    final def parForeachSeqElement[T, U](
        data:                 IndexedSeq[T],
        parallelizationLevel: Int           = NumberOfThreadsForCPUBoundTasks,
        isInterrupted:        () => Boolean = defaultIsInterrupted
    )(
        f: Function[T, U]
    ): Unit = {
        val dataLength = data.length

        if (dataLength == 0)
            return ;

        val index = new AtomicInteger(0)
        var exceptions: ConcurrentExceptions = null
        def addSuppressed(throwable: Throwable): Unit = index.synchronized {
            if (exceptions == null) exceptions = new ConcurrentExceptions
            exceptions.addSuppressed(throwable)
        }

        def analyzeArrayElements(): Unit = {
            var i: Int = -1
            while ({ i = index.getAndIncrement; i } < dataLength && !isInterrupted()) {
                try {
                    f(data(i))
                } catch {
                    case ct: ControlThrowable =>
                        val t = new Throwable("unsupported non-local return", ct)
                        addSuppressed(t)
                    case t: Throwable =>
                        addSuppressed(t)
                }
            }
        }

        if (parallelizationLevel == 1 || dataLength == 1) {
            analyzeArrayElements()
        } else {
            ///
            // HANDLE CASE WHERE WE HAVE EFFECTIVE PARALLELIZATION (LEVEL > 1)
            //

            // Start parallel execution
            val maxThreads = Math.min(parallelizationLevel, dataLength)
            val latch = new CountDownLatch(maxThreads)
            val pool = OPALUnboundedThreadPool
            try {
                var t = 0
                while (t < maxThreads) {
                    pool.execute { () =>
                        try {
                            analyzeArrayElements()
                        } finally {
                            latch.countDown()
                        }
                    }
                    t += 1
                }
                latch.await()
            } catch {
                case t: Throwable => addSuppressed(t) // <= actually, we should never get here...
            }
        }
        if (exceptions != null) {
            throw exceptions;
        }

    }

}
