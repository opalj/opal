/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import scala.collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.fpcf.PropertyKey.computeFastTrackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId
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
abstract class PKECPropertyStore extends ParallelPropertyStore { store ⇒

    //
    //
    // PARALLELIZATION RELATED FUNCTIONALITY
    //
    //

    protected[this] def awaitPoolQuiescence(): Unit

    protected[this] def forkPropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    ): Unit

    protected[this] def forkResultHandler(r: PropertyComputationResult): Unit

    protected[this] def forkOnUpdateContinuation(
        c:  OnUpdateContinuation,
        e:  Entity,
        pk: SomePropertyKey
    ): Unit

    protected[this] def forkOnUpdateContinuation(
        c:       OnUpdateContinuation,
        finalEP: SomeFinalEP
    ): Unit

    protected[this] def parallelize(r: Runnable): Unit

    //
    //
    // CORE DATA STRUCTURES
    //
    //

    // Per PropertyKind we use one concurrent hash map to store the entities' properties.
    // The value encompasses the current property along with some helper information.
    private[this] val properties: Array[ConcurrentHashMap[Entity, EPKState]] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    // The following "var"s/"arrays" do not need to be volatile/thread safe, because the updates –
    // which are only done while the store is quiescent – are done within the driver thread and
    // are guaranteed to be visible to all relevant tasks.

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

    override def supportsFastTrackPropertyComputations: Boolean = true

    private[this] val fastTrackPropertiesCounter = new AtomicInteger(0)
    override def fastTrackPropertiesCount: Int = fastTrackPropertiesCounter.get()

    private[this] val fastTrackPropertyComputationsCounter = new AtomicInteger(0)
    override def fastTrackPropertyComputationsCount: Int = {
        fastTrackPropertyComputationsCounter.get()
    }
    protected[this] def incrementFastTrackPropertyComputationsCounter(): Unit = {
        fastTrackPropertyComputationsCounter.incrementAndGet()
    }

    private[this] var quiescenceCounter = 0
    override def quiescenceCount: Int = quiescenceCounter

    private[this] val scheduledOnUpdateComputationsCounter = new AtomicInteger(0)
    override def scheduledOnUpdateComputationsCount: Int = {
        scheduledOnUpdateComputationsCounter.get()
    }

    private[this] val scheduledTasksCounter = new AtomicInteger(0)
    override def scheduledTasksCount: Int = scheduledTasksCounter.get()
    protected[this] def incrementScheduledTasksCounter(): Unit = {
        scheduledTasksCounter.incrementAndGet()
    }

    private[this] val fallbacksUsedForComputedPropertiesCounter = new AtomicInteger(0)
    override def fallbacksUsedForComputedPropertiesCount: Int = {
        fallbacksUsedForComputedPropertiesCounter.get()
    }
    override protected[this] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = {
        fallbacksUsedForComputedPropertiesCounter.incrementAndGet()
    }

    override def entities(p: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        this.properties.iterator.flatMap { propertiesPerKind ⇒
            propertiesPerKind
                .elements().asScala
                .collect { case EPKState(eps: SomeEPS) if p(eps) ⇒ eps.e }
        }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        properties(pk.id)
            .values().iterator().asScala
            .collect { case EPKState(eps: EPS[Entity, P] @unchecked) ⇒ eps }
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        for { EPKState(ELUBP(e, `lb`, `ub`)) ← properties(lb.id).elements().asScala } yield { e }
    }

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = {
        for { EPKState(ELBP(e, `lb`)) ← properties(lb.id).elements().asScala } yield { e }
    }

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = {
        for { EPKState(EUBP(e, `ub`)) ← properties(ub.id).elements().asScala } yield { e }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        this.properties.iterator.flatMap { map ⇒
            val ePKState = map.get(e)
            if (ePKState != null && ePKState.eOptionP.isEPS)
                Iterator.single(ePKState.eOptionP.asInstanceOf[EPS[E, Property]])
            else
                Iterator.empty
        }

    }

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        val state = properties(pk.id).get(e)
        state != null && {
            val eOptionP = state.eOptionP
            eOptionP.hasUBP || eOptionP.hasLBP
        }
    }

    override def isKnown(e: Entity): Boolean = {
        properties.exists(propertiesOfKind ⇒ propertiesOfKind.containsKey(e))
    }

    //
    //
    // CORE IMPLEMENTATION
    //
    //

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {
        forkPropertyComputation(e, pc)
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
        val oldP = properties(p.id).put(e, EPKState(FinalEP(e, p)))
        if (oldP != null) {
            throw new IllegalStateException(s"$e had already the property $oldP")
        }
    }

    override def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] ⇒ InterimEP[E, P]
    ): Unit = {
        val pkId = pk.id
        val propertiesOfKind = properties(pkId)
        val newInterimP: SomeInterimEP =
            propertiesOfKind.get(e) match {
                case null  ⇒ pc(EPK(e, pk))
                case state ⇒ pc(state.eOptionP.asInstanceOf[EOptionP[E, P]])
            }

        propertiesOfKind.put(e, EPKState(newInterimP))
    }

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        val propertiesOfKind = properties(pkId)

        /*
        var isNewEPKState = false
        val epkState = propertiesOfKind.computeIfAbsent(e, (_) ⇒ {
            isNewEPKState = true
            EPKState(epk)
        })
        */
        val newEPKState = EPKState(epk) // eager constructing an EPKState
        val epkState = propertiesOfKind.putIfAbsent(e, newEPKState)
        val isNewEPKState = epkState == null

        if (!isNewEPKState) {
            // just return the current value
            return epkState.eOptionP.asInstanceOf[EOptionP[E, P]];
        }

        // The entity was not yet known ... let's check if:
        //  - we have to trigger a lazy property computation
        //     - we can compute a fast track result
        //  - have to compute the fallback because no analysis is scheduled
        val lc = lazyComputations(pkId)
        if (lc != null) {
            if (useFastTrackPropertyComputations) {
                incrementFastTrackPropertyComputationsCounter()
                val r = computeFastTrackPropertyBasedOnPKId[P](this, e, pkId)
                if (r.isDefined) {
                    val p = r.get
                    val finalP = FinalEP(e, p)
                    finalUpdate(finalP, potentiallyIdemPotentUpdate = true)
                    return finalP;
                }
            }
            forkPropertyComputation(e, lc.asInstanceOf[PropertyComputation[E]])
            epk
        } else if (!propertyKindsComputedInThisPhase(pkId)) {
            val r = computeFallback[E, P](e, pkId)
            finalUpdate(r, potentiallyIdemPotentUpdate = true)
            r
        } else {
            epk
        }
    }

    // THIS METHOD IS NOT INTENDED TO BE USED BY CPropertyStores- IT IS ONLY MEANT TO
    // BE USED BY EXTERNAL TASKS.
    override def handleResult(r: PropertyComputationResult): Unit = forkResultHandler(r)

    private[this] def notifyDepender(dependerEPK: SomeEPK, eps: SomeEPS): Unit = {
        val dependerState = properties(dependerEPK.pk.id).get(dependerEPK.e)
        val c = dependerState.getAndClearOnUpdateComputation()
        if (c != null) {
            forkOnUpdateContinuation(c, eps.e, eps.pk)
        }
    }

    private[this] def notifyDepender(dependerEPK: SomeEPK, finalEP: SomeFinalEP): Unit = {
        val dependerState = properties(dependerEPK.pk.id).get(dependerEPK.e)
        val c = dependerState.getAndClearOnUpdateComputation()
        if (c != null) {
            forkOnUpdateContinuation(c, finalEP)
        }
    }

    def clearDependees(depender: SomeEPK, dependees: Traversable[SomeEOptionP]): Unit = {
        dependees foreach { dependee ⇒
            val dependeeState = properties(dependee.pk.id).get(dependee.e)
            dependeeState.removeDepender(depender)
        }
    }

    private[this] def finalUpdate(
        finalEP:                     SomeFinalEP,
        potentiallyIdemPotentUpdate: Boolean     = false
    ): Unit = {
        val newState = EPKState(finalEP)
        val oldState = properties(finalEP.pk.id).put(finalEP.e, newState)
        if (oldState != null) {
            if (!oldState.isRefinable) {
                if (!potentiallyIdemPotentUpdate)
                    throw new IllegalStateException(
                        s"the old state $oldState is already final (new: $finalEP)"
                    )
                else
                    return ; // IDEMPOTENT UPDATE
            }
            // We had a state object which means we may have dependers.
            //
            // We now have to inform the dependers. Though this is a
            // non-atomic operation, we can still simply inform all dependers
            // because is it guaranteed that they have not seen the final value since dependencies
            // on final values are not allowed!
            //
            // Note that – when we register a depender – it is the responsibility of the registrar
            // to check that it has seen the last value after updating the dependers list!
            val oldEOptionP = oldState.eOptionP
            if (debug) oldEOptionP.checkIsValidPropertiesUpdate(finalEP, Nil)

            // ... the following line may clear dependers that have not been triggered, but
            // this doesn't matter, because when registration happens, it is the responsibility
            // of that thread to afterwards check that the value hasn't changed.
            oldState.getAndClearDependers().foreach(epk ⇒ notifyDepender(epk, finalEP))

            clearDependees(finalEP.toEPK, oldState.dependees)
        }
    }

    // NOTES regarding concurrency:
    // W.r.t. one e/pk there will always be at most one update across all threads.
    private[this] def interimUpdate(
        interimEP: SomeInterimEP,
        c:         OnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        val psPerKind = properties(interimEP.pk.id)

        // 1. Update the property if necessary.
        var notificationRequired = false
        val state = {
            val newState = EPKState(interimEP, c, dependees)
            val existingState = psPerKind.putIfAbsent(interimEP.e, newState)
            if (existingState == null)
                newState
            else {
                val oldEOptionP = existingState.updateEOptionP(interimEP, debug)
                notificationRequired = interimEP.isUpdatedComparedTo(oldEOptionP)
                // Before we set the "new" OnUpdateComputation function, we first have
                // to deregister with all old dependees to avoid that the continuation
                // function is called by an outdated dependee from which it doesn't expect
                // any more updates!
                // IMPROVE Only deregister those dependees that are necessary; this however requires collaboration with the analyses to make efficient decisions possible (ATTENTION: currently we only register as long as necessary!)
                clearDependees(interimEP.toEPK, existingState.dependees)
                existingState.setDependees(dependees)
                existingState.setOnUpdateComputation(c)
                existingState
            }
        }

        // 2. Register with dependees (as depender) and while doing so check if the value
        //    was updated.
        //    We stop the registration with the dependees when the continuation function
        //    is triggered.
        val dependerEPK = interimEP.toEPK
        dependees forall { processedDependee ⇒
            state.hasPendingOnUpdateComputation && {
                val dependeeState = properties(processedDependee.pk.id).get(processedDependee.e)
                dependeeState.addDepender(dependerEPK)
                val currentDependee = dependeeState.eOptionP
                if (currentDependee.isUpdatedComparedTo(processedDependee)) {
                    // A dependee was updated; let's trigger the OnUpdateContinuation if it
                    // wasn't already triggered concurrently.
                    val currentC = state.getAndClearOnUpdateComputation()
                    if (currentC != null) {
                        forkOnUpdateContinuation(currentC, processedDependee.e, processedDependee.pk)
                    }
                    false // we don't need to register further dependers
                } else {
                    true
                }
            }
        }

        // 3. Notify dependers
        if (notificationRequired) {
            state.getAndClearDependers().foreach(epk ⇒ notifyDepender(epk, interimEP))
        }
    }

    private[this] def removeDependerFromDependees(dependerEPK: SomeEPK): Unit = {
        for {
            dependee ← properties(dependerEPK.pk.id).get(dependerEPK.e).dependees
        } {
            properties(dependee.pk.id).get(dependee.e).removeDepender(dependerEPK)
        }
    }

    protected[this] def processResult(r: PropertyComputationResult): Unit = handleExceptions {

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
                finalUpdate(finalEP)

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { finalEP ⇒ finalUpdate(finalEP) }

            case InterimResult.id ⇒
                val interimR = r.asInterimResult
                interimUpdate(interimR.eps, interimR.c, interimR.dependees)

            /*

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

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        val maxPKIndex = PropertyKey.maxId

        val continueComputation = new AtomicBoolean(false)
        do {
            continueComputation.set(false)
            awaitPoolQuiescence()
            quiescenceCounter += 1
            if (debug) trace("analysis progress", s"reached quiescence $quiescenceCounter")

            // We have reached quiescence....

            // 1. Let's search for all EPKs (not EPS) and use the fall back for them.
            //    (Recall that we return fallback properties eagerly if no analysis is
            //     scheduled or will be scheduled, However, it is still possible that we will
            //     not have computed a property for a specific entity if the underlying
            //     analysis doesn't compute one; in that case we need to put in fallback
            //     values.)
            var pkIdIterator = 0
            while (pkIdIterator <= maxPKIndex) {
                if (propertyKindsComputedInThisPhase(pkIdIterator)) {
                    val pkId = pkIdIterator
                    parallelize(() ⇒ {
                        val epkStateIterator =
                            properties(pkId)
                                .values.iterator().asScala
                                .filter { epkState ⇒
                                    epkState.isEPK &&
                                        // There is no suppression; i.e., we have no dependees
                                        epkState.dependees.isEmpty
                                }
                        if (epkStateIterator.hasNext) continueComputation.set(true)
                        epkStateIterator.foreach { epkState ⇒
                            println("State without dependees: "+epkState)
                            val e = epkState.e
                            val reason = PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                            val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                            if (traceFallbacks) {
                                trace("analysis progress", s"used fallback $p for $e")
                            }
                            fallbacksUsedForComputedPropertiesCounter.incrementAndGet()
                            finalUpdate(FinalEP(e, p))
                        }
                    })
                }
                pkIdIterator += 1
            }
            awaitPoolQuiescence()

            // 2... suppression

            // 3. Let's finalize remaining interim EPS; e.g., those related to
            //    collaboratively computed properties or "just all" if we don't have suppressed
            //    notifications. Recall that we may have cycles if we have no suppressed
            //    notifications, because in the latter case, we may have dependencies.
            //    We used no fallbacks, but we may still have collaboratively computed properties
            //    (e.g. CallGraph) which are not yet final; let's finalize them in the specified
            //    order (i.e., let's finalize the subphase)!
            while (!continueComputation.get() && subPhaseId < subPhaseFinalizationOrder.length) {
                val pksToFinalize = subPhaseFinalizationOrder(subPhaseId)
                if (debug) {
                    trace(
                        "analysis progress",
                        pksToFinalize.map(PropertyKey.name).mkString("finalization of: ", ", ", "")
                    )
                }
                // The following will also kill dependers related to anonymous computations using
                // the generic property key: "AnalysisKey"; i.e., those without explicit properties!
                pksToFinalize foreach { pk ⇒
                    val propertyKey = PropertyKey.key(pk.id)
                    parallelize(() ⇒ {
                        val dependeesIt = properties(pk.id).elements().asScala.filter(_.hasDependees)
                        if (dependeesIt.hasNext) continueComputation.set(true)
                        dependeesIt foreach { epkState ⇒
                            removeDependerFromDependees(EPK(epkState.e, propertyKey))
                        }
                    })
                }
                awaitPoolQuiescence()

                pksToFinalize foreach { pk ⇒
                    parallelize(() ⇒ {
                        val interimEPSStates = properties(pk.id).values().asScala.filter(_.isRefinable)
                        interimEPSStates foreach { interimEPKState ⇒
                            finalUpdate(interimEPKState.eOptionP.toFinalEP)
                        }
                    })
                }
                awaitPoolQuiescence()

                subPhaseId += 1
            }
        } while (continueComputation.get())

        // TODO assert that we don't have any more InterimEPKStates
    }

}

