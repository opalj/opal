/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import org.opalj.collection.mutable.RefArrayStack

/**
 * A task manager which performs all tasks sequentially; hence, turning the parallel
 * properties store into a sequential store. This store is intended to be used for debugging
 * and evaluation (i.e., the memory overhead of the parallel store when compared to the
 * sequential store) purposes only.
 */
class SeqTasksManager( final val MaxEvaluationDepth: Int) extends TasksManager {

    // NOTHING TO DO IN THIS SPECIAL CASE
    def prepareThreadPool()(implicit store: PKECPropertyStore): Unit = {}
    def cleanUpThreadPool()(implicit store: PKECPropertyStore): Unit = {}

    private[this] var currentEvaluationDepth = 0

    private[this] val runnables = RefArrayStack.empty[Runnable]

    def isIdle: Boolean = {
        runnables.isEmpty
    }

    def shutdown(): Unit = {
        runnables.clear()
    }

    def awaitPoolQuiescence()(implicit store: PKECPropertyStore): Unit = {
        while (runnables.nonEmpty) {
            if (store.doTerminate) throw new InterruptedException()
            val runnable = runnables.pop()
            runnable.run()
        }
    }

    def parallelize(f: ⇒ Unit)(implicit store: PKECPropertyStore): Unit = {
        f
    }

    def forkResultHandler(
        r: PropertyComputationResult
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        runnables.push(() ⇒ store.processResult(r))
    }

    def schedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        runnables.push(() ⇒ store.processResult(pc(e)))
    }

    def forkLazyPropertyComputation[E <: Entity, P <: Property](
        epk: EPK[E, P],
        pc:  PropertyComputation[E]
    )(
        implicit
        store: PKECPropertyStore
    ): EOptionP[E, P] = {
        if (currentEvaluationDepth < MaxEvaluationDepth) {
            currentEvaluationDepth += 1
            try {
                store.processResult(pc(epk.e))
                store(epk)
            } finally {
                currentEvaluationDepth -= 1
            }
        } else {
            runnables.push(() ⇒ store.processResult(pc(epk.e)))
            epk
        }
    }

    def forkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        val latestEPS = store(e, pk).asInstanceOf[SomeEPS]
        runnables.push(() ⇒ store.processResult(c(latestEPS)))
    }

    def forkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    )(
        implicit
        store: PKECPropertyStore
    ): Unit = {
        runnables.push(() ⇒ store.processResult(c(finalEP)))
    }

}
