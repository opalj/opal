/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

trait TaskManagerFactory {

    @volatile var NumberOfThreadsForProcessingPropertyComputations: Int = {
        // We need at least one thread for processing property computations.
        Math.max(NumberOfThreadsForCPUBoundTasks, 1)
    }

}

abstract class TasksManager {

    final val Debug = PropertyStore.Debug

    def MaxEvaluationDepth: Int

    def isIdle: Boolean

    /**
     * Called to enable the task manager to clean up all threads.
     */
    def shutdown()(implicit ps: PKECPropertyStore): Unit

    /**
     * Called after the setup of a phase has completed.
     * This, e.g., enables the task manager to initialize its threads.
     * The task manager is allowed to – but doesn't have to – immediately start the execution
     * of scheduled tasks.
     *
     * In general, the task manager has to assume that all data structures that are
     * initialized during the setup phase – and which will not be mutated while the analyses
     * are run – are not explicitly synchronized.
     */
    def phaseSetupCompleted()(implicit store: PKECPropertyStore): Unit

    def awaitPoolQuiescence()(implicit store: PKECPropertyStore): Unit

    def parallelize(f: ⇒ Unit)(implicit store: PKECPropertyStore): Unit = {
        incrementScheduledTasksCounter()
        doParallelize(f)
    }

    def doParallelize(f: ⇒ Unit)(implicit store: PKECPropertyStore): Unit

    final def forkResultHandler(r: PropertyComputationResult)(implicit store: PKECPropertyStore): Unit = {
        incrementScheduledTasksCounter()
        doForkResultHandler(r)
    }

    def doForkResultHandler(r: PropertyComputationResult)(implicit store: PKECPropertyStore): Unit

    final def schedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        incrementScheduledTasksCounter()
        doSchedulePropertyComputation(e, pc)
    }

    def doSchedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): Unit

    /**
     * Schedule or execute the given lazy property computation for the given entity.
     *
     * It is the responsibility of the task manager to ensure that we don't run in
     * a `StackOverflowError` if if executes the property computation eagerly.
     *
     * *Potential Optimizations*
     * Run the property computation by a thread that has just analyzed the entity...
     * if no thread is analyzing the entity analyze it using the current thread to minimize
     * the overall number of notifications.
     */
    final def forkLazyPropertyComputation[E <: Entity, P <: Property](
        e:  EPK[E, P],
        pc: PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): EOptionP[E, P] = {
        incrementScheduledTasksCounter()
        doForkLazyPropertyComputation(e, pc)
    }

    def doForkLazyPropertyComputation[E <: Entity, P <: Property](
        e:  EPK[E, P],
        pc: PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): EOptionP[E, P]

    final def forkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        incrementOnUpdateContinuationsCounter()
        doForkOnUpdateContinuation(c, e, pk)
    }

    def doForkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    )(
        implicit
        store: PKECPropertyStore
    ): Unit

    final def forkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        incrementOnUpdateContinuationsCounter()
        doForkOnUpdateContinuation(c, finalEP)
    }

    def doForkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    )(
        implicit
        store: PKECPropertyStore
    ): Unit

    private[this] val scheduledOnUpdateComputationsCounter = new AtomicInteger(0)
    protected[this] def incrementOnUpdateContinuationsCounter(): Unit = {
        if (Debug) scheduledOnUpdateComputationsCounter.incrementAndGet()
    }
    final def scheduledOnUpdateComputationsCount: Int = {
        scheduledOnUpdateComputationsCounter.get()
    }

    private[this] val scheduledTasksCounter = new AtomicInteger(0)
    protected[this] def incrementScheduledTasksCounter(): Unit = {
        if (Debug) scheduledTasksCounter.incrementAndGet()
    }
    final def scheduledTasksCount: Int = scheduledTasksCounter.get()

}
