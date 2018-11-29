/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

import java.lang.System.identityHashCode
import java.util.ArrayDeque

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.collection.{Map ⇒ SomeMap}
import scala.collection.mutable.AnyRefMap

import org.opalj.graphs

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.OPALLogger.error

import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.fastTrackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.computeFastTrackPropertyBasedOnPKId

/**
 * A non-concurrent implementation of the property store.
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
    @volatile var dependeeUpdateHandling: DependeeUpdateHandling = EagerDependeeUpdateHandling

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
            "scheduled tasks" -> scheduledTasksCount,
            "scheduled on update computations" -> scheduledOnUpdateComputationsCount,
            "fast-track properties" -> fastTrackPropertiesCount,
            "computations of fallback properties for computed properties" -> fallbacksUsedForComputedPropertiesCounter,
            "quiescence" -> quiescenceCount
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

    // Those computations that will only be scheduled if the result is required
    private[this] var lazyComputations: Array[SomePropertyComputation] = {
        new Array(PropertyKind.SupportedPropertyKinds)
    }

    private[this] var triggeredComputations: Array[ArrayBuffer[SomePropertyComputation]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { new ArrayBuffer[SomePropertyComputation](1) }
    }

    // The list of scheduled computations
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    @volatile private[this] var previouslyComputedPropertyKinds: Array[Boolean] = {
        new Array[Boolean](SupportedPropertyKinds)
    }

    private[this] var computedPropertyKinds: Array[Boolean] = _ /*false*/ // has to be set before usage

    private[this] var delayedPropertyKinds: Array[Boolean] = _ /*false*/ // has to be set before usage

    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        require(e ne null)
        val pValue = ps(pk.id).get(e)
        pValue.isDefined && { val ub = pValue.get.ub; ub != null && ub != PropertyIsLazilyComputed }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        require(e ne null)
        for {
            epks ← ps.iterator
            eOptionP ← epks.get(e)
            eps ← eOptionP.toEPS
        } yield {
            eps
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        for {
            epks ← ps.iterator
            (e,eOptionP) ← epks.iterator
            eps ← eOptionP.toEPS
            if propertyFilter(eps)
        } yield {
            e
        }
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        require(lb ne null)
        require(ub ne null)
        entities((otherEPS: SomeEPS) ⇒ lb == otherEPS.lb && ub == otherEPS.ub)
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps(pk.id).valuesIterator.collect { case eps : SomeEPS ⇒ eps.asInstanceOf[EPS[Entity,P]]       }
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
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(properties=${ps.iterator.map(_.size).sum})"
        }
    }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk))
    }

    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        val e = epk.e
        val pk = epk.pk
        val pkId = pk.id

        ps(pkId).get(e) match {
            case None ⇒
                // the entity is unknown ...
                if (debug && computedPropertyKinds == null) {
                    throw new IllegalStateException("setup phase was not called")
                }
                val isComputed = computedPropertyKinds(pkId)

                lazyComputations(pkId) match {
                    case null ⇒
                        if (isComputed || delayedPropertyKinds(pkId)) {
                            ps(pkId).put(e,epk)
                            epk
                        } else {
                            val reason = {
                                if (previouslyComputedPropertyKinds(pkId))
                                    PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                else
                                    PropertyIsNotComputedByAnyAnalysis
                            }
                            val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                            val finalP = FinalP(e, p.asInstanceOf[P])
                            update(finalP, Nil)
                            finalP
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        val fastTrackPropertyOption: Option[P] =
                            if (isComputed && useFastTrackPropertyComputations)
                                computeFastTrackPropertyBasedOnPKId[P](this, e, pkId)
                            else
                                None
                        fastTrackPropertyOption match {
                            case Some(p) ⇒
                                val finalP = FinalP(e, p.asInstanceOf[P])
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

            case Some(eOptionP : EOptionP[E,P]@unchecked) ⇒                eOptionP
        }
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        apply[E, P](EPK(e, pk))
    }

    override def doRegisterLazyPropertyComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && !tasks.isEmpty) {
            throw new IllegalStateException(
                "lazy computations should only be registered while no computations are running"
            )
        }
        lazyComputations(pk.id) = pc
    }

    override def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && !tasks.isEmpty) {
            throw new IllegalStateException(
                "triggered computations should only be registered while no computations are running"
            )
        }
        triggeredComputations(pk.id) += pc
        // trigger the computation for entities which already have values
        for {
            eOptionP <- ps(pk.id).valuesIterator
            if eOptionP.isEPS
        } {
            pc(eOptionP.e)
        }

    }

    private[this] def triggerComputations(e: Entity, pkId: Int): Unit = {
        val triggeredComputations = this.triggeredComputations(pkId)
        if (triggeredComputations != null) {
            triggeredComputations foreach (pc ⇒
                scheduleEagerComputationForEntity(e)(pc.asInstanceOf[PropertyComputation[Entity]])
                )
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

    private[this] def clearDependees(pValue: PropertyValue, pValueEPK: SomeEPK): Unit = {
        // TODO...
        val dependees = pValue.dependees
        if (dependees == null)
            return ;

        for {
            eOptP @ EOptionP(oldDependeeE, oldDependeePK) ← dependees // <= the old ones
        } {
            val oldDependeePKId = oldDependeePK.id
            // Please recall, that we don't create support data-structures
            // (i.e., PropertyValue) eagerly... but they should have been
            // created by now or the dependees should be empty!
            val dependeePValue = ps(oldDependeePKId)(oldDependeeE)
            val dependeeIntermediatePValue = dependeePValue.asIntermediate
            val dependersOfDependee = dependeeIntermediatePValue.dependers
            dependeeIntermediatePValue.dependers = dependersOfDependee - pValueEPK
        }
        pValue.asIntermediate.dependees = null
    }

    /**
     * Updates the entity; returns true if no property already existed and is also not computed.
     * That is, setting the value was w.r.t. the current state of the property OK.
     */
    private[this] def update(
        eps : SomeEPS,
        newDependees: Traversable[SomeEOptionP]
    ): Boolean = {
        if (debug && e == null) {
            throw new IllegalArgumentException("the entity must not be null");
        }
        val pkId = ub.key.id
        ps(pkId).get(e) match {
            case None ⇒
                // The entity is unknown (=> there are no dependers/dependees):
                ps(pkId).put(e, PropertyValue(lb, ub, newDependees))
                triggerComputations(e, pkId)
                // registration with the new dependees is done when processing InterimResult
                true

            case Some(pValue: IntermediatePropertyValue) ⇒
                // The entity is known and we have a property value for the respective
                // kind; i.e., we may have (old) dependees and/or also dependers.
                val oldIsFinal = pValue.isFinal
                val oldLB = pValue.lb
                val oldUB = pValue.ub

                // 1. Check and update property:
                if (debug) {
                    if (oldIsFinal) {
                        throw new IllegalStateException(
                            s"already final: $e@${identityHashCode(e).toHexString}/$ub"
                        )
                    }
                    if (lb.isOrderedProperty) {
                        try {
                            val lbAsOP = lb.asOrderedProperty
                            if (oldLB != null && oldLB != PropertyIsLazilyComputed) {
                                val oldLBWithUBType = oldLB.asInstanceOf[lbAsOP.Self]
                                lbAsOP.checkIsEqualOrBetterThan(e, oldLBWithUBType)
                                val pValueUBAsOP = oldUB.asOrderedProperty
                                val ubWithOldUBType = ub.asInstanceOf[pValueUBAsOP.Self]
                                pValueUBAsOP.checkIsEqualOrBetterThan(e, ubWithOldUBType)
                            }
                        } catch {
                            case t: Throwable ⇒
                                throw new IllegalArgumentException(
                                    s"entity=$e illegal update to: lb=$lb; ub=$ub; "+
                                        newDependees.mkString("newDependees={", ", ", "}")+
                                        "; cause="+t.getMessage,
                                    t
                                )
                        }
                    }
                }
                pValue.lb = lb
                pValue.ub = ub
                // Updating lb and/or ub MAY CHANGE the PropertyValue's isFinal property!
                val newPValueIsFinal = pValue.isFinal

                if (oldLB == null || oldLB == PropertyIsLazilyComputed) {
                    triggerComputations(e, pkId)
                }

                // 2. Clear old dependees (remove onUpdateContinuation from dependees)
                //    and then update dependees.
                val epk = pValue.toEPKUnsafe(e)
                clearDependees(pValue, epk)
                if (newPValueIsFinal)
                    ps(pkId).put(e, new FinalPropertyValue(ub))
                else
                    pValue.dependees = newDependees

                // 3. Notify dependers if necessary
                if (lb != oldLB || ub != oldUB || newPValueIsFinal) {
                    pValue.dependers foreach { depender ⇒
                        val (dependerEPK, onUpdateContinuation) = depender
                        val t: QualifiedTask =
                            if (newPValueIsFinal) {
                                new OnFinalUpdateComputationTask(
                                    this,
                                    FinalP(e, ub),
                                    onUpdateContinuation
                                )
                            } else {
                                new OnUpdateComputationTask(
                                    this,
                                    epk,
                                    onUpdateContinuation
                                )
                            }
                        scheduledOnUpdateComputationsCounter += 1
                        if (delayHandlingOfDependerNotification)
                            tasks.addLast(t)
                        else
                            tasks.addFirst(t)
                        // Clear depender => dependee lists.
                        // Given that we have triggered the depender, we now have
                        // to remove the respective onUpdateContinuation from all
                        // dependees of the respective depender to avoid that the
                        // onUpdateContinuation is triggered multiple times!
                        val dependerPKId = dependerEPK.pk.id
                        val dependerPValue = ps(dependerPKId)(dependerEPK.e).asIntermediate
                        dependerPValue.dependees foreach { epkOfDependeeOfDepender ⇒
                            if (epkOfDependeeOfDepender.toEPK != epk) {
                                // We have to avoid checking against the "current" dependee
                                // because it is already final!
                                val dependeePKIdOfDepender = epkOfDependeeOfDepender.pk.id
                                val pValueOfDependeeOfDepender =
                                    ps(dependeePKIdOfDepender)(epkOfDependeeOfDepender.e)
                                pValueOfDependeeOfDepender.asIntermediate.dependers -= dependerEPK
                            }
                        }
                        dependerPValue.dependees = Nil
                    }
                    pValue.dependers = Map.empty
                }

                oldLB == null /*AND/OR oldUB == null*/

            case finalPValue ⇒
                throw new IllegalStateException(s"$e: update of $finalPValue")
        }
    }

    override def doSet(e: Entity, p: Property): Unit = handleExceptions {
        val key = p.key
        val pkId = key.id

        val oldPV = ps(pkId).put(e, new FinalP(e,p))
        if (oldPV.isDefined) {
            throw new IllegalStateException(s"$e has already a property $oldPV")
        }
    }

    override def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] ⇒ EPS[E, P]
    ): Unit = {
        val pkId = pk.id
        val propertiesOfKind = ps(pkId)
        val newEPS =
            propertiesOfKind.get(e) match {
                case None ⇒ pc(EPK(e, pk))
                case Some(oldEPS) ⇒ pc(oldEPS)
            }
        if (newEPS.isFinal) {
            throw new IllegalArgumentException(s"$newEPS must not be final")
        }
        propertiesOfKind.put(e, newEPS)
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
                val Results(results) = r
                results.foreach(r ⇒ handleResult(r))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒ update(FinalP(ep.e, ep.p), newDependees = Nil) }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                // IMPROVE The Result should take the FinalP
                val Result(e, p) = r
                update(FinalP(e, p), Nil)

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                type E = e.type
                type P = Property
                val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
                val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                newEPSOption foreach { newEPS ⇒ update(newEPS, Nil) }

            case InterimResult.id ⇒
                val InterimResult(e, lb, ub, processedDependees, c, _) = r

                def checkNonFinal(dependee: SomeEOptionP): Unit = {
                    if (dependee.isFinal) {
                        throw new IllegalArgumentException(s"$r: final dependee: $dependee")
                    }
                }

                def isDependeeUpdated(
                    currentDependee: SomeEOptionP, // may contain newer info than "newDependee"
                    processedDependee:    SomeEOptionP
                ): Boolean = {
                    currentDependee.isEPS && (
                        currentDependee.isFinal ||
                            currentDependee.lb != processedDependee.lb ||
                            currentDependee.ub != processedDependee.ub
                    )
                }

                // 1. let's check if a new dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val noUpdates = dependeeUpdateHandling match {

                    case EagerDependeeUpdateHandling ⇒
                        val processedDependeesIterator = processedDependees.toIterator
                        while (processedDependeesIterator.hasNext) {
                            val processedDependee = processedDependeesIterator.next()
                            if (debug) checkNonFinal(processedDependee )

                            val processedDependeeE = processedDependee .e
                            val processedDependeePKId = processedDependee .pk.id
                            val dependeeEPS = ps(processedDependeePKId)(processedDependeeE)
                            if (isDependeeUpdated(dependeeEPS, processedDependee)) {
                                // => dependeeEPS can't be an EPK...
                                immediateOnUpdateComputationsCounter += 1
                                val newR = c(dependeeEPS.asEPS)
                                if (debug && newR == r) {
                                    throw new IllegalStateException(
                                        s"useless on-update continuation:\n\told: $r\n\tnew: $newR"
                                    )
                                }
                                handleResult(newR)
                                return ;
                            }
                        }
                        true // all updates are handled; otherwise we have an early return

                    case dependeeUpdateHandling: LazyDependeeUpdateHandling ⇒
                        processedDependees forall { processedDependee ⇒
                            if (debug) checkNonFinal(processedDependee)
                            val processedDependeeE = processedDependee.e
                            val processedDependeePK = processedDependee.pk
                            val processedDependeePKId = processedDependeePK.id
                            val dependeeEPS = ps( processedDependeePKId)(processedDependeeE)
                            if (isDependeeUpdated(dependeeEPS, processedDependee)) {
                                // There were updates...
                                // hence, we will update the value for other analyses
                                // which want to get the most current value in the meantime,
                                // but we postpone notification of other analyses which are
                                // depending on it until we have the updated value (minimize
                                // the overall number of notifications.)
                                // println(s"update: $e => $p (isFinal=false;notifyDependers=false)")
                                scheduledOnUpdateComputationsCounter += 1
                                if (dependeeEPS.isFinal) {
                                    val dependeeFinalP = dependeeEPS.asFinal
                                    val t = OnFinalUpdateComputationTask(this,dependeeFinalP,c)
                                    if (dependeeUpdateHandling.delayHandlingOfFinalDependeeUpdates)
                                        tasks.addLast(t)
                                    else
                                        tasks.addFirst(t)
                                } else {
                                    val t =
                                        OnUpdateComputationTask(this,
                                            EPK(dependeeE, dependeePK),
                                            c
                                        )
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

                if (noUpdates) {
                    // 2.1. update the value (trigger dependers/clear old dependees)
                    update(e, lb, ub, newDependees)

                    // 2.2 The most current value of every dependee was taken into account
                    //     register with new (!) dependees.
                    val dependerEPK = EPK(e, ub)
                    val dependency = (dependerEPK, c)

                    newDependees foreach { dependee ⇒
                        val dependeeE = dependee.e
                        val dependeePKId = dependee.pk.id

                        ps(dependeePKId).get(dependeeE) match {
                            case None ⇒
                                // the dependee is not known
                                ps(dependeePKId).put(
                                    dependeeE,
                                    new IntermediatePropertyValue(dependerEPK, c)
                                )

                            case Some(dependeePValue: IntermediatePropertyValue) ⇒
                                val dependeeDependers = dependeePValue.dependers
                                dependeePValue.dependers = dependeeDependers + dependency

                            case _ /*dependeePValue*/ ⇒
                                throw new UnknownError(
                                    "fatal internal error: can't update dependees of final property"
                                )
                        }
                    }
                } else {
                    // 2.1. update the value (trigger dependers/clear old dependees)
                    // There was an update and we already scheduled the computation... hence,
                    // we have no live dependees any more.
                    update(e, lb, ub, Nil)
                }
        }
    }

    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind]
    ): Unit = handleExceptions {
        if (debug && tasks.size > 0) {
            throw new IllegalStateException(
                "setup phase can only be called as long as no tasks are scheduled"
            )
        }
        val currentComputedPropertyKinds = this.computedPropertyKinds
        if (currentComputedPropertyKinds != null) {
            currentComputedPropertyKinds.iterator.zipWithIndex foreach { e ⇒
                val (isComputed, pkId) = e
                previouslyComputedPropertyKinds(pkId) |= isComputed
            }
        }

        val newComputedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        this.computedPropertyKinds = newComputedPropertyKinds
        computedPropertyKinds foreach { pk ⇒ newComputedPropertyKinds(pk.id) = true }

        val newDelayedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        this.delayedPropertyKinds = newDelayedPropertyKinds
        delayedPropertyKinds foreach { pk ⇒ newDelayedPropertyKinds(pk.id) = true }
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for fallback
        var isSuspended: Boolean = false
        do {
            continueComputation = false

            while (!tasks.isEmpty && !isSuspended) {
                val task = tasks.pollFirst()
                task.apply()
                isSuspended = this.isSuspended()
            }
            if (tasks.isEmpty) quiescenceCounter += 1

            if (!isSuspended) {
                // We have reached quiescence. let's check if we have to
                // fill in fallbacks or if we have to resolve cyclic computations.

                // 1. Let's search all EPKs for which we have no analyses scheduled in the
                //    future and use the fall back for them.
                //    (Recall that we return fallback properties eagerly if no analysis is
                //     scheduled or will be scheduled; but it is still possible that we will
                //     not have a property for a specific entity, if the underlying analysis
                //     doesn't compute one; in that case we need to put in fallback values.)
                val maxPKIndex = ps.length
                var pkId = 0
                while (pkId < maxPKIndex) {
                    if (!delayedPropertyKinds(pkId)) {
                        ps(pkId) foreach { ePValue ⇒
                            val (e, pValue) = ePValue
                            // Check that we have no running computations and that the
                            // property will not be computed later on.
                            if (pValue.ub == null) {
                                assert(pValue.dependees.isEmpty)
                                val reason = {
                                    if (previouslyComputedPropertyKinds(pkId) ||
                                        computedPropertyKinds(pkId))
                                        PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                    else
                                        PropertyIsNotComputedByAnyAnalysis
                                }
                                val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                                if (traceFallbacks) {
                                    val message = s"used fallback $p (reason=$reason) for $e"
                                    trace("analysis progress", message)
                                }
                                fallbacksUsedForComputedPropertiesCounter += 1
                                update(e, p, p, Nil)

                                continueComputation = true
                            }
                        }
                    }
                    pkId += 1
                }

                // 2. let's search for cSCCs that only consist of properties which will not be
                //    updated later on
                if (!continueComputation) {
                    val epks = ArrayBuffer.empty[SomeEOptionP]
                    val maxPKIndex = ps.length
                    var pkId = 0
                    while (pkId < maxPKIndex) {
                        ps(pkId) foreach { ePValue ⇒
                            val (e, pValue) = ePValue
                            val ub = pValue.ub
                            if (ub != null // analyses must always commit some value; hence,
                                // Both of the following tests are necessary because we may have
                                // properties which are only computed in a later phase; in that
                                // case we may have EPKs related to entities which are not used.
                                && pValue.dependees.nonEmpty // Can this node can be part of a cycle?
                                && pValue.dependers.nonEmpty // Can this node can be part of a cycle?
                                ) {
                                assert(ub != PropertyIsLazilyComputed)
                                epks += (EPK(e, ub.key): SomeEOptionP)
                            }
                        }
                        pkId += 1
                    }

                    val cSCCs = graphs.closedSCCs(
                        epks,
                        (epk: SomeEOptionP) ⇒ ps(epk.pk.id)(epk.e).dependees
                    )
                    for (cSCC ← cSCCs) {
                        // 1. clear all dependees of all members of a cycle to avoid
                        //    inner cycle notifications!
                        for (epk ← cSCC) {
                            val e = epk.e
                            val pk = epk.pk
                            val pkId = pk.id
                            val pValue = ps(pkId)(e)
                            clearDependees(pValue, EPK(e, pk))
                        }
                        // 2. set all values
                        for (epk ← cSCC) {
                            val e = epk.e
                            val pkId = epk.pk.id
                            val pValue = ps(pkId)(e)
                            val lb = pValue.lb
                            val ub = pValue.ub
                            val headEPS = InterimP(e, lb, ub)
                            val newP = PropertyKey.resolveCycle(this, headEPS)
                            if (traceCycleResolutions) {
                                val cycleAsText =
                                    if (cSCC.size > 10)
                                        cSCC.take(10).mkString("", ",", "...")
                                    else
                                        cSCC.mkString(",")

                                info(
                                    "analysis progress",
                                    s"resolving cycle(iteration:$quiescenceCounter): $cycleAsText by updating $e:ub=$ub with $newP"
                                )
                            }
                            update(e, newP, newP, Nil)
                        }
                        continueComputation = true
                    }
                }

                if (!continueComputation) {
                    // We used no fallbacks and found no cycles, but we may still have
                    // (collaboratively computed) properties (e.g. CallGraph) which are
                    // not yet final; let's finalize them!
                    val maxPKIndex = ps.length
                    var pkId = 0
                    var toBeFinalized: List[(AnyRef, Property)] = Nil
                    while (pkId < maxPKIndex) {
                        if (!delayedPropertyKinds(pkId)) {
                            ps(pkId) foreach { ePValue ⇒
                                val (e, pValue) = ePValue
                                // Check that we have no running computations and that the
                                // property will not be computed later on.
                                if (!pValue.isFinal && pValue.lb != pValue.ub && pValue.dependees.isEmpty) {
                                    toBeFinalized ::= ((e, pValue.ub))
                                }
                            }
                        }
                        pkId += 1
                    }
                    if (toBeFinalized.nonEmpty) {
                        toBeFinalized foreach { ep ⇒
                            val (e, p) = ep
                            update(e, p, p, Nil) // commit as Final value
                        }
                        continueComputation = true
                    }
                }
            }
        } while (continueComputation)

        if (debug && !isSuspended) {
            // let's search for "unsatisfied computations" related to "forced properties"
            // TODO support forced properties if we have real lazy evaluation...
            val maxPKIndex = ps.length
            var pkId = 0
            while (pkId < maxPKIndex) {
                ps(pkId) foreach { ePValue ⇒
                    val (e, pValue) = ePValue
                    if (!pValue.isFinal) {
                        error(
                            "analysis progress",
                            s"intermediate property state: $e ⇒ $pValue"
                        )
                    }
                }
                pkId += 1
            }
        }
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
