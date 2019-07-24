/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.{Arrays ⇒ JArrays}

import com.typesafe.config.Config

import scala.collection.JavaConverters._

import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.LogContext
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds

/**
 * A concurrent implementation of the property store which executes the scheduled computations
 * in parallel.
 *
 * The number of threads that is used for parallelizing the computation is determined by:
 * `NumberOfThreadsForProcessingPropertyComputations`
 *
 * @author Michael Eichberg
 */
final class PKECPropertyStore(
        final val ctx:                   Map[Class[_], AnyRef],
        implicit final val tasksManager: TasksManager
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore { store ⇒

    private[par] implicit def self: PKECPropertyStore = this

    override def MaxEvaluationDepth: Int = tasksManager.MaxEvaluationDepth
    override def shutdown(): Unit = tasksManager.shutdown()
    override def isIdle: Boolean = tasksManager.isIdle

    //
    //
    // PARALLELIZATION RELATED FUNCTIONALITY
    //
    //

    import tasksManager.prepareThreadPool
    import tasksManager.awaitPoolQuiescence
    import tasksManager.parallelize
    import tasksManager.forkResultHandler
    import tasksManager.forkOnUpdateContinuation
    import tasksManager.forkLazyPropertyComputation
    import tasksManager.schedulePropertyComputation
    import tasksManager.cleanUpThreadPool

    // --------------------------------------------------------------------------------------------
    //
    // STATISTICS
    //
    // --------------------------------------------------------------------------------------------

    private[this] var quiescenceCounter = 0
    override def quiescenceCount: Int = quiescenceCounter

    private[this] val scheduledOnUpdateComputationsCounter = new AtomicInteger(0)
    override def scheduledOnUpdateComputationsCount: Int = {
        scheduledOnUpdateComputationsCounter.get()
    }
    private[fpcf] def incrementOnUpdateContinuationsCounter(): Unit = {
        if (debug) scheduledOnUpdateComputationsCounter.incrementAndGet()
    }

    private[this] val scheduledTasksCounter = new AtomicInteger(0)
    override def scheduledTasksCount: Int = scheduledTasksCounter.get()
    private[fpcf] def incrementScheduledTasksCounter(): Unit = {
        if (debug) scheduledTasksCounter.incrementAndGet()
    }

    private[this] val fallbacksUsedForComputedPropertiesCounter = new AtomicInteger(0)
    override def fallbacksUsedForComputedPropertiesCount: Int = {
        fallbacksUsedForComputedPropertiesCounter.get()
    }
    override private[fpcf] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = {
        if (debug) fallbacksUsedForComputedPropertiesCounter.incrementAndGet()
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // --------------------------------------------------------------------------------------------

    // Per PropertyKind we use one concurrent hash map to store the entities' properties.
    // The value (EPKState) encompasses the current property along with some helper information.
    private[par] val properties: Array[ConcurrentHashMap[Entity, EPKState]] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    /**
     * Computations that will be triggered when a new property becomes available.
     *
     * Please note, that the triggered computations have to be registered strictly before the first
     * computation is started.
     */
    private[par] val triggeredComputations: Array[Array[SomePropertyComputation]] = {
        new Array(SupportedPropertyKinds)
    }

    private[par] val forcedEPKs: ConcurrentLinkedQueue[SomeEPK] = new ConcurrentLinkedQueue()

    // --------------------------------------------------------------------------------------------
    //
    // BASIC QUERY METHODS (ONLY TO BE CALLED WHEN THE STORE IS QUIESCENT)
    //
    // --------------------------------------------------------------------------------------------

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val ps = for {
                (entitiesMap, pkId) ← properties.iterator.zipWithIndex.take(PropertyKey.maxId + 1)
            } yield {
                entitiesMap.values().iterator().asScala
                    .map(_.eOptionP.toString.replace("\n", "\n\t"))
                    .toList.sorted
                    .mkString(s"Entities for property key $pkId:\n\t", "\n\t", "\n")
            }
            ps.mkString("PropertyStore(\n\t", "\n\t", "\n)")
        } else {
            s"PropertyStore(properties=${properties.iterator.map(_.size).sum})"
        }
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

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - NOT THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {
        schedulePropertyComputation(e, pc)
    }

    override def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {

        // Recall that the scheduler has to take care of registering a triggered computation
        // before the first analysis derives a respective value!
        // Hence, there is no need to immediately check that we have to trigger a computation.

        val pkId = pk.id
        val oldComputations: Array[SomePropertyComputation] = triggeredComputations(pkId)
        var newComputations: Array[SomePropertyComputation] = null

        if (oldComputations == null) {
            newComputations = Array[SomePropertyComputation](pc)
        } else {
            newComputations = JArrays.copyOf(oldComputations, oldComputations.length + 1)
            newComputations(oldComputations.length) = pc
        }
        triggeredComputations(pkId) = newComputations
    }

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

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        forcedEPKs.add(EPK(e, pk))
    }

    private[par] def triggerForcedEPKs(): Boolean = {
        val hadForcedEPKs = !forcedEPKs.isEmpty
        var forcedEPK = forcedEPKs.poll()
        while (forcedEPK != null) {
            this(forcedEPK)
            forcedEPK = forcedEPKs.poll()
        }
        hadForcedEPKs
    }

    override def handleResult(r: PropertyComputationResult): Unit = forkResultHandler(r)

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        val psOfKind = properties(pkId)

        // In the following, we just ensure that we only create a new EPKState object if
        // the chances are high that we need it. Conceptually, we just do a "putIfAbsent"
        var oldEPKState = psOfKind.get(e)
        if (oldEPKState != null) {
            // just return the current value
            return oldEPKState.eOptionP.asInstanceOf[EOptionP[E, P]];
        }
        val epkState = EPKState(epk) /* eagerly construct EPKState */
        oldEPKState = psOfKind.putIfAbsent(e, epkState)
        if (oldEPKState != null) {
            return oldEPKState.eOptionP.asInstanceOf[EOptionP[E, P]];
        }

        // -----------------------------------------------------------------------------------------
        // ---- This part is executed exactly once per EP Pair and (hence), never concurrently.
        // ---- But, given that the EPK State object is already registered, it may be possible
        // ---- that dependers are registered!
        //
        // WE CREATED A NEW EPK STATE OBJECT - LET'S CHECK WHAT NEEDS TO BE DONE:
        //  - trigger lazy computation ?
        //  - compute fall back value ? (If no analysis is scheduled.)
        //  - "just wait" ? (If the property is eagerly computed but the respective computation
        //    did not yet complete.)

        val lc = lazyComputations(pkId)
        if (lc != null) {
            forkLazyPropertyComputation(epk, lc.asInstanceOf[PropertyComputation[E]])
        } else if (propertyKindsComputedInThisPhase(pkId)) {
            val transformerSpecification = transformersByTargetPK(pkId)
            if (transformerSpecification != null) {
                // ... we have a transformer that can produce a property of the required kind;
                // let's check if we can invoke it now or have to invoke it later.
                val (sourcePK, transform) = transformerSpecification
                val sourceEPK = EPK(e, sourcePK)
                var sourceEOptionP: SomeEOptionP = null
                var sourceEPKState: EPKState = null
                var cSet: Boolean = false
                do {
                    // "apply" is necessary to ensure that all necessary lazy analyses get triggered.
                    sourceEOptionP = apply(sourceEPK)
                    if (sourceEOptionP.isFinal) {
                        val sourceP = sourceEOptionP.asFinal.lb
                        val finalEP = transform(e, sourceP).asInstanceOf[FinalEP[E, P]]
                        finalUpdate(finalEP)
                        return finalEP;
                    } else {
                        // Add this transformer as a depender to the transformer's source;
                        // this strictly requires that intermediate values are suppressed.
                        sourceEPKState = properties(sourcePK.id).get(e)
                        // We are not yet registered with the dependee; hence we can't have concurrent
                        // notifications even though we set the dependees here!
                        if (!cSet) {
                            epkState.c = (eps) ⇒ { Result(transform(e, eps.lb /*or ub*/ )) }
                            cSet = true
                        }
                        epkState.dependees = Set(sourceEOptionP)
                    }
                } while (!sourceEPKState.addDepender(sourceEOptionP, epk))
            }
            epk
        } else {
            // ... we have no lazy computation
            // ... the property is also not computed
            val finalEP = computeFallback[E, P](e, pkId)
            finalUpdate(finalEP, potentiallyIdemPotentUpdate = true)
            finalEP
        }
    }

    // NOTES REGARDING CONCURRENCY
    // Before a continuation function is called a depender has to be removed from
    // its dependees!
    private[par] def removeDependerFromDependeesAndClearDependees(
        depender:         SomeEPK,
        dependerEPKState: EPKState
    ): Unit = {
        val dependees = dependerEPKState.dependees
        dependees foreach { dependee ⇒
            val dependeeEPKState = properties(dependee.pk.id).get(dependee.e)
            dependeeEPKState.removeDepender(depender)
        }
        dependerEPKState.clearDependees()
    }

    private[this] def removeDependerFromDependeesAndClearDependees(dependerEPK: SomeEPK): Unit = {
        removeDependerFromDependeesAndClearDependees(
            dependerEPK,
            properties(dependerEPK.pk.id).get(dependerEPK.e)
        )
    }

    private[this] def notifyDepender(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        interimEP:   SomeInterimEP
    ): Unit = {
        val dependerPKId = dependerEPK.pk.id
        if (suppressInterimUpdates(dependerPKId)(interimEP.pk.id))
            return ;

        val dependerEPKState = properties(dependerPKId).get(dependerEPK.e)
        val cOption = dependerEPKState.prepareInvokeC(oldEOptionP)
        if (cOption.isDefined) {
            // We first have to remove the depender from the dependees before
            // we can fork the computation of the continuation function.
            removeDependerFromDependeesAndClearDependees(dependerEPK, dependerEPKState)
            forkOnUpdateContinuation(cOption.get, interimEP.e, interimEP.pk)
        }
    }

    private[this] def notifyDepender(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        finalEP:     SomeFinalEP
    ): Unit = {
        val dependerPKId = dependerEPK.pk.id
        val dependerEPKState = properties(dependerPKId).get(dependerEPK.e)
        val cOption = dependerEPKState.prepareInvokeC(oldEOptionP)
        if (cOption.isDefined) {
            // We first have to remove the depender from the dependees before
            // we can fork the computation of the continuation function.
            removeDependerFromDependeesAndClearDependees(dependerEPK, dependerEPKState)
            forkOnUpdateContinuation(cOption.get, finalEP)
        }
    }

    private[this] def finalUpdate(
        finalEP:                     SomeFinalEP,
        potentiallyIdemPotentUpdate: Boolean     = false
    ): Unit = {
        val oldEPKState = properties(finalEP.pk.id).put(finalEP.e, EPKState(finalEP))
        if (oldEPKState != null) {
            if (oldEPKState.isFinal) {
                if (potentiallyIdemPotentUpdate)
                    return ; // IDEMPOTENT UPDATE
                else
                    throw new IllegalStateException(
                        s"already final: $oldEPKState; illegal property update: $finalEP)"
                    )
            }

            if (debug) oldEPKState.eOptionP.checkIsValidPropertiesUpdate(finalEP, Nil)

            // Recall that we do not clear the dependees eagerly when we have to register a transformer
            // DOESN'T WORK: assert(oldState.dependees.isEmpty)

            // We have to update the value of the oldEPKState to ensure that clients that
            // want to register a depender see the most current value.
            val oldEOptionP = oldEPKState.eOptionP
            val dependers = oldEPKState.finalUpdate(finalEP)
            // We now have to inform the dependers.
            // We can simply inform all dependers because is it guaranteed that they have not seen
            // the final value since dependencies on final values are not allowed!
            // However, it is possible that the depender is actually no longer interested in the
            // update, which is checked for by notifyDepender.
            dependers.foreach(epk ⇒ notifyDepender(epk, oldEOptionP, finalEP))
        }
        // If the oldState was null then there is nothing to do.
    }

    // NOTES REGARDING CONCURRENCY
    // W.r.t. one EPK there may be multiple executions of this method concurrently!
    private[this] def interimUpdate(
        interimEP: SomeInterimEP,
        c:         OnUpdateContinuation,
        hint:      PropertyComputationHint,
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        val psPerKind = properties(interimEP.pk.id)

        // 0. Get EPKState object.
        var epkStateUpdateRequired = true
        val epkState = {
            var epkState = psPerKind.get(interimEP.e)
            if (epkState == null) {
                val newEPKState = EPKState(interimEP, c, dependees)
                epkState = psPerKind.putIfAbsent(interimEP.e, newEPKState)
                if (epkState == null) {
                    epkStateUpdateRequired = false
                    newEPKState
                } else {
                    epkState
                }
            } else {
                epkState
            }
        }

        // 1. Update the property if necessary.
        //    Though, we can have concurrent executions of this method, it is still always
        //    the case that we will only see monotonic updates; i.e., this part of this
        //    method is never executed concurrently; only the first part may be executed
        //    concurrently with the second part.
        val eOptionPWithDependersOption: Option[(SomeEOptionP, Traversable[SomeEPK])] =
            if (epkStateUpdateRequired) {
                epkState.update(interimEP, c, dependees, debug)
            } else {
                None
            }

        // ATTENTION:   - - - - - - - - - - - H E R E   A R E   D R A G O N S - - - - - - - - - - -
        //              As soon as we register with the first dependee, we can have concurrent
        //              updates, which (in an extreme case) can already be completely finished
        //              between every two statements of this method! That is, the dependees
        //              and the continuation function given to this method may already be outdated!
        //              Hence, before we call the given OnUpdateContinuation due to an updated
        //              dependee, we have to check if it is still the current one!

        // 2. Register with dependees (as depender) and while doing so check if the value
        //    was updated.
        //    We stop the registration with the dependees when the continuation function
        //    is triggered.
        val dependerEPK = interimEP.toEPK
        dependees forall { processedDependee ⇒
            epkState.isCurrentC(c) && {
                val psPerDependeeKind = properties(processedDependee.pk.id)
                val dependeeEPKState = psPerDependeeKind.get(processedDependee.e)
                if (!dependeeEPKState.addDepender(processedDependee, dependerEPK)) {
                    // addDepender failed... i.e., the dependee was updated...
                    val cOption = epkState.prepareInvokeC(c)
                    if (cOption.isDefined) {
                        val c = cOption.get
                        // we now remove our dependee registrations...
                        dependees forall { registeredDependee ⇒
                            if (registeredDependee ne processedDependee) {
                                properties(registeredDependee.pk.id).get(registeredDependee.e).removeDepender(dependerEPK)
                                true
                            } else {
                                false
                            }
                        }
                        forkOnUpdateContinuation(c, processedDependee.e, processedDependee.pk)
                    } else {
                        // clear possibly dangling dependees... which can happen if
                        // we have registered with a dependee which is updated while
                        // we still process the current dependees; based on the result
                        // of running "c" the set of dependees is changed based on a EPK
                        // basis when compared to this dependees. Now, we have to remove
                        // those dependees that are no longer relevant.
                        // Basically, we check for each dependee if the dependee is still
                        // relevant. If not, we remove it. Note that a client is NOT
                        // allowed to ever reregister a dependency. I.e., a client can change
                        // the set of dependees, but the new set must never contain a dependee
                        // – w.r.t. the underlying EPK – which was already a dependee but which
                        // is not part of the directly preceding set.
                        // IMPROVE consider using
                        val currentDependees = epkState.dependees
                        dependees forall { registeredDependee ⇒
                            if (registeredDependee ne processedDependee) {
                                if (currentDependees.forall(_ != registeredDependee)) {
                                    properties(registeredDependee.pk.id).get(registeredDependee.e).removeDepender(dependerEPK)
                                }
                                true
                            } else {
                                false
                            }
                        }
                    }
                    false
                } else {
                    true
                }
            }
        }

        // 3. Notify dependers
        if (eOptionPWithDependersOption.isDefined) {
            val (oldEOptionP, dependers) = eOptionPWithDependersOption.get
            dependers.foreach(epk ⇒ notifyDepender(epk, oldEOptionP, interimEP))
        }
    }

    private[par] def processResult(r: PropertyComputationResult): Unit = handleExceptions {

        r.id match {

            case NoResult.id ⇒ {
                // A computation reported no result; i.e., it is not possible to
                // compute a/some property/properties for a given entity.
            }

            //
            // Result containers
            //

            case Results.id ⇒ r.asResults.foreach { processResult }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs, propertyComputationsHint) = r
                processResult(ir)
                npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                    val (pc, e) = npc
                    schedulePropertyComputation(e, pc)
                }

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
                interimUpdate(
                    interimR.eps,
                    interimR.c,
                    interimR.hint,
                    interimR.dependees
                )

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
*/
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

        prepareThreadPool()

        val continueComputation = new AtomicBoolean(false)
        do {
            continueComputation.set(false)

            triggerForcedEPKs() // Ignored the return value because we call awaitPoolQuiescence next

            awaitPoolQuiescence()
            quiescenceCounter += 1
            if (debug) trace("analysis progress", s"reached quiescence $quiescenceCounter")

            // We have reached quiescence....

            // 1. Let's search for all EPKs (not EPS) and use the fall back for them.
            //    (Recall that we return fallback properties eagerly if no analysis is
            //    scheduled or will be scheduled, However, it is still possible that we will
            //    not have computed a property for a specific entity if the underlying
            //    analysis doesn't compute one; in that case we need to put in fallback
            //    values.)
            var pkIdIterator = 0
            while (pkIdIterator <= maxPKIndex) {
                if (propertyKindsComputedInThisPhase(pkIdIterator)) {
                    val pkId = pkIdIterator
                    parallelize {
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
                            // TODO TRACE: println("State without dependees: "+epkState)
                            val e = epkState.e
                            val reason = PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                            val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                            if (traceFallbacks) {
                                trace("analysis progress", s"used fallback $p for $e")
                            }
                            incrementFallbacksUsedForComputedPropertiesCounter()
                            finalUpdate(FinalEP(e, p))
                        }
                    }
                }
                pkIdIterator += 1
            }
            awaitPoolQuiescence()

            // 2... suppression

            if (triggerForcedEPKs()/* forces the evaluation as a side-effect */) {
                awaitPoolQuiescence()
                continueComputation.set(true)
            }

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
                    parallelize {
                        val dependeesIt = properties(pk.id).elements().asScala.filter(_.hasDependees)
                        if (dependeesIt.hasNext) continueComputation.set(true)
                        dependeesIt foreach { epkState ⇒
                            removeDependerFromDependeesAndClearDependees(EPK(epkState.e, propertyKey))
                        }
                    }
                }
                awaitPoolQuiescence()

                pksToFinalize foreach { pk ⇒
                    parallelize {
                        val interimEPSStates = properties(pk.id).values().asScala.filter(_.isRefinable)
                        interimEPSStates foreach { interimEPKState ⇒
                            finalUpdate(interimEPKState.eOptionP.toFinalEP)
                        }
                    }
                }
                awaitPoolQuiescence()

                if (triggerForcedEPKs()) {
                    awaitPoolQuiescence()
                    continueComputation.set(true)
                }

                subPhaseId += 1
            }
        } while (continueComputation.get())

        // TODO assert that we don't have any more InterimEPKStates

        cleanUpThreadPool()
    }

}

