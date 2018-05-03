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
package org.opalj.fpcf

import java.util.concurrent.atomic.AtomicLong

import com.phaller.rasync._
import com.phaller.rasync.lattice.Key
import com.phaller.rasync.lattice.Updater
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.LogContext

import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.runtime.universe.Type

class ReactiveAsyncPropertyStore private (
        final val ctx:         Map[Type, AnyRef],
        final val parallelism: Int
)(
        implicit
        val logContext: LogContext
) extends PropertyStore {

    // TODO Properties must be immutable!!!!!!!!!!!

    private implicit val handlerPool: HandlerPool = new HandlerPool(parallelism = parallelism)

    implicit object RAUpdater extends Updater[PropertyValue] {
        override val initial: PropertyValue = null

        override def update(current: PropertyValue, next: PropertyValue): PropertyValue = next

        override def ignoreIfFinal: Boolean = true
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
    val ps: Array[PropertyTrieMap] = Array.fill(SupportedPropertyKinds) { TrieMap.empty }

    // This map keeps track of the dependencies. Key is (Entity, PropertyKind.id)
    val dependencyMap = TrieMap.empty[(Entity, Int), Traversable[SomeEOptionP]]

    // PropertyKinds that will be computed now or at a later time
    var computedPropertyKinds: Set[PropertyKind] = Set.empty
    var delayedPropertyKinds: Set[PropertyKind] = Set.empty

    // Counters for debug purposes
    val debugCounter = TrieMap.empty[String, AtomicLong]
    var debugInResolve = false
    var debugInFallback = false
    val dependencyCounter = TrieMap.empty[Int, AtomicLong]

    // This datastructure holds the to be lazily computed properties. The key is the property kind
    // id and the value is the some property computation function. It will be called in the apply
    // method to trigger computations.
    val lazyTasks = TrieMap.empty[Int, SomePropertyComputation]

    // Tasks that were already scheduled. The value is if the task is still running
    val startedLazyTasks = TrieMap.empty[(Int, Entity), Boolean]

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    override def toString(printProperties: Boolean): String = {
        val counters = s"\tCounters\n"+
            f"\t\t${"Number"}%10s  ${"Name"}%-90s\n"+
            f"\t\t${"------"}%10s  ${"----"}%-90s\n"+
            debugCounter.toSeq.sortBy(x ⇒ x._1).map { x ⇒
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

    /**
     * Simple counter of the number of tasks that were executed to perform an initial
     * computation of a property for some entity.
     */
    override def scheduledTasks: Int = debugCounter
        .getOrElseUpdate("EagerlyScheduledComputations", new AtomicLong(0))
        .intValue()

    /**
     * Simple counter of the number of tasks (OnUpdateContinuations) that were executed
     * in response to an updated property.
     */
    override def scheduledOnUpdateComputations: Int = debugCounter
        .getOrElseUpdate("handleResult.IntermediateResult.continuationFunction", new AtomicLong(0))
        .intValue()

    override def eagerOnUpdateComputations: Int = -1

    /**
     * Returns `true` if the given entity is known to the property store. Here, `isKnown` can mean
     *  - that we actually have a property, or
     *  - a computation is scheduled/running to compute some property, or
     *  - an analysis has a dependency on some (not yet finally computed) property, or
     *  - that the store just eagerly created the data structures necessary to associate
     * properties with the entity.
     */
    override def isKnown(e: Entity): Boolean = ps.contains(e)

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean =
        isKnown(e) && ps(pk.id).contains(e) && {
            val pv = ps(pk.id)(e).cell.getResult()
            pv.ub != null && pv.ub != PropertyIsLazilyComputed
        }

    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        // Here we query the internal property store. We don't work on a snapshot, on the entity
        // level, because we want to update it
        val pkId = pk.id
        val res: EOptionP[E, P] = ps(pkId).get(e) match {
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
                lazyTasks.get(pkId).foreach { pc ⇒
                    if (startedLazyTasks.putIfAbsent((pkId, e), true).isEmpty) {
                        incCounter("apply.lazy.schedule")
                        scheduleForEntity(e)(pc.asInstanceOf[PropertyComputation[Entity]])
                    } else {
                        incCounter("apply.lazy.ignore")
                    }
                }
                EPK(e, pk)
        }

        res
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
            .filter(_.contains(e))
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
                    pv.lb == lb && pv.ub == ub
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
                            val eps = cc.cell.getResult.toEPS(e).get
                            propertyFilter(eps)
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
    override def registerLazyPropertyComputation[P <: Property](pk: PropertyKey[P], pc: SomePropertyComputation): Unit = {
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
        this.computedPropertyKinds ++= computedPropertyKinds
        this.delayedPropertyKinds ++= delayedPropertyKinds
    }

    /**
     * Schedules the execution of the given `PropertyComputation` function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierarchy in a top-down manner.
     */
    override def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        assert(computedPropertyKinds.nonEmpty, "setupPhase must be called with at least one computedPropertyKinds")
        incCounter("EagerlyScheduledComputations")

        // Trigger the execution asynchronously
        handlerPool.execute(() ⇒ {
            // Here we call handleResult to store the result in the property store
            val r = pc(e)
            if (r.id == IntermediateResult.id) {
                val IntermediateResult(_, _, _, d, _) = r
                dependencyCounter.getOrElseUpdate(d.size, new AtomicLong(0)).incrementAndGet()
                incCounter("Dependencies", d.size)
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
    override def handleResult(r: PropertyComputationResult): Unit = {
        incCounter("handleResult")
        incCounter(s"handleResult ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")

        r.id match {
            case NoResult.id ⇒
                incCounter("handleResult.NoResult")
                incCounter(s"handleResult.NoResult ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")

            case Result.id ⇒
                val Result(e, p) = r
                incCounter("handleResult.Result")
                incCounter(s"handleResult.Result ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")
                // For a final result just put the final value into the cell
                val psE = ps(p.key.id)
                val cc = psE.getOrElseUpdate(
                    e, CellCompleter[RAKey, PropertyValue](new RAKey(e, p.key))
                )
                assert(!cc.cell.isComplete)
                cc.putFinal(new PropertyValue(p))
                dependencyMap.remove((e, p.key.id))

            case IntermediateResult.id ⇒
                val IntermediateResult(e, lb, ub, dependees, c) = r
                if (dependees.isEmpty) {
                    assert(false, "IntermediateResult without dependees")
                }

                incCounter("handleResult.IntermediateResult")
                incCounter(s"handleResult.IntermediateResult ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")

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
                    cc.putNext(new PropertyValue(lb, ub))
                }

                val oldDependees = dependencyMap.getOrElse((e, lb.key.id), Traversable.empty)
                val newDependees = dependees.filterNot(oldDependees.toSet)
                val removedDependees = oldDependees.filterNot(dependees.toSet)
                dependencyMap.put((e, lb.key.id), dependees)

                // 2. Check which dependencies the cell has. Remove dependencies that are
                // no longer necessary.

                removedDependees foreach { someEOptionP ⇒
                    val psE = ps(someEOptionP.pk.id)
                    // When querying the properties, the cell already exists. At some point it was
                    // added and in step 3 the cell was generated
                    val dependeeCell = psE(someEOptionP.e).cell
                    cc.cell.removeNextCallbacks(dependeeCell)
                }

                incCounter("handleResult.IntermediateResult.removedCallbacks", removedDependees.size)

                // 3. Register new dependencies
                incCounter("handleResult.IntermediateResult.dependees", dependees.size)
                newDependees foreach { someEOptionP ⇒
                    val psE = ps(someEOptionP.pk.id)
                    val dependeeCell = psE.getOrElseUpdate(
                        someEOptionP.e,
                        CellCompleter[RAKey, PropertyValue](
                            new RAKey(someEOptionP.e, someEOptionP.pk)
                        )
                    ).cell

                    // whenNext is also called if dependeeCell is already final
                    // cell1.whenNextSequential(cell2, ...)
                    // cell1.whenNextSequential(cell3, ...)
                    // -> runs sequential

                    // cell1.whenNextSequential(cell3, ...)
                    // cell2.whenNextSequential(cell3, ...)
                    // -> runs parallel

                    // if dependeeCell putNext() is called, whenNext is NOT called currently!
                    // Only if putFinal was called
                    cc.cell.whenSequential(dependeeCell, (p, _) ⇒ {
                        incCounter("handleResult.IntermediateResult.continuationFunction")
                        incCounter(s"handleResult.IntermediateResult.continuationFunction ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")

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

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, nextComputations) = r

                incCounter("handleResult.IncrementalResult")
                incCounter(s"handleResult.IncrementalResult ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")
                // nextComputations haven't been scheduled yet. So here we can eagerly schedule
                // them.

                // Stop timing here, because we have recursion and time would count double
                handleResult(ir)

                nextComputations foreach {
                    case (c, e) ⇒ scheduleForEntity(e)(c)
                }

            case Results.id ⇒
                val Results(results) = r
                incCounter("handleResult.Results")
                incCounter(s"handleResult.Results ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")
                // Stop timing here, because we have recursion and time would count double
                results foreach handleResult

            case MultiResult.id ⇒
                val MultiResult(mr) = r
                incCounter("handleResult.MultiResult")
                incCounter(s"handleResult.MultiResult ${if (debugInResolve) "*RESOLVE*" else if (debugInFallback) "*FALLBACK*" else "*ANALYSIS*"}")
                mr foreach { someFinalEP ⇒
                    val psE = ps(someFinalEP.p.key.id)
                    val cc = psE.getOrElseUpdate(
                        someFinalEP.e,
                        CellCompleter[RAKey, PropertyValue](
                            new RAKey(someFinalEP.e, someFinalEP.p.key)
                        )
                    )
                    assert(!cc.cell.isComplete)
                    cc.putFinal(new PropertyValue(someFinalEP.p))
                    dependencyMap.remove((someFinalEP.e, someFinalEP.p.key.id))
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
        val fut = handlerPool.quiescentResolveCell
        Await.ready(fut, Duration.Inf)

        debugInResolve = false
        debugInFallback = false
    }

    private def incCounter(key: String, n: Int = 1): Long = {
        debugCounter.getOrElseUpdate(key, new AtomicLong(0)).addAndGet(n.toLong)
    }

    class RAKey(val e: Entity, val pk: PropertyKey[Property]) extends Key[PropertyValue] {
        override def resolve[K <: Key[PropertyValue]](
            cells: Iterable[Cell[K, PropertyValue]]
        ): Iterable[(Cell[K, PropertyValue], PropertyValue)] = {
            debugInResolve = true
            debugInFallback = false

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

            Iterable((headCell, new PropertyValue(result.p)))
        }

        override def fallback[K <: Key[PropertyValue]](
            cells: Iterable[Cell[K, PropertyValue]]
        ): Iterable[(Cell[K, PropertyValue], PropertyValue)] = {
            debugInResolve = false
            debugInFallback = true

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
                    if (cell.getResult() != null || cell.isADependee) {
                        (cell, new PropertyValue(PropertyKey.fallbackProperty(
                            ReactiveAsyncPropertyStore.this,
                            cell.key.asInstanceOf[RAKey].e,
                            cell.key.asInstanceOf[RAKey].pk
                        )))
                    } else {
                        incCounter("FallbackCalls.getResult==null")
                        (cell, cell.getResult)
                    }
                }

            incCounter("FallbackCells", res.size)

            res
        }

        override def toString = s"ReactivePropertyStoreKey $e"
    }

    /**
     * PropertyValue that is stored inside the cell.
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
        new ReactiveAsyncPropertyStore(contextMap, Math.max(NumberOfThreadsForCPUBoundTasks, 2))
    }
}