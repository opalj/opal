/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import scala.language.existentials

import java.util.ArrayDeque

import scala.collection.mutable
import scala.collection.{Map ⇒ SomeMap}
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.ArrayBuffer

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.computeFastTrackPropertyBasedOnPKId

/**
 * A reasonably optimized, complete, but non-concurrent implementation of the property store.
 * Primarily intended to be used for evaluation, debugging and prototyping purposes.
 *
 * @author Michael Eichberg
 */
final class PKESequentialPropertyStore private (
        val ctx: Map[Class[_], AnyRef]
)(
        implicit
        val logContext: LogContext
) extends SeqPropertyStore { store ⇒

    // --------------------------------------------------------------------------------------------
    //
    // CAPABILITIES
    //
    // --------------------------------------------------------------------------------------------

    final def supportsFastTrackPropertyComputations: Boolean = true

    // --------------------------------------------------------------------------------------------
    //
    // STATISTICS
    //
    // --------------------------------------------------------------------------------------------

    /**
     * Controls in which order updates are processed/scheduled.
     *
     * May be changed (concurrently) at any time.
     */
    @volatile var dependeeUpdateHandling: DependeeUpdateHandling = new DependeeUpdateHandling

    /**
     * Controls in which order dependers are processed/scheduled.
     *
     * May be changed (concurrently) at any time.
     */
    @volatile var delayHandlingOfDependerNotification: Boolean = true

    private[this] var scheduledTasksCounter: Int = 0
    def scheduledTasksCount: Int = scheduledTasksCounter

    private[this] var scheduledOnUpdateComputationsCounter: Int = 0
    def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputationsCounter

    private[this] var immediateOnUpdateComputationsCounter: Int = 0
    def immediateOnUpdateComputationsCount: Int = immediateOnUpdateComputationsCounter

    private[this] var fallbacksUsedForComputedPropertiesCounter: Int = 0
    def fallbacksUsedForComputedPropertiesCount: Int = fallbacksUsedForComputedPropertiesCounter

    private[this] var quiescenceCounter = 0
    def quiescenceCount: Int = quiescenceCounter

    private[this] var fastTrackPropertiesCounter = 0
    def fastTrackPropertiesCount: Int = fastTrackPropertiesCounter

    def statistics: SomeMap[String, Int] = {
        mutable.LinkedHashMap(
            "scheduled tasks" ->
                scheduledTasksCount,
            "scheduled on update computations" ->
                scheduledOnUpdateComputationsCount,
            "fast-track properties" ->
                fastTrackPropertiesCount,
            "computations of fallback properties for computed properties" ->
                fallbacksUsedForComputedPropertiesCounter,
            "quiescence" ->
                quiescenceCount
        )
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // --------------------------------------------------------------------------------------------

    // If the map's value is an epk a lazy analysis was started if it exists.
    private[this] val ps: Array[mutable.AnyRefMap[Entity, SomeEOptionP]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { mutable.AnyRefMap.empty }
    }

    private[this] val dependers: Array[AnyRefMap[Entity, AnyRefMap[SomeEPK, (OnUpdateContinuation, PropertyComputationHint)]]] = {
        Array.fill(SupportedPropertyKinds) { new AnyRefMap() }
    }

    private[this] val dependees: Array[AnyRefMap[Entity, Traversable[SomeEOptionP]]] = {
        Array.fill(SupportedPropertyKinds) { new AnyRefMap() }
    }

    // The registered triggered computations along with the set of entities for which the analysis was triggered
    private[this] var triggeredComputations: Array[mutable.AnyRefMap[SomePropertyComputation, mutable.HashSet[Entity]]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { mutable.AnyRefMap.empty }
    }

    // The list of scheduled computations
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    private[this] var subPhaseId: Int = 0
    private[this] var hasSuppressedNotifications: Boolean = false

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val properties = for {
                (epks, pkId) ← ps.iterator.zipWithIndex
                (e, eOptionP) ← epks.iterator
            } yield {
                val propertyKindName = PropertyKey.name(pkId)
                s"$e -> $propertyKindName[$pkId] = $eOptionP"
            }
            properties.mkString("PropertyStore(\n\t\t", "\n\t\t", "\n)")
        } else {
            s"PropertyStore(#properties=${ps.iterator.map(_.size).sum})"
        }
    }

    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        require(e ne null)
        val eOptionPOption = ps(pk.id).get(e)
        eOptionPOption.isDefined && {
            val eOptionP = eOptionPOption.get
            eOptionP.hasUBP || eOptionP.hasLBP
        }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        require(e ne null)
        for {
            epks ← ps.iterator
            eOptionPOption = epks.get(e)
            if eOptionPOption.isDefined
            eOptionP = eOptionPOption.get
            if eOptionP.isEPS
        } yield {
            eOptionP.asEPS.asInstanceOf[EPS[E, Property]]
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        // We have no further EPKs when we are quiescent!
        for {
            epks ← ps.iterator
            (e, eOptionP) ← epks.iterator
            eps ← eOptionP.toEPS
            if propertyFilter(eps)
        } yield {
            e
        }
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        require(lb ne null)
        require(ub ne null)
        assert(lb.key == ub.key)
        for { ELUBP(e, `lb`, `ub`) ← ps(lb.key.id).valuesIterator } yield { e }
    }

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = {
        require(lb ne null)
        for { ELBP(e, `lb`) ← ps(lb.key.id).valuesIterator } yield { e }
        entities { otherEPS: SomeEPS ⇒ otherEPS.hasLBP && lb == otherEPS.lb }
    }

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = {
        require(ub ne null)
        for { EUBP(e, `ub`) ← ps(ub.key.id).valuesIterator } yield { e }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps(pk.id).valuesIterator.collect { case eps: SomeEPS ⇒ eps.asInstanceOf[EPS[Entity, P]] }
    }

    override def finalEntities[P <: Property](p: P): Iterator[Entity] = {
        entities((otherEPS: SomeEPS) ⇒ otherEPS.isFinal && otherEPS.asFinal.p == p)
    }

    override protected[this] def newPhaseInitialized(
        propertyKindsComputedInThisPhase:  Set[PropertyKind],
        propertyKindsComputedInLaterPhase: Set[PropertyKind],
        suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]],
        finalizationOrder:                 List[List[PropertyKind]]
    ): Unit = {
        subPhaseId = 0
        hasSuppressedNotifications = suppressInterimUpdates.nonEmpty
    }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk))
    }

    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        val e = epk.e
        val pk = epk.pk
        val pkId = pk.id

        if (debug && propertyKindsComputedInLaterPhase(pkId)) {
            throw new IllegalArgumentException(
                s"querying of property kind ($pk) computed in a later phase"
            )
        }

        ps(pkId).get(e) match {
            case None ⇒
                // the entity is unknown ...
                lazyComputations(pkId) match {
                    case null ⇒
                        if (propertyKindsComputedInThisPhase(pkId)) {
                            val transformerSpecification = transformersByTargetPK(pkId)
                            if (transformerSpecification != null) {
                                // ... we have a transformer that can produce a property
                                // of the required kind; let's check if we can invoke it now or
                                // have to invoke it later.
                                val (sourcePK, transform) = transformerSpecification
                                val sourceEOptionP = ps(sourcePK.id).get(e)
                                if (sourceEOptionP.isDefined && sourceEOptionP.get.isFinal) {
                                    val FinalP(sourceP) = sourceEOptionP.get
                                    val finalEP = transform(e, sourceP).asInstanceOf[FinalEP[E, P]]
                                    update(finalEP, Nil)
                                    return finalEP;
                                } else {
                                    val sourceEPK = EPK(e, sourcePK)
                                    if (sourceEOptionP.isEmpty) {
                                        // We have to "apply" to ensure that all necessary lazy analyses
                                        // get triggered!
                                        apply(sourceEPK)
                                    }
                                    // Add this transformer as a depender to the transformer's
                                    // source; this works, because notifications about intermediate
                                    // values are suppressed.
                                    // This will happen only once, because afterwards an EPK
                                    // will be stored in the properties data structure and
                                    // then returned.
                                    val c: OnUpdateContinuation = {
                                        case FinalP(p) ⇒ Result(transform(e, p))
                                    }
                                    dependers(sourcePK.id)
                                        .getOrElseUpdate(e, AnyRefMap.empty)
                                        .put(epk, (c, DefaultPropertyComputation))
                                    dependees(pkId).put(e, List(sourceEPK))
                                }
                            }
                            ps(pkId).put(e, epk)
                            epk
                        } else {
                            val reason = {
                                if (propertyKindsComputedInEarlierPhase(pkId))
                                    PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                else
                                    PropertyIsNotComputedByAnyAnalysis
                            }
                            val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                            if (traceFallbacks) {
                                trace("analysis progress", s"used fallback $p for $e")
                            }
                            val finalEP = FinalEP(e, p.asInstanceOf[P])
                            update(finalEP, Nil)
                            finalEP
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        val fastTrackPropertyOption: Option[P] =
                            if (useFastTrackPropertyComputations) {
                                fastTrackPropertiesCounter += 1
                                computeFastTrackPropertyBasedOnPKId[P](this, e, pkId)
                            } else {
                                None
                            }
                        fastTrackPropertyOption match {
                            case Some(p) ⇒
                                val finalP = FinalEP(e, p.asInstanceOf[P])
                                update(finalP, Nil)
                                finalP
                            case None ⇒
                                // associate e with EPK to ensure that we do not schedule
                                // multiple (lazy) computations => the entity is now known
                                ps(pkId).put(e, epk)
                                scheduleLazyComputationForEntity(e)(lc)
                                // return the "current" result
                                epk
                        }
                }

            case Some(eOptionP: EOptionP[E, P] @unchecked) ⇒
                eOptionP
        }
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        apply[E, P](EPK(e, pk))
    }

    override protected[this] def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        val triggeredEntities = mutable.HashSet.empty[Entity]
        triggeredComputations(pk.id) += (pc, triggeredEntities)
        // trigger the computation for entities which already have values
        for {
            eOptionP ← ps(pk.id).valuesIterator
            if eOptionP.isEPS
            e = eOptionP.e
            if !triggeredEntities.contains(e)
        } {
            triggeredEntities += e
            val entityPC = pc.asInstanceOf[PropertyComputation[Entity]]
            val task = new PropertyComputationTask(this, e, entityPC)
            tasks.addLast(task)
            scheduledTasksCounter += 1
        }
    }

    private[this] def triggerComputations(e: Entity, pkId: Int): Unit = {
        val triggeredComputations = this.triggeredComputations(pkId)
        if (triggeredComputations != null) {
            triggeredComputations foreach { pcEntities ⇒
                val (pc, triggeredEntities) = pcEntities
                if (!triggeredEntities.contains(e)) {
                    triggeredEntities += e
                    scheduleEagerComputationForEntity(e)(pc.asInstanceOf[PropertyComputation[Entity]])
                }
            }
        }
    }

    private[this] def scheduleLazyComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new PropertyComputationTask(this, e, pc))
    }

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new PropertyComputationTask(this, e, pc))
    }

    private[this] def removeDependerFromDependees(dependerEPK: SomeEPK): Unit = {
        val dependerPKId = dependerEPK.pk.id
        val e = dependerEPK.e
        for {
            epkDependees ← dependees(dependerPKId).get(e)
            EOptionP(oldDependeeE, oldDependeePK) ← epkDependees // <= the old ones
            oldDependeePKId = oldDependeePK.id
            dependeeDependers ← dependers(oldDependeePKId).get(oldDependeeE)
        } {
            dependeeDependers -= dependerEPK
            if (dependeeDependers.isEmpty) {
                dependers(oldDependeePKId).remove(oldDependeeE)
            }
        }
        dependees(dependerPKId).remove(e)
    }

    /**
     * Updates the entity; returns true if no property already existed and is also not computed.
     * That is, setting the value was w.r.t. the current state of the property OK.
     */
    private[this] def update(
        eps: SomeEPS,
        // RECALL, IF THE EPS IS THE RESULT OF A PARTIAL RESULT UPDATE COMPUTATION, THEN
        // NEWDEPENDEES WILL ALWAYS BE EMPTY!
        newDependees: Traversable[SomeEOptionP]
    ): Unit = {
        val pkId = eps.pk.id
        val e = eps.e
        val isFinal = eps.isFinal
        val notificationRequired = ps(pkId).put(e, eps) match {
            case None ⇒
                // The entity was unknown; i.e., there can't be any dependees - no one queried
                // the property.
                triggerComputations(e, pkId)
                if (newDependees.nonEmpty) {
                    val oldDependees = dependees(pkId).put(e, newDependees)
                    assert(oldDependees.isEmpty)
                }
                // registration with the new dependees is done when processing InterimResult

                // let's check if we have dependers!
                true

            case Some(oldEOptionP) ⇒
                // The entity is already known and therefore we may have (old) dependees
                // and/or also dependers.
                if (oldEOptionP.isEPK) {
                    triggerComputations(e, pkId)
                }
                if (debug) oldEOptionP.checkIsValidPropertiesUpdate(eps, newDependees)
                if (newDependees.isEmpty)
                    dependees(pkId).remove(e)
                else
                    dependees(pkId).put(e, newDependees)
                eps.isUpdatedComparedTo(oldEOptionP)
        }
        if (notificationRequired) {
            dependers(pkId).get(e).foreach { dependersOfEPK ⇒
                dependersOfEPK foreach { cHint ⇒
                    val (dependerEPK, (c, hint)) = cHint
                    if (isFinal || !suppressInterimUpdates(dependerEPK.pk.id)(pkId)) {
                        if (hint == DefaultPropertyComputation) {
                            val t: QualifiedTask =
                                if (isFinal) {
                                    new OnFinalUpdateComputationTask(this, eps.asFinal, c)
                                } else {
                                    new OnUpdateComputationTask(this, eps.toEPK, c)
                                }
                            if (delayHandlingOfDependerNotification)
                                tasks.addLast(t)
                            else
                                tasks.addFirst(t)
                        } else { // we have a cheap property computation
                            tasks.addFirst(new HandleResultTask(this, c(eps)))
                        }
                        scheduledOnUpdateComputationsCounter += 1
                        removeDependerFromDependees(dependerEPK)
                    } else if (traceSuppressedNotifications) {
                        trace("analysis progress", s"suppressed notification: $eps → $dependerEPK")
                    }
                }
            }
        }
    }

    override def doSet(e: Entity, p: Property): Unit = handleExceptions {
        val key = p.key
        val pkId = key.id

        val oldPV = ps(pkId).put(e, new FinalEP(e, p))
        if (oldPV.isDefined) {
            throw new IllegalStateException(s"$e has already a property $oldPV")
        }
    }

    override def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] ⇒ InterimEP[E, P]
    ): Unit = {
        val pkId = pk.id
        val propertiesOfKind = ps(pkId)
        val newEPS =
            propertiesOfKind.get(e) match {
                case None         ⇒ pc(EPK(e, pk))
                case Some(oldEPS) ⇒ pc(oldEPS.asInstanceOf[EOptionP[E, P]])
            }
        propertiesOfKind.put(e, newEPS)
    }

    override def handleResult(r: PropertyComputationResult): Unit = handleExceptions {

        def handlePartialResults(prs: Traversable[SomePartialResult]): Unit = {
            // It is ok if prs is empty!
            prs foreach { pr ⇒ handlePartialResult(pr.e, pr.pk, pr.u) }
        }

        def handlePartialResult(
            e:  Entity,
            pk: SomePropertyKey,
            u:  UpdateComputation[_ <: Entity, _ <: Property]
        ): Unit = {
            type E = e.type
            type P = Property
            val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
            val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
            newEPSOption foreach { newEPS ⇒ update(newEPS, Nil /*<= w.r.t. the "newEPS"!*/ ) }
        }

        /* Returns `true`if no dependee was updated in the meantime. */
        def processDependees(
            processedDependees: Traversable[SomeEOptionP],
            c:                  OnUpdateContinuation
        ): Boolean = {
            processedDependees forall { processedDependee ⇒
                val processedDependeeE = processedDependee.e
                val processedDependeePK = processedDependee.pk
                val processedDependeePKId = processedDependeePK.id
                val currentDependee = ps(processedDependeePKId)(processedDependeeE)
                if (currentDependee.isUpdatedComparedTo(processedDependee)) {
                    // There were updates...
                    // hence, we will update the value for other analyses
                    // which want to get the most current value in the meantime,
                    // but we postpone notification of other analyses which are
                    // depending on it until we have the updated value (minimize
                    // the overall number of notifications.)
                    // println(s"update: $e => $p (isFinal=false;notifyDependers=false)")
                    scheduledOnUpdateComputationsCounter += 1
                    if (currentDependee.isFinal) {
                        val dependeeFinalP = currentDependee.asFinal
                        val t = OnFinalUpdateComputationTask(this, dependeeFinalP, c)
                        if (dependeeUpdateHandling.delayHandlingOfFinalDependeeUpdates)
                            tasks.addLast(t)
                        else
                            tasks.addFirst(t)
                    } else {
                        val t = OnUpdateComputationTask(this, processedDependee.toEPK, c)
                        if (dependeeUpdateHandling.delayHandlingOfNonFinalDependeeUpdates)
                            tasks.addLast(t)
                        else
                            tasks.addFirst(t)
                    }
                    false
                } else {
                    true // <= no update
                }
            }
        }

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Iterator[(PropertyComputation[e],e)]*/ , _) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleEagerComputationForEntity(e)(pc) }

            case Results.id ⇒
                val Results(results) = r
                results.foreach(r ⇒ handleResult(r))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒ update(FinalEP(ep.e, ep.p), newDependees = Nil) }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                update(r.asResult.finalEP, Nil)

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                handlePartialResult(e, pk, u)

            case InterimPartialResult.id ⇒
                val InterimPartialResult(prs, processedDependees, c) = r
                // 1. let's check if a new dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val noUpdates = processDependees(processedDependees, c)

                val sourceE = new Object() // an arbitrary, but unique object
                if (noUpdates) {
                    // 2. update the value and trigger dependers/clear old dependees;
                    //    the most current value of every dependee was taken into account
                    //    register with the (!) dependees.
                    handlePartialResults(prs)
                    val dependerAK = EPK(sourceE, AnalysisKey)
                    processedDependees foreach { dependee ⇒
                        val dependeeDependers =
                            dependers(dependee.pk.id).getOrElseUpdate(dependee.e, AnyRefMap.empty)
                        dependeeDependers += (dependerAK, (c, DefaultPropertyComputation))
                    }
                } else {
                    // 2. update the value (trigger dependers/clear old dependees)
                    //    There was an update and we already scheduled the computation... hence,
                    //     we have no live dependees any more.
                    handlePartialResults(prs)
                }
                dependees(AnalysisKeyId).put(sourceE, processedDependees)

            case InterimResult.id ⇒
                val InterimResult(interimP: SomeEPS, processedDependees, c, pcHint) = r

                // 1. let's check if a new dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val noUpdates = processDependees(processedDependees, c)

                if (noUpdates) {
                    // 2. update the value and trigger dependers/clear old dependees;
                    //    the most current value of every dependee was taken into account
                    //    register with the (!) dependees.
                    update(interimP, processedDependees)
                    val dependerEPK = interimP.toEPK
                    processedDependees foreach { dependee ⇒
                        val dependeeDependers =
                            dependers(dependee.pk.id).getOrElseUpdate(dependee.e, AnyRefMap.empty)
                        dependeeDependers += (dependerEPK, (c, pcHint))
                    }
                } else {
                    // 2. update the value (trigger dependers/clear old dependees)
                    //    There was an update and we already scheduled the computation... hence,
                    //     we have no live dependees any more.
                    update(interimP, Nil)
                }
        }
    }

    override protected[this] def isIdle: Boolean = tasks.size == 0

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        require(subPhaseId == 0, "unpaired waitOnPhaseCompletion call")

        val maxPKIndex = PropertyKey.maxId
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for fallback
        do {
            continueComputation = false

            while (!tasks.isEmpty) {
                val task = tasks.pollFirst()
                if (doTerminate) throw new InterruptedException()
                task.apply()
            }
            assert(tasks.isEmpty)
            quiescenceCounter += 1

            // We have reached quiescence.
            // Let's check if we have to fill in fallbacks.

            // 1. Let's search for all EPKs (not EPS) and use the fall back for them.
            //    (Recall that we return fallback properties eagerly if no analysis is
            //     scheduled or will be scheduled, However, it is still possible that we will
            //     not have computed a property for a specific entity, if the underlying
            //     analysis doesn't compute one; in that case we need to put in fallback
            //     values.)
            var pkId = 0
            while (pkId <= maxPKIndex) {
                if (propertyKindsComputedInThisPhase(pkId)) {
                    val epkIterator =
                        ps(pkId)
                            .valuesIterator
                            .filter { eOptionP ⇒
                                eOptionP.isEPK &&
                                    // It is not a transformer which still waits...
                                    dependees(pkId).get(eOptionP.e).isEmpty
                            }
                    continueComputation ||= epkIterator.hasNext
                    epkIterator.foreach { eOptionP ⇒
                        val e = eOptionP.e
                        val reason = PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                        val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                        if (traceFallbacks) {
                            trace("analysis progress", s"used fallback $p for $e")
                        }
                        fallbacksUsedForComputedPropertiesCounter += 1
                        update(FinalEP(e, p), Nil)
                    }
                }
                pkId += 1
            }

            // 2. Let's search for entities with interim properties where some dependers
            //    were not yet notified about intermediate updates. In this case, the
            //    current results of the dependers cannot be finalized; instead, we need
            //    to finalize (the cyclic dependent) dependees first and notify the
            //    dependers.
            if (!continueComputation && hasSuppressedNotifications) {
                // Collect all InterimEPs to find cycles.
                val interimEPs = ArrayBuffer.empty[SomeEOptionP]
                var pkId = 0
                while (pkId <= maxPKIndex) {
                    if (propertyKindsComputedInThisPhase(pkId)) {
                        ps(pkId).valuesIterator foreach { eps ⇒
                            if (eps.isRefinable) interimEPs += eps
                        }
                    }
                    pkId += 1
                }

                val successors = (interimEP: SomeEOptionP) ⇒ {
                    dependees(interimEP.pk.id).getOrElse(interimEP.e, Nil)
                }
                val cSCCs = graphs.closedSCCs(interimEPs, successors)
                continueComputation = cSCCs.nonEmpty
                for (cSCC ← cSCCs) {
                    // Clear all dependees of all members of a cycle to avoid inner cycle
                    // notifications!
                    for (interimEP ← cSCC) { removeDependerFromDependees(interimEP.toEPK) }
                    // 2. set all values
                    for (interimEP ← cSCC) { update(interimEP.toFinalEP, Nil) }
                }
            }

            // 3. Let's finalize remaining interim EPS; e.g., those related to
            //    collaboratively computed properties or "just all" if we don't have suppressed
            //    notifications. Recall that we may have cycles if we have no suppressed
            //    notifications, because in the latter case, we may have dependencies.
            //    We used no fallbacks, but we may still have collaboratively computed properties
            //    (e.g. CallGraph) which are not yet final; let's finalize them in the specified
            //    order (i.e., let's finalize the subphase)!
            while (!continueComputation && subPhaseId < subPhaseFinalizationOrder.length) {
                val pksToFinalize = subPhaseFinalizationOrder(subPhaseId)
                if (debug) {
                    trace(
                        "analysis progress",
                        pksToFinalize.map(PropertyKey.name).mkString("finalization of: ", ",", "")
                    )
                }
                pksToFinalize foreach { pk ⇒
                    val interimEPSs = ps(pk.id).valuesIterator.filter(_.isRefinable).toList
                    continueComputation = interimEPSs.nonEmpty
                    interimEPSs foreach { interimEP ⇒ removeDependerFromDependees(interimEP.toEPK) }
                }
                pksToFinalize foreach { pk ⇒
                    val interimEPSs = ps(pk.id).valuesIterator.filter(_.isRefinable).toList
                    interimEPSs foreach { interimEP ⇒ update(interimEP.toFinalEP, Nil) }
                }
                // Clear "dangling" maps in the depender/dependee data structures:
                pksToFinalize foreach { pk ⇒
                    dependees(pk.id) == null // <= we are really done
                    dependers(pk.id) == null // <= we are really done
                }
                subPhaseId += 1
            }
            if (debug && continueComputation) {
                trace(
                    "analysis progress",
                    s"finalization of sub phase $subPhaseId "+
                        s"of ${subPhaseFinalizationOrder.length} led to new results"
                )
            }
        } while (continueComputation)

        if (exception != null) throw exception;
    }

    def shutdown(): Unit = {}
}

/**
 * Factory for creating `EPKSequentialPropertyStore`s.
 *
 * @author Michael Eichberg
 */
object PKESequentialPropertyStore extends PropertyStoreFactory {

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKESequentialPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        new PKESequentialPropertyStore(contextMap)
    }
}
