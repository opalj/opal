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

import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.Type
import scala.collection.mutable.OpenHashMap
import scala.collection.mutable.LongMap
import org.opalj.collection.immutable.Chain
import org.opalj.log.LogContext

/**
 * A non-concurrent implementation of the property store. Entities are generally only stored if
 * required. I.e.,
 *  - they have a property OR
 *  - a computation is already scheduled that will compute the property OR
 *  - we have a `depender`.
 */
class SequentialPropertyStore private (
        val ctx:        Map[Type, AnyRef],
        val logContext: LogContext
) extends PropertyStore { store ⇒

    type PKId = Long

    private class PropertyValue(
            // p is :
            //   - null as long as no computation is scheduled/runnning.
            //   - PropertyIsLazilyComputed if the computation is scheduled
            //   - a concrete Property.
            // We require that we always have an explicit representation
            // for "bottom" w.r.t. the underlying lattice; i.e.,
            // using null as the bottom value is NOT supported.
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
            var dependees: Traversable[SomeEOptionP]
    ) {

        def this(p: Property, isFinal: Boolean) { this(p, isFinal, Map.empty, Traversable.empty) }

        /**
         * @return `true` if the computation of the value of this property has finished and
         *        we have a (phase) final result.
         */
        def isResult: Boolean = dependees.isEmpty

        override def toString: String = {
            "PropertyValue("+
                "\n\tproperty="+p+
                "\n\tisFinal="+isFinal+
                "\n\t#dependers="+dependers.size+
                "\n\t#dependees="+dependees.size+
                ")"
        }
    }
    private object PropertyValue {
        def lazilyComputed: PropertyValue = {
            new PropertyValue(PropertyIsLazilyComputed, isFinal = false)
        }

        def noProperty: PropertyValue = {
            new PropertyValue(null, isFinal = false)
        }
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
    private[this] val ps: OpenHashMap[Entity, LongMap[PropertyValue]] = OpenHashMap.empty

    // Those computations that will only be scheduled if the result is required
    private[this] var lazyComputations: LongMap[SomePropertyComputation] = LongMap.empty

    // The list of scheduled computations
    private[this] var tasks: Chain[() ⇒ Unit] = Chain.empty

    private[this] var computedPropertyKinds: Set[PropertyKind] = null

    private[this] var openPropertyKinds: Set[PropertyKind] = null

    override def isKnown(e: Entity): Boolean = ps.get(e).isDefined

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

    override def scheduleLazyPropertyComputation[P](
        pk: PropertyKey[P],
        pc: SomePropertyComputation
    ): Unit = {
        lazyComputations += ((pk.id.toLong, pc))
    }

    override def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        tasks = (() ⇒ handleResult(pc(e))) :&: tasks
    }

    // triggers lazy property computations!
    override def apply[P <: Property](e: Entity, pk: PropertyKey[P]): EOptionP[e.type, P] = {
        apply(EPK(e, pk))
    }

    // triggers lazy property computations!
    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[epk.e.type, P] = {
        val e = epk.e
        val pkId = epk.pk.id.toLong

        val r = ps.get(e) match {
            case None ⇒
                // the entity is unknown
                lazyComputations.get(pkId) match {
                    case Some(lc) ⇒
                        // create PropertyValue to ensure that we do not schedule
                        // multiple (lazy) computations => the entity is now known
                        ps += ((e, LongMap((pkId, PropertyValue.lazilyComputed))))
                        scheduleForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])
                    case None ⇒
                        ps += ((e, LongMap((pkId, PropertyValue.noProperty))))
                }
                // return the "current" result
                epk

            case Some(pkIdPValue) ⇒ pkIdPValue.get(pkId) match {

                case Some(pValue) ⇒
                    val p = pValue.p
                    if (p != null && p != PropertyIsLazilyComputed)
                        // we have a property
                        EPS(e, pValue.p.asInstanceOf[P], pValue.isFinal)
                    else
                        // we do not (yet) have a value, but a lazy property
                        // computation is already scheduled (if available)
                        epk

                case None ⇒
                    // the entity is known, but the property kind was never
                    // queried before or there is no computation whatsoever..
                    lazyComputations.get(pkId) match {
                        case Some(lc) ⇒
                            // create PropertyState to ensure that we do not schedule
                            // multiple (lazy) computations => the entity is now known
                            pkIdPValue += ((pkId, PropertyValue.lazilyComputed))
                            scheduleForEntity(e)(lc.asInstanceOf[PropertyComputation[E]])

                        case None ⇒
                            pkIdPValue += ((pkId, PropertyValue.noProperty))
                    }
                    epk
            }
        }
        r.asInstanceOf[EOptionP[epk.e.type, P]]
    }

    override def set(e: Entity, p: Property): Unit = {
        val pkId = p.key.id.toLong

        /*user-level*/ assert(
            lazyComputations.get(pkId).isEmpty,
            s"lazy computation scheduled for property kind $pkId"
        )

        val oldP = update(e, p, isFinal = true, Traversable.empty)
        if (oldP != null) {
            throw new IllegalStateException(
                s"associating $e with $p failed; property is already set $oldP"
            )
        }
    }

    private[this] def update(
        e:         Entity,
        p:         Property,
        isFinal:   Boolean,
        dependees: Traversable[SomeEOptionP]
    ): Property = {
        val pkId = p.key.id
        ps.get(e) match {
            case None ⇒
                // the entity is not known
                ps += ((e, LongMap((pkId.toLong, new PropertyValue(p, isFinal, Map.empty, dependees)))))
                null

            case Some(pkIdPValue) ⇒
                // the entity is known
                pkIdPValue.get(pkId.toLong) match {
                    case None ⇒
                        // we have no property of the respective kind
                        pkIdPValue += ((pkId.toLong, new PropertyValue(p, isFinal, Map.empty, dependees)))
                        null

                    case Some(pValue) ⇒
                        val oldP = pValue.p
                        if (debug && p.isOrderedProperty) {
                            p.asOrderedProperty.checkIsValidSuccessorOf(oldP)
                        }
                        pValue.dependees = dependees
                        pValue.p = p
                        if (pValue.isFinal) throw new IllegalStateException(s"$e/$p property is final")
                        pValue.isFinal = isFinal
                        pValue.dependers.foreach {
                            case (epk, onUpdateContinuation) ⇒
                                tasks :&:= (
                                    if (isFinal)
                                        () ⇒ handleResult(
                                        onUpdateContinuation(e, p, isFinal)
                                    )
                                    else
                                        () ⇒ handleResult({
                                            val newestPValue = ps(e)(pkId.toLong)
                                            onUpdateContinuation(
                                                e, newestPValue.p, newestPValue.isFinal
                                            )
                                        })
                                )
                                // clear depender/dependee lists...
                                // given that we have triggered the depender, we now have to remove
                                // the respective onUpdateContinuation from all dependees of the
                                // respective depender to avoid that the onUpdateContinuation is
                                // triggered multiple times
                                val dependerPValue = ps(epk.e)(epk.pk.id.toLong)
                                dependerPValue.dependees.foreach { epkOfDepeendeeOfDepender ⇒
                                    val pValueOfDependeeOfDepender = ps(epkOfDepeendeeOfDepender.e)(epkOfDepeendeeOfDepender.pk.id.toLong)
                                    pValueOfDependeeOfDepender.dependers = pValueOfDependeeOfDepender.dependers - (epkOfDepeendeeOfDepender.toEPK)
                                }
                                dependerPValue.dependees = Traversable.empty
                        }
                        pValue.dependers = Map.empty

                        oldP
                }
        }
    }

    override def handleResult(r: PropertyComputationResult): Unit = {

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a property for a given entity.

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
                    val e = ep.e
                    val p = ep.p
                    update(e, p, isFinal = true, dependees = Traversable.empty)
                }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(e, p) = r
                update(e, p, isFinal = true, Traversable.empty)

            case PhaseResult.id ⇒
                val PhaseResult(e, p) = r
                update(e, p, isFinal = false, Traversable.empty)

            case PartialResult.id ⇒ (r: @unchecked) match {
                case PartialResult(e, pk, u) ⇒
                    val eOptionP = apply(e, pk)
                    val newPOption = if (eOptionP.hasProperty) u(Some(eOptionP.p)) else u(None)

                    newPOption.foreach { newP ⇒
                        update(e, newP, isFinal = false, Traversable.empty)
                    }
            }

            case IntermediateResult.id ⇒
                val IntermediateResult(e, p, dependees /*: Traversable[SomeEOptionP]*/ , c) = r
                // 1. let's check if a dependee is already updated...
                val noUpdates = dependees.forall { dependee ⇒
                    assert(
                        !dependee.isFinal,
                        s"dependency to final property ${dependee.e}/${dependee.p}"
                    )
                    val dependeeE = dependee.e
                    val dependeePKId = dependee.pk.id.toLong
                    val dependeePValue = ps(dependeeE)(dependeePKId)
                    val isDependeeUpdated =
                        dependeePValue.p != null &&
                            dependeePValue.p != PropertyIsLazilyComputed && (
                                // we (now) have some property
                                dependeePValue.isFinal ||
                                dependee.hasNoProperty ||
                                dependee.p != dependeePValue.p
                            )

                    if (isDependeeUpdated) {
                        if (!isInterrupted()) {
                            tasks :&:= (
                                if (dependeePValue.isFinal)
                                    () ⇒ handleResult(c(dependeeE, dependeePValue.p, true))
                                else
                                    () ⇒ handleResult({
                                        val newestPValue = ps(dependeeE)(dependeePKId)
                                        c(dependeeE, newestPValue.p, newestPValue.isFinal)
                                    })
                            )
                        }
                        false
                    } else {
                        true // <= no update
                    }
                }
                // 2. the most current value of every dependee was taken into account
                if (noUpdates) {
                    // 2.1. register with depeendees
                    dependees.foreach { dependee ⇒
                        val dependeeE = dependee.e
                        val dependeePKId = dependee.pk.id.toLong
                        val dependeePValue = ps(dependeeE)(dependeePKId)
                        val dependency = (EPK(e, p), c)
                        dependeePValue.dependers = dependeePValue.dependers + (dependency)
                    }

                    // 2.2. update the value (trigger dependers)
                    update(e, p, isFinal = false, dependees)
                }
        }
    }

    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        openPropertyKinds:     Set[PropertyKind]
    ): Unit = {
        this.computedPropertyKinds = computedPropertyKinds
        this.openPropertyKinds = openPropertyKinds
    }

    override def waitOnPhaseCompletion(): Unit = {
        var continue: Boolean = true
        while (continue) {
            continue = false
            while (tasks.nonEmpty && !isInterrupted()) {
                val task = tasks.head
                tasks = tasks.tail
                task.apply()
            }
            // we have reached quiescence. let's check if we have to
            // fill in fallbacks or if we have to resolve cyclic computations
            println("todo")
        }
    }

}

object SequentialPropertyStore {

    def apply[T <: AnyRef: TypeTag](
        context: T
    )(
        implicit
        logContext: LogContext
    ): PropertyStore = {
        apply(PropertyStoreContext[T](context))
    }

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PropertyStore = {
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        new SequentialPropertyStore(contextMap, logContext)
    }
}
