/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import scala.language.existentials

import scala.collection.mutable
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.ArrayBuffer

import com.typesafe.config.Config

import org.opalj.control.foreachWithIndex
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.OPALLogger.info
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.computeFastTrackPropertyBasedOnPKId

/**
 * A reasonably optimized, complete, but non-concurrent implementation of the property store.
 * Primarily intended to be used for evaluation, debugging and prototyping purposes.
 *
 * @author Michael Eichberg
 */
final class PKESequentialPropertyStore protected (
        final val ctx:          Map[Class[_], AnyRef],
        final val tasksManager: TasksManager
)(
        implicit
        val logContext: LogContext
) extends SeqPropertyStore { store ⇒

    info("property store", s"using $tasksManager for managing tasks")

    import PKESequentialPropertyStore.EntityDependers

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

    private[this] var scheduledTasksCounter: Int = 0
    override def scheduledTasksCount: Int = scheduledTasksCounter

    private[this] var scheduledOnUpdateComputationsCounter: Int = 0
    override def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputationsCounter

    private[this] var fallbacksUsedForComputedPropertiesCounter: Int = 0
    override def fallbacksUsedForComputedPropertiesCount: Int = {
        fallbacksUsedForComputedPropertiesCounter
    }
    override private[fpcf] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = {
        fallbacksUsedForComputedPropertiesCounter += 1
    }

    private[this] var quiescenceCounter = 0
    override def quiescenceCount: Int = quiescenceCounter

    private[this] var fastTrackPropertiesCounter = 0
    override def fastTrackPropertiesCount: Int = fastTrackPropertiesCounter
    override private[fpcf] def incrementFastTrackPropertiesCounter(): Unit = {
        fastTrackPropertiesCounter += 1
    }

    private[this] var fastTrackPropertyComputationsCounter = 0
    override def fastTrackPropertyComputationsCount: Int = fastTrackPropertyComputationsCounter
    override private[fpcf] def incrementFastTrackPropertyComputationsCounter(): Unit = {
        fastTrackPropertyComputationsCounter += 1
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

    private[this] val dependers: Array[AnyRefMap[Entity, EntityDependers]] = {
        Array.fill(SupportedPropertyKinds) { new AnyRefMap() }
    }

    private[this] val dependees: Array[AnyRefMap[Entity, Traversable[SomeEOptionP]]] = {
        Array.fill(SupportedPropertyKinds) { new AnyRefMap() }
    }

    private[seq] def dependeesCount(epk: SomeEPK): Int = {
        dependees(epk.pk.id).get(epk.e) match {
            case Some(dependees) ⇒ dependees.size
            case None            ⇒ 0
        }
    }

    private[seq] def dependees(epk: SomeEPK): Traversable[SomeEOptionP] = {
        dependees(epk.pk.id).getOrElse(epk.e, Nil)
    }

    private[seq] def dependersCount(epk: SomeEPK): Int = {
        dependers(epk.pk.id).get(epk.e) match {
            case Some(dependers) ⇒ dependers.size
            case None            ⇒ 0
        }
    }

    private[seq] def dependers(epk: SomeEPK): Traversable[SomeEPK] = {
        dependers(epk.pk.id).get(epk.e) match {
            case Some(dependers) ⇒ dependers.keys
            case None            ⇒ Nil
        }
    }

    // The registered triggered computations along with the set of entities for which the analysis was triggered
    private[this] var triggeredComputations: Array[mutable.AnyRefMap[SomePropertyComputation, mutable.HashSet[Entity]]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { mutable.AnyRefMap.empty }
    }

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

    override def isKnown(e: Entity): Boolean = ps.exists(_.contains(e))

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
        for { ELUBP(e, `lb`, `ub`) ← ps(lb.id).valuesIterator } yield { e }
    }

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = {
        require(lb ne null)
        for { ELBP(e, `lb`) ← ps(lb.id).valuesIterator } yield { e }
    }

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = {
        require(ub ne null)
        for { EUBP(e, `ub`) ← ps(ub.id).valuesIterator } yield { e }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps(pk.id).valuesIterator.collect { case eps: SomeEPS ⇒ eps.asInstanceOf[EPS[Entity, P]] }
    }

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
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
                                    val c: OnUpdateContinuation = (eps) ⇒ {
                                        val FinalP(p) = eps
                                        Result(transform(e, p))
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
                            val finalEP = computeFallback(e, pkId)
                            update(finalEP, Nil)
                            finalEP
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        val fastTrackPropertyOption: Option[P] =
                            if (useFastTrackPropertyComputations) {
                                fastTrackPropertyComputationsCounter += 1
                                computeFastTrackPropertyBasedOnPKId[P](this, e, pkId)
                            } else {
                                None
                            }
                        fastTrackPropertyOption match {
                            case Some(p) ⇒
                                fastTrackPropertiesCounter += 1
                                val finalP = FinalEP(e, p)
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
        if (debug) {
            val pkId = pk.id
            if (lazyComputations(pkId) == null && transformersByTargetPK(pkId) == null) {
                throw new IllegalArgumentException(s"force for a non-lazily computed property: $pk")
            }
        }
        apply[E, P](EPK(e, pk))
    }

    override protected[this] def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        val triggeredEntities = mutable.HashSet.empty[Entity]
        triggeredComputations(pk.id) += (pc, triggeredEntities)
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
        tasksManager.pushInitialTask(new PropertyComputationTask(this, e, pc))
    }

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasksManager.pushInitialTask(new PropertyComputationTask(this, e, pc))
    }

    private[this] def removeDependerFromDependees(dependerEPK: SomeEPK): Unit = {
        val dependerPKId = dependerEPK.pk.id
        val e = dependerEPK.e
        for {
            epkDependees ← dependees(dependerPKId).remove(e)
            EOptionP(oldDependeeE, oldDependeePK) ← epkDependees // <= the old ones
            oldDependeePKId = oldDependeePK.id
            dependeeDependers ← dependers(oldDependeePKId).get(oldDependeeE)
        } {
            dependeeDependers -= dependerEPK
            if (dependeeDependers.isEmpty) {
                dependers(oldDependeePKId).remove(oldDependeeE)
            }
        }
    }

    /**
     * Updates the entity; returns true if no property already existed and is also not computed.
     * That is, setting the value was w.r.t. the current state of the property OK.
     */
    private[this] def update(
        eps: SomeEPS,
        // RECALL, IF THE EPS IS THE RESULT OF A PARTIAL RESULT UPDATE COMPUTATION, THEN
        // NEW DEPENDEES WILL ALWAYS BE EMPTY!
        newDependees: Traversable[SomeEOptionP]
    ): Unit = {
        val pkId = eps.pk.id
        val e = eps.e
        val notificationRequired = ps(pkId).put(e, eps) match {
            case None ⇒
                // The entity was unknown; i.e., there can't be any dependees - no one queried
                // the property.
                triggerComputations(e, pkId)
                if (newDependees.nonEmpty) {
                    dependees(pkId).put(e, newDependees)
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
            val isFinal = eps.isFinal
            val bottomness =
                if (eps.hasUBP && eps.ub.isOrderedProperty)
                    eps.ub.asOrderedProperty.bottomness
                else
                    OrderedProperty.DefaultBottomness
            val theDependers = dependers(pkId).get(e)
            theDependers.foreach { dependersOfEPK ⇒
                dependersOfEPK foreach { cHint ⇒
                    val (dependerEPK, (c, hint)) = cHint
                    if (isFinal || !suppressInterimUpdates(dependerEPK.pk.id)(pkId)) {
                        val t: QualifiedTask =
                            if (isFinal) {
                                new OnFinalUpdateComputationTask(this, eps.asFinal, c)
                            } else {
                                new OnUpdateComputationTask(this, eps.toEPK, c)
                            }
                        tasksManager.push(t, newDependees, dependersOfEPK.keys, bottomness, hint)
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
            throw new IllegalStateException(s"$e had already the property $oldPV")
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

    private[this] def handlePartialResult(
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

    @inline private[this] def handlePartialResults(prs: Traversable[SomePartialResult]): Unit = {
        // It is ok if prs is empty!
        prs foreach { pr ⇒ handlePartialResult(pr.e, pr.pk, pr.u) }
    }

    /* Returns `true` if no dependee was updated in the meantime. */
    private[this] def processDependeesOfInterimPartialResult(
        partialResults:     Traversable[SomePartialResult],
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
                scheduledOnUpdateComputationsCounter += 1
                val t = if (currentDependee.isFinal) {
                    val dependeeFinalP = currentDependee.asFinal
                    OnFinalUpdateComputationTask(this, dependeeFinalP, c)
                } else {
                    OnUpdateComputationTask(this, processedDependee.toEPK, c)
                }
                tasksManager.push(t, processedDependees, partialResults.map(pr ⇒ pr.epk))
                false
            } else {
                true // <= no update
            }
        }
    }

    private[this] def processDependeesOfInterimResult(
        initialEPS:       SomeEPS,
        initialDependees: Traversable[SomeEOptionP],
        initialC:         OnUpdateContinuation
    ): (SomeEPS, Traversable[SomeEOptionP], OnUpdateContinuation) = {
        // The idea is to stack/aggregate all changes in dependees.
        val e = initialEPS.e
        val pk = initialEPS.pk

        var nextEPS = initialEPS
        var nextDependees = initialDependees
        var nextC = initialC

        var continue = false
        do {
            continue = false
            nextDependees exists /* <= used for early termination purposes */ { nextDependee ⇒
                val nextDependeeE = nextDependee.e
                val nextDependeePK = nextDependee.pk
                val nextDependeePKId = nextDependeePK.id
                val currentDependee = ps(nextDependeePKId)(nextDependeeE)
                if (currentDependee.isUpdatedComparedTo(nextDependee)) {
                    nextC(currentDependee.asEPS) match {
                        case InterimResult(newEPS @ SomeEPS(`e`, `pk`), newDependees, newC, _) ⇒
                            nextEPS = newEPS
                            nextC = newC
                            nextDependees = newDependees
                            continue = true

                        case Result(finalEP @ SomeFinalEP(`e`, `pk`)) ⇒
                            nextEPS = finalEP
                            nextDependees = Nil
                            nextC = null
                        // continue remains "false"

                        case r ⇒
                            // Actually this shouldn't happen, though it is not a problem!
                            scheduledOnUpdateComputationsCounter += 1
                            tasksManager.push(HandleResultTask(store, r), nextDependees, /*FIXME*/ Nil)
                            // The last comparable result still needs to be stored,
                            // but obviously, no further relevant computations need to be
                            // carried out.
                            nextDependees = Nil
                            nextC = null
                    }

                    true // <= abort processing current dependees
                } else {
                    false
                }
            }
        } while (continue)

        (nextEPS, nextDependees, nextC)
    }

    override def handleResult(r: PropertyComputationResult): Unit = handleExceptions {

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Iterator[(PropertyComputation[e],e)]*/ , _) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleEagerComputationForEntity(e)(pc) }

            case Results.id ⇒
                r.asResults.foreach(r ⇒ handleResult(r))

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
                val noUpdates = processDependeesOfInterimPartialResult(prs, processedDependees, c)

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
                    //    we have no live dependees any more.
                    handlePartialResults(prs)
                }
                dependees(AnalysisKeyId).put(sourceE, processedDependees)

            case InterimResult.id ⇒
                val ir = r.asInterimResult
                val eps = ir.eps
                val dependees = ir.dependees
                val c = ir.c
                // 1. let's check if a dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val (newEPS, newDependees, newC) = processDependeesOfInterimResult(eps, dependees, c)

                assert(newEPS.e == eps.e)
                assert(newEPS.pk == eps.pk)

                // 2. update the value and trigger dependers/clear old dependees;
                update(newEPS, newDependees)
                if (newDependees.nonEmpty) {
                    val dependerEPK = newEPS.toEPK
                    newDependees foreach { dependee ⇒
                        val dependeeDependers =
                            dependers(dependee.pk.id).getOrElseUpdate(dependee.e, AnyRefMap.empty)
                        dependeeDependers += (dependerEPK, (newC, DefaultPropertyComputation))
                    }
                }
        }
    }

    override def isIdle: Boolean = tasksManager.isEmpty

    protected[this] def processTasks(): Unit = {
        while (!tasksManager.isEmpty) {
            val task = tasksManager.poll()
            if (doTerminate) throw new InterruptedException()
            task.apply()
        }
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        require(subPhaseId == 0, "unpaired waitOnPhaseCompletion call")

        if (triggeredComputations.exists(_.nonEmpty)) {
            // Let's trigger triggered computations for those entities, which have values!
            foreachWithIndex(ps) { (epss, pkId) ⇒
                epss foreach { eps ⇒
                    val (e, eOptionP) = eps
                    if (eOptionP.isEPS) {
                        triggerComputations(e, pkId)
                    }
                }
            }
        }

        val maxPKIndex = PropertyKey.maxId
        var continueComputation: Boolean = false
        do {
            continueComputation = false

            processTasks()

            quiescenceCounter += 1
            if (debug) {
                trace("analysis progress", s"reached quiescence $quiescenceCounter")
            }

            // We have reached quiescence....

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
                                    // There is no suppression; i.e., we have no dependees
                                    dependees(pkId).get(eOptionP.e).isEmpty
                            }
                    continueComputation |= epkIterator.hasNext
                    epkIterator.foreach { eOptionP ⇒
                        val e = eOptionP.e
                        val r = computeFallback(e, pkId)
                        update(r, Nil)
                    }
                }
                pkId += 1
            }

            // 2. Let's search for entities with interim properties where some dependers
            //    were not yet notified about intermediate updates. In this case, the
            //    current results of the dependers cannot be finalized; instead, we need
            //    to finalize (the cyclic dependent) dependees first and notify the
            //    dependers.
            //    Recall, that collaboratively computed properties are not allowed to be
            //    part of a cyclic computation if we also have suppressed notifications.
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
                // The following will also kill dependers related to anonymous computations using
                // the generic property key: "AnalysisKey"; i.e., those without explicit properties!
                pksToFinalize foreach { pk ⇒
                    val dependeesIt = dependees(pk.id).keysIterator
                    continueComputation |= dependeesIt.nonEmpty
                    dependeesIt foreach { e ⇒
                        removeDependerFromDependees(EPK(e, PropertyKey.key(pk.id)))
                    }
                }
                pksToFinalize foreach { pk ⇒
                    val interimEPSs = ps(pk.id).valuesIterator.filter(_.isRefinable)
                    interimEPSs foreach { interimEP ⇒
                        val finalEP = interimEP.toFinalEP
                        update(finalEP, Nil)
                    }
                }
                // Clear "dangling" maps in the depender/dependee data structures:
                pksToFinalize foreach { pk ⇒
                    dependees(pk.id) == null // <= we are really done
                    dependers(pk.id) == null // <= we are really done
                }
                subPhaseId += 1
            }
            if (debug && continueComputation && !tasksManager.isEmpty) {
                trace(
                    "analysis progress",
                    s"finalization of sub phase $subPhaseId of "+
                        s"${subPhaseFinalizationOrder.length} led to ${tasksManager.size} updates "
                )
            }
        } while (continueComputation)

        if (exception != null) throw exception;
    }

    def shutdown(): Unit = {}
}

