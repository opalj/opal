/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveAction
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

import org.opalj.log.LogContext
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds

/**
 * A concurrent implementation of the property store which executes the scheduled computations
 * in parallel using a ForkJoinPool.
 *
 * We use `NumberOfThreadsForProcessingPropertyComputations` threads for processing the
 * scheduled computations.
 *
 * @author Michael Eichberg
 */
final class PKECPropertyStore private (
        val ctx:                                              Map[Class[_], AnyRef],
        val NumberOfThreadsForProcessingPropertyComputations: Int
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore { store ⇒

    //
    //
    // CORE DATA STRUCTURES
    //
    //

    // Per PropertyKind we use one concurrent hash map to store the entities' properties.
    // The value encompasses the current property along with some helper information.
    private[this] val properties: Array[ConcurrentHashMap[Entity, State]] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    private[this] val pool: ForkJoinPool = {
        new ForkJoinPool(
            NumberOfThreadsForProcessingPropertyComputations,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            (_: Thread, e: Throwable) ⇒ { collectException(e) },
            true
        )
    }

    // The following "var"s/"arrays" do not need to be volatile/thread safe, because the updates –
    // which are only done while the store is quiescent – are done within the driver thread and
    // are guaranteed to be visible to all relevant fork/join tasks.

    /** Computations that will be triggered when a new property becomes available. */
    private[this] val triggeredComputations: Array[Array[SomePropertyComputation]] = {
        new Array(SupportedPropertyKinds)
    }

    //
    //
    // BASIC QUERY METHODS
    //
    //

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val ps = for {
                (entitiesMap, pkId) ← properties.iterator.zipWithIndex.take(PropertyKey.maxId + 1)
                // TODO... better handling...
            } yield {
                s"$entitiesMap"
            }
            ps.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(properties=${properties.iterator.map(_.size).sum})"
        }
    }

    override def supportsFastTrackPropertyComputations: Boolean = false

    override def fastTrackPropertiesCount: Int = ???
    override def immediateOnUpdateComputationsCount: Int = ???
    override def quiescenceCount: Int = ???
    override def scheduledOnUpdateComputationsCount: Int = ???
    override def scheduledTasksCount: Int = ???
    override def statistics: scala.collection.Map[String, Int] = ???

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = ???
    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = ???
    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = ???
    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = ???
    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = ???
    override def finalEntities[P <: Property](p: P): Iterator[Entity] = ???

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = ???

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        val state = properties(pk.id).get(e)
        state != null && {
            val eOptionP = state.eOptionP.get()
            eOptionP.hasUBP || eOptionP.hasLBP
        }
    }

    override def isKnown(e: Entity): Boolean = properties.exists(psOfKind ⇒ psOfKind.contains(e))

    //
    //
    // CORE IMPLEMENTATION
    //
    //

    private[this] def forkPropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    ): Unit = {
        val action = new RecursiveAction { def compute(): Unit = store.processResult(pc(e)) }
        action.fork()
    }

    private[this] def forkResultHandler(r: PropertyComputationResult): Unit = {
        val action = new RecursiveAction { def compute(): Unit = store.processResult(r) }
        action.fork()
    }

    /*private[this]*/ def forkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    ): Unit = {
        val action = new RecursiveAction {
            def compute(): Unit = {
                // get the newest value before we actually call the onUpdateContinuation
                val newEPS = store(e, pk).asEPS
                // IMPROVE ... see other forkOnUpdateContinuation
                store.processResult(c(newEPS))
            }
        }
        action.fork()
    }

    private[this] def forkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    ): Unit = {
        val action = new RecursiveAction {
            def compute(): Unit = {
                // IMPROVE: Instead of naively calling "c" with finalEP, it may be worth considering which other updates have happened to figure out which update may be the "beste"
                store.processResult(c(finalEP))
            }
        }
        action.fork()
    }

    def shutdown(): Unit = store.pool.shutdown()

    override def isIdle: Boolean = store.pool.isQuiescent

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {
        store.pool.execute(() ⇒ processResult(pc(e)))
    }

    override def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {

        val pkId = pk.id
        val oldComputations: Array[SomePropertyComputation] = triggeredComputations(pkId)
        var newComputations: Array[SomePropertyComputation] = null

        if (oldComputations == null) {
            newComputations = Array[SomePropertyComputation](pc)
        } else {
            newComputations = java.util.Arrays.copyOf(oldComputations, oldComputations.length + 1)
            newComputations(oldComputations.length) = pc
        }
        triggeredComputations(pkId) = newComputations

        // Let's check if we need to trigger it right away due to already existing values...
        println(oldComputations)
        ???

    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = this(e, pk)

    override def doSet(e: Entity, p: Property): Unit = {
        properties(p.id).put(e, State(FinalEP(e, p)))
    }

    override def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] ⇒ InterimEP[E, P]
    ): Unit = {
        val pkId = pk.id
        val propertiesOfKind = properties(pkId)
        val newInterimP =
            propertiesOfKind.get(e) match {
                case null  ⇒ pc(EPK(e, pk))
                case state ⇒ pc(state.eOptionP.asInstanceOf[EOptionP[E, P]])
            }

        propertiesOfKind.put(e, State(newInterimP))
    }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        this(EPK(e, pk))
    }

    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = ???

    // THIS METHOD IS NOT INTENDED TO BE USED BY THE FRAMEWORK ITSELF - IT IS ONLY MEANT TO
    // BE USED BY EXTERNAL TASKS.
    override def handleResult(r: PropertyComputationResult): Unit = {
        // We need to execute everything in a ForkJoinPoolTask, because the subsequent handling
        // expects that.
        pool.execute(() ⇒ store.processResult(r))
    }

    /*
    private[this] def notifyDepender(
        dependerEPK: SomeEPK,
        c:           OnUpdateContinuation,
        eps:         SomeEPS
    ): Unit = {
        val dependerState = properties(dependerEPK.pk.id).get(dependerEPK.e)
        if (dependerState.updateComputationTriggered.compareAndSet(false, true)) {
            forkOnUpdateContinuation(c, eps.e, eps.pk)
        }
    }
    */

    private[this] def notifyDepender(
        dependerEPK: SomeEPK,
        c:           OnUpdateContinuation,
        finalEP:     SomeFinalEP
    ): Unit = {
        val dependerState = properties(dependerEPK.pk.id).get(dependerEPK.e)
        if (dependerState.updateComputationTriggered.compareAndSet(false, true)) {
            forkOnUpdateContinuation(c, finalEP)
        }
    }

    private[this] def finalUpdate(finalEP: SomeFinalEP): Traversable[SomeEOptionP] = {
        val newState = State(finalEP)
        val oldState = properties(finalEP.pk.id).putIfAbsent(finalEP.e, newState)
        if (oldState != null) {
            // We had a state object which means we may have dependers.
            //
            // We now have to update the value and inform the dependers. Though this is a
            // non-atomic operation, we can still simply inform all dependers
            // because is it guaranteed that they have not seen the final value since dependencies
            // on final values are not allowed!
            //
            // Note that – when we register a depender – it is the responsibility of the depender to
            // check that it has seen the last value after updating the dependers list!
            val oldEOptionP = oldState.eOptionP.getAndSet(finalEP)
            if (debug) oldEOptionP.checkIsValidPropertiesUpdate(finalEP, Nil)

            oldState.dependers.forEach((epk, c) ⇒ notifyDepender(epk, c, finalEP))
            // ... the following line may clear dependers that have not been triggered, but
            // this doesn't matter, because when registration happens, it is the responsibility
            // of that thread to afterwards check that the value hasn't changed.
            oldState.dependers.clear()

            val oldDependees = oldState.dependees
            oldState.dependees = null
            oldDependees
        } else {
            Nil
        }
    }

    def clearDependees(depender: SomeEPK, dependees: Traversable[SomeEOptionP]): Unit = {
        dependees foreach { dependee ⇒
            val dependeeState = properties(dependee.pk.id).get(dependee.e)
            dependeeState.dependers.remove(depender)
        }
    }

    /**
     * Always executed by a `ForkJoinTask` to process the given result.
     */
    private[this] def processResult(r: PropertyComputationResult): Unit = handleExceptions {

        r.id match {

            case NoResult.id ⇒ {
                // A computation reported no result; i.e., it is not possible to
                // compute a/some property/properties for a given entity.
            }

            //
            // Result containers
            //

            case Results.id ⇒ r.asResults.foreach {
                // IMPROVE Determine if direct evaluation would be better than spawning new threads.
                forkResultHandler
            }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs, propertyComputationsHint) = r
                npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                    val (pc, e) = npc
                    forkPropertyComputation(e, pc)
                }
                processResult(ir)

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(finalEP) = r
                clearDependees(finalEP.toEPK, finalUpdate(finalEP))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { finalEP ⇒
                    clearDependees(finalEP.toEPK, finalUpdate(finalEP))
                }
            /*
            case InterimResult.id ⇒
                val interimR = r.asInterimResult
                val eps = interimR.eps
*/
            /*
            case IdempotentResult.id ⇒
                val IdempotentResult(ep @ FinalP(e, p)) = r
                val pkId = p.key.id
                val epk = ep.toEPK
                val propertiesOfEntity = properties(pkId)
                assert(!dependees(pkId).containsKey(e))
                forceDependersNotifications -= epk
                if (!propertiesOfEntity.containsKey(e)) {
                    finalUpdate(e, p, pcrs)
                } else {
                    /*we already have a value*/
                    redundantIdempotentResultsCounter += 1
                    if (debug) {
                        val oldEP = propertiesOfEntity.get(e)
                        if (oldEP != ep) {
                            throw new IllegalArgumentException(s"$e: unexpected update $oldEP => $ep")
                        }
                    }
                }

            case PartialResult.id ⇒
                val PartialResult(e, pk, _) = r
                type E = e.type
                type P = Property
                val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])

                val partialResultsQueue = partialResults(pk.id).get(e)
                var nextPartialResult = partialResultsQueue.poll()
                var newEPSOption : Option[EPS[E,P]] = None
                var doForceEvaluation = forceEvaluation
                var theForceDependersNotifications = forceDependersNotifications
                do {
                    val NewProperty(PartialResult(_, _, u), nextForceEvaluation, nextForceDependersNotification  ) = nextPartialResult

                    newEPSOption match {
                        case Some(updatedEOptionP) ⇒
                            XXX updateEOption has to be stored... if the update does not result in a new result!
                    newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](updatedEOptionP)
                        case None ⇒
                            newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                            if (newEPSOption.isEmpty) {
                                if (tracer.isDefined) {
                                    val partialResult = r.asInstanceOf[SomePartialResult]
                                    tracer.get.uselessPartialResult(partialResult, eOptionP)
                                }
                                uselessPartialResultComputationCounter += 1
                            }
                    }
                    nextPartialResult = partialResultsQueue.poll()
                } while (nextPartialResult != null)

                if (newEPSOption.isDefined) {
                    val newEPS = newEPSOption.get
                    val epk = newEPS.toEPK
                    if (clearDependees(epk) > 0) {
                        throw new IllegalStateException(
                            s"partial result ($r) for property with dependees (and continuation function)"
                        )
                    }
                    forceDependersNotifications -= epk
                    if (isPropertyKeyForSimplePropertyBasedOnPKId(pk.id))
                        updateAndNotifyForSimpleP(newEPS.e, newEPS.ub, isFinal = false, pcrs = pcrs)
                    else
                        updateAndNotifyForRegularP(newEPS.e, newEPS.lb, newEPS.ub, pcrs = pcrs)
                }

            case InterimResult.id ⇒
                val InterimResult(e, lb, ub, seenDependees, c, onUpdateContinuationHint) = r
                val pk = ub.key
                val pkId = pk.id
                val epk = EPK(e, pk)

                if (forceEvaluation) forcedComputations(pkId).put(e, e)

                assertNoDependees(e, pkId)

                // 1. let's check if a seen dependee is already updated; if so, we directly
                //    schedule/execute the continuation function again to continue computing
                //    the property
                val seenDependeesIterator = seenDependees.toIterator
                while (seenDependeesIterator.hasNext) {
                    val seenDependee = seenDependeesIterator.next()

                    if (debug && seenDependee.isFinal) {
                        throw new IllegalStateException(
                            s"$e (lb=$lb, ub=$ub): dependency to final property: $seenDependee"
                        )
                    }

                    val seenDependeeE = seenDependee.e
                    val seenDependeePKId = seenDependee.pk.id
                    val propertiesOfEntity = properties(seenDependeePKId)
                    // seenDependee is guaranteed to be not null
                    // currentDependee may be null => newDependee is an EPK => no update
                    val currentDependee = propertiesOfEntity.get(seenDependeeE)
                    if (currentDependee != null && seenDependee != currentDependee) {
                        // Make the current result available for other threads, but
                        // do not yet trigger dependers; however, we have to ensure
                        // that the dependers are eventually triggered if any update
                        // was relevant!
                        val updateAndNotifyState = updateAndNotifyForRegularP(
                            e, lb, ub,
                            notifyDependersAboutNonFinalUpdates = false,
                            pcrs
                        )
                        if (updateAndNotifyState.isNotificationRequired) {
                            forceDependersNotifications += epk
                        }

                        if (tracer.isDefined)
                            tracer.get.immediateDependeeUpdate(
                                e, pk, seenDependee, currentDependee, updateAndNotifyState
                            )

                        if (onUpdateContinuationHint == CheapPropertyComputation) {
                            directDependeeUpdatesCounter += 1
                            // we want to avoid potential stack-overflow errors...
                            pcrs += c(currentDependee)
                        } else {
                            scheduledDependeeUpdatesCounter += 1
                            if (currentDependee.isFinal) {
                                val t = ImmediateOnFinalUpdateComputationTask(
                                    store,
                                    currentDependee.asFinal,
                                    previousResult = r,
                                    forceDependersNotifications,
                                    c
                                )
                                appendTask(seenDependees.size, t)
                            } else {
                                val t = ImmediateOnUpdateComputationTask(
                                    store,
                                    currentDependee.toEPK,
                                    previousResult = r,
                                    forceDependersNotifications,
                                    c
                                )
                                appendTask(seenDependees.size, t)
                            }
                            // We will postpone the notification to the point where
                            // the result(s) are handled...
                            forceDependersNotifications = Set.empty
                        }

                        return ;
                    }
                }

                // When we reach this point, all potential dependee updates are taken into account;
                // otherwise we would have had an early return

                // 2.1.  Update the value (trigger dependers/clear old dependees).
                if (updateAndNotifyForRegularP(e, lb, ub, pcrs = pcrs).areDependersNotified) {
                    forceDependersNotifications -= epk
                }

                // 2.2.  The most current value of every dependee was taken into account
                //       register with new (!) dependees.
                this.dependees(pkId).put(e, seenDependees)
                val updateFunction = (c, onUpdateContinuationHint)
                seenDependees foreach { dependee ⇒
                    val dependeeE = dependee.e
                    val dependeePKId = dependee.pk.id
                    dependers(dependeePKId).
                        computeIfAbsent(dependeeE, _ ⇒ new JHashMap()).put(epk, updateFunction)
                }

            case SimplePInterimResult.id ⇒
                // TODO Unify handling with InterimResult (avoid code duplication)
                val SimplePInterimResult(e, ub, seenDependees, c, onUpdateContinuationHint) = r
                val pk = ub.key
                val pkId = pk.id
                val epk = EPK(e, pk)

                if (forceEvaluation) forcedComputations(pkId).put(e, e)

                assertNoDependees(e, pkId)

                // 1. let's check if a seen dependee is already updated; if so, we directly
                //    schedule/execute the continuation function again to continue computing
                //    the property
                val seenDependeesIterator = seenDependees.toIterator
                while (seenDependeesIterator.hasNext) {
                    val seenDependee = seenDependeesIterator.next()

                    if (debug && seenDependee.isFinal) {
                        throw new IllegalStateException(
                            s"$e/$pk: dependency to final property: $seenDependee"
                        )
                    }

                    val seenDependeeE = seenDependee.e
                    val seenDependeePKId = seenDependee.pk.id
                    val propertiesOfEntity = properties(seenDependeePKId)
                    // seenDependee is guaranteed to be not null
                    // currentDependee may be null => newDependee is an EPK => no update
                    val currentDependee = propertiesOfEntity.get(seenDependeeE)
                    if (currentDependee != null && seenDependee != currentDependee) {
                        // Make the current result available for other threads, but
                        // do not yet trigger dependers; however, we have to ensure
                        // that the dependers are eventually triggered if any update
                        // was relevant!
                        val updateAndNotifyState = updateAndNotifyForSimpleP(
                            e, ub, false,
                            notifyDependersAboutNonFinalUpdates = false,
                            pcrs
                        )
                        if (updateAndNotifyState.isNotificationRequired) {
                            forceDependersNotifications += epk
                        }

                        if (tracer.isDefined)
                            tracer.get.immediateDependeeUpdate(
                                e, pk, seenDependee, currentDependee, updateAndNotifyState
                            )

                        if (onUpdateContinuationHint == CheapPropertyComputation) {
                            directDependeeUpdatesCounter += 1
                            // we want to avoid potential stack-overflow errors...
                            pcrs += c(currentDependee)
                        } else {
                            scheduledDependeeUpdatesCounter += 1
                            if (currentDependee.isFinal) {
                                val t = ImmediateOnFinalUpdateComputationTask(
                                    store,
                                    currentDependee.asFinal,
                                    previousResult = r,
                                    forceDependersNotifications,
                                    c
                                )
                                appendTask(seenDependees.size, t)
                            } else {
                                val t = ImmediateOnUpdateComputationTask(
                                    store,
                                    currentDependee.toEPK,
                                    previousResult = r,
                                    forceDependersNotifications,
                                    c
                                )
                                appendTask(seenDependees.size, t)
                            }
                            // We will postpone the notification to the point where
                            // the result(s) are handled...
                            forceDependersNotifications = Set.empty
                        }

                        return ;
                    }
                }

                // When we reach this point, all potential dependee updates are taken into account;
                // otherwise we would have had an early return

                // 2.1.  Update the value (trigger dependers/clear old dependees).
                if (updateAndNotifyForSimpleP(e, ub, isFinal = false, pcrs = pcrs).areDependersNotified) {
                    forceDependersNotifications -= epk
                }

                // 2.2.  The most current value of every dependee was taken into account;
                //       register with new (!) dependees.
                this.dependees(pkId).put(e, seenDependees)
                val updateFunction = (c, onUpdateContinuationHint)
                seenDependees foreach { dependee ⇒
                    val dependeeE = dependee.e
                    val dependeePKId = dependee.pk.id
                    dependers(dependeePKId).
                        computeIfAbsent(dependeeE, _ ⇒ new JHashMap()).
                        put(epk, updateFunction)
                }*/
        }

        /*
        do {
            while (pcrs.nonEmpty) {
                processResult(pcrs.pop())
            }
            if (forceDependersNotifications.nonEmpty) {
                val epk = forceDependersNotifications.head
                forceDependersNotifications = forceDependersNotifications.tail
                val eps = properties(epk.pk.id).get(epk.e)
                if (tracer.isDefined) tracer.get.delayedNotification(eps)

                notifyDependers(eps, pcrs)
            }
        } while (forceDependersNotifications.nonEmpty || pcrs.nonEmpty)
        */
    }

    override def waitOnPhaseCompletion(): Unit = {
        if (!pool.awaitQuiescence(Long.MaxValue, TimeUnit.DAYS)) {
            throw new UnknownError("pool failed to reach quiescence")
        }
    }

}

