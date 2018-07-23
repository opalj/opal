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
package org.opalj.fpcf.par

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

import com.phaller.rasync._
import com.phaller.rasync.lattice.Key
import com.phaller.rasync.lattice.Updater
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.IncrementalResult
import org.opalj.fpcf.IntermediateResult
import org.opalj.fpcf.MultiResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyComputation
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyIsLazilyComputed
import org.opalj.fpcf.PropertyIsNotComputedByAnyAnalysis
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.fpcf.PropertyStoreFactory
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPK
import org.opalj.fpcf.SomeEPS
import org.opalj.log.LogContext

import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.reflect.runtime.universe.Type

class ReactiveAsyncPropertyStore private (
        final val ctx:         Map[Type, AnyRef],
        final val parallelism: Int
)(
        implicit
        val logContext: LogContext
) extends PropertyStore {
    // Queue that hold all thrown exceptions inside ReactiveAsync. This is needed because the
    // exceptions are not thrown in the main thread, i.e. the thread that called waitOnPhaseCompletion.
    // waitOnPhaseCompletion checks this list and throws the first exception to the client.
    private val thrownExceptionsInHandlerPool = new ConcurrentLinkedQueue[Throwable]()

    private implicit val handlerPool: HandlerPool = new HandlerPool(
        parallelism = parallelism,
        unhandledExceptionHandler = {
            case _: RejectedExecutionException ⇒
            // HandlerPool was shut down due an exception in the analysis. Ignore those
            // exceptions.
            case t ⇒
                thrownExceptionsInHandlerPool.add(t)
                t.printStackTrace()
        }
    )

    implicit object RAUpdater extends Updater[PropertyValue] {
        override val initial: PropertyValue = null

        override def update(current: PropertyValue, next: PropertyValue): PropertyValue = next
    }

    // Our internal property store is an Array of TrieMaps. We use it because of its lock free
    // data structure and performant snapshot feature.
    // PropertyKindId ->
    //           Entity ->
    //                             Cell (holds property value)
    //           Entity ->
    //                             Cell (holds property value)
    // ...
    //
    // For lazy computation: Once a EPK pair holds a cell, someone scheduled the execution. This
    // means we have the computation will be executed and we don't have to schedule it again.
    // For eager computation: We cannot have this assumption, because on the scheduleForEntity
    // method we don't know what properties are computed in the given function. This will only be
    // known once the execution is done.
    private type PropertyTrieMap = TrieMap[Entity, CellCompleter[RAKey, PropertyValue]]
    val ps: Array[PropertyTrieMap] = Array.fill(SupportedPropertyKinds) {
        TrieMap.empty
    }

    // This map keeps track of the dependencies. Key is (Entity, PropertyKind.id)
    val dependencyMap = TrieMap.empty[SomeEPK, Traversable[SomeEPK]]

    // Map of locks for partial result. The update function must run sequentially for an EPK pair,
    // we have to set a lock for each one. EPK(e, pk).synchronized does not work as it creates a new
    // object each time and therefore does not lock correctly.
    val partialResultLockMap = TrieMap.empty[SomeEPK, ReentrantLock]

    // PropertyKinds that will be computed now or at a later time
    private[this] var computedPropertyKinds: Array[Boolean] = _
    /*false*/
    // has to be set before usage
    private[this] var delayedPropertyKinds: Array[Boolean] = _ /*false*/
    // has to be set before usage

    @volatile private[this] var previouslyComputedPropertyKinds: Array[Boolean] = {
        new Array[Boolean](SupportedPropertyKinds)
    }

    // Counters for debug purposes
    val profilingCounter = TrieMap.empty[String, AtomicLong]
    val dependencyCounter = TrieMap.empty[Int, AtomicLong]

    // This datastructure holds the to be lazily computed properties. The key is the property kind
    // id and the value is the some property computation function. It will be called in the apply
    // method to trigger computations.
    val lazyTasks = TrieMap.empty[Int, PropertyComputation[_]]

    // Tasks that were already scheduled.
    val startedLazyTasks = TrieMap.empty[SomeEPK, Boolean]

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    override def toString(printProperties: Boolean): String = {
        val counters = s"\tCounters\n"+
            f"\t\t${"Number"}%10s  ${"Name"}%-90s\n"+
            f"\t\t${"------"}%10s  ${"----"}%-90s\n"+
            profilingCounter.toSeq.sortBy(x ⇒ x._1).map { x ⇒
                f"\t\t${x._2.get}%10s  ${x._1}%-90s\n"
            }.mkString

        val entities = s"\tEntities\n"+
            f"\t\tTotal number of cells: ${ps.map(_.size).sum}"

        "ReactiveAsyncPropertyStore(\n"+
            counters+
            "\n"+
            entities+
            "\n"+
            s"\tThreads: ${handlerPool.parallelism}\n"+
            ")\n\n"
    }

    override def supportsFastTrackPropertyComputations: Boolean = false

    /**
     * Simple counter of the number of tasks that were executed to perform an initial
     * computation of a property for some entity.
     */
    override def scheduledTasksCount: Int = profilingCounter
        .getOrElseUpdate("EagerlyScheduledComputations", new AtomicLong(0))
        .intValue()

    /**
     * Simple counter of the number of tasks ([[org.opalj.fpcf.OnUpdateContinuation]]s) that were executed
     * in response to an updated property.
     */
    override def scheduledOnUpdateComputationsCount: Int = profilingCounter
        .getOrElseUpdate("handleResult.IntermediateResult.continuationFunction", new AtomicLong(0))
        .intValue()

    /**
     * The number of times a property was directly computed again due to an updated
     * dependee.
     */
    override def immediateOnUpdateComputationsCount: Int = 0

    /**
     * The number of resolved closed strongly connected components.
     *
     * Please note, that depending on the implementation strategy and the type of the
     * closed strongly connected component, the resolution of one strongly connected
     * component may require multiple phases. This is in particular true if a cSCC is
     * resolved by committing an arbitrary value as a final value and we have a cSCC
     * which is actually a chain-like cSCC. In the latter case committing a single value
     * as final which just break up the chain, but will otherwise (in case of chains with
     * more than three elements) lead to new cSCCs which the require detection and
     * resolution.
     */
    override def resolvedCSCCsCount: Int = profilingCounter
        .getOrElseUpdate("ResolveCalls", new AtomicLong(0))
        .intValue()

    /** The number of times the property store reached quiescence. */
    override def quiescenceCount: Int = profilingCounter
        .getOrElseUpdate("QuiescenceCount", new AtomicLong(0))
        .intValue()

    /** The number of properties that were computed using a fast-track. */
    override def fastTrackPropertiesCount: Int = 0

    /** Core statistics. */
    override def statistics: collection.Map[String, Int] =
        profilingCounter.map(c ⇒ (c._1, c._2.intValue()))

    /**
     * Returns `true` if the given entity is known to the property store. Here, `isKnown` can mean
     *  - that we actually have a property, or
     *  - a computation is scheduled/running to compute some property, or
     *  - an analysis has a dependency on some (not yet finally computed) property, or
     *  - that the store just eagerly created the data structures necessary to associate
     * properties with the entity.
     */
    override def isKnown(e: Entity): Boolean =
        ps.exists(psE ⇒ psE.contains(e)) || startedLazyTasks.keySet.exists(k ⇒ k.e == e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean =
        isKnown(e) && ps(pk.id).contains(e) && {
            val pv = ps(pk.id)(e).cell.getResult()
            pv != null && pv.ub != null && pv.ub != PropertyIsLazilyComputed
        }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(e, pk, false)
    }

    private[this] def apply[E <: Entity, P <: Property](
        e:     E,
        pk:    PropertyKey[P],
        force: Boolean
    ): EOptionP[E, P] = {
        // Here we query the internal property store. We don't work on a snapshot, on the entity
        // level, because we want to update it
        val pkId = pk.id
        ps(pkId).get(e) match {
            case Some(cc) ⇒
                incCounter("apply.hit")
                // We have a cell, that means that someone triggered the computation already
                if (cc.cell.getResult() != null) { // null because of Lattice.empty!
                    incCounter("apply.hit.withResult")
                    val r = cc.cell.getResult.toEPS(e).get
                    if (cc.cell.isComplete) {
                        r.toUBEP
                    } else {
                        r
                    }
                } else {
                    incCounter("apply.hit.withoutResult")
                    EPK(e, pk)
                }
            case None ⇒
                incCounter("apply.miss")
                // We haven't calculated the property yet. Check if the property has to be computed
                // lazily
                lazyTasks.get(pkId) match {
                    case None ⇒
                        val isComputed = computedPropertyKinds(pkId)
                        if (isComputed || delayedPropertyKinds(pkId)) {
                            EPK(e, pk)
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
                    case Some(pc) ⇒
                        if (startedLazyTasks.putIfAbsent(EPK(e, pk), true).isEmpty) {
                            incCounter("apply.lazy.schedule")
                            scheduleEagerComputationForEntity(e)(pc.asInstanceOf[PropertyComputation[E]])
                        } else {
                            incCounter("apply.lazy.ignore")
                        }
                        EPK(e, pk)
                }
        }
    }

    /**
     * Enforce the evaluation of the specified property kind for the given entity, even
     * if the property is computed lazily and no "eager computation" requires the results
     * anymore.
     * Using `force` is in particular necessary in a case where a specific analysis should
     * be scheduled lazily because the computed information is not necessary for all entities,
     * but strictly required for some elements.
     * E.g., if you want to compute a property for some piece of code, but not for those
     * elements of the used library that are strictly necessary.
     * For example, if we want to compute the purity of the methods of a specific application,
     * we may have to compute the property for some entities of the libraries, but we don't
     * want to compute them for all.
     */
    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        // apply triggers lazy computation
        apply(e, pk, true)
    }

    /**
     * Returns the property of the respective property kind `pk` currently associated
     * with the given element `e`.
     *
     * This is the most basic method to get some property and it is the preferred way
     * if (a) you know that the property is already available – e.g., because some
     * property computation function was strictly run before the current one – or
     * if (b) the property is computed using a lazy property computation - or
     * if (c) it may be possible to compute a final answer even if the property
     * of the entity is not yet available.
     *
     * @note In general, the returned value may change over time but only such that it
     *       is strictly more precise.
     * @note Querying a property may trigger the computation of the property.
     * @param  epk An entity/property key pair.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *         `Final|IntermediateEP(e,Property)` otherwise.
     */
    override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        apply(epk.e, epk.pk)
    }

    /**
     * Returns an iterator of the different properties associated with the given element.
     *
     * This method is the preferred way to get a snapshot of all properties of an entity and should
     * be used if you know that all properties are already computed. Using this method '''will not
     * trigger''' the computation of a property.
     *
     * @note The returned traversable operates on a snapshot.
     * @note Does not trigger lazy property computations.
     * @param e An entity stored in the property store.
     * @return `Iterator[Property]`
     */
    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        ps
            .toIterator
            .filter(psE ⇒ psE.contains(e) && psE(e).cell.getResult() != null)
            .map { psE ⇒
                val cc = psE.readOnlySnapshot.apply(e)
                cc.cell.getResult().toEPS[E, Property](cc.cell.key.e.asInstanceOf[E]).get
            }
    }

    /**
     * Returns all entities which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * @note Does not trigger lazy property computations.
     */
    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps(pk.id)
            .readOnlySnapshot()
            .toIterator
            .filter {
                case (_, cc) ⇒ cc.cell.getResult != null
            }
            .map {
                case (e, cc) ⇒ cc.cell.getResult.toEPS(e).get
            }
    }

    /**
     * Returns all entities that currently have the given property bounds.
     * (In case of final properties the bounds are equal.)
     *
     * @note Does not trigger lazy property computations.
     */
    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        ps(ub.key.id)
            .readOnlySnapshot()
            .toIterator
            .filter {
                case (_, cc) ⇒
                    val pv = cc.cell.getResult
                    pv != null && pv.lb == lb && pv.ub == ub
            }
            .map(_._1)
    }

    /**
     * The set of all entities which already have an entity property state that passes
     * the given filter.
     *
     * This method returns a snapshot.
     *
     * @note Does not trigger lazy property computations.
     */
    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        ps
            .toIterator
            .flatMap { psE ⇒
                psE
                    .readOnlySnapshot()
                    .toIterator
                    .filter {
                        case (e, cc) ⇒
                            val res = cc.cell.getResult
                            res != null && propertyFilter(res.toEPS(e).get)
                    }
                    .map(_._1)
            }
    }

    /**
     * Directly associates the given property `p` with property kind `pk` with the given entity
     * `e` if `e` has no property of the respective kind. The set property is always final.
     *
     * @note This method must not be used '''if there might be another computation that
     *       computes the property kind `pk` for `e` and which returns the respective property
     *       as a result'''.
     *
     *       A use case is an analysis that does use the property store while executing the analysis,
     *       but which wants to store the results in the store. Such an analysis '''must
     *       be executed before any other analysis is scheduled'''.
     *
     *       If a different property is already associated with the given entity, an
     *       IllegalStateException is thrown.
     */
    override def set(e: Entity, p: Property): Unit = {
        val pkId = p.key.id
        val psE = ps(pkId)

        // Put a new final cell in the property store.
        val cc = CellCompleter[RAKey, PropertyValue](
            new RAKey(e, p.key)
        )
        cc.putFinal(new PropertyValue(p))

        // putIfAbsent returns None if no value was previously assigned
        val previousValue = psE.putIfAbsent(e, cc)

        // It's possible we already have a cell if another one has a dependency on it. In this case,
        // the value of the cell is null and previousValue was already assigned.
        if (previousValue.nonEmpty && previousValue.get.cell.getResult == null) {
            previousValue.get.putFinal(new PropertyValue(p))
        } else if (previousValue.nonEmpty) {
            throw new IllegalStateException(s"Property $p already set for entity $e = ${previousValue.get.cell.getResult}")
        }

    }

    /**
     * Registers a function that lazily computes a property for an element
     * of the store if the property of the respective kind is requested.
     * Hence, a first request of such a property will always first return no result.
     *
     * The computation is triggered by a(n in)direct call of this store's `apply` method.
     *
     * This store ensures that the property computation function `pc` is never invoked more
     * than once for the same element at the same time. If `pc` is invoked again for a specific
     * element then only because a dependee has changed!
     *
     * In general, the result can't be an `IncrementalResult` and `scheduleLazyPropertyComputation`
     * cannot be used for properties which should be computed by staged analyses.
     *
     * '''A lazy computation must never return a [[NoResult]]; if the entity cannot be processed an
     * exception has to be thrown or the bottom value has to be returned.'''
     *
     * Setting `scheduleLazyPropertyComputation` is only supported as long as the store is not
     * queried. In general, this requires that lazy property computations are scheduled before
     * any eager analysis that potentially reads the value.
     */
    override def registerLazyPropertyComputation[E <: Entity, P <: Property](pk: PropertyKey[P], pc: PropertyComputation[E]): Unit = {
        assert(computedPropertyKinds.nonEmpty, "setupPhase must be called with at least one computedPropertyKinds")
        assert(!profilingCounter.contains("EagerlyScheduledComputations"), "lazy computations should only be registered while no analysis are scheduled")

        lazyTasks += (pk.id -> pc)
        incCounter("LazilyScheduledComputations")
    }

    /**
     * Needs to be called before an analysis is scheduled to inform the property store which
     * properties will be computed now and which are computed in a later phase. The later
     * information is used to decide when we use a fallback.
     *
     * @param computedPropertyKinds The kinds of properties for which we will schedule computations.
     * @param delayedPropertyKinds  The set of property kinds which will (also) be computed
     *                              in a later phase; no fallback will be used for dependencies to properties of the
     *                              respective kind.
     */
    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind]
    ): Unit = {
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

    /**
     * Schedules the execution of the given `PropertyComputation` function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierarchy in a top-down manner.
     */
    override def scheduleEagerComputationForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        assert(computedPropertyKinds.nonEmpty, "setupPhase must be called with at least one computedPropertyKinds")
        incCounter("EagerlyScheduledComputations")

        // Trigger the execution asynchronously
        handlerPool.execute(() ⇒ {
            // Here we call handleResult to store the result in the property store
            val r = pc(e)
            r.id match {
                case Result.id ⇒
                    dependencyCounter.getOrElseUpdate(0, new AtomicLong(0)).incrementAndGet()
                case IntermediateResult.id ⇒
                    val IntermediateResult(_, _, _, d, _, _) = r
                    dependencyCounter.getOrElseUpdate(d.size, new AtomicLong(0)).incrementAndGet()
                    incCounter("DependenciesLowerBound", d.size)
                case _ ⇒
            }
            handleResult(r)
        })
    }

    /**
     * Processes the result. Generally, not called by analyses. If this function is directly
     * called, the caller has to ensure that we don't have overlapping results and that the
     * given result is a meaningful update of the previous property associated with the respective
     * entity - if any!
     */
    override def handleResult(r: PropertyComputationResult, forceEvaluation: Boolean): Unit = {
        incCounter("handleResult")

        if (isSuspended()) {
            handlerPool.interrupt()
        }

        r.id match {
            case NoResult.id ⇒
                incCounter("handleResult.NoResult")

            case Result.id ⇒
                val Result(e, p) = r
                incCounter("handleResult.Result")
                // For a final result just put the final value into the cell
                val psE = ps(p.key.id)
                val cc = psE.getOrElseUpdate(
                    e, CellCompleter[RAKey, PropertyValue](new RAKey(e, p.key))
                )
                if (debug && cc.cell.isComplete) {
                    throw new IllegalArgumentException(s"Property $p for entity $e is already final")
                }
                assert(!cc.cell.isComplete)

                // For ordered properties: Check if the new value is better than the current one
                val currentResult = cc.cell.getResult()
                if (p.isOrderedProperty && currentResult != null) {
                    try {
                        val pAsOP = p.asOrderedProperty
                        pAsOP.checkIsEqualOrBetterThan(e, currentResult.lb.asInstanceOf[pAsOP.Self])
                        val storedUbAsOP = currentResult.ub.asOrderedProperty
                        storedUbAsOP.checkIsEqualOrBetterThan(e, p.asInstanceOf[storedUbAsOP.Self])
                    } catch {
                        case t: Throwable ⇒
                            throw new IllegalArgumentException(
                                s"entity=$e illegal update to: lb=$p; ub=$p; "+
                                    "; cause="+t.getMessage,
                                t
                            )
                    }
                }
                cc.putFinal(new PropertyValue(p))
                dependencyMap.remove(EPK(e, p.key))
                partialResultLockMap.remove(EPK(e, p.key))

            case IntermediateResult.id ⇒
                val IntermediateResult(e, lb, ub, dependees, c, _) = r

                if (debug) {
                    if (lb.key.id != ub.key.id) {
                        throw new IllegalArgumentException("lower and upper bound have different keys!")
                    }
                    if (dependees.nonEmpty && lb == ub) {
                        throw new IllegalArgumentException(s"final property $lb with dependees: $dependees")
                    }
                    if (dependees.isEmpty) {
                        throw new IllegalArgumentException("IntermediateResult without dependees")
                    }
                    if (lb.isOrderedProperty) {
                        try {
                            val ubAsOP = ub.asOrderedProperty
                            ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self])
                        } catch {
                            case t: Throwable ⇒
                                throw new IllegalArgumentException(
                                    s"entity=$e illegal update to: lb=$lb; ub=$ub; "+
                                        dependees.mkString("newDependees={", ", ", "}")+
                                        "; cause="+t.getMessage,
                                    t
                                )
                        }
                    }
                }

                incCounter("handleResult.IntermediateResult")

                // 1. For intermediate results, first put the intermediate value in the cell
                val psE = ps(ub.key.id)
                val cc = psE.getOrElseUpdate(
                    e,
                    CellCompleter[RAKey, PropertyValue](
                        new RAKey(e, lb.key)
                    )
                )

                // Don't put a new result if it didn't change. Otherwise, this causes a trigger for
                // all dependencies.
                val oldResult = cc.cell.getResult()
                if (oldResult == null || oldResult.lb != lb || oldResult.ub != ub) {
                    // For ordered properties: Check if the new value is better than the current one
                    if (lb.isOrderedProperty && oldResult != null) {
                        try {
                            val lbAsOP = lb.asOrderedProperty
                            lbAsOP.checkIsEqualOrBetterThan(e, oldResult.lb.asInstanceOf[lbAsOP.Self])
                            val storedUbAsOP = oldResult.ub.asOrderedProperty
                            storedUbAsOP.checkIsEqualOrBetterThan(e, ub.asInstanceOf[storedUbAsOP.Self])
                        } catch {
                            case t: Throwable ⇒
                                throw new IllegalArgumentException(
                                    s"entity=$e illegal update to: lb=$lb; ub=$ub; "+
                                        dependees.mkString("newDependees={", ", ", "}")+
                                        "; cause="+t.getMessage,
                                    t
                                )
                        }
                    }
                    cc.putNext(new PropertyValue(lb, ub))
                }

                // We have to synchronize to the cell. ReactiveAsync creates a lock on it whenever
                // a dependee has an update for us. It can happen that here we got our first result
                // from the analysis and register dependencies. One of it returns and calls the
                // continuation function. This function removes a dependency, that is not yet
                // registered in ReactiveAsync as we here are still iterating over `newDependees`.
                // Now it tries to get the cell from `ps`, but it is not yet in there and we crash
                // with a NoSuchElementException. This lock prevents us from processing updates for
                // this cell (`cc`) and executes them once all dependencies were registered
                // successfully.
                if (oldResult == null) {
                    // oldResult == null iff this is the first time handleResult was called for the
                    // entity and the cell didn't exists yet or another entity build a dependency
                    // on `e`.
                    cc.sequential {
                        updateDependencies()
                    }
                } else {
                    // If the result already exists we are in the continuation function. We already
                    // have obtained a lock on cc inside ReactiveAsync and for `e` the continuation
                    // function is not called concurrently.
                    updateDependencies()
                }

                def updateDependencies(): Unit = {
                    val oldDependees = dependencyMap.getOrElse(EPK(e, lb.key), Traversable.empty)
                    val dependeesEPK = dependees.map(_.toEPK)
                    val newDependees = dependeesEPK.filterNot(oldDependees.toSet)
                    val removedDependees = oldDependees.filterNot(dependeesEPK.toSet)

                    // 2. Check which dependencies the cell has. Remove dependencies that are
                    // no longer necessary.
                    removedDependees foreach { someEOptionP ⇒
                        val psE = ps(someEOptionP.pk.id)
                        // When querying the properties, the cell already exists. At some point it was
                        // added and in step 3 the cell was generated
                        val dependeeCell = psE(someEOptionP.e).cell
                        cc.cell.removeDependency(dependeeCell)
                    }
                    dependencyMap.put(EPK(e, lb.key), dependeesEPK)

                    incCounter("handleResult.IntermediateResult.removedCallbacks", removedDependees.size)

                    // 3. Register new dependencies
                    incCounter("handleResult.IntermediateResult.newDependees", newDependees.size)
                    newDependees foreach { someEOptionP ⇒
                        val psE = ps(someEOptionP.pk.id)
                        val dependeeCell = psE.getOrElseUpdate(
                            someEOptionP.e,
                            CellCompleter[RAKey, PropertyValue](
                                new RAKey(someEOptionP.e, someEOptionP.pk)
                            )
                        ).cell

                        // whenNext is also called if dependeeCell is already final
                        // cell1.whenSequential(cell2, ...)
                        // cell1.whenSequential(cell3, ...)
                        // -> runs sequential

                        // cell1.whenSequential(cell3, ...)
                        // cell2.whenSequential(cell3, ...)
                        // -> runs parallel
                        cc.cell.whenSequential(dependeeCell, (p, _) ⇒ {
                            incCounter("handleResult.IntermediateResult.continuationFunction")
                            assert(!cc.cell.isComplete)

                            // Ignore null updates. They can occur if we are in fallback and a cell
                            // was not scheduled
                            if (p != null && !someEOptionP.is(p)) {
                                // EPS.apply creates a FinalEP if lp == ub
                                val newEPs = c(EPS(dependeeCell.key.e, p.lb, p.ub))
                                handleResult(newEPs)
                            }
                            NoOutcome
                        })
                    }
                }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, nextComputations, _) = r

                incCounter("handleResult.IncrementalResult")
                handleResult(ir)

                nextComputations foreach {
                    case (c, e) ⇒ scheduleEagerComputationForEntity(e)(c)
                }

            case Results.id ⇒
                val Results(results) = r
                incCounter("handleResult.Results")
                results.foreach(r ⇒ handleResult(r, forceEvaluation))

            case MultiResult.id ⇒
                val MultiResult(mr) = r
                incCounter("handleResult.MultiResult")
                mr foreach { someFinalEP ⇒
                    val psE = ps(someFinalEP.p.key.id)
                    val cc = psE.getOrElseUpdate(
                        someFinalEP.e,
                        CellCompleter[RAKey, PropertyValue](
                            new RAKey(someFinalEP.e, someFinalEP.p.key)
                        )
                    )
                    if (debug && cc.cell.isComplete) {
                        throw new IllegalArgumentException(s"Property ${someFinalEP.p} for entity ${someFinalEP.e} is already final")
                    }
                    assert(!cc.cell.isComplete)
                    // For ordered properties: Check if the new value is better than the current one
                    val currentResult = cc.cell.getResult()
                    if (someFinalEP.p.isOrderedProperty && currentResult != null) {
                        val pAsOP = someFinalEP.p.asOrderedProperty
                        pAsOP.checkIsEqualOrBetterThan(
                            someFinalEP.e,
                            currentResult.lb.asInstanceOf[pAsOP.Self]
                        )
                        val storedUbAsOP = currentResult.ub.asOrderedProperty
                        storedUbAsOP.checkIsEqualOrBetterThan(
                            someFinalEP.e,
                            someFinalEP.p.asInstanceOf[storedUbAsOP.Self]
                        )
                    }
                    cc.putFinal(new PropertyValue(someFinalEP.p))
                    dependencyMap.remove(EPK(someFinalEP.e, someFinalEP.p.key))
                    partialResultLockMap.remove(EPK(someFinalEP.e, someFinalEP.p.key))
                }
            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                type E = e.type
                type P = Property

                val l = partialResultLockMap.getOrElseUpdate(EPK(e, pk), new ReentrantLock)
                l.lock()

                try {
                    val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
                    val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                    newEPSOption foreach { newEPS ⇒
                        val psE = ps(newEPS.ub.key.id)
                        val cc = psE.getOrElseUpdate(
                            e, CellCompleter[RAKey, PropertyValue](new RAKey(e, newEPS.ub.key))
                        )
                        val currentResult = cc.cell.getResult()
                        if (newEPS.lb.isOrderedProperty && currentResult != null) {
                            val lbAsOP = newEPS.lb.asOrderedProperty
                            lbAsOP.checkIsEqualOrBetterThan(e, currentResult.lb.asInstanceOf[lbAsOP.Self])
                            val storedUbAsOP = currentResult.ub.asOrderedProperty
                            storedUbAsOP.checkIsEqualOrBetterThan(e, newEPS.ub.asInstanceOf[storedUbAsOP.Self])
                        }
                        cc.putNext(new PropertyValue(newEPS.lb, newEPS.ub))
                    }
                } finally {
                    l.unlock()
                }
        }

    }

    /**
     * Awaits the completion of all property computation functions which were previously registered.
     * As soon as all initial computations have finished dependencies on E/P pairs for which
     * no value was computed and will be computed(!) (see `setupPhase` for details) will be
     * identified and the fallback value will be used. After that, cycle resolution will be
     * performed. I.e., first all _closed_ strongly connected components will be identified
     * that do not contain any properties for which we will compute (in a future phase) any
     * more refined values. Then the values will be made final.
     *
     * If the store is interrupted, waitOnPhaseCompletion will return as soon as all running
     * computations are finished. By updating the isInterrupted state and calling
     * waitOnPhaseCompletion again computations can be continued.
     *
     * @note If a second thread is used to register [[PropertyComputation]] functions
     *       no guarantees are given; it is recommended to schedule all property computation
     *       functions using one thread and using that thread to call this method.
     */
    override def waitOnPhaseCompletion(): Unit = {
        thrownExceptionsInHandlerPool.clear()
        if (this.isSuspended())
            return ;

        handlerPool.resume()

        handlerPool.onQuiescent(() ⇒ incCounter("QuiescenceCount"))

        val fut = handlerPool.quiescentResolveCell
        var interrupted: Boolean = false

        while (!interrupted && !fut.isCompleted && thrownExceptionsInHandlerPool.isEmpty) {
            try {
                Await.ready(fut, 1.second)
            } catch {
                case _: TimeoutException ⇒ interrupted = this.isSuspended()
            }
        }

        // If an exception occured in a ReactiveAsync task, throw the first exception to the client
        if (!thrownExceptionsInHandlerPool.isEmpty) {
            handlerPool.shutdown()
            throw thrownExceptionsInHandlerPool.peek()
        }

        incCounter("dependencyMap.size", dependencyMap.size)
    }

    override def shutdown(): Unit = {}

    private def incCounter(key: String, n: Int = 1): Long = {
        profilingCounter.getOrElseUpdate(key, new AtomicLong(0)).addAndGet(n.toLong)
    }

    class RAKey(val e: Entity, val pk: PropertyKey[Property]) extends Key[PropertyValue] {
        override def resolve[K <: Key[PropertyValue]](
            cells: Iterable[Cell[K, PropertyValue]]
        ): Iterable[(Cell[K, PropertyValue], PropertyValue)] = {
            // Self cycles are not resolved but retain their current result
            if (cells.size == 1) {
                return cells.map(c ⇒ (c, new PropertyValue(c.getResult.ub)))
            }

            incCounter("RAKey.resolve.cells.size", cells.size)
            incCounter("ResolveCalls")

            val headCell = cells.head
            val headCellKey = headCell.key.asInstanceOf[RAKey]
            val result = PropertyKey.resolveCycle(
                ReactiveAsyncPropertyStore.this,
                headCell.getResult().toEPS[headCellKey.e.type, Property](headCellKey.e).get
            )

            Iterable((headCell, new PropertyValue(result)))
        }

        override def fallback[K <: Key[PropertyValue]](
            cells: Iterable[Cell[K, PropertyValue]]
        ): Iterable[(Cell[K, PropertyValue], PropertyValue)] = {
            incCounter("RAKey.fallback.cells.size", cells.size)
            incCounter("FallbackCalls")

            val res = cells
                // Do not compute fallback properties for entities, that have never been scheduled.
                // This can happen if an entity creates a dependency to another entity that was not
                // scheduled. This unscheduled entity must not get a fallback value.
                //
                // This behaviour can be examined in the L0PurityAnalysis. Remove this filter and
                // run OPAL-Validate/it:testOnly org.opalj.fpcf.PropertyStoreCompareTest
                // XDrawArc was dependee for its base class X11Renderer, but was only scheduled for
                // X11Renderer$X11TracingRenderer. Therefore, we don't have a result for XDrawArc in
                // class X11Renderer.
                .map { cell ⇒
                    val key = cell.key.asInstanceOf[RAKey]
                    if (!delayedPropertyKinds.contains(key.pk) && (cell.getResult() != null || cell.isADependee)) {
                        val reason = {
                            if (previouslyComputedPropertyKinds(key.pk.id) ||
                                computedPropertyKinds(key.pk.id))
                                PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                            else
                                PropertyIsNotComputedByAnyAnalysis
                        }
                        (cell, new PropertyValue(PropertyKey.fallbackProperty(
                            ReactiveAsyncPropertyStore.this,
                            reason,
                            key.e,
                            key.pk
                        )))
                    } else {
                        incCounter("FallbackCalls.getResult==null")
                        (cell, cell.getResult)
                    }
                }

            incCounter("FallbackCells", res.size)

            res
        }

        override def toString = s"RAKey $e (pkId ${pk.id})"
    }

    /**
     * PropertyValue that is stored inside the cell.
     *
     * @param lb The lower bound of the value in the lattice.
     * @param ub The upper bound of the value in the lattice. ub == lb if the value is final
     */
    class PropertyValue(
            var lb: Property,
            var ub: Property
    ) {
        def this(p: Property) {
            this(p, p)
        }

        def isFinal: Boolean = ub != null && ub != PropertyIsLazilyComputed && ub == lb

        def toEPS[E <: Entity, P <: Property](e: E): Option[EPS[E, P]] = {
            if (ub == null || ub == PropertyIsLazilyComputed)
                None
            else
                Some(EPS(e, lb.asInstanceOf[P], ub.asInstanceOf[P]))
        }
    }

}

object ReactiveAsyncPropertyStore extends PropertyStoreFactory {
    override def apply(
        parallelism: Int,
        context:     PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): ReactiveAsyncPropertyStore = {
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        new ReactiveAsyncPropertyStore(contextMap, parallelism)
    }

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): ReactiveAsyncPropertyStore = {
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        new ReactiveAsyncPropertyStore(contextMap, NumberOfThreadsForCPUBoundTasks)
    }
}