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

import scala.reflect.runtime.universe.Type
import java.lang.System.identityHashCode

import org.opalj.graphs
import scala.collection.mutable.AnyRefMap
import java.util.ArrayDeque

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId

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
) extends PropertyStore { store ⇒

    /*
     * Controls in which order updates are processed/scheduled.
     *
     * May be changed at any time.
     */
    @volatile var dependeeUpdateHandling: DependeeUpdateHandling = EagerDependeeUpdateHandling

    @volatile var delayHandlingOfDependerNotification: Boolean = true

    protected[this] var scheduledTasksCounter: Int = 0
    final def scheduledTasks: Int = scheduledTasksCounter

    protected[this] var scheduledOnUpdateComputationsCounter: Int = 0
    final def scheduledOnUpdateComputations: Int = scheduledOnUpdateComputationsCounter

    protected[this] var eagerOnUpdateComputationsCounter: Int = 0
    final def eagerOnUpdateComputations: Int = eagerOnUpdateComputationsCounter

    protected[this] var fallbacksUsedForComputedPropertiesCounter: Int = 0
    final def fallbacksUsedForComputedProperties: Int = fallbacksUsedForComputedPropertiesCounter

    protected[this] var resolvedCyclesCounter: Int = 0
    final def resolvedCycles: Int = resolvedCyclesCounter

    private[this] var quiescenceCounter = 0

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // --------------------------------------------------------------------------------------------

    private[this] val ps: Array[AnyRefMap[Entity, PropertyValue]] = {
        Array.fill(PropertyKind.SupportedPropertyKinds) { new AnyRefMap(50000) }
    }

    // Those computations that will only be scheduled if the result is required
    private[this] var lazyComputations: Array[SomePropertyComputation] = {
        new Array(PropertyKind.SupportedPropertyKinds)
    }

    // The list of scheduled computations
    private[this] var tasks: ArrayDeque[QualifiedTask] = new ArrayDeque(50000)

    // private[this] var computedPropertyKinds: IntTrieSet = null // has to be set before usage
    private[this] var computedPropertyKinds: Array[Boolean] = null // has to be set before usage

    // private[this] var delayedPropertyKinds: IntTrieSet = null // has to be set before usage
    private[this] var delayedPropertyKinds: Array[Boolean] = null // has to be set before usage

    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        require(e ne null)
        ps(pk.id).get(e) match {
            case Some(pValue) ⇒
                val ub = pValue.ub
                ub != null && ub != PropertyIsLazilyComputed
            case None ⇒
                false
        }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        require(e ne null)
        for {
            ePValue ← ps.iterator
            pValue ← ePValue.get(e)
            eps ← pValue.toEPS(e)
        } yield {
            eps
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        for {
            ePValue ← ps.iterator
            (e, pValue) ← ePValue
            eps ← pValue.toEPS(e)
            if propertyFilter(eps)
        } yield {
            e
        }
    }

    /**
     * Returns all entities which have the given property bounds based on an "==" (equals)
     * comparison.
     */
    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        require(lb ne null)
        require(ub ne null)
        entities((otherEPS: SomeEPS) ⇒ lb == otherEPS.lb && ub == otherEPS.ub)
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        for {
            (e, pValue) ← ps(pk.id).iterator
            eps ← pValue.toEPS[Entity](e)
        } yield {
            eps.asInstanceOf[EPS[Entity, P]]
        }
    }

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val properties = for {
                (ePValue, pkId) ← ps.iterator.zipWithIndex
                (e, pValue) ← ePValue
            } yield {
                val propertyKindName = PropertyKey.name(pkId)
                s"$e -> $propertyKindName[$pkId] = $pValue"
            }
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(properties=${ps.iterator.map(_.size).sum})"
        }
    }

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

    override def scheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new EagerPropertyComputationTask(this, e, pc))
    }

    private[this] def scheduleLazyComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = handleExceptions {
        scheduledTasksCounter += 1
        tasks.addLast(new LazyPropertyComputationTask(this, e, pc))
    }

    // triggers lazy property computations!
    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk), false)
    }

    // triggers lazy property computations!
    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        apply(epk, false)
    }

    private[this] def apply[E <: Entity, P <: Property](
        epk:   EPK[E, P],
        force: Boolean
    ): EOptionP[E, P] = {
        val e = epk.e
        val pk = epk.pk
        val pkId = pk.id

        ps(pkId).get(e) match {
            case None ⇒
                // the entity is unknown ...
                lazyComputations(pkId) match {
                    case null ⇒
                        if (debug && computedPropertyKinds == null) {
                            /*&& delayedPropertyKinds ne null (not necessary)*/
                            throw new IllegalStateException("setup phase was not called")
                        }
                        if (computedPropertyKinds(pkId) || delayedPropertyKinds(pkId)) {
                            epk
                        } else {
                            val p = PropertyKey.fallbackProperty(this, e, pk)
                            if (force) { set(e, p) }
                            FinalEP(e, p)
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        // create PropertyValue to ensure that we do not schedule
                        // multiple (lazy) computations => the entity is now known
                        ps(pkId).put(e, PropertyValue.lazilyComputed)
                        scheduleLazyComputationForEntity(e)(lc)
                        // return the "current" result
                        epk

                }

            case Some(pValue) ⇒
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

    def force(e: Entity, pk: SomePropertyKey): Unit = apply[Entity, Property](EPK(e, pk), true)

    /**
     * Returns the `PropertyValue` associated with the given Entity / PropertyKey or `null`.
     */
    private[seq] def getPropertyValue(e: Entity, pkId: Int): PropertyValue = {
        ps(pkId).get(e) match {
            case None         ⇒ null
            case Some(pValue) ⇒ pValue
        }
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
        val pkId = ub.key.id
        /*user level*/ assert(ub.key == lb.key)
        /*user level*/ assert(
            !lb.isOrderedProperty || {
                val ubAsOP = ub.asOrderedProperty
                ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self]); true
            }
        )
        ps(pkId).get(e) match {
            case None ⇒
                // The entity is unknown (=> there are no dependers/dependees):
                ps(pkId).put(e, PropertyValue(lb, ub, newDependees))
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

                    val dependeePValue = ps(oldDependeePKId)(oldDependeeE)
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
                                // TODO....
                                new EagerOnFinalUpdateComputationTask(
                                    this,
                                    FinalEP(e, ub),
                                    onUpdateContinuation
                                )
                            } else {
                                new EagerOnUpdateComputationTask(
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

            case Some(finalPValue) ⇒
                throw new IllegalStateException(s"$e: update of $finalPValue")
        }
    }

    override def set(e: Entity, p: Property): Unit = handleExceptions {
        val key = p.key
        val pkId = key.id

        if (debug && lazyComputations(pkId) != null) {
            throw new IllegalArgumentException(
                s"$e: setting $p is not supported; lazy computation is scheduled for $key"
            )
        }

        if (!update(e, p, p, Nil)) {
            throw new IllegalStateException(s"$e: setting $p failed due to existing property")
        }
    }

    override def handleResult(
        r:                  PropertyComputationResult,
        wasLazilyTriggered: Boolean
    ): Unit = handleExceptions {

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleEagerComputationForEntity(e)(pc) }

            case Results.id ⇒
                val Results(results) = r
                results.foreach(handleResult)

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
                val IntermediateResult(e, lb, ub, newDependees, c) = r

                def checkNonFinal(dependee: SomeEOptionP): Unit = {
                    if (dependee.isFinal) {
                        throw new IllegalStateException(
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
                                eagerOnUpdateComputationsCounter += 1
                                val newEP =
                                    if (dependeePValue.isFinal) {
                                        FinalEP(dependeeE, dependeePValue.ub)
                                    } else {
                                        EPS(dependeeE, dependeePValue.lb, dependeePValue.ub)
                                    }
                                handleResult(c(newEP), wasLazilyTriggered)
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
                                            dependeeE, dependeePValue.ub,
                                            c,
                                            wasLazilyTriggered
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
                                            c,
                                            wasLazilyTriggered
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

                            case Some(dependeePValue) ⇒
                                throw new UnknownError(
                                    "fatal internal error; "+
                                        "can't update dependees of final property"
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
    ): Unit = {
        assert(tasks.isEmpty)

        // this.computedPropertyKinds = IntTrieSet.empty ++ computedPropertyKinds.iterator.map(_.id)
        this.computedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        computedPropertyKinds foreach { pk ⇒ this.computedPropertyKinds(pk.id) = true }

        // this.delayedPropertyKinds = IntTrieSet.empty ++ delayedPropertyKinds.iterator.map(_.id)
        this.delayedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        delayedPropertyKinds foreach { pk ⇒ this.delayedPropertyKinds(pk.id) = true }
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for fallback and cycle resolution:
        var isInterrupted: Boolean = false
        do {
            continueComputation = false

            while (!tasks.isEmpty && !isInterrupted) {
                val task = tasks.pollFirst()
                task.apply()
                isInterrupted = this.isInterrupted()
            }
            if (tasks.isEmpty) quiescenceCounter += 1

            if (!isInterrupted) {
                // We have reached quiescence. let's check if we have to
                // fill in fallbacks or if we have to resolve cyclic computations.

                // 1. Let's search all EPKs for which we have no analyses scheduled in the
                //    future and use the fall back for them.
                //    (Recall that we return fallback properties eagerly if no analysis is
                //     scheduled or will be scheduled; but it is still possible that we will
                //     not have a property for a specific entity, if the underlying analysis
                //     doesn't compute one; in that case we need to put in fallback values.)
                for {
                    (ePValue, pkId) ← ps.iterator.zipWithIndex
                    (e, pValue) ← ePValue
                } {
                    // Check that we have no running computations and that the
                    // property will not be computed later on.
                    if (pValue.ub == null && !delayedPropertyKinds(pkId)) {
                        // assert(pv.dependers.isEmpty)

                        val fallbackProperty = fallbackPropertyBasedOnPkId(this, e, pkId)
                        if (traceFallbacks) {
                            trace(
                                "analysis progress",
                                s"used fallback $fallbackProperty for $e "+
                                    "(though an analysis was supposedly scheduled)"
                            )
                        }
                        fallbacksUsedForComputedPropertiesCounter += 1
                        update(e, fallbackProperty, fallbackProperty, Nil)

                        continueComputation = true
                    }
                }

                // 2. let's search for cSCCs that only consist of properties which will not be
                //    updated later on
                if (!continueComputation) {
                    val epks: Traversable[SomeEOptionP] =
                        for {
                            (ePValue, pkId) ← ps.zipWithIndex
                            (e, pValue) ← ePValue
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
                        (epk: SomeEOptionP) ⇒ ps(epk.pk.id)(epk.e).dependees
                    )
                    for { cSCC ← cSCCs } {
                        val headEPK = cSCC.head
                        val e = headEPK.e
                        val pkId = headEPK.pk.id
                        val lb = ps(pkId)(e).lb
                        val ub = ps(pkId)(e).ub
                        val headEPS = IntermediateEP(e, lb, ub)
                        val newEP = PropertyKey.resolveCycle(this, headEPS)
                        val cycleAsText =
                            if (cSCC.size > 10)
                                cSCC.take(10).mkString("", ",", "...")
                            else
                                cSCC.mkString(",")
                        if (traceCycleResolutions) {
                            info(
                                "analysis progress",
                                s"resolving cycle(iteration:$quiescenceCounter): $cycleAsText ⇒ $newEP"
                            )
                        }
                        resolvedCyclesCounter += 1
                        update(newEP.e, newEP.p, newEP.p, Nil)
                        continueComputation = true
                    }
                }

                if (!continueComputation) {
                    // We used no fallbacks and found no cycles, but we may still have
                    // (collaboratively computed) properties (e.g. CallGraph) which are
                    // not yet final; let's finalize them!
                    for {
                        (ePValue, pkId) ← ps.zipWithIndex
                        (e, pValue) ← ePValue
                    } {
                        val lb = pValue.lb
                        val ub = pValue.ub
                        val isFinal = pValue.isFinal
                        // Check that we have no running computations and that the
                        // property will not be computed later on.
                        if (!isFinal && lb != ub && !delayedPropertyKinds.contains(pkId)) {
                            update(e, ub, ub, Nil) // commit as Final value
                            continueComputation = true
                        }
                    }
                }
            }
        } while (continueComputation)

        if (debug && !isInterrupted) {
            // let's search for "unsatisfied computations"
            for {
                ePValue ← ps
                (e, pValue) ← ePValue
                if !pValue.isFinal
            } {
                error("analysis progress", s"intermediate property state: $e ⇒ $pValue")
            }
        }
    }
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
