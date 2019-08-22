/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch

import scala.collection.mutable.Buffer

import org.opalj.log.OPALLogger

/**
 * A task manager which performs all tasks in parallel using a standard fixed thread pool.
 * This store is intended to be used for debugging and evaluation purposes only, because it
 * doesn't support any kind of "smart" scheduling strategies.
 *
 * @author Michael Eichberg
 */
class ParTasksManager( final val MaxEvaluationDepth: Int) extends TasksManager {

    private val MaxThreads = org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

    // The idea is to start the execution of analyses when the store's "waitOnPhaseCompletion"
    // method is called.

    private[this] val queuedTasks: Buffer[Runnable] = Buffer.empty

    private[this] var nextThreadId: AtomicInteger = _
    private[this] var es: ExecutorService = _
    private[this] var tasks: AtomicInteger = _
    private[this] var currentEvaluationDepth: Int = _

    @volatile private[this] var latch: CountDownLatch = new CountDownLatch(1)

    def prepareThreadPool()(implicit ps: PKECPropertyStore): Unit = {
        // Initialize the thread pool and all helper data structures.
        nextThreadId = new AtomicInteger(1)
        es = Executors.newFixedThreadPool(
            MaxThreads,
            (r: Runnable) ⇒ {
                val threadId = nextThreadId.getAndIncrement()
                new Thread(r, s"PKECPropertyStore-Thread #$threadId")
            }: Thread
        )
        tasks = new AtomicInteger(0)
        currentEvaluationDepth = 0

        // Submit the scheduled tasks.
        queuedTasks.foreach { t ⇒ es.submit(t); tasks.incrementAndGet() }
        queuedTasks.clear()
    }

    def cleanUpThreadPool()(implicit ps: PKECPropertyStore): Unit = {
        assert(
            ps.doTerminate || tasks == null || tasks.get == 0,
            "some tasks are still running/are still scheduled"
        )
        try {
            val es = this.es
            if (es != null) es.shutdownNow()
        } catch {
            case e: Throwable ⇒
                OPALLogger.error("property store", "shutdown failed", e)(ps.logContext)
        }
        es = null
        nextThreadId = null
        tasks = null
    }

    def shutdown()(implicit ps: PKECPropertyStore): Unit = {
        cleanUpThreadPool()

        // We have to ensure that "the every/the last latch" is count down!
        latch.countDown()
    }

    def isIdle: Boolean = { val tasks = this.tasks; tasks == null || tasks.get == 0 }

    def awaitPoolQuiescence()(implicit ps: PKECPropertyStore): Unit = {
        // Recall the overall program flow:
        // 1. register computations
        // 2. wait on phase completion is called
        //    2.1. prepare thread pool is called
        //         (this may already lead to the execution of analyses)
        //    2.2. await pool quiescence is called
        //         (potentially multiple times, but never concurrently!)
        //    2.3. clean up thread pool is called

        // We have to get the latch _before_ checking that there are tasks
        // otherwise it may happen that between querying the number of
        // tasks and calling await a new latch was created for the next
        // phase. In this case, this new latch may not be count down.
        val latch = this.latch
        // We have to check if "tasks" is still valid or if – due to an exception –
        // the store was already shut down.
        val tasks = this.tasks
        if (tasks != null && tasks.get > 0) {
            if (ps.doTerminate) {
                throw new InterruptedException();
            }
            latch.await()
        }
    }

    private def decrementTasks()(implicit ps: PKECPropertyStore): Unit = {
        if (tasks.decrementAndGet() == 0) {
            val oldLatch = latch
            latch = new CountDownLatch(1)
            oldLatch.countDown()
        }

        if (ps.doTerminate) {
            latch.countDown()
            throw new InterruptedException()
        }
    }

    def doParallelize(f: ⇒ Unit)(implicit store: PKECPropertyStore): Unit = {
        val r: Runnable = () ⇒ try {
            // FIXME handleExceptions
            f
        } finally {
            decrementTasks()
        }
        if (es != null) {
            es.submit(r)
            tasks.incrementAndGet()
        } else {
            queuedTasks += r
        }
    }

    def doForkResultHandler(
        result: PropertyComputationResult
    )(
        implicit
        ps: PKECPropertyStore
    ): Unit = {
        val r: Runnable = () ⇒ try { ps.processResult(result) } finally { decrementTasks() }
        es.submit(r)
        tasks.incrementAndGet()
    }

    def doSchedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    )(
        implicit
        ps: PKECPropertyStore
    ): Unit = {
        val r: Runnable = () ⇒ try {
            val r = try { pc(e) } catch { case t: Throwable ⇒ ps.collectAndThrowException(t) }
            ps.processResult(r)
        } finally {
            decrementTasks()
        }
        if (es != null) {
            es.submit(r)
            tasks.incrementAndGet()
        } else {
            queuedTasks += r
        }
    }

    def doForkLazyPropertyComputation[E <: Entity, P <: Property](
        epk: EPK[E, P],
        pc:  PropertyComputation[E]
    )(
        implicit
        ps: PKECPropertyStore
    ): EOptionP[E, P] = {
        if (currentEvaluationDepth < MaxEvaluationDepth) {
            currentEvaluationDepth += 1
            try {
                if (ps.doTerminate)
                    throw new InterruptedException()

                if (ps.tracer.isDefined)
                    ps.tracer.get
                        .immediateEvaluationOfLazyComputation(epk, currentEvaluationDepth, pc)

                val r = try { pc(epk.e) } catch { case t: Throwable ⇒ ps.collectAndThrowException(t) }
                ps.processResult(r)
                val newEOptionP = ps(epk)
                newEOptionP
            } finally {
                currentEvaluationDepth -= 1
            }
        } else {
            val r: Runnable = () ⇒ try {
                val r = try { pc(epk.e) } catch { case t: Throwable ⇒ ps.collectAndThrowException(t) }
                ps.processResult(r)
            } finally {
                decrementTasks()
            }
            es.submit(r)
            tasks.incrementAndGet()
            epk
        }
    }

    def doForkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    )(
        implicit
        ps: PKECPropertyStore
    ): Unit = {
        val r: Runnable = () ⇒
            try {
                val r = try {
                    val latestEPS = ps(e, pk).asEPS
                    c(latestEPS)
                } catch { case t: Throwable ⇒ ps.collectAndThrowException(t) }
                ps.processResult(r)
            } finally {
                decrementTasks()
            }
        es.submit(r)
        tasks.incrementAndGet()
    }

    def doForkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    )(
        implicit
        ps: PKECPropertyStore
    ): Unit = {
        es.submit((() ⇒
            try {
                val r = try { c(finalEP) } catch { case t: Throwable ⇒ ps.collectAndThrowException(t) }
                ps.processResult(r)
            } finally {
                decrementTasks()
            }): Runnable)
        tasks.incrementAndGet()
    }

}
