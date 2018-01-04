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

import scala.collection.mutable.OpenHashMap
import scala.collection.mutable.LongMap

import org.opalj.collection.immutable.Chain

/**
 * A non-concurrent implementation of the property store. Entities are generally only stored if
 * required. I.e.,
 *  - they have a property OR
 *  - a computation is already scheduled that will compute the property OR
 *  - we have a `depender``
 */
class SequentialPropertyStore(
        val ctx: Map[Type, AnyRef]
) extends PropertyStore { store ⇒

    type PKId = Long

    // map from
    // entity =>
    //        (long) map from
    //           property kind id =>
    //                PropertyState
    private[this] val ps: OpenHashMap[Entity, LongMap[PropertyState]] = OpenHashMap.empty
    private[this] var lazyComputations: LongMap[SomePropertyComputation] = LongMap.empty
    private[this] var tasks: Chain[() ⇒ Unit] = Chain.empty

    private class ScheduledOnUpdateContinutation(
            var onUpdateContinuation: OnUpdateContinuation,
            var isScheduled:          Boolean              = false
    )

    private class PropertyState(
            // Only null as long as the initial - already scheduled computation – has
            // not returned. Never null afterwards - we require that we always have
            // an explicit representation for "bottom" w.r.t. the underlying lattice; i.e.,
            // using null as the bottom value is NOT supported.
            var p: Property,
            // Both lists will be cleared when we schedule a related computation.
            // (we will get the new list of dependees by the computation anyway)
            var dependers: Chain[ScheduledOnUpdateContinutation], // those who are interested in this property
            var dependees: Option[(ScheduledOnUpdateContinutation, Traversable[SomeEPK])] // the other properties on which this property depends
    ) {
        assert(dependees.isEmpty || dependees.get._2.nonEmpty)

        def this(p: Property) { this(p, Chain.empty, None) }
        def this() { this(null, Chain.empty, None) }

        /**
         * @return `true` if the computation of the value of this property has finished and
         *        we have result – at least w.r.t. the current phase.
         */
        def isResult: Boolean = dependees.isEmpty

        override def toString: String = {
            "PropertyState:"+
                "\n\tproperty:"+p+
                "\n\tdependeers:"+dependers.size +
                dependees.mkString("\n\tdependees:\n\t\t", "\n\t\t", "\n")
        }
    }

    def properties(e: Entity): Traversable[Property] = { // IMPROVE use traversable once?
        assert(e ne null)
        for {
            epkpss ← ps.get(e).toSeq // the entities are lazily initialized!
            (pkid, pState) ← epkpss
            p = pState.p
            if p != null
        } yield {
            p
        }
    }

    def entities(propertyFilter: Property ⇒ Boolean): Traversable[Entity] = { // IMPROVE use traversable once?
        for {
            pkIdPState ← ps.values
            pState ← pkIdPState.values
            p = pState.p
            if p != null && propertyFilter(p)
        } yield {
            p
        }
    }

    /**
     * Returns all entities which have the given property based on an "==" (equals) comparison
     * with the given property.
     */
    def entities[P <: Property](p: P): Traversable[Entity] = { // IMPROVE use traversable once?
        assert(p ne null)
        entities((otherP: Property) ⇒ p == otherP)
    }

    def entities[P <: Property](pk: PropertyKey[P]): Traversable[EP[Entity, P]] = { // IMPROVE use traversable once?
        for {
            (e, pkIdPState) ← ps
            pState ← pkIdPState.values
            p = pState.p
            if p != null && p.id == pk.id
        } yield {
            EP(e, p.asInstanceOf[P])
        }
    }

    def toString(printProperties: Boolean): String = {
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

    def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        tasks = (() ⇒ handleResult(pc(e))) :&: tasks
    }

    def scheduleLazyPropertyComputation[P](
        pk: PropertyKey[P],
        pc: SomePropertyComputation
    ): Unit = {
        lazyComputations += ((pk.id.toLong, pc))
    }

    def apply[P <: Property](
        e:  Entity,
        pk: PropertyKey[P]
    ): EOptionP[e.type, P] = {
        apply(EPK(e, pk).asInstanceOf[EPK[e.type, P]])
    }

    def apply[E <: Entity, P <: Property](
        epk: EPK[E, P]
    ): EOptionP[epk.e.type, P] = {
        val e = epk.e
        val pkId = epk.pk.id.toLong

        val r = ps.get(e) match {
            case None ⇒
                // the entity is unknown
                lazyComputations.get(pkId).foreach { pc ⇒
                    // create PropertyState to ensure that we do not schedule
                    // multiple (lazy) computations => the entity is now known
                    ps += ((e, LongMap((pkId, new PropertyState()))))
                    scheduleForEntity(e)(pc.asInstanceOf[PropertyComputation[E]])
                }
                // return the "current" result
                epk

            case Some(pkIdPState) ⇒ pkIdPState.get(pkId) match {

                case Some(pState) ⇒
                    if (pState.p ne null)
                        // we have a property
                        EP(e, pState.p)
                    else
                        // we do not (yet) have a value, but a lazy property
                        // computation is already scheduled (if available)
                        epk

                case None ⇒
                    // the entity is known, but the property kind was never
                    // queried before or there is no computation whatsoever..
                    lazyComputations.get(pkId).foreach { pc ⇒
                        // create PropertyState to ensure that we do not schedule
                        // multiple (lazy) computations => the entity is now known
                        pkIdPState += ((pkId, new PropertyState()))
                        scheduleForEntity(e)(pc.asInstanceOf[PropertyComputation[E]])
                    }
                    epk
            }

        }
        r.asInstanceOf[EOptionP[epk.e.type, P]]
    }

    def set(e: Entity, p: Property): Unit = {
        /*
        val pkId = p.key.id.toLong
        ps.get(e) match {

            case None ⇒
                // completely new entity...
                ps += ((e, LongMap((pkId, new PropertyState(p)))))

            case Some(pkIdPState) ⇒
                // the entity is known ...
                pkIdPState.get(pkId) match {

                    case None ⇒
                        // the property is not referred to by anyone
                        pkIdPState += ((pkId, new PropertyState(p)))

                    case Some(pState) ⇒
                        // we have a dependency...
                        if (pState.p != null) {
                            throw new IllegalStateException(
                                s"$e already has property ${pState.p}"+
                                    s"(update to $p failed)"
                            )
                        }
                        pState.update(e, p,)
                }
        }*/
    }

    def handleResult(r: PropertyComputationResult): Unit = {
        def update(e: Entity, p: Property, ut: UpdateType): Unit = {
            ???
        }

        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not able to
            // compute a property for a given entity and basically
            // falls back to the fallback property. */

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                handleResult(ir)
                npcs foreach { npc ⇒ val (pc, e) = npc; scheduleForEntity(e)(pc) }

            case Results.id ⇒
                val Results(results) = r
                results.foreach(handleResult(_))

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒
                    val e = ep.e
                    val p = ep.p
                    update(e, p, FinalUpdate)
                }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(e, p) = r
                update(e, p, FinalUpdate)

            case RefinableResult.id ⇒
                val RefinableResult(e, p) = r
                update(e, p, IntermediateUpdate)

            case IntermediateResult.id ⇒
                val IntermediateResult(
                    dependerEP,
                    dependees /*: Traversable[SomeEOptionP]*/ ,
                    c) = r
                // 1) update store
                // 2) schedule(!) dependers
                // 3) register c with dependees
                ???
        }

    }

    def waitOnPropertyComputationCompletion(
        resolveCycles:                         Boolean,
        useFallbacksForIncomputableProperties: Boolean
    ): Unit = {
        var continue: Boolean = true
        while (continue) {
            continue = false
            while (tasks.nonEmpty) {
                val task = tasks.head
                tasks = tasks.tail
                task.apply()
            }
            // we have reached quiescence. let's check if we have to
            // fill in fall backs or if we have cyclic computations
            if (resolveCycles) {
                println("todo")
            }
            if (useFallbacksForIncomputableProperties) {
                println("todo")
            }
        }
    }

}
