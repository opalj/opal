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

/**
 * A property store manages the execution of computations of properties related to specific
 * entities (e.g., methods, fields and classes of a program). These computations may require and
 * provide information about other entities of the store and the property store implements the logic
 * to handle the dependencies between the entities. Furthermore, the property store parallelizes
 * the computation of the properties as far as possible without requiring users to take care of it;
 * users are also generally not required to think about the concurrency when implementing an
 * analysis.
 *
 * ==Usage==
 * The general strategy, when using the PropertyStore, is to always continue computing the property
 * of an entity and to collect the dependencies on those elements that are relevant.
 * I.e., if some information is not or just not completely available, the analysis should
 * still continue using the provided information and (internally) records the dependency.
 * Later on, when the analysis has computed its result, it reports the same and informs the
 * framework about its dependencies.
 *
 * ===Core Requirements on Property Computation Functions===
 *  - (One Lazy Function per Property Kind) A specific kind of property is (in each
 *    phase) always computed by only one registered `PropertyComputation` function.
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
 *    that the same analysis running concurrently on two different class files do not derive
 *    information about the same method.
 *  - (Monoton) If a `PropertyComputation` function calculates (refines) a (new) property for
 *    a specific element then the result must be equal or more specific within one execution
 *    phase.
 *
 * ===Cyclic Dependencies===
 * In general, it may happen that some analyses cannot make any progress, because
 * they are mutually dependent. In this case the computation of a property `p` of an entity `e1`
 * depends on the property `p'` of an entity `e2` that requires the property `p` of the entity `e1`.
 * In this case a registered strategy is used to resolve the cyclic dependency. If no strategy is
 * available all current values will be committed, if no "current" value is available the fallback
 * value will be committed.
 *
 * ==Thread Safety==
 * A PropertyStore is thread-safe.
 *
 * ==Multi-Threading==
 * The PropertyStore uses its own fixed size ThreadPool with at most
 * [[org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks]] threads.
 *
 * ==Common Abbreviations==
 * - e =         Entity
 * - p =         Property
 * - ps =        Properties (the properties of an entity)
 * - pk =        Property Key
 * - pc =        Property Computation
 * - lpc =       Lazy Property Computation
 * - c =         Continuation (The rest of a computation if a specific, dependent property was computed.)
 * - EPK =       Entity and a PropertyKey
 * - EP =        Entity and an associated Property
 * - EOptionP =  Entity and either a PropertyKey or (if available) a Property
 *
 * @author Michael Eichberg
 */
abstract class PropertyStore {

    /** Immutable map which stores the context objects given at initialization time. */
    val ctx: Map[Type, AnyRef]

    /** Looks up the context object of the given type. */
    final def context[T: TypeTag]: T = {
        val t = typeOf[T]
        ctx.getOrElse(t, { throw ContextNotAvailableException(t, ctx) }).asInstanceOf[T]
    }

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    def toString(printProperties: Boolean): String

    /**
     * Returns a short string representation of the property store related to the key figures.
     */
    override def toString: String = toString(false)

    // We have to register the cycle resolution strategies with the store as such unless the cycle only consists of elements with the same PK!

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note   Querying the properties of the given entities will trigger lazy and direct property
     *         computations.
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
     * @note Querying the properties of the given entities will trigger lazy and direct property
     *      computations.
     * @note The returned collection can be used to create an [[IntermediateResult]].
     */
    final def apply[E <: Entity, P <: Property](
        es:  Traversable[E],
        pmi: PropertyMetaInformation { type Self <: P }
    ): Traversable[EOptionP[E, P]] = {
        apply(es, pmi.key)
    }

    def apply[P <: Property](e: Entity, pk: PropertyKey[P]): EOptionP[e.type, P]

