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

import scala.reflect.runtime.universe.Type

import java.lang.System.identityHashCode
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.LongMap

import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId

/**
 * A non-concurrent implementation of the property store. Entities are generally only stored on
 * demand. I.e.,
 *  - they have a property OR
 *  - a computation is already scheduled that will compute the property OR
 *  - we have a `depender`.
 */
class SequentialPropertyStore private (

        val ctx: Map[Type, AnyRef]
)(
        implicit
        val logContext: LogContext
) extends PropertyStore { store ⇒

    type PKId = Long

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

    // Those computations that will only be scheduled if the result is required
    private[this] var lazyComputations: LongMap[SomePropertyComputation] = LongMap.empty

    // The list of scheduled computations
    private[this] var tasks: Chain[() ⇒ Unit] = Chain.empty

    private[this] var computedPropertyKinds: IntTrieSet = null // has to be set before usage

    private[this] var delayedPropertyKinds: IntTrieSet = null // has to be set before usage

    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        ps.contains(e) && {
            val pkPValue = ps(e)
            val pkId = pk.id.toLong
            pkPValue.contains(pkId) && {
                val p = pkPValue(pkId).p
                p != null && p != PropertyIsLazilyComputed
            }
        }
    }

    override def properties(e: Entity): Iterator[Property] = {
        require(e ne null)
        for {
            epkpss ← ps.get(e).toIterator // the entities are lazily initialized!
            (pkid, pValue) ← epkpss
            p = pValue.p
            if p != null && p != PropertyIsLazilyComputed
        } yield {
            p
        }
    }

    override def entities(propertyFilter: Property ⇒ Boolean): Iterator[Entity] = {
        for {
            (e, pkIdPValue) ← ps.iterator
            pValue ← pkIdPValue.values
            p = pValue.p
            if p != null && p != PropertyIsLazilyComputed && propertyFilter(p)
        } yield {
            e
        }
    }

    /**
     * Returns all entities which have the given property based on an "==" (equals) comparison
     * with the given property.
     */
    override def entities[P <: Property](p: P): Iterator[Entity] = {
        require(p ne null)
        entities((otherP: Property) ⇒ p == otherP)
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        for {
            (e, pkIdPValue) ← ps.iterator
            pValue ← pkIdPValue.values
            p = pValue.p
            if p != null && p != PropertyIsLazilyComputed && p.id == pk.id
        } yield {
            EPS(e, p.asInstanceOf[P], pValue.isFinal)
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

    override def registerLazyPropertyComputation[P](
        pk: PropertyKey[P],
        pc: SomePropertyComputation
    ): Unit = {
        lazyComputations += ((pk.id.toLong, pc))
    }

    override def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        tasks :&:= (() ⇒ handleResult(pc(e)))
    }

    // triggers lazy property computations!
    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk))
    }

    // triggers lazy property computations!
    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        val e = epk.e
        val pkId = epk.pk.id.toLong

        ps.get(e) match {
            case None ⇒
                // the entity is unknown
                lazyComputations.get(pkId) foreach { lc ⇒
                    // create PropertyValue to ensure that we do not schedule
                    // multiple (lazy) computations => the entity is now known
                    ps += ((e, LongMap((pkId, PropertyValue.lazilyComputed))))
                    scheduleForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])
                }
                // return the "current" result
                epk

            case Some(pkIdPValue) ⇒ pkIdPValue.get(pkId) match {

                case None ⇒
                    // the entity is known, but the property kind was never
                    // queried before or there is no computation whatsoever..
                    lazyComputations.get(pkId) foreach { lc ⇒
                        // create PropertyState to ensure that we do not schedule
                        // multiple (lazy) computations => the entity is now known
                        pkIdPValue += ((pkId, PropertyValue.lazilyComputed))
                        scheduleForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])
                    }
                    epk

                case Some(pValue) ⇒
                    val p = pValue.p
                    if (p != null && p != PropertyIsLazilyComputed)
                        // we have a property
                        EPS(e, pValue.p.asInstanceOf[P], pValue.isFinal)
                    else
                        // We do not (yet) have a value, but a lazy property
                        // computation is already scheduled (if available).
                        // Recall that it is a strict requirement that a
                        // dependee which is listed in the set of dependees
                        // of an IntermediateResult must have been queried
                        // before.
                        epk
            }
        }
    }

    /**
     * Returns the `PropertyValue` associated with the given Entity / PropertyKey or `null`.
     * <
     */
    private[fpcf] def getPropertyValue(e: Entity, pkId: PKId): PropertyValue = {
        if (!ps.contains(e))
            return null;
        val pkPValue = ps(e)

        if (!pkPValue.contains(pkId))
            return null;

        pkPValue(pkId)
    }

    private[this] def update(
        e: Entity,
        p: Property,
        // Recall that isFinal can be false even though we have a property but no new dependees ;
        // This is generally the case for collaboratively computed properties or
        // properties for which a computation was eagerly scheduled due to an
        // updated dependee.
        isFinal:         Boolean,
        notifyDependers: Boolean,
        newDependees:    Traversable[SomeEOptionP]
    ): Property = {

        val pkId = p.key.id
        ps.get(e) match {
            case None ⇒
                // The entity is unknown:
                ps += ((
                    e,
                    LongMap((pkId.toLong, new PropertyValue(p, isFinal, Map.empty, newDependees)))
                ))
                // Recall that registration with the new dependees is done by
                // IntermediateResult if required.
                null

            case Some(pkIdPValue) ⇒
                // The entity is known:
                pkIdPValue.get(pkId.toLong) match {

                    case None ⇒
                        // But, we have no property of the respective kind:
                        pkIdPValue += ((
                            pkId.toLong,
                            new PropertyValue(p, isFinal, Map.empty, newDependees)
                        ))
                        // Recall that registration with the new dependees is done by
                        // IntermediateResult if required.
                        null

                    case Some(pValue) ⇒
                        // The entity is known and we have a property value for the respective
                        // kind; i.e., we may have (old) dependees and also dependers.

                        // 1. Check and update property:
                        val oldP = pValue.p
                        if (debug && p.isOrderedProperty) {
                            p.asOrderedProperty.checkIsValidSuccessorOf(oldP)
                        }
                        pValue.p = p
                        if (pValue.isFinal) {
                            val message = s"already final: $e@${identityHashCode(e).toHexString}/$p"
                            throw new IllegalStateException(message)
                        }

                        // 2. Clear old dependees (remove onUpdateContinuation from dependees)
                        //    and then update dependees
                        val epk = EPK(e, p)
                        for {
                            EOptionP(oldDependeeE, oldDependeePk) ← pValue.dependees
                        } {
                            val dependeePValue = ps(oldDependeeE)(oldDependeePk.id.toLong)
                            val dependersOfDependee = dependeePValue.dependers
                            dependeePValue.dependers = dependersOfDependee - epk
                        }
                        pValue.dependees = newDependees

                        // 3. Notify dependers if necessary
                        assert((isFinal && notifyDependers) || !isFinal)
                        if (p != oldP || isFinal) {
                            pValue.isFinal = isFinal

                            if (notifyDependers) {
                                pValue.dependers foreach { depender ⇒
                                    val (epk, onUpdateContinuation) = depender
                                    tasks :&:= (
                                        if (isFinal) {
                                            () ⇒ handleResult(onUpdateContinuation(FinalEP(e, p)))
                                        } else {
                                            () ⇒
                                                {
                                                    // get the most current pValue(!)
                                                    val pValue = ps(e)(pkId.toLong)
                                                    val eps = EPS(e, pValue.p, pValue.isFinal)
                                                    handleResult(onUpdateContinuation(eps))
                                                }
                                        }
                                    )
                                    // Clear depender/dependee lists.
                                    // Given that we have triggered the depender, we now have
                                    // to remove the respective onUpdateContinuation from all
                                    // dependees of the respective depender to avoid that the
                                    // onUpdateContinuation is triggered multiple times!
                                    val dependerPValue = ps(epk.e)(epk.pk.id.toLong)
                                    dependerPValue.dependees foreach { epkOfDepeendeeOfDepender ⇒
                                        val pValueOfDependeeOfDepender = ps(epkOfDepeendeeOfDepender.e)(epkOfDepeendeeOfDepender.pk.id.toLong)
                                        pValueOfDependeeOfDepender.dependers = pValueOfDependeeOfDepender.dependers - (epkOfDepeendeeOfDepender.toEPK)
                                    }
                                    dependerPValue.dependees = Nil
                                }
                                pValue.dependers = Map.empty
                            }
                        }

                        oldP
                }
        }
    }

    override def set(e: Entity, p: Property): Unit = {
        val pkId = p.key.id.toLong

        /*user-level*/ assert(
            lazyComputations.get(pkId).isEmpty,
            s"lazy computation scheduled for property kind $pkId"
        )

        val oldP = update(e, p, isFinal = true, notifyDependers = true, Nil)
        if (oldP != null) {
            throw new IllegalStateException(s"associating $e with $p failed; property is $oldP")
        }
    }

    override def handleResult(r: PropertyComputationResult): Unit = {

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleForEntity(e)(pc) }

            case Results.id ⇒
                val Results(results) = r
                results.foreach(handleResult)

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒
                    update(ep.e, ep.p, isFinal = true, notifyDependers = true, newDependees = Nil)
                }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(e, p) = r
                update(e, p, isFinal = true, notifyDependers = true, Nil)

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                val eOptionP = apply(e, pk)
                val newPOption = if (eOptionP.hasProperty) u(Some(eOptionP.p)) else u(None)
                newPOption foreach { newP ⇒
                    update(e, newP, isFinal = false, notifyDependers = true, Nil)
                }

            case IntermediateResult.id ⇒
                val IntermediateResult(e, p, newDependees /*: Traversable[SomeEOptionP]*/ , c) = r
                // 1. let's check if a new dependee is already updated...
                //    If so, we directly schedule a task again to compute the property.
                val noUpdates = newDependees forall { newDependee ⇒
                    assert(
                        !newDependee.isFinal,
                        s"dependency listed to final property ${newDependee.e}/${newDependee.p}"
                    )
                    val dependeeE = newDependee.e
                    val dependeePKId = newDependee.pk.id.toLong
                    val dependeePValue = getPropertyValue(dependeeE, dependeePKId)
                    val isDependeeUpdated =
                        dependeePValue != null && dependeePValue.p != null &&
                            dependeePValue.p != PropertyIsLazilyComputed && (
                                // we (now) have some property
                                dependeePValue.isFinal || // ... at least the state is updated
                                newDependee.hasNoProperty || // ... we definitively have a new value
                                newDependee.p != dependeePValue.p
                            )

                    if (isDependeeUpdated) {
                        tasks :&:= (
                            if (dependeePValue.isFinal)
                                () ⇒ handleResult(c(FinalEP(dependeeE, dependeePValue.p)))
                            else
                                () ⇒ handleResult({
                                    val newestPValue = ps(dependeeE)(dependeePKId)
                                    c(EPS(dependeeE, newestPValue.p, newestPValue.isFinal))
                                })
                        )
                        false
                    } else {
                        true // <= no update
                    }
                }

                // 2. the most current value of every dependee was taken into account
                if (noUpdates) {
                    // 2.1. update the value (trigger dependers/clear old dependees)
                    // println(s"update: $e => $p (isFinal=false;notifyDependers=true)")
                    update(e, p, isFinal = false, notifyDependers = true, newDependees)

                    // 2.2. register with new (!) dependees
                    newDependees foreach { dependee ⇒
                        val dependeeE = dependee.e
                        val dependeePKId = dependee.pk.id.toLong
                        val dependerEPK = EPK(e, p)
                        val dependency = (dependerEPK, c)
                        ps.get(dependeeE) match {
                            case None ⇒
                                // the dependee is not known
                                ps += ((
                                    dependeeE,
                                    LongMap((dependeePKId, new PropertyValue(dependerEPK, c)))
                                ))

                            case Some(pkPValue) ⇒
                                pkPValue.get(dependeePKId) match {

                                    case None ⇒
                                        val newPValue = new PropertyValue(dependerEPK, c)
                                        pkPValue += (dependeePKId, newPValue)

                                    case Some(dependeePValue) ⇒
                                        val dependeeDependers = dependeePValue.dependers
                                        dependeePValue.dependers = dependeeDependers + dependency
                                }
                        }
                    }
                } else {
                    // There were updates... hence, we will update the value for other analyses
                    // which want to get the most current value in the meantime, but we postpone
                    // notification of other analyses which are depending on it until we have
                    // the updated value (minimize the overall number of notifications.)
                    // println(s"update: $e => $p (isFinal=false;notifyDependers=false)")
                    update(e, p, isFinal = false, notifyDependers = false, Nil)
                }
        }
    }

    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind]
    ): Unit = {
        this.computedPropertyKinds = IntTrieSet.empty ++ computedPropertyKinds.iterator.map(_.id)
        this.delayedPropertyKinds = IntTrieSet.empty ++ delayedPropertyKinds.iterator.map(_.id)
    }

    override def waitOnPhaseCompletion(): Unit = {
        var continueComputation: Boolean = false
        do {
            continueComputation = false

            while (tasks.nonEmpty && !isInterrupted()) {
                val task = tasks.head
                tasks = tasks.tail
                task.apply()
            }

            if (!isInterrupted()) {
                // We have reached quiescence. let's check if we have to
                // fill in fallbacks or if we have to resolve cyclic computations.

                // 1. let's search all EPKs for which we have no analyses scheduled and
                //    use the fall back for them
                for {
                    (e, pkIdPV) ← ps
                    (pkLongId, pv) ← pkIdPV
                } {
                    val pkId = pkLongId.toInt
                    // Check that we have no running computations and that the
                    // property will not be computed later on.
                    if (pv.p == null && !delayedPropertyKinds.contains(pkId)) {
                        // assert(pv.dependers.isEmpty)

                        val fallbackProperty = fallbackPropertyBasedOnPkId(this, e, pkId)
                        val fallbackResult = Result(e, fallbackProperty)
                        handleResult(fallbackResult)

                        continueComputation = true
                    }
                }

                // 2. let's search for cSCCs that only consists of properties which will not be
                //    updated later on
                if (!continueComputation) {
                    val epks: Traversable[SomeEOptionP] =
                        for {
                            (e, pkIdPValue) ← ps
                            (pkLongId, pValue) ← pkIdPValue
                            pkId = pkLongId.toInt
                            if pValue.p != null // analyses must always commit some value; hence,
                            // Both of the following tests are necessary because we may have
                            // properties which are only computed in a later phase; in that
                            // case we may have EPKs related to entities which are not used.
                            if pValue.dependees.nonEmpty // Can this node can be part of a cycle?
                            if pValue.dependers.nonEmpty // Can this node can be part of a cycle?
                        } yield {
                            EPK(e, pValue.p.key): SomeEOptionP
                        }

                    val cSCCs = graphs.closedSCCs(
                        epks,
                        (epk: SomeEOptionP) ⇒ ps(epk.e)(epk.pk.id.toLong).dependees
                    )
                    for {
                        cSCC ← cSCCs
                    } {
                        val headEPK = cSCC.head
                        val e = headEPK.e
                        val p = ps(e)(headEPK.pk.id.toLong).p
                        val headEPS = IntermediateEP(e, p)
                        val newEP = PropertyKey.resolveCycle(this, headEPS)
                        val cycleAsText =
                            if (cSCC.size > 10)
                                cSCC.take(10).mkString("", ",", "...")
                            else
                                cSCC.mkString(",")
                        info("analysis progress", s"resolving cycle: ${cycleAsText} => $newEP")
                        update(newEP.e, newEP.p, isFinal = true, notifyDependers = true, Nil)
                        continueComputation = true
                    }
                }

                if (!continueComputation) {
                    // We used no fallbacks and found no cycles, but we may still have
                    // (collaboratively computed) properties (e.g. CallGraph) which are
                    // not yet final; let's finalize them!
                }
            }
        } while (continueComputation)
    }

}

// NOTE
// For collaboratively computed properties isFinal maybe false, but we do not have dependees!
private[fpcf] class PropertyValue(
        // p is :
        //   - null if some analyses depend on it, but no lazy computation is scheduled
        //   - PropertyIsLazilyComputed if the computation is scheduled (to avoid rescheduling)
        //   - a concrete Property.
        var p:       Property,
        var isFinal: Boolean,
        //
        // Both of the following maps are maintained eagerly in the
        // the sense that, if an update happens, the on update
        // continuation will directly be scheduled and the corresponding
        // maps will be cleared respectively.
        //
        // Those who are interested in this property;
        // the keys are those EPKs with a dependency on this one:
        var dependers: Map[SomeEPK, OnUpdateContinuation],
        // The properties on which this property depends;
        // required to remove the onUpdateContinuation for this
        // property from the dependers maps of the dependees.
        // Note, dependees can even be empty for non-final properties
        // in case of collaboratively computed properties OR if a task
        // which computes the next value is already scheduled!
        var dependees: Traversable[SomeEOptionP]
) {

    def this(p: Property) { this(p, true, Map.empty, Nil) }

    def this(dependers: Map[SomeEPK, OnUpdateContinuation]) { this(null, false, dependers, Nil) }

    def this(epk: SomeEPK, c: OnUpdateContinuation) { this(null, false, Map(epk -> c), Nil) }

    override def toString: String = {
        "PropertyValue("+
            "\n\tproperty="+p+
            "\n\tisFinal="+isFinal+
            "\n\t#dependers="+dependers.size+
            "\n\t#dependees="+dependees.size+
            ")"
    }
}
private[fpcf] object PropertyValue {
    def lazilyComputed: PropertyValue = {
        new PropertyValue(PropertyIsLazilyComputed, isFinal = false, Map.empty, Nil)
    }
}

/**
 * Factory for creating `SequentialPropertyStore`s.
 */
object SequentialPropertyStore extends PropertyStoreFactory {

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PropertyStore = {
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        new SequentialPropertyStore(contextMap)
    }
}
