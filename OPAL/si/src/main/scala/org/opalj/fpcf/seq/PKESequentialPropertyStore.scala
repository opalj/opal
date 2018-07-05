/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package seq

import java.lang.System.identityHashCode
import java.util.ArrayDeque
import java.util.IdentityHashMap

import scala.reflect.runtime.universe.Type
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.collection.{Map ⇒ SomeMap}

import org.opalj.graphs
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId
import org.opalj.fpcf.PropertyKey.fastTrackPropertyBasedOnPkId

/**
 * A non-concurrent implementation of the property store. Entities are generally only stored on
 * demand. I.e.,
 *  - they have a property OR
 *  - a computation is already scheduled that will compute the property OR
 *  - we have a `depender`.
 *
 * @author Michael Eichberg
 */
final class PKESequentialPropertyStore private (
        val ctx: Map[Type, AnyRef]
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

    private[this] var resolvedCSCCsCounter: Int = 0
    def resolvedCSCCsCount: Int = resolvedCSCCsCounter

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
            "quiescence" -> quiescenceCount,
            "resolved cSCCs" -> resolvedCSCCsCount
        )
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // --------------------------------------------------------------------------------------------

    private[this] val ps: Array[IdentityHashMap[Entity, PropertyValue]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { new IdentityHashMap( /*1024*64)*/ ) }
    }

    // Those computations that will only be scheduled if the result is required
    private[this] var lazyComputations: Array[SomePropertyComputation] = {
        new Array(PropertyKind.SupportedPropertyKinds)
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
        pValue != null && { val ub = pValue.ub; ub != null && ub != PropertyIsLazilyComputed }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        require(e ne null)
        for {
            ePValue ← ps.iterator
            pValue = ePValue.get(e)
            if pValue != null
            eps ← pValue.toEPS(e)
        } yield {
            eps
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        val entities = ArrayBuffer.empty[Entity]
        val max = ps.length
        var i = 0
        while (i < max) {
            ps(i).forEach { (e, pValue) ⇒
                val eps = pValue.toEPS[Entity](e)
                if (eps.isDefined && propertyFilter(eps.get)) entities += e
            }
            i += 1
        }
        entities.toIterator
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        require(lb ne null)
        require(ub ne null)
        entities((otherEPS: SomeEPS) ⇒ lb == otherEPS.lb && ub == otherEPS.ub)
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps(pk.id).entrySet().iterator().asScala.flatMap { ePValue ⇒
            ePValue.getValue.toEPSUnsafe[Entity, P](ePValue.getKey)
        }
    }

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val properties = for {
                (ePValue, pkId) ← ps.iterator.zipWithIndex
                ePValue ← ePValue.entrySet().asScala.iterator
                e = ePValue.getKey
                pValue = ePValue.getValue
            } yield {
                val propertyKindName = PropertyKey.name(pkId)
                s"$e -> $propertyKindName[$pkId] = $pValue"
            }
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(properties=${ps.iterator.map(_.size).sum})"
        }
    }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk), force = false)
    }

    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        apply(epk, force = false)
    }

    private[this] def apply[E <: Entity, P <: Property](
        epk:   EPK[E, P],
        force: Boolean
    ): EOptionP[E, P] = {
        val e = epk.e
        val pk = epk.pk
        val pkId = pk.id

        ps(pkId).get(e) match {
            case null ⇒
                // the entity is unknown ...
                if (debug && computedPropertyKinds == null) {
                    throw new IllegalStateException("setup phase was not called")
                }
                val isComputed = computedPropertyKinds(pkId)

                lazyComputations(pkId) match {
                    case null ⇒
                        if (isComputed || delayedPropertyKinds(pkId)) {
                            epk
                        } else {
                            val reason = {
                                if (previouslyComputedPropertyKinds(pkId))
                                    PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                else
                                    PropertyIsNotComputedByAnyAnalysis
                            }
                            val p = fallbackPropertyBasedOnPkId(this, reason, e, pkId)
                            if (force) {
                                set(e, p)
                            }
                            FinalEP(e, p.asInstanceOf[P])
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        val fastTrackPropertyOption: Option[P] =
                            if (isComputed && useFastTrackPropertyComputations)
                                fastTrackPropertyBasedOnPkId(this, e, pkId).asInstanceOf[Option[P]]
                            else
                                None
                        fastTrackPropertyOption match {
                            case Some(p) ⇒
                                fastTrackPropertiesCounter += 1
                                set(e, p, isFastTrackProperty = true)
                                FinalEP(e, p.asInstanceOf[P])
                            case None ⇒
                                // create PropertyValue to ensure that we do not schedule
                                // multiple (lazy) computations => the entity is now known
                                ps(pkId).put(e, PropertyValue.lazilyComputed)
                                scheduleLazyComputationForEntity(e)(lc)
                                // return the "current" result
                                epk

                        }
                }

            case pValue ⇒
                val ub = pValue.ub // or lb... doesn't matter
                if (ub != null && ub != PropertyIsLazilyComputed)
                    // we have a property
                    EPS(e, pValue.lb.asInstanceOf[P], pValue.ub.asInstanceOf[P])
                else {
                    //... ub is null or is PropertyIsLazilyComputed...
                    // We do not (yet) have a value, but a lazy property
                    // computation is already scheduled (if available).
                    // Recall that it is a strict requirement that a
                    // dependee which is listed in the set of dependees
                    // of an IntermediateResult must have been queried;
                    // however the sequential store does not create the
                    // data-structure eagerly!
                    if (debug && ub == null && lazyComputations(pkId) != null) {
                        throw new IllegalStateException(
                            "registered lazy computation was not triggered; "+
                                "this happens, e.g., if the list of dependees contains EPKs "+
                                "that are instantiated by the client but never queried"
                        )
                    }
                    epk
                }
        }
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        apply[E, P](EPK(e, pk), true)
    }

    /**
     * Returns the `PropertyValue` associated with the given Entity / PropertyKey or `null`.
     */
    private[seq] final def getPropertyValue(e: Entity, pkId: Int): PropertyValue = ps(pkId).get(e)

    override def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && !tasks.isEmpty) {
            throw new IllegalStateException(
                "lazy computations should only be registered while no analysis are scheduled"
            )
        }
        lazyComputations(pk.id) = pc.asInstanceOf[SomePropertyComputation]
    }

    private[this] def scheduleLazyComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new PropertyComputationTask(this, e, pc))
    }

    override def scheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new PropertyComputationTask(this, e, pc))
    }

    /**
     * Updates the entity; returns true if no property already existed and is also not computed;
     * i.e., setting the value was w.r.t. the current state of the property state OK.
     */
    private[this] def update(
        e: Entity,
        // Recall that ub != lb even though we have no new dependees ;
        // This is generally the case for collaboratively computed properties or
        // properties for which a computation was eagerly scheduled due to an
        // updated dependee.
        lb:           Property,
        ub:           Property,
        newDependees: Traversable[SomeEOptionP]
    ): Boolean = {
        if (debug && e == null) {
            throw new IllegalArgumentException("the entity must not be null");
        }
        val pkId = ub.key.id
        ps(pkId).get(e) match {
            case null ⇒
                // The entity is unknown (=> there are no dependers/dependees):
                ps(pkId).put(e, PropertyValue(lb, ub, newDependees))
                // registration with the new dependees is done when processing IntermediateResult
                true

            case pValue: IntermediatePropertyValue ⇒
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

                // 2. Clear old dependees (remove onUpdateContinuation from dependees)
                //    and then update dependees.
                val epk = EPK(e, ub /*or lb*/ )
                for {
                    eOptP @ EOptionP(oldDependeeE, oldDependeePK) ← pValue.dependees // <= the old ones
                } {
                    val oldDependeePKId = oldDependeePK.id
                    // Please recall, that we don't create support data-structures
                    // (i.e., PropertyValue) eagerly... but they should have been
                    // created by now or the dependees should be empty!

                    val dependeePValue = ps(oldDependeePKId).get(oldDependeeE)
                    val dependeeIntermediatePValue = dependeePValue.asIntermediate
                    val dependersOfDependee = dependeeIntermediatePValue.dependers
                    dependeeIntermediatePValue.dependers = dependersOfDependee - epk
                }
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
                                    FinalEP(e, ub),
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
                        val dependerPValue = ps(dependerPKId).get(dependerEPK.e).asIntermediate
                        dependerPValue.dependees foreach { epkOfDependeeOfDepender ⇒
                            if (epkOfDependeeOfDepender.toEPK != epk) {
                                // We have to avoid checking against the "current" dependee
                                // because it is already final!
                                val dependeePKIdOfDepender = epkOfDependeeOfDepender.pk.id
                                val pValueOfDependeeOfDepender =
                                    ps(dependeePKIdOfDepender).get(epkOfDependeeOfDepender.e)
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

    override def set(e: Entity, p: Property): Unit = set(e, p, false)

    def set(e: Entity, p: Property, isFastTrackProperty: Boolean): Unit = handleExceptions {
        val key = p.key
        val pkId = key.id

        if (debug && !isFastTrackProperty && lazyComputations(pkId) != null) {
            throw new IllegalStateException(
                s"$e: setting $p is not supported; lazy computation is scheduled for $key"
            )
        }

        if (!update(e, p, p, Nil)) {
            throw new IllegalStateException(s"$e: setting $p failed due to existing property")
        }
    }

    override def handleResult(
        r:               PropertyComputationResult,
        forceEvaluation: Boolean                   = true // acutally ignored, but conceptually "true"
    ): Unit = handleExceptions {

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
                results.foreach(r ⇒ handleResult(r, forceEvaluation))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒ update(ep.e, ep.p, ep.p, newDependees = Nil) }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(e, p) = r
                update(e, p, p, Nil)

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                type E = e.type
                type P = Property
                val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
                val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                newEPSOption foreach { newEPS ⇒ update(e, newEPS.lb, newEPS.ub, Nil) }

            case IntermediateResult.id ⇒
                val IntermediateResult(e, lb, ub, newDependees, c, _) = r

                def checkNonFinal(dependee: SomeEOptionP): Unit = {
                    if (dependee.isFinal) {
                        throw new IllegalArgumentException(
                            s"$e (lb=$lb, ub=$ub): dependency to final property: $dependee"
                        )
                    }
                }

                def isDependeeUpdated(
                    dependeePValue: PropertyValue, // may contains newer info than "newDependee"
                    newDependee:    SomeEOptionP
                ): Boolean = {
                    dependeePValue != null && dependeePValue.ub != null &&
                        dependeePValue.ub != PropertyIsLazilyComputed && (
                            // ... we have some property
                            // 1) check that (implicitly)  the state of
                            // the current value must have been changed
                            dependeePValue.isFinal ||
                            // 2) check if the given dependee did not yet have a property
                            newDependee.hasNoProperty ||
                            // 3) the properties are different
                            newDependee.ub != dependeePValue.ub ||
                            newDependee.lb != dependeePValue.lb
                        )
                }

                // 1. let's check if a new dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val noUpdates = dependeeUpdateHandling match {

                    case EagerDependeeUpdateHandling ⇒
                        val theDependeesIterator = newDependees.toIterator
                        while (theDependeesIterator.hasNext) {
                            val newDependee = theDependeesIterator.next()
                            if (debug) checkNonFinal(newDependee)

                            val dependeeE = newDependee.e
                            val dependeePKId = newDependee.pk.id
                            val dependeePValue = getPropertyValue(dependeeE, dependeePKId)
                            if (isDependeeUpdated(dependeePValue, newDependee)) {
                                immediateOnUpdateComputationsCounter += 1
                                val newEP =
                                    if (dependeePValue.isFinal) {
                                        FinalEP(dependeeE, dependeePValue.ub)
                                    } else {
                                        EPS(dependeeE, dependeePValue.lb, dependeePValue.ub)
                                    }
                                val newR = c(newEP)
                                if (debug && newR == r) {
                                    throw new IllegalStateException(
                                        "an on-update continuation resulted in the same result as before:\n"+
                                            s"\told: $r\n\tnew: $newR"
                                    )
                                }
                                handleResult(newR)
                                return ;
                            }
                        }
                        true // all updates are handled; otherwise we have an early return

                    case dependeeUpdateHandling: LazyDependeeUpdateHandling ⇒
                        newDependees forall { newDependee ⇒
                            if (debug) checkNonFinal(newDependee)
                            val dependeeE = newDependee.e
                            val dependeePK = newDependee.pk
                            val dependeePKId = dependeePK.id
                            val dependeePValue = getPropertyValue(dependeeE, dependeePKId)
                            if (isDependeeUpdated(dependeePValue, newDependee)) {
                                // There were updates...
                                // hence, we will update the value for other analyses
                                // which want to get the most current value in the meantime,
                                // but we postpone notification of other analyses which are
                                // depending on it until we have the updated value (minimize
                                // the overall number of notifications.)
                                // println(s"update: $e => $p (isFinal=false;notifyDependers=false)")
                                scheduledOnUpdateComputationsCounter += 1
                                if (dependeePValue.isFinal) {
                                    val t =
                                        OnFinalUpdateComputationTask(
                                            this,
                                            FinalEP(dependeeE, dependeePValue.ub),
                                            c
                                        )
                                    if (dependeeUpdateHandling.delayHandlingOfFinalDependeeUpdates)
                                        tasks.addLast(t)
                                    else
                                        tasks.addFirst(t)
                                } else {
                                    val t =
                                        OnUpdateComputationTask(
                                            this,
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
                            case null ⇒
                                // the dependee is not known
                                ps(dependeePKId).put(
                                    dependeeE,
                                    new IntermediatePropertyValue(dependerEPK, c)
                                )

                            case dependeePValue: IntermediatePropertyValue ⇒
                                val dependeeDependers = dependeePValue.dependers
                                dependeePValue.dependers = dependeeDependers + dependency

                            case dependeePValue ⇒
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
                previouslyComputedPropertyKinds(pkId) = isComputed
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
        // We need a consistent interrupt state for fallback and cycle resolution:
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
                        ps(pkId).forEach { (e, pValue) ⇒
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
                                val p = fallbackPropertyBasedOnPkId(this, reason, e, pkId)
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
                        ps(pkId) forEach { (e, pValue) ⇒
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
                        (epk: SomeEOptionP) ⇒ ps(epk.pk.id).get(epk.e).dependees
                    )
                    for (cSCC ← cSCCs) {
                        val headEPK = cSCC.head
                        val e = headEPK.e
                        val pkId = headEPK.pk.id
                        val pValue = ps(pkId).get(e)
                        val lb = pValue.lb
                        val ub = pValue.ub
                        val headEPS = IntermediateEP(e, lb, ub)
                        val newP = PropertyKey.resolveCycle(this, headEPS)
                        if (traceCycleResolutions) {
                            val cycleAsText =
                                if (cSCC.size > 10)
                                    cSCC.take(10).mkString("", ",", "...")
                                else
                                    cSCC.mkString(",")

                            info(
                                "analysis progress",
                                s"resolving cycle(iteration:$quiescenceCounter): $cycleAsText by updatding $e:ub=$ub with $newP"
                            )
                        }
                        resolvedCSCCsCounter += 1
                        update(e, newP, newP, Nil)
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
                            ps(pkId).forEach { (e, pValue) ⇒
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
                ps(pkId) forEach { (e, pValue) ⇒
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
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        new PKESequentialPropertyStore(contextMap)
    }
}
