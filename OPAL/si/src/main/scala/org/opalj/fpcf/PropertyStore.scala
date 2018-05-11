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
import scala.reflect.runtime.universe.typeOf

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.info

/**
 * A property store manages the execution of computations of properties related to specific
 * entities (e.g., methods, fields and classes of a program). These computations may require and
 * provide information about other entities of the store and the property store implements the logic
 * to handle the computations related to the dependencies between the entities.
 * Furthermore, the property store may parallelize the computation of the properties as far as
 * possible without requiring users to take care of it;
 * users are also generally not required to think about the concurrency when implementing an
 * analysis.
 *
 * ==Usage==
 * The correct strategy, when using the PropertyStore, is to always continue computing the property
 * of an entity and to collect the dependencies on those elements that are (still) relevant.
 * I.e., if some information is not or just not completely available, the analysis should
 * still continue using the provided information and (internally) records the dependency.
 * Later on, when the analysis has computed its result, it reports the same and informs the
 * framework about its dependencies. Based on the later the framework will call back the analysis
 * when a dependency is updated. In general, an analysis should always try to minimize the number
 * of dependencies to the minimum set to enable the property store to suspend computations that
 * are no longer required.
 *
 * ===Core Requirements on Property Computation Functions (Modular Static Analysis)===
 *  The following requirements ensure correctness and determinism of the result.
 *  - (At Most One Lazy Function per Property Kind) A specific kind of property is (in each
 *    phase) always computed by only one registered lazy `PropertyComputation` function.
 *    No other analysis is (conceptually) allowed to derive a value for an E/PK pairing
 *    for which a lazy function is registered.
 *  - (Thread-Safe PropertyComputation functions) If a single instance of a property computation
 *    function (which is the standard case) is scheduled for computing the properties of multiple
 *    entities, that function has to be thread safe. I.e., the function may
 *    be executed concurrently for different entities.
 *  - (Non-Overlapping Results) [[PropertyComputation]] functions that are invoked on different
 *    entities have to compute result sets that are disjoint.
 *    For example, an analysis that performs a computation on class files and
 *    that derives properties of a specific kind related to a class file's methods must ensure
 *    that two concurrent calls of the same analysis - running concurrently on two different
 *    class files - does not derive information about the same method.
 *  - (Monoton) If a `PropertyComputation` function calculates (refines) a (new) property for
 *    a specific element then the result must be equal or more specific.
 *
 * ===Closed-strongly Connected Component Dependencies===
 * In general, it may happen that some analyses cannot make any progress, because
 * they are mutually dependent. In this case the computation of a property `p` of an entity `e1`
 * depends on the property `p'` of an entity `e2` that requires the property `p` of the entity `e1`.
 * In this case a registered strategy is used to resolve the cyclic dependency. If no strategy is
 * available all current values will be committed, if no "current" value is available the fallback
 * value will be committed.
 *
 * ==Thread Safety==
 * The sequential property stores are not thread-safe; the parallelized implementation(s) is
 * thread-safe.
 *
 * ==Common Abbreviations==
 * - e =         Entity
 * - p =         Property
 * - ps =        Properties (the properties of an entity - one per kind)
 * - pk =        Property Key
 * - pc =        Property Computation
 * - lpc =       Lazy Property Computation
 * - c =         Continuation (The part of the analysis that factors in all properties of dependees)
 * - EPK =       Entity and a PropertyKey
 * - EP =        Entity and an associated Property
 * - EOptionP =  Entity and either a PropertyKey or (if available) a Property
 *
 * @author Michael Eichberg
 */
abstract class PropertyStore {

    //
    //
    // CONTEXT RELATED FUNCTIONALITY
    // (Required by analyses that use the property store to query the context.)
    //
    //

    /** Immutable map which stores the context objects given at initialization time. */
    val ctx: Map[Type, AnyRef]

    /**
     * Looks up the context object of the given type. This is a comparatively expensive operation;
     * the result should be cached.
     */
    final def context[T: TypeTag]: T = {
        val t = typeOf[T]
        ctx.getOrElse(t, { throw ContextNotAvailableException(t, ctx) }).asInstanceOf[T]
    }

    //
    //
    // INTERRUPTION RELATED FUNCTIONALITY
    // (Required by the property store to determine if it should abort executing tasks.)
    //
    //

    /**
     * The callback function that is regularly called by the property store to test if
     * the property store should stop executing new tasks. Given that the given method is
     * called frequently, it should be reasonably efficient. The method has to be thread-safe.
     *
     * The default method tests if the current thread is interrupted.
     *
     * Interruption of the property store will leave the property store in a consistent state
     * and the computation can be continued later on by updating this function (if necessary)
     * and calling `waitOnPhaseCompletion` again. I.e., interruption can be used for debugging
     * purposes!
     */
    @volatile var isInterrupted: () ⇒ Boolean = concurrent.defaultIsInterrupted

    //
    //
    // DEBUGGING AND COMPREHENSION RELATED FUNCTIONALITY
    //
    //

    /**
     * If "debug" is `true` and we have an update related to an ordered property,
     * we will then check if the update is correct!
     */
    final def debug: Boolean = PropertyStore.Debug

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    def toString(printProperties: Boolean): String

    /**
     * Returns a short string representation of the property store showing core figures.
     */
    override def toString: String = toString(false)

    /**
     * Simple counter of the number of tasks that were executed to perform an initial
     * computation of a property for some entity.
     */
    def scheduledTasks: Int

    /**
     * Simple counter of the number of tasks (OnUpdateContinuations) that were executed
     * in response to an updated property.
     */
    def scheduledOnUpdateComputations: Int

    /**
     * The number of times a property was directly computed again due to an updated
     * dependee.
     */
    def eagerOnUpdateComputations: Int

    //
    //
    // CORE FUNCTIONALITY
    //
    //

    /**
     * Returns `true` if the given entity is known to the property store. Here, `isKnown` can mean
     *  - that we actually have a property, or
     *  - a computation is scheduled/running to compute some property, or
     *  - an analysis has a dependency on some (not yet finally computed) property, or
     *  - that the store just eagerly created the data structures necessary to associate
     *    properties with the entity.
     */
    def isKnown(e: Entity): Boolean

    /**
     * Tests if we have a property for the entity with the respective kind. If `hasProperty`
     * returns `true` a subsequent `apply` will return an `EPS` (no `EPK`).
     */
    final def hasProperty(epk: SomeEPK): Boolean = hasProperty(epk.e, epk.pk)

    def hasProperty(e: Entity, pk: PropertyKind): Boolean

    /**
     * Returns the current property of the respected kind associated with the given entity.
     *
     * This method is generally only useful when the property store has reached
     * quiescence and – as a client – I do not want to distinguish between the case if
     * a specific value was computed or not.
     */
    def currentPropertyOrFallback[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    ): EPS[E, P] = {
        this(e, pk) match {
            case eps: EPS[E, P] ⇒ eps
            case _              ⇒ FinalEP(e, PropertyKey.fallbackProperty(this, e, pk))
        }
    }

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note   Querying the properties of the given entities will trigger lazy computations.
     * @note   The returned collection can be used to create an [[IntermediateResult]].
     */
    final def apply[E <: Entity, P <: Property](
        es: Traversable[E],
        pk: PropertyKey[P]
    ): Traversable[EOptionP[E, P]] = {
        es.map(e ⇒ apply(EPK(e, pk)))
    }

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note  Querying the properties of the given entities will trigger lazy computations.
     * @note  The returned collection can be used to create an [[IntermediateResult]].
     */
    final def apply[E <: Entity, P <: Property](
        es:  Traversable[E],
        pmi: PropertyMetaInformation { type Self <: P }
    ): Traversable[EOptionP[E, P]] = {
        apply(es, pmi.key)
    }