object PKECPropertyStore extends ParallelPropertyStoreFactory {

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKECPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        new PKECPropertyStore(contextMap, NumberOfThreadsForProcessingPropertyComputations)
    }

    def create(
        context: Map[Class[_], AnyRef] // ,PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKECPropertyStore = {

        new PKECPropertyStore(context, NumberOfThreadsForProcessingPropertyComputations)
    }

}

/**
 *
 * @param eOptionP An atomic reference holding the current property extension; we need to
 *                 use an atomic reference to enable concurrent update operations as required
 *                 by properties computed using partial results.
 *                 The atomic reference is never null.
 */
class State(val eOptionP: AtomicReference[SomeEOptionP]) {

    // True, if an onUpdateContinuation was triggered until the result has been processed;
    // set to `true` when the OnUpdateContinuation was triggered. Set to `false` after the results
    // has been processed and a new onUpdateContinuation has been set, but before the last seen
    // dependees are checked for updates. That is, while checking for updates, it may happen that
    // the new on update continuation is triggered "externally".
    val updateComputationTriggered: AtomicBoolean = new AtomicBoolean(false)

    val dependers: ConcurrentHashMap[SomeEPK, OnUpdateContinuation] = new ConcurrentHashMap()

    // dependees is never updated concurrently
    @volatile var dependees: Traversable[SomeEOptionP] = Nil
}

object State {
    def apply(eOptionP: SomeEOptionP): State = {
        new State(new AtomicReference[SomeEOptionP](eOptionP))
    }
}