object PKECPropertyStore extends PropertyStoreFactory {

    final val TasksManagerKey = "org.opalj.fpcf.par.PKECPropertyStore.TasksManager"
    final val MaxEvaluationDepthKey = "org.opalj.fpcf.par.PKECPropertyStore.MaxEvaluationDepth"

    final val Strategies = List(
        "Seq"
    )

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKECPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        val config =
            contextMap.get(classOf[Config]) match {
                case Some(config: Config) ⇒ config
                case _                    ⇒ org.opalj.BaseConfig
            }
        val taskManagerId = config.getString(TasksManagerKey)
        val maxEvaluationDepth = config.getInt(MaxEvaluationDepthKey)
        apply(taskManagerId, maxEvaluationDepth)(contextMap)
    }

    def apply(
        taskManagerId:      String,
        maxEvaluationDepth: Int
    )(
        context: Map[Class[_], AnyRef] = Map.empty
    )(
        implicit
        logContext: LogContext
    ): PKECPropertyStore = {
        val tasksManager: TasksManager = taskManagerId match {
            case "Seq" ⇒ new SeqTasksManager(maxEvaluationDepth)

            case _     ⇒ throw new IllegalArgumentException(s"unknown task manager $taskManagerId")
        }

        val ps = new PKECPropertyStore(context, tasksManager)
        ps
    }

}