    def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P]

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
     * @note   In general, the returned value may change over time but only such that it
     *         is strictly more precise.
     * @note   Querying a property may trigger the (lazy) computation of the property.
     * @param  epk An entity/property key pair.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *         `Final|IntermediateEP(e,Property)` otherwise.
     */
    def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P]

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
    def force(e: Entity, pk: SomePropertyKey): Unit

    /**
     * Returns an iterator of the different properties associated with the given element.
     *
     * This method is the preferred way to get a snapshot of all properties of an entity and should
     * be used if you know that all properties are already computed. Using this method '''will not
     * trigger''' the computation of a property.
     *
     * @note The returned traversable operates on a snapshot.
     *
     * @note Does not trigger lazy property computations.
     *
     * @param e An entity stored in the property store.
     * @return `Iterator[Property]`
     */
    def properties[E <: Entity](e: E): Iterator[EPS[E, Property]]

    /**
     * Returns all entities which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * @note Does not trigger lazy property computations.
     */
    def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]]

    /**
     * Returns all entities that currently have the given property bounds.
     * (In case of final properties the bounds are equal.)
     *
     * @note Does not trigger lazy property computations.
     */
    def entities[P <: Property](lb: P, ub: P): Iterator[Entity]

    /**
     * The set of all entities which already have an entity property state that passes
     * the given filter.
     *
     * This method returns a snapshot.
     *
     * @note Does not trigger lazy property computations.
     */
    def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity]

    /**
     * Returns all final entities with the given property.
     *
     * @note Does not trigger lazy property computations.
     */
    def finalEntities[P <: Property](p: P): Iterator[Entity] = entities(p, p)

    /**
     * Directly associates the given property `p` with property kind `pk` with the given entity
     * `e` if `e` has no property of the respective kind. The set property is always final.
     *
     * @note    This method must not be used '''if there might be another computation that
     *          computes the property kind `pk` for `e` and which returns the respective property
     *          as a result'''.
     *
     * A use case is an analysis that does use the property store while executing the analysis,
     * but which wants to store the results in the store. Such an analysis '''must
     * be executed before any other analysis is scheduled'''.
     *
     * If a different property is already associated with the given entity, an
     * IllegalStateException is thrown.
     */
    @throws[IllegalStateException]
    def set(e: Entity, p: Property): Unit

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
    def registerLazyPropertyComputation[P <: Property](
        pk: PropertyKey[P],
        pc: SomePropertyComputation
    ): Unit

    /**
     * Needs to be called before an analysis is scheduled to inform the property store which
     * properties will be computed now and which are computed in a later phase. The later
     * information is used to decide when we use a fallback.
     *
     * @param computedPropertyKinds The kinds of properties for which we will schedule computations.
     *
     * @param delayedPropertyKinds The set of property kinds which will (also) be computed
     *        in a later phase; no fallback will be used for dependencies to properties of the
     *        respective kind.
     */
    def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind] = Set.empty
    ): Unit

    /**
     * Will call the given function `c` for all elements of `es` in parallel.
     *
     * @see [[scheduleForEntity]] for details.
     */
    def scheduleForEntities[E <: Entity](
        es: TraversableOnce[E]
    )(
        c: PropertyComputation[E]
    ): Unit = {
        es.foreach(e ⇒ scheduleForEntity(e)(c))
    }

    /**
     * Schedules the execution of the given `PropertyComputation` function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierarchy in a top-down manner.
     */
    def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit

    /**
     * Processes the result. Generally, not called by analyses. If this function is directly
     * called, the caller has to ensure that we don't have overlapping results and that the
     * given result is a meaningful update of the previous property associated with the respective
     * entity - if any!
     */
    def handleResult(r: PropertyComputationResult): Unit

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
     * @note   If a second thread is used to register [[PropertyComputation]] functions
     *         no guarantees are given; it is recommended to schedule all property computation
     *         functions using one thread and using that thread to call this method.
     */
    def waitOnPhaseCompletion(): Unit

}

object PropertyStore {

    final val DebugKey = "org.opalj.debug.fpcf.PropertyStore.Debug"

    private[this] var debug: Boolean = {
        val initialDebug = BaseConfig.getBoolean(DebugKey)
        updateDebug(initialDebug)
        initialDebug
    }

    // We think of it as a runtime constant (which can be changed for testing purposes).
    def Debug: Boolean = debug

    def updateDebug(newDebug: Boolean): Unit = {
        implicit val logContext = GlobalLogContext
        debug =
            if (newDebug) {
                info("OPAL", s"$DebugKey: debugging support on")
                true
            } else {
                info("OPAL", s"$DebugKey: debugging support off")
                false
            }
    }

}
