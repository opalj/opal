/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

trait TaskManagerFactory {

    @volatile var NumberOfThreadsForProcessingPropertyComputations: Int = {
        // We need at least one thread for processing property computations.
        Math.max(NumberOfThreadsForCPUBoundTasks, 1)
    }

}

abstract class TaskManager {

    /**
     * Called to enable the task manager to initialize its thread. Called after the setup
     * of a phase has completed. The task manager is allowed to immediately start the
     * execution of scheduled tasks.
     * 
     * In general, the task manager has to assume that all data structures that are 
     * initialized during the setup phase – and which will not be mutated while the analyses
     * are run – are not explicitly synchronized. 
     */
    def prepareThreadPool()(implicit store: PKECPropertyStore) : Unit

    /**
     * Called to enable the task manager to clean up all threads.
     * 
     * Recall that a single phase may have multiple sub phases and that quiescence may be
     * reached multiple times.
     */
    def cleanUpThreadPool()(implicit store: PKECPropertyStore) : Unit


    def awaitPoolQuiescence()(implicit store: PKECPropertyStore): Unit

    def parallelize(f : => Unit)(implicit store: PKECPropertyStore): Unit 

     def forkResultHandler(r: PropertyComputationResult)(implicit store: PKECPropertyStore ): Unit 

     def schedulePropertyComputation[E <: Entity](
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
     def forkLazyPropertyComputation[E <: Entity, P <: Property](
        e:  EPK[E,P],
        pc: PropertyComputation[E]
    )(
        implicit 
        store: PKECPropertyStore
        ): EOptionP[E,P]



     def forkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    )(
        implicit 
        store: PKECPropertyStore
        ): Unit

    def forkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    )(
        implicit
         store: PKECPropertyStore
        ): Unit

}