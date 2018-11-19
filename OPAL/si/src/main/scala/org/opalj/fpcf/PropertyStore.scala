/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.util.concurrent.ConcurrentHashMap

import org.opalj.collection.immutable.IntTrieSet
import scala.util.control.ControlThrowable
import scala.collection.{Map ⇒ SomeMap}

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds

/**
 * A property store manages the execution of computations of properties related to concrete
 * entities as well as artificial entities (e.g., methods, fields and classes of a program, but
 * also the call graph as such etc.). These computations may require and
 * provide information about other entities of the store and the property store implements the logic
 * to handle the computations related to the dependencies between the entities.
 * Furthermore, the property store may parallelize the computation of the properties as far as
 * possible without requiring users to take care of it;
 * users are also generally not required to think about the concurrency when implementing an
 * analysis as long as only immutable data-structures are used.
 * The concepts are also described in the SOAP paper:
 * "Lattice Based Modularization of Static Analyses"
 * (https://conf.researchr.org/event/issta-2018/soap-2018-papers-lattice-based-modularization-of-static-analyses)
 *
 * ==Usage==
 * The correct strategy, when using the PropertyStore, is to always continue computing the property
 * of an entity and to collect the dependencies on those elements that are (still) relevant.
 * I.e., if some information is not or just not completely available, the analysis should
 * still continue using the provided information and (internally) record the dependency.
 * Later on, when the analysis has computed its result, it reports the same and informs the
 * framework about its dependencies. Based on the later the framework will call back the analysis
 * when a dependency is updated. In general, an analysis should always try to minimize the number
 * of dependencies to the minimum set to enable the property store to suspend computations that
 * are no longer required.
 *
 * ===Core Requirements on Property Computation Functions (Modular Static Analyses)===
 *  The following requirements ensure correctness and determinism of the result.
 *  - '''At Most One Lazy Function per Property Kind''' A specific kind of property is (in each
 *    phase) always computed by only one registered lazy `PropertyComputation` function.
 *    No other analysis is (conceptually) allowed to derive a value for an E/PK pairing
 *    for which a lazy function is registered. It is also not allowed to schedule a computation
 *    eagerly if a lazy computation is also registered.
 *  - '''Thread-Safe PropertyComputation functions''' If a single instance of a property computation
 *    function (which is the standard case) is scheduled for computing the properties of multiple
 *    entities, that function has to be thread safe. I.e., the function may
 *    be executed concurrently for different entities. The [[OnUpdateContinuation]] functions
 *    are, however, executed sequentially w.r.t. one E/PK pair.
 *  - '''Non-Overlapping Results''' [[PropertyComputation]] functions that are invoked on different
 *    entities have to compute result sets that are disjoint unless a [[PartialResult]] is used.
 *    For example, an analysis that performs a computation on class files and
 *    that derives properties of a specific kind related to a class file's methods must ensure
 *    that two concurrent calls of the same analysis - running concurrently on two different
 *    class files - does not derive information about the same method. If results for a specific
 *    entity are collaboratively computed, then a [[PartialResult]] has to be used.
 *
 *  - '''Monoton''' If a PropertyComputation` function calculates (refines) a (new) property for
 *    a specific element, then the result must be equal or more specific.
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
 * The sequential property stores are not thread-safe; the parallelized implementation(s) are
 * thread-safe in the following manner:
 *  - a client has to use the SAME thread (the driver thread) to call the (1) [[setupPhase]],
 *    (2) [[registerLazyPropertyComputation]] or [[registerTriggeredComputation]], (3)
 *    [[scheduleEagerComputationForEntity]] / [[scheduleEagerComputationsForEntities]],
 *    (4) [[force]] and (finally) [[PropertyStore#waitOnPhaseCompletion]] methods.
 *    Hence, the previously mentioned methods MUST NOT be called by
 *    PropertyComputation/OnUpdateComputation functions. The methods to query the store (`apply`)
 *    are thread-safe and can be called at any time.
 *
 * ==Common Abbreviations==
 *  - e =         Entity
 *  - p =         Property
 *  - pk =        Property Key
 *  - pc =        Property Computation
 *  - lpc =       Lazy Property Computation
 *  - c =         Continuation (The part of the analysis that factors in all properties of dependees)
 *  - EPK =       Entity and a PropertyKey
 *  - EPS =       Entity and an intermediate Property
 *  - EP =        Entity and some (final or intermediate) Property
 *  - EOptionP =  Entity and either a PropertyKey or (if available) a Property
 *
 * ==Exceptions==
 * In general, exceptions are only thrown if debugging is turned on due to the costs of checking
 * for the respective violations. That is, if debugging is turned off, many potential errors leading
 * to "incomprehensible" results will not be reported. Hence, after debugging an analysis turn
 * debugging (and assertions!) off to get the best performance.
 *
 * We will throw `IllegalArgumentException`'s iff a parameter is in itself invalid. E.g., the lower
 * and upper bound do not have the same [[PropertyKind]]. In all other cases
 * `IllegalStateException`s are thrown. All exceptions are either thrown immediately or eventually,
 * when [[PropertyStore#waitOnPhaseCompletion]] is called. In the latter case, the exceptions are
 * accumulated in the first thrown exception using suppressed exceptions.
 *
 * @author Michael Eichberg
 */
abstract class PropertyStore {

    implicit val logContext: LogContext

    //
    //
    // FUNCTIONALITY TO ASSOCIATE SOME INFORMATION WITH THE STORE THAT
    // (TYPICALLY) HAS THE SAME AS THE PROPERTYSTORE
    //
    //

    private[this] val externalInformation = new ConcurrentHashMap[AnyRef, AnyRef]()

    /**
     * Attaches or returns some information associated with the property store using a key object.
     *
     * This facility is in particular well suited to attach information with the property store
     * which has the same life-time. For example, this mechanism is used to associate the
     * property store specific cycle resolution strategies with the store.
     *
     * This method is thread-safe. However, the client which adds information to the store
     * has to ensure that the overall process of adding/querying/removing is well defined and
     * the ordered is ensured.
     */
    final def getOrCreateInformation[T <: AnyRef](key: AnyRef, f: ⇒ T): T = {
        externalInformation.computeIfAbsent(key, _ ⇒ f).asInstanceOf[T]
    }

    /**
     * Returns the information stored in the store, if any.
     *
     * This method is thread-safe. However, the client which adds information to the store
     * has to ensure that the overall process of adding/querying/removing is well defined and
     * the ordered is ensured.
     */
    final def getInformation[T <: AnyRef](key: AnyRef): Option[T] = {
        Option(externalInformation.get(key).asInstanceOf[T])
    }

    /**
     * Returns the information stored in the store and removes the key, if any.
     *
     * This method is thread-safe. However, the client which adds information to the store
     * has to ensure that the overall process of adding/querying/removing is well defined and
     * the ordered is ensured.
     */
    final def getAndClearInformation[T <: AnyRef](key: AnyRef): Option[T] = {
        Option(externalInformation.remove(key).asInstanceOf[T])
    }

    //
    //
    // CONTEXT RELATED FUNCTIONALITY
    // (Required by analyses that use the property store to query the context.)
    //
    //

    /** Immutable map which stores the context objects given at initialization time. */
    val ctx: Map[Class[_], AnyRef]

    /**
     * Looks up the context object of the given type. This is a comparatively expensive operation;
     * the result should be cached.
     */
    final def context[T](key: Class[T]): T = {
        ctx.getOrElse(key, { throw ContextNotAvailableException(key, ctx) }).asInstanceOf[T]
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
     * Suspending the property store will leave the property store in a consistent state
     * and the computation can be continued later on by updating this function (if necessary)
     * and calling `waitOnPhaseCompletion` again. I.e., interruption can be used for debugging
     * purposes!
     */
    @volatile var isSuspended: () ⇒ Boolean = () ⇒ false

    def shutdown(): Unit

    //
    //
    // DEBUGGING AND COMPREHENSION RELATED FUNCTIONALITY
    //
    //

    /**
     * If "debug" is `true` and we have an update related to an ordered property,
     * we will then check if the update is correct!
     */
    final val debug: Boolean = PropertyStore.Debug

    final def traceFallbacks: Boolean = PropertyStore.TraceFallbacks

    final def traceCycleResolutions: Boolean = PropertyStore.TraceCycleResolutions

    def supportsFastTrackPropertyComputations: Boolean
    @volatile var useFastTrackPropertyComputations: Boolean = true

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     *
     * @param printProperties If `true` prints the properties of all entities.
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
    def scheduledTasksCount: Int

    /**
     * Simple counter of the number of tasks ([[OnUpdateContinuation]]s) that were executed
     * in response to an updated property.
     */
    def scheduledOnUpdateComputationsCount: Int

    /**
     * The number of times a property was directly computed again due to an updated
     * dependee.
     */
    def immediateOnUpdateComputationsCount: Int

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
    def resolvedCSCCsCount: Int

    /** The number of times the property store reached quiescence. */
    def quiescenceCount: Int

    /** The number of properties that were computed using a fast-track. */
    def fastTrackPropertiesCount: Int

    /**
     * Reports core statistics; this method is only guaranteed to report ''final'' results
     * if it is called while the store is quiescent.
     */
    def statistics: SomeMap[String, Int]

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
     * returns `true` a subsequent `apply` will return an `EPS` (not an `EPK`).
     */
    final def hasProperty(epk: SomeEPK): Boolean = hasProperty(epk.e, epk.pk)

    /** See `hasProperty(SomeEPK)` for details. **/
    def hasProperty(e: Entity, pk: PropertyKind): Boolean

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
     * Returns all entities that currently have the given property bounds based on an "==" (equals)
     * comparison..
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
     * The store does not guarantee that the value is set before before a later scheduled
     * analysis is executed. I.e., no guarantee is given that the value is set
     * immediately.
     *
     * A use case is an analysis that does use the property store while executing the analysis,
     * but which wants to store the results in the store. Such an analysis '''must
     * be executed before any other analysis is scheduled'''.
     *
     * @throws IllegalStateException If a different property is already associated with the
     *         given entity or a lazy computation is registered. I.e., `set` has to be called
     *         before respective lazy property computations are registered. The exception may
     *         be thrown eventually!
     *
     * @note   This method must not be used '''if there might be another computation that
     *         computes the property kind `pk` for `e` and which returns the respective property
     *         as a result'''.
     */
    def set(e: Entity, p: Property): Unit

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note   Querying the properties of the given entities will trigger lazy computations.
     * @note   The returned collection can be used to create an [[IntermediateResult]].
     *         @see `apply(epk:EPK)` for details.
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
     * @see  `apply(epk:EPK)` for details.
     */
    final def apply[E <: Entity, P <: Property](
        es:  Traversable[E],
        pmi: PropertyMetaInformation { type Self <: P }
    ): Traversable[EOptionP[E, P]] = {
        apply(es, pmi.key)
    }

    /** @see `apply(epk:EPK)` for details. */
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
     * @note   [[setupPhase]] has to be called before calling apply!
     *
     * @throws IllegalStateException If setup phase was not called or
     *         a previous computation result contained an epk which was not queried.
     *         (Both state are ALWAYS illegal, but are only explicitly checked for if debug
     *         is turned on!)
     * @param  epk An entity/property key pair.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *         `Final|IntermediateEP(e,Property)` otherwise.
     */
    def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P]

    /**
     * Enforce the evaluation of the specified property kind for the given entity, even
     * if the property is computed lazily and no "eager computation" requires the results
     * anymore. Force also ensures that the property is stored in the store even if
     * the fallback value is used.
     * Using `force` is in particular necessary in cases where a specific analysis should
     * be scheduled lazily because the computed information is not necessary for all entities,
     * but strictly required for some elements.
     * E.g., if you want to compute a property for some piece of code, but not for those
     * elements of the used library that are strictly necessary.
     * For example, if we want to compute the purity of the methods of a specific application,
     * we may have to compute the property for some entities of the libraries, but we don't
     * want to compute them for all.
     *
     * @note   Triggers lazy evaluations.
     */
    def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit

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
     * cannot be used for properties which should be computed by phased analyses.
     *
     * ''A lazy computation must never return a [[NoResult]]; if the entity cannot be processed an
     * exception has to be thrown or the bottom value has to be returned.''
     *
     * '''Calling `registerLazyPropertyComputation` is only supported as long as the store is not
     * queried and no computations are already running.
     * In general, this requires that lazy property computations are scheduled before any eager
     * analysis that potentially reads the value.'''
     */
    def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk:       PropertyKey[P],
        pc:       PropertyComputation[E],
        finalEPs: TraversableOnce[FinalP[E, P]] = Iterator.empty
    ): Unit

    private[fpcf] val simultaneouslyLazilyComputedPropertyKinds: Array[IntTrieSet /*Set[PKId]*/ ] = {
        Array.fill(SupportedPropertyKinds)(IntTrieSet.empty)
    }

    /**
     * Registers a function that lazily computes multiple properties of different kinds
     * at the same time for an element of the store.
     *
     * For further details see [[registerLazyPropertyComputation]].
     */
    def registerLazyMultiPropertyComputation[E <: Entity, P <: Property](
        pc:  PropertyComputation[E],
        pks: PropertyKey[P]*
    ): Unit = {
        if (pks.isEmpty) {
            throw new IllegalArgumentException("pks is empty")
        };
        if (pks.size == 1) {
            registerLazyPropertyComputation(pks.head, pc)
        } else {
            pks foreach { pk ⇒
                registerLazyPropertyComputation(pk, pc)
                var simultaneouslyComputed = IntTrieSet.empty
                pks filter (_ != pk) foreach { otherPk ⇒
                    simultaneouslyComputed += otherPk.id
                }
                if (simultaneouslyComputed.isEmpty) {
                    val message = pks.mkString("pks is not disjunct: ", ", ", "")
                    throw new IllegalArgumentException(message)
                }
            }
        }
    }

    /**
     * Registers a property computation that is eagerly triggered when a property of the given kind
     * is derived for some entity for the first time. Note, that the property computation
     * function – as usual – has to be thread safe (only on-update continuation functions are
     * guaranteed to be executed sequentially per E/PK pair). The primary use case is to
     * kick-start the computation of some e/pk as soon as an entity "becomes relevant".
     *
     * In general, it also possible to have a standard analysis that just queries the properties
     * of the respective entities and which maintains the list of dependees. However, if the
     * list of dependees becomes larger and (at least initially) encompasses a significant fraction
     * or even all entities of a specific kind, the overhead that is generated in the framework
     * becomes very huge. In this case, it is way more efficient to register a triggered
     * computation.
     *
     * For example, if you want to do some processing (kick-start further computations) related
     * to methods that are reached, it is more efficient to register a property computation
     * that is triggered when a method's `Caller` property is set. Please note, that the property
     * computation is allowed to query and depend on the property that initially kicked-off the
     * computation in the first place. '''Querying the property store may in particular be required
     * to identify the reason why the property was set'''. For example, if the `Caller` property
     * was set to the fallback due to a depending computation, it may be necessary to distinguish
     * between the case "no callers" and "unknown callers"; in case of the final property
     * "no callers" the result may very well be [[NoResult]].
     *
     * @param pk The property key.
     * @param pc The computation that is (potentially concurrently) called to kick-start a
     *           computation related to the given entity.
     */
    def registerTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit

    /**
     * Needs to be called before an analysis is scheduled to inform the property store which
     * properties will be computed now and which are computed in a later phase. The later
     * information is used to decide when we use a fallback.
     *
     * @note `setupPhase` even needs to be called if just fallback values should be computed; in
     *        this case both sets have to be empty.
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
     * @see [[scheduleEagerComputationForEntity]] for details.
     */
    def scheduleEagerComputationsForEntities[E <: Entity](
        es: TraversableOnce[E]
    )(
        c: PropertyComputation[E]
    ): Unit = {
        es.foreach(e ⇒ scheduleEagerComputationForEntity(e)(c))
    }

    /**
     * Schedules the execution of the given `PropertyComputation` function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierarchy in a top-down manner.
     *
     * @note   It is NOT possible to use scheduleEagerComputationForEntity for properties which
     *         are also computed by a lazy property computation; use `force` instead!
     *
     * @note   If any computation resulted in an exception, then the scheduling will fail and
     *         the exception related to the failing computation will be thrown again.
     */
    def scheduleEagerComputationForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit

    /**
     * Processes the result eventually; generally, not directly called by analyses.
     * If this function is directly called, the caller has to ensure that we don't have overlapping
     * results and that the given result is a meaningful update of the previous property
     * associated with the respective entity - if any!
     *
     * @throws IllegalStateException If the result cannot be applied.
     * @note   If any computation resulted in an exception, then `handleResult` will fail and
     *         the exception related to the failing computation will be thrown again.
     */
    def handleResult(r: PropertyComputationResult, forceEvaluation: Boolean = false): Unit

    /**
     * Awaits the completion of all property computations which were previously scheduled.
     * As soon as all initial computations have finished, dependencies on E/P pairs for which
     * no value was computed and also will not be computed in the future(!) (see `setupPhase`
     * for details), will be identified and the fallback value will be used. After that, cycle
     * resolution will be performed. I.e., first all _closed_ strongly connected components
     * will be identified that do not contain any properties for which we will compute (in a
     * future phase) any more refined values. Then the values will be made final.
     *
     * If the store is suspended, waitOnPhaseCompletion will return as soon as all running
     * computations are finished. By updating the `isSuspended` state and calling
     * `waitOnPhaseCompletion` again computations can be continued.
     *
     * @note If a second thread is used to register [[org.opalj.fpcf.PropertyComputation]] functions
     *       no guarantees are given; it is recommended to schedule all property computation
     *       functions using one thread and to also use that thread to call this method.
     * @note If a computation fails with an exception, the property store will stop in due time
     *       and return the thrown exception. No strong guarantees are given which exception
     *       is returned in case of concurrent execution with multiple exceptions.
     * @note In case of an exception, the analyses are aborted as fast as possible and the
     *       store is no longer usable.
     */
    def waitOnPhaseCompletion(): Unit

    /** ONLY INTENDED TO BE USED BY TESTS TO AVOID MISGUIDING TEST REPORTS! */
    private[fpcf] var suppressError: Boolean = false

    /**
     * Called when the first top-level exception occurs.
     * Intended to be overridden by subclasses.
     */
    protected[this] def onFirstException(t: Throwable): Unit = {
        shutdown()
        if (!suppressError) {
            val storeId = "PropertyStore@"+System.identityHashCode(this).toHexString
            error("analysis progress", s"$storeId: analysis resulted in exception", t)
        }
    }

    @volatile protected[this] var exception: Throwable = _ /*null*/

    protected[this] def collectException(t: Throwable): Unit = {
        if (exception != null) {
            if (exception != t && !exception.isInstanceOf[InterruptedException]) {
                exception.addSuppressed(t)
            }
        } else {
            // double-checked locking... we don't care about performance if everything falls
            // apart anyway.
            this.synchronized {
                if (exception ne null) {
                    if (exception ne t) {
                        exception.addSuppressed(t)
                    }
                } else {
                    exception = t
                    onFirstException(t)
                }
            }
        }
    }

    @inline protected[this] def collectAndThrowException(t: Throwable): Nothing = {
        collectException(t)
        throw t;
    }

    @inline protected[this] def handleExceptions[U](f: ⇒ U): U = {
        if (exception ne null)
            throw exception;

        try {
            f
        } catch {
            case ct: ControlThrowable ⇒ throw ct;
            case t: Throwable         ⇒ collectAndThrowException(t)
        }
    }
}

/**
 * Manages general configuration options. Please note, that changes of these options
 * can be done at any time.
 */
object PropertyStore {

    final val DebugKey = "org.opalj.fpcf.PropertyStore.Debug"

    private[this] var debug: Boolean = {
        val initialDebug = BaseConfig.getBoolean(DebugKey)
        updateDebug(initialDebug)
        initialDebug
    }

    /**
     * Determines if newly created property stores are created with debug turned on or off.
     *
     * Does NOT affect existing instances!
     */
    def Debug: Boolean = debug

    /**
     * Determines if new `PropertyStore` instances run with debugging or without debugging.
     *
     */
    def updateDebug(newDebug: Boolean): Unit = {
        implicit val logContext = GlobalLogContext
        debug =
            if (newDebug) {
                info("OPAL", s"$DebugKey: debugging support on for new PropertyStores")
                true
            } else {
                info("OPAL", s"$DebugKey: debugging support off for new PropertyStores")
                false
            }
    }

    //
    // The following settings are primarily about comprehending analysis results than
    // about debugging analyses.
    //

    final val TraceFallbacksKey = "org.opalj.fpcf.PropertyStore.TraceFallbacks"

    private[this] var traceFallbacks: Boolean = {
        val initialTraceFallbacks = BaseConfig.getBoolean(TraceFallbacksKey)
        updateTraceFallbacks(initialTraceFallbacks)
        initialTraceFallbacks
    }

    // We think of it as a runtime constant (which can be changed for testing purposes).
    def TraceFallbacks: Boolean = traceFallbacks

    def updateTraceFallbacks(newTraceFallbacks: Boolean): Unit = {
        implicit val logContext = GlobalLogContext
        traceFallbacks =
            if (newTraceFallbacks) {
                info("OPAL", s"$TraceFallbacksKey: usages of fallbacks are reported")
                true
            } else {
                info("OPAL", s"$TraceFallbacksKey: fallbacks are not reported")
                false
            }
    }

    final val TraceCycleResolutionsKey = "org.opalj.fpcf.PropertyStore.TraceCycleResolutions"

    private[this] var traceCycleResolutions: Boolean = {
        val initialTraceCycleResolutions = BaseConfig.getBoolean(TraceCycleResolutionsKey)
        updateTraceCycleResolutions(initialTraceCycleResolutions)
        initialTraceCycleResolutions
    }

    // We think of it as a runtime constant (which can be changed for testing purposes).
    def TraceCycleResolutions: Boolean = traceCycleResolutions

    def updateTraceCycleResolutions(newTraceCycleResolutions: Boolean): Unit = {
        implicit val logContext = GlobalLogContext
        traceCycleResolutions =
            if (newTraceCycleResolutions) {
                info("OPAL", s"$TraceCycleResolutionsKey: cycle resolutions are reported")
                true
            } else {
                info("OPAL", s"$TraceCycleResolutionsKey: cycle resolutions are not reported")
                false
            }
    }

}