/**
 * Factory for creating `PKESequentialPropertyStore`s.
 *
 * The task manager that will be used to instantiate the project will be extracted from the
 * `PropertyStoreContext` if the context contains a `Config` object. The fallback is the
 * `ManyDependeesOfDependersLastTasksManager`.
 *
 * @author Michael Eichberg
 */
object PKESequentialPropertyStore extends PropertyStoreFactory {

    final type EntityDependers = AnyRefMap[SomeEPK, (OnUpdateContinuation, PropertyComputationHint)]

    final val TasksManagerKey = "org.opalj.fpcf.seq.PKESequentialPropertyStore.TasksManager"

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKESequentialPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        val taskManagerId =
            contextMap.get(classOf[Config]) match {
                case Some(config: Config) ⇒ config.getString(TasksManagerKey)
                case _                    ⇒ "ManyDependeesOfDependersLast" // <= default
            }
        apply(taskManagerId)(contextMap)
    }

    final val Strategies = List(
        "FIFO",
        "LIFO",
        "ManyDirectDependenciesLast",
        "ManyDirectDependersLast",
        "ManyDependeesOfDirectDependersLast",
        "ManyDependeesAndDependersOfDirectDependersLast",
        "ForwardAllDependeesLast",
        "ForwardAllDependeesFirst",
        "BackwardAllDependeesLast",
        "BackwardAllDependeesFirst"
    )

    def apply(
        taskManagerId: String
    )(
        context: Map[Class[_], AnyRef] = Map.empty
    )(
        implicit
        logContext: LogContext
    ): PKESequentialPropertyStore = {
        taskManagerId match {

            case "FIFO" ⇒
                new PKESequentialPropertyStore(context, new FIFOTasksManager)

            case "LIFO" ⇒
                new PKESequentialPropertyStore(context, new LIFOTasksManager)

            case "ManyDirectDependenciesLast" ⇒
                new PKESequentialPropertyStore(context, new ManyDirectDependenciesLastTasksManager)

            case "ManyDirectDependersLast" ⇒
                new PKESequentialPropertyStore(context, new ManyDirectDependersLastTasksManager)

            case "ManyDependeesOfDirectDependersLast" ⇒
                val taskManager = new ManyDependeesOfDirectDependersLastTasksManager
                val ps = new PKESequentialPropertyStore(context, taskManager)
                taskManager.setSeqPropertyStore(ps)
                ps

            case "ManyDependeesAndDependersOfDirectDependersLast" ⇒
                val taskManager = new ManyDependeesAndDependersOfDirectDependersLastTasksManager
                val ps = new PKESequentialPropertyStore(context, taskManager)
                taskManager.setSeqPropertyStore(ps)
                ps

            case "ForwardAllDependeesLast" | "ForwardAllDependeesFirst" |
                "BackwardAllDependeesLast" | "BackwardAllDependeesFirst" ⇒
                val forward = taskManagerId.startsWith("Forward")
                val manyDependeeslast = taskManagerId.endsWith("Last")
                val taskManager = new AllDependeesTasksManager(forward, manyDependeeslast)
                val ps = new PKESequentialPropertyStore(context, taskManager)
                taskManager.setSeqPropertyStore(ps)
                ps

            case _ ⇒
                throw new IllegalArgumentException(s"unknown task manager $taskManagerId")
        }

    }
}