    /**
     * Returns the property of the respective property kind `pk` currently associated
     * with the given element `e`.
     *
     * This is the most basic method to get some property and it is the preferred way
     * if (a) you know that the property is already available – e.g., because some
     * property computation function was strictly run before the current one – or
     * if (b) the property is computed using a direct or a lazy property computation - or
     * if (c) it may be possible to compute a final answer even if the property
     * of the entity is not yet available.
     *
     * @note In general, the returned value may change over time but only such that it
     *      is strictly more precise.
     * @note Querying a property may trigger the computation of the property if the underlying
     *      function is a lazy property computation function.
     * @param epk An entity/property key pair.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *      `EP(e,Property)` otherwise; in the later case `EP` may encapsulate a property that
     *      is the final result of a computation `ep.isPropertyFinal === true` even though the
     *      property as such is in general refineable. Hence, to determine if the property in
     *      the current analysis context is final it is necessary to call the `EP` object's
     *      `isPropertyFinal` method.
     */
    def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[epk.e.type, P]

    /**
     * Returns an iterator of the different properties associated with the given element.
     *
     * This method is the preferred way to get a snapshot all properties of an entity and should
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
    def properties(e: Entity): Traversable[Property]

    /**
     * Returns all entities which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * @note Lazy property computations are not triggered.
     */
    def entities[P <: Property](pk: PropertyKey[P]): Traversable[EP[Entity, P]]

    /**
     * Returns all entities that currently have a specific property.
     *
     * @note Does not trigger lazy property computations.
     */
    def entities[P <: Property](p: P): Traversable[Entity]

    /**
     * The set of all entities which already have a property that passes the given filter.
     *
     * This method returns a snapshot.
     *
     * @note This method will not trigger lazy property computations.
     */
    def entities(propertyFilter: Property ⇒ Boolean): Traversable[Entity]

    /**
     * Directly associate the given property `p` with the given entity `e` if `e` has no property
     * of the respective kind!
     *
     * This method must not be used '''if there might be a regular scheduled computation that
     * computes the property `p` for `e`'''.
     *
     * A use case is an analysis that does not interact with the property store while
     * executing the analysis, but wants to store some results in the store.
     * (If the property store is "just" used for parallelizing the execution of the analysis
     * it is still possible to use `set`.)
     *
     * If a property is already associated with the given entity, an IllegalStateException is thrown
     * to prevent programming errors.
     */
    def set(e: Entity, p: Property): Unit

    /**
     * Registers a function that lazily computes a property for an element
     * of the store if the property of the respective kind is requested.
     * Hence, a first request of such a property will always first return the result "None".
     *
     * The computation is triggered by a(n in)direct call of this store's `apply` method.
     *
     * This store ensures that the property computation function `pc` is never invoked more
     * than once for the same element at the same time. If `pc` is invoked again for a specific
     * element then only because a dependee has changed!
     */
    def scheduleLazyPropertyComputation[P <: Property](
        pk: PropertyKey[P],
        pc: SomePropertyComputation
    ): Unit

    /**
     * Will call the given function `c` for all elements of `es` in parallel; all elements of `es`
     * have to be entities known to the property store.
     */
    def scheduleForEntities[E <: Entity](es: TraversableOnce[E])(c: PropertyComputation[E]): Unit = {
        es.foreach(e ⇒ scheduleForEntity(e)(c))
    }

    /**
     * Schedules the execution of the given PropertyComputation function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierachy in a top-down manner.
     */
    def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit

    /**
     * Processes the result. Generally, not called by analyses.
     */
    def handleResult(r: PropertyComputationResult): Unit

    /**
     * Awaits the completion of all property computation functions which were previously registered.
     *
     * @note  If a second thread is used to register [[PropertyComputation]] functions
     *        no guarantees are given; it is recommended to schedule all property computation
     *        functions using one thread and using that thread to call this method.
     *
     * @param resolveCycles If `true`, cycles will be resolved.
     */
    def waitOnPropertyComputationCompletion(
        resolveCycles:                         Boolean = true,
        useFallbacksForIncomputableProperties: Boolean = true
    ): Unit

}

class PropertyStoreContext[+T <: AnyRef] private (val t: Type, val data: T) {

    def asTuple: (Type, T) = (t, data)
}

object PropertyStoreContext {

    def apply[T <: AnyRef](t: Type, data: T): PropertyStoreContext[T] = {
        new PropertyStoreContext(t, data)
    }

    def apply[T <: AnyRef: TypeTag](data: T): PropertyStoreContext[T] = {
        new PropertyStoreContext[T](typeOf[T], data)
    }

}
