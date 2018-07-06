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

import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.LongMap
import scala.collection.mutable
import scala.collection.{Map ⇒ SomeMap}
import org.opalj.collection.mutable.AnyRefAppendChain
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId

import scala.collection.mutable.ArrayBuffer

/**
 * A non-concurrent implementation of the property store. Entities are generally only stored on
 * demand. I.e.,
 *  - they have a property OR
 *  - a computation is already scheduled that will compute the property OR
 *  - we have a `depender`.
 *
 *  Compared to the [[PKESequentialPropertyStore]] this implementation can be considered a
 *  bare-bone implementation that primarily supports the strictly required features.
 *  Furthermore, this implementation is less polished w.r.t. the usage of data-structures.
 */
final class EPKSequentialPropertyStore private (
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

    final def supportsFastTrackPropertyComputations: Boolean = false

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

    type PKId = Long

    private[this] var scheduledTasksCounter: Int = 0
    def scheduledTasksCount: Int = scheduledTasksCounter

    private[this] var scheduledOnUpdateComputationsCounter: Int = 0
    def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputationsCounter

    private[this] var immediateOnUpdateComputationsCounter: Int = 0
    def immediateOnUpdateComputationsCount: Int = immediateOnUpdateComputationsCounter

    private[this] var resolvedCSCCsCounter: Int = 0
    def resolvedCSCCsCount: Int = resolvedCSCCsCounter

    private[this] var quiescenceCounter = 0
    def quiescenceCount: Int = quiescenceCounter

    def fastTrackPropertiesCount: Int = 0 // The EPK store does not support fast track properties

    def statistics: SomeMap[String, Int] = {
        mutable.LinkedHashMap(
            "scheduled tasks" -> scheduledTasksCount,
            "scheduled on update computations" -> scheduledOnUpdateComputationsCount,
            "immediate on update computations" -> immediateOnUpdateComputationsCount,
            "quiescence" -> quiescenceCount,
            "resolved cSCCs" -> resolvedCSCCsCount
        )
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // --------------------------------------------------------------------------------------------

    // map from
    // entity =>
    //        (long) map from
    //           property kind id =>
    //                PropertyValue
    private[this] val ps: AnyRefMap[Entity, LongMap[PropertyValue]] = AnyRefMap.empty

    /** Those computations that will only be scheduled if the result is required. */
    private[this] var lazyComputations: LongMap[SomePropertyComputation] = LongMap.empty

    private[this] var triggeredComputations: Array[ArrayBuffer[SomePropertyComputation]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { new ArrayBuffer[SomePropertyComputation](1) }
    }

    /** The list of scheduled computations. */
    private[this] var tasks: AnyRefAppendChain[() ⇒ Unit] = new AnyRefAppendChain()

    private[this] var previouslyComputedPropertyKinds: IntTrieSet = IntTrieSet.empty

    private[this] var computedPropertyKinds: IntTrieSet = _ /*null*/ // has to be set before usage

    private[this] var delayedPropertyKinds: IntTrieSet = _ /*null*/ // has to be set before usage

    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        require(e ne null)
        ps.contains(e) && {
            val pkPValue = ps(e)
            val pkId = pk.id.toLong
            pkPValue.contains(pkId) && {
                val ub = pkPValue(pkId).ub
                ub != null && ub != PropertyIsLazilyComputed
            }
        }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        require(e ne null)
        for {
            epkpss ← ps.get(e).toIterator // the entities are lazily initialized!
            pValue ← epkpss.valuesIterator
            eps ← pValue.toEPS(e)
        } yield {
            eps
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        for {
            (e, pkIdPValue) ← ps.iterator
            pValue ← pkIdPValue.values
            eps ← pValue.toEPS(e)
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
        for {
            (e, pkIdPValue) ← ps.iterator
            (pkId, pValue) ← pkIdPValue
            if pkId == pk.id
            eps ← pValue.toEPS[Entity](e)
        } yield {
            eps.asInstanceOf[EPS[Entity, P]]
        }
    }

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val properties = for {
                (e, pkIdPStates) ← ps
                (pkId, pState) ← pkIdPStates
            } yield {
                val propertyKindName = PropertyKey.name(pkId.toInt)
                s"$e -> $propertyKindName[$pkId] = $pState"
            }
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(entitiesCount=${ps.size})"
        }
    }

    override def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && tasks.nonEmpty) {
            throw new IllegalStateException(
                "lazy computations should only be registered while no analysis are scheduled"
            )
        }
        lazyComputations.put(pk.id.toLong, pc)
    }

    override def registerTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && !tasks.isEmpty) {
            throw new IllegalStateException(
                "triggered computations should only be registered as long as no analysis is scheduled"
            )
        }
        triggeredComputations(pk.id) += pc
    }

    private[this] def triggerComputations(e: Entity, pkId: Int): Unit = {
        val triggeredComputations = this.triggeredComputations(pkId)
        if (triggeredComputations != null) {
            triggeredComputations foreach (pc ⇒
                scheduleEagerComputationForEntity(e)(pc.asInstanceOf[PropertyComputation[Entity]]))
        }
    }

    override def scheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.append(() ⇒ handleResult(pc(e)))
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
        val pkIdInt = pk.id
        val pkId = pkIdInt.toLong

        ps.get(e) match {
            case None ⇒
                // the entity is unknown
                lazyComputations.get(pkId) match {
                    case Some(lc) ⇒
                        // create PropertyValue to ensure that we do not schedule
                        // multiple (lazy) computations => the entity is now known
                        ps += ((e, LongMap((pkId, PropertyValue.lazilyComputed))))
                        scheduleEagerComputationForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])
                        // return the "current" result
                        epk

                    case None ⇒
                        if (debug && computedPropertyKinds == null) {
                            /*&& delayedPropertyKinds ne null (not necessary)*/
                            throw new IllegalStateException("setup phase was not called")
                        }
                        if (computedPropertyKinds.contains(pkIdInt) ||
                            delayedPropertyKinds.contains(pkIdInt)) {
                            epk
                        } else {
                            val reason = {
                                if (previouslyComputedPropertyKinds.contains(pkIdInt))
                                    PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                else
                                    PropertyIsNotComputedByAnyAnalysis
                            }
                            val p = PropertyKey.fallbackProperty(this, reason, e, pk)
                            if (force) { set(e, p) }
                            FinalEP(e, p)
                        }
                }

            case Some(pkIdPValue) ⇒ pkIdPValue.get(pkId) match {

                case None ⇒
                    // the entity is known, but the property kind was never
                    // queried before or there is no computation whatsoever..
                    lazyComputations.get(pkId) match {
                        case Some(lc) ⇒
                            // create PropertyValue to ensure that we do not schedule
                            // multiple (lazy) computations => the entity is now known
                            pkIdPValue += ((pkId, PropertyValue.lazilyComputed))
                            scheduleEagerComputationForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])
                            epk

                        case None ⇒
                            if (debug && computedPropertyKinds == null) {
                                /*&& delayedPropertyKinds ne null (not necessary)*/
                                throw new IllegalStateException("setup phase was not called")
                            }
                            if (computedPropertyKinds.contains(pkIdInt) ||
                                delayedPropertyKinds.contains(pkIdInt)) {
                                epk
                            } else {
                                val fallbackReason = {
                                    if (previouslyComputedPropertyKinds.contains(pkIdInt))
                                        PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                    else
                                        PropertyIsNotComputedByAnyAnalysis
                                }
                                val p = PropertyKey.fallbackProperty(this, fallbackReason, e, pk)
                                FinalEP(e, p)
                            }
                    }

                case Some(pValue) ⇒
                    val ub = pValue.ub // or lb... doesn't matter
                    if (ub != null && ub != PropertyIsLazilyComputed)
                        // we have a property
                        EPS(e, pValue.lb.asInstanceOf[P], pValue.ub.asInstanceOf[P])
                    else {
                        // We do not (yet) have a value, but a lazy property
                        // computation is already scheduled (if available).
                        // Recall that it is a strict requirement that a
                        // dependee which is listed in the set of dependees
                        // of an IntermediateResult must have been queried;
                        // however the sequential store does not create the
                        // data-structure eagerly!
                        if (debug && ub == null && lazyComputations.getOrElse(pkId, null) != null) {
                            throw new IllegalStateException(
                                "registered lazy computation was not triggerd, "+
                                    "this happens, e.g., if the list of dependees contains EPKs "+
                                    "which are directly instantiated without being queried before"
                            )
                        }
                        epk
                    }
            }
        }
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        apply(EPK(e, pk), true)
    }

    /**
     * Returns the `PropertyValue` associated with the given Entity / PropertyKey or `null`.
     */
    private[seq] def getPropertyValue(e: Entity, pkId: PKId): PropertyValue = {
        if (!ps.contains(e))
            return null;
        val pkPValue = ps(e)

        if (!pkPValue.contains(pkId))
            return null;

        pkPValue(pkId)
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
            throw new IllegalArgumentException("the entity must not be null")
        }
        val pkIdInt = ub.key.id
        val pkId = pkIdInt.toLong

        ps.get(e) match {
            case None ⇒
                // The entity is unknown (=> there are no dependers/dependees):
                ps += ((
                    e,
                    LongMap((pkId, PropertyValue(lb, ub, newDependees)))
                ))
                triggerComputations(e, pkIdInt)
                // registration with the new dependees is done when processing IntermediateResult
                true

            case Some(pkIdPValue) ⇒ /* The entity is known: */ pkIdPValue.get(pkId) match {

                case None ⇒
                    // A property of the respective kind was not yet stored/requested.
                    // (=> there are no dependers/dependees):
                    pkIdPValue += ((pkId, PropertyValue(lb, ub, newDependees)))
                    triggerComputations(e, pkIdInt)
                    // registration with the new dependees is done when processing IntermediateResult
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

                    if (oldUB == null || oldUB == PropertyIsLazilyComputed) {
                        triggerComputations(e, pkIdInt)
                    }

                    // 2. Clear old dependees (remove onUpdateContinuation from dependees)
                    //    and then update dependees.
                    val epk = EPK(e, ub /*or lb*/ )
                    for {
                        eOptP @ EOptionP(oldDependeeE, oldDependeePK) ← pValue.dependees // <= the old ones
                    } {
                        val oldDependeePKId = oldDependeePK.id.toLong
                        // Please recall, that we don't create support data-structures
                        // (i.e., PropertyValue) eagerly... but they should have been
                        // created by now or the dependees should be empty!
                        assert(
                            ps(oldDependeeE).contains(oldDependeePKId),
                            s"$e(${ub.key}): dependee without support data structure: $eOptP"
                        )
                        val dependeePValue = ps(oldDependeeE)(oldDependeePKId)
                        val dependeeIntermediatePValue = dependeePValue.asIntermediate
                        val dependersOfDependee = dependeeIntermediatePValue.dependers
                        dependeeIntermediatePValue.dependers = dependersOfDependee - epk
                    }
                    if (newPValueIsFinal)
                        ps(e)(pkId) = new FinalPropertyValue(ub)
                    else
                        pValue.dependees = newDependees

                    // 3. Notify dependers if necessary
                    if (lb != oldLB || ub != oldUB || newPValueIsFinal) {

                        pValue.dependers foreach { depender ⇒
                            val (dependerEPK, onUpdateContinuation) = depender
                            val t =
                                if (newPValueIsFinal) {
                                    () ⇒ handleResult(onUpdateContinuation(FinalEP(e, ub)))
                                } else {
                                    () ⇒
                                        {
                                            // get the most current pValue when the depender
                                            // is eventually evaluated; the effectiveness
                                            // of this check depends on the scheduling strategy(!)
                                            val pValue = ps(e)(pkId)
                                            val eps = EPS(e, pValue.lb, pValue.ub)
                                            handleResult(onUpdateContinuation(eps))
                                        }
                                }
                            scheduledOnUpdateComputationsCounter += 1
                            if (delayHandlingOfDependerNotification)
                                tasks.append(t)
                            else
                                tasks.prepend(t)
                            // Clear depender => dependee lists.
                            // Given that we have triggered the depender, we now have
                            // to remove the respective onUpdateContinuation from all
                            // dependees of the respective depender to avoid that the
                            // onUpdateContinuation is triggered multiple times!
                            val dependerPKId = dependerEPK.pk.id.toLong
                            val dependerPValue = ps(dependerEPK.e)(dependerPKId).asIntermediate
                            dependerPValue.dependees foreach { epkOfDependeeOfDepender ⇒
                                if (epkOfDependeeOfDepender.toEPK != epk) {
                                    // We have to avoid checking against the "current" dependee
                                    // because it is already final!
                                    val dependeePKIdOfDepender = epkOfDependeeOfDepender.pk.id.toLong
                                    val pValueOfDependeeOfDepender =
                                        ps(epkOfDependeeOfDepender.e)(dependeePKIdOfDepender)
                                    pValueOfDependeeOfDepender.asIntermediate.dependers -= dependerEPK
                                }
                            }
                            dependerPValue.dependees = Nil
                        }
                        pValue.dependers = Map.empty
                    }

                    oldLB == null /*AND/OR oldUB == null*/

                case Some(finalPValue) ⇒
                    throw new IllegalStateException(s"$e: update of $finalPValue")
            }
        }
    }

    override def set(e: Entity, p: Property): Unit = handleExceptions {
        val pkId = p.key.id.toLong

        if (debug && lazyComputations.get(pkId).nonEmpty) {
            throw new IllegalStateException(
                s"$e: setting $p is not supported; "+
                    s"lazy computation scheduled for property kind ${p.key}"
            )
        }

        if (!update(e, p, p, Nil)) {
            throw new IllegalStateException(s"$e: setting $p failed due to existing property")
        }
    }

    override def handleResult(
        r:               PropertyComputationResult,
        forceEvaluation: Boolean                   = true // ignored... but conceptually "true"
    ): Unit = handleExceptions {
        r.id match {

            case NoResult.id ⇒ {
                // A computation reported no result; i.e., it is not possible to
                // compute a/some property/properties for a given entity.
            }

            case Results.id ⇒
                val Results(results) = r
                results.foreach(r ⇒ handleResult(r, forceEvaluation))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒ update(ep.e, ep.p, ep.p, newDependees = Nil) }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Iterator[(PropertyComputation[e],e)]*/ , _) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleEagerComputationForEntity(e)(pc) }

            //
            // Methods which actually store results...
            //

            case ExternalResult.id ⇒ throw new UnknownError(s"unexteped result: $r")

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
                            val dependeePKId = newDependee.pk.id.toLong
                            val dependeePValue = getPropertyValue(dependeeE, dependeePKId)

                            if (isDependeeUpdated(dependeePValue, newDependee)) {
                                immediateOnUpdateComputationsCounter += 1
                                val newR =
                                    if (dependeePValue.isFinal) {
                                        c(FinalEP(dependeeE, dependeePValue.ub))
                                    } else {
                                        val newEP = EPS(dependeeE, dependeePValue.lb, dependeePValue.ub)
                                        c(newEP)
                                    }
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

                    case LazyDependeeUpdateHandling(
                        delayHandlingOfFinalDependeeUpdates,
                        delayHandlingOfNonFinalDependeeUpdates
                        ) ⇒
                        newDependees forall { newDependee ⇒
                            if (debug) checkNonFinal(newDependee)
                            val dependeeE = newDependee.e
                            val dependeePKId = newDependee.pk.id.toLong
                            val dependeePValue = getPropertyValue(dependeeE, dependeePKId)
                            if (isDependeeUpdated(dependeePValue, newDependee)) {
                                // There were updates... hence, we will update the value for other analyses
                                // which want to get the most current value in the meantime, but we postpone
                                // notification of other analyses which are depending on it until we have
                                // the updated value (minimize the overall number of notifications.)
                                // println(s"update: $e => $p (isFinal=false;notifyDependers=false)")

                                scheduledOnUpdateComputationsCounter += 1
                                if (dependeePValue.isFinal) {
                                    def t(): Unit = {
                                        val newEP = FinalEP(dependeeE, dependeePValue.ub)
                                        handleResult(c(newEP))
                                    }
                                    if (delayHandlingOfFinalDependeeUpdates)
                                        tasks.append(() ⇒ t())
                                    else
                                        tasks.prepend(() ⇒ t())
                                } else {
                                    val t = () ⇒ {
                                        handleResult({
                                            val newestPValue = ps(dependeeE)(dependeePKId)
                                            c(EPS(dependeeE, newestPValue.lb, newestPValue.ub))
                                        })
                                    }
                                    if (delayHandlingOfNonFinalDependeeUpdates)
                                        tasks.append(t)
                                    else
                                        tasks.prepend(t)
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
                        val dependeePKId = dependee.pk.id.toLong

                        ps.get(dependeeE) match {
                            case None ⇒
                                // the dependee is not known
                                ps += ((
                                    dependeeE,
                                    LongMap((
                                        dependeePKId,
                                        new IntermediatePropertyValue(dependerEPK, c)
                                    ))
                                ))

                            case Some(dependeePKIdPValue) ⇒
                                dependeePKIdPValue.get(dependeePKId) match {

                                    case None ⇒
                                        val pValue = new IntermediatePropertyValue(dependerEPK, c)
                                        dependeePKIdPValue += (dependeePKId, pValue)

                                    case Some(dependeePValue: IntermediatePropertyValue) ⇒
                                        val dependeeDependers = dependeePValue.dependers
                                        dependeePValue.dependers = dependeeDependers + dependency

                                    case Some(dependeePValue) ⇒
                                        throw new UnknownError(
                                            "fatal internal error; "+
                                                "can't update dependees of final property"
                                        )
                                }
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
    ): Unit = {
        if (debug && !tasks.isEmpty) {
            throw new IllegalStateException(
                "setup phase can only be called as long as no tasks are scheduled"
            )
        }
        if (this.computedPropertyKinds != null) {
            this.previouslyComputedPropertyKinds ++= this.computedPropertyKinds
        }

        this.computedPropertyKinds = IntTrieSet.empty ++ computedPropertyKinds.iterator.map(_.id)
        this.delayedPropertyKinds = IntTrieSet.empty ++ delayedPropertyKinds.iterator.map(_.id)
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for fallback and cycle resolution:
        var isSuspended: Boolean = false
        do {
            continueComputation = false

            while (tasks.nonEmpty && !isSuspended) {
                tasks.take().apply()
                isSuspended = this.isSuspended()
            }
            if (tasks.isEmpty) quiescenceCounter += 1

            if (!isSuspended) {
                // We have reached quiescence. let's check if we have to
                // fill in fallbacks or if we have to resolve cyclic computations.

                // 1. let's search all EPKs for which we have no analyses scheduled in the
                //    future and use the fall back for them
                //    (Recall that we return fallback properties eagerly if no analysis is
                //     scheduled or will be scheduled; but it is still possible that we will
                //     not have a property for a specific entity, if the underlying analysis
                //     doesn't compute one; in that case we need to put in fallback values.)
                for {
                    (e, pkIdPV) ← ps
                    (pkLongId, pValue) ← pkIdPV
                } {
                    val pkId = pkLongId.toInt
                    // Check that we have no running computations and that the
                    // property will not be computed later on.
                    if (pValue.ub == null && !delayedPropertyKinds.contains(pkId)) {
                        assert(pValue.dependees.isEmpty)
                        val reason = {
                            if (previouslyComputedPropertyKinds.contains(pkId) ||
                                computedPropertyKinds.contains(pkId))
                                PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                            else
                                PropertyIsNotComputedByAnyAnalysis
                        }
                        val p = fallbackPropertyBasedOnPkId(this, reason, e, pkId)
                        if (traceFallbacks) {
                            val message = s"used fallback $p (reason=$reason) for $e"
                            info("analysis progress", message)
                        }
                        handleResult(Result(e, p))
                        continueComputation = true
                    }
                }

                // 2. let's search for cSCCs that only consist of properties which will not be
                //    updated later on
                if (!continueComputation) {
                    val epks: Traversable[SomeEOptionP] =
                        for {
                            (e, pkIdPValue) ← ps
                            (pkLongId, pValue) ← pkIdPValue
                            pkId = pkLongId.toInt
                            ub = pValue.ub
                            if ub != null // analyses must always commit some value; hence,
                            // Both of the following tests are necessary because we may have
                            // properties which are only computed in a later phase; in that
                            // case we may have EPKs related to entities which are not used.
                            if pValue.dependees.nonEmpty // Can this node can be part of a cycle?
                            if pValue.dependers.nonEmpty // Can this node can be part of a cycle?
                        } yield {
                            assert(ub != PropertyIsLazilyComputed)
                            EPK(e, ub.key): SomeEOptionP
                        }

                    val cSCCs = graphs.closedSCCs(
                        epks,
                        (epk: SomeEOptionP) ⇒ ps(epk.e)(epk.pk.id.toLong).dependees
                    )
                    for {
                        cSCC ← cSCCs
                    } {
                        resolvedCSCCsCounter
                        val headEPK = cSCC.head
                        val e = headEPK.e
                        val lb = ps(e)(headEPK.pk.id.toLong).lb
                        val ub = ps(e)(headEPK.pk.id.toLong).ub
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
                        update(e, newP, newP, Nil)
                        continueComputation = true
                    }
                }

                if (!continueComputation) {
                    // We used no fallbacks and found no cycles, but we may still have
                    // (collaboratively computed) properties (e.g. CallGraph) which are
                    // not yet final; let's finalize them!

                    var toBeFinalized: List[(AnyRef, Property)] = Nil
                    for {
                        (e, pkIdPV) ← ps
                        (pkLongId, pValue) ← pkIdPV
                    } {
                        val pkId = pkLongId.toInt
                        val lb = pValue.lb
                        val ub = pValue.ub
                        val isFinal = pValue.isFinal
                        // Check that we have no running computations and that the
                        // property will not be computed later on.
                        if (!isFinal &&
                            lb != ub &&
                            !delayedPropertyKinds.contains(pkId) &&
                            pValue.dependees.isEmpty) {
                            toBeFinalized ::= ((e, ub))
                        }
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
            // let's search for "unsatisfied computations"
            for {
                // entity =>
                //        (long) map from
                //           property kind id =>
                //                PropertyValue
                // private[this] val ps: AnyRefMap[Entity, LongMap[PropertyValue]] = AnyRefMap.empty
                (e, pkIdPValue) ← ps
                (pkId, pValue) ← pkIdPValue
                if !pValue.isFinal
            } {
                error("analysis progress", s"unexpected intermediate property state: $e ⇒ $pValue")
            }
        }
    }

    def shutdown(): Unit = {}

}

/**
 * Factory for creating `EPKSequentialPropertyStore`s.
 */
object EPKSequentialPropertyStore extends PropertyStoreFactory {

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): EPKSequentialPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        new EPKSequentialPropertyStore(contextMap)
    }
}
