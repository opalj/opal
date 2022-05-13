/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.util.concurrent.ConcurrentHashMap
import java.util.{Arrays => JArrays}
import java.util.concurrent.RejectedExecutionException

import scala.util.control.ControlThrowable
import scala.collection.mutable

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug => trace}
import org.opalj.log.OPALLogger.error
import org.opalj.collection.IntIterator
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId

/**
 * A property store manages the execution of computations of properties related to concrete
 * entities as well as artificial entities (for example, methods, fields and classes of a program,
 * but, for another example, also the call graph or the project as such). These computations may
 * require and provide information about other entities of the store and the property store
 * implements the logic to handle the computations related to the dependencies between the entities.
 * Furthermore, the property store may parallelize the computation of the properties as far as
 * possible without requiring users to take care of it;
 * users are also generally not required to think about the concurrency when implementing an
 * analysis as long as the properties only use immutable data-structures and the analyses
 * only use immutable data structures when interacting the property store.
 * The most basic concepts are also described in the SOAP paper:
 * "Lattice Based Modularization of Static Analyses"
 * (https://conf.researchr.org/event/issta-2018/soap-2018-papers-lattice-based-modularization-of-static-analyses)
 *
 * ==Usage==
 * The correct strategy, when using the PropertyStore, is to always continue computing the property
 * of an entity and to collect the dependencies on those elements that are (still) relevant.
 * I.e., if some information is not or just not completely available, the analysis should
 * still continue using the provided information and (internally) record the dependency, by storing
 * the returned property extension.
 * Later on, when the analysis has computed its (interim) result, it reports the same and informs
 * the framework about its dependencies.
 * Based on the later the framework will call back the analysis when a dependency is updated.
 * In general, an analysis should always try to minimize the number
 * of dependencies to the minimum set to enable the property store to suspend computations that
 * are no longer required.
 *
 * ===Core Requirements on Property Computation Functions (Modular Static Analyses)===
 *  The following requirements ensure correctness and determinism of the result.
 *  - '''At Most One Lazy Function per Property Kind''' A specific kind of property is
 *    always computed by only one registered lazy `PropertyComputation` function.
 *    No other analysis is (conceptually) allowed to derive a value for an E/PK pairing
 *    for which a lazy function is registered. It is also not allowed to schedule a computation
 *    eagerly if a lazy computation is also registered.
 *
 *  - '''Thread-Safe PropertyComputation functions''' If a single instance of a property computation
 *    function (which is the standard case) is scheduled for computing the properties of multiple
 *    entities, that function has to be thread safe. I.e., the function may
 *    be executed concurrently for different entities. The [[OnUpdateContinuation]] functions
 *    are, however, executed sequentially w.r.t. one E/PK pair. This model generally does not
 *    require that users have to think about concurrent issues as long as the initial function
 *    is actually a pure function, which is usually a non-issue.
 *
 *  - '''Non-Overlapping Results''' [[PropertyComputation]] functions that are invoked on different
 *    entities have to compute result sets that are disjoint unless a [[PartialResult]] is used.
 *    For example, an analysis that performs a computation on class files and
 *    that derives properties of a specific kind related to a class file's methods must ensure
 *    that two concurrent calls of the same analysis - running concurrently on two different
 *    class files - do not derive information about the same method. If results for a specific
 *    entity are collaboratively computed, then a [[PartialResult]] has to be used.
 *
 *  - '''If some partial result potentially contributes to the property of an entity,
 *    the first partial result has to set the property to the default (typically "most precise")
 *    value.'''
 *
 *  - '''Monoton''' a function which computes a property has to be monotonic.
 *
 * ===Cyclic Dependencies===
 * In general, it may happen that some analyses are mutually dependent and therefore no
 * final value is directly computed. In this case the current extension (the most precise result)
 * of the properties are committed as the final values when the phase end. If the analyses only
 * computed a lower bound that one will be used.
 *
 * ==Thread Safety==
 * The sequential property store is not thread-safe; the parallelized implementation enables
 * limited concurrent access:
 *  - a client has to use the SAME thread (the driver thread) to call
 *    (0) [[set]] and [[preInitialize]] to initialize the property store,
 *    (1) [[org.opalj.fpcf.PropertyStore!.setupPhase(configuration:org\.opalj\.fpcf\.PropertyKindsConfiguration)*]],
 *    (2) [[registerLazyPropertyComputation]] or [[registerTriggeredComputation]],
 *    (3) [[scheduleEagerComputationForEntity]] / [[scheduleEagerComputationsForEntities]],
 *    (4) [[force]] and
 *    (5) (finally) [[PropertyStore#waitOnPhaseCompletion]].
 *    go back to (1).
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
 *  - EPS =       Entity, Property and the State (final or intermediate)
 *  - EP =        Entity and some (final or intermediate) Property
 *  - EOptionP =  Entity and either a PropertyKey or (if available) a Property
 *  - ps =        Property Store
 *
 * ==Exceptions==
 * In general, exceptions are only thrown if debugging is turned on due to the costs of checking
 * for the respective violations. That is, if debugging is turned off, many potential errors leading
 * to "incomprehensible" results will not be reported. Hence, after debugging an analysis turn
 * debugging (and assertions!) off to get the best performance.
 *
 * We will throw `IllegalArgumentException`'s iff a parameter is in itself invalid. E.g., the lower
 * bound is ``above`` the upper bound. In all other cases `IllegalStateException`s are thrown.
 * All exceptions are either thrown immediately or eventually, when
 * [[PropertyStore#waitOnPhaseCompletion]] is called. In the latter case, the exceptions are
 * accumulated in the first thrown exception using suppressed exceptions.
 *
 * @author Michael Eichberg
 */
abstract class PropertyStore {

    implicit val logContext: LogContext

    //
    //
    // FUNCTIONALITY TO ASSOCIATE SOME INFORMATION WITH THE STORE THAT
    // (TYPICALLY) HAS THE SAME LIFETIME AS THE STORE
    //
    //

    private[this] val externalInformation = new ConcurrentHashMap[AnyRef, AnyRef]()

    /**
     * Attaches or returns some information associated with the property store using a key object.
     *
     * This method is thread-safe. However, the client which adds information to the store
     * has to ensure that the overall process of adding/querying/removing is well defined and
     * the ordered is ensured.
     */
    final def getOrCreateInformation[T <: AnyRef](key: AnyRef, f: => T): T = {
        externalInformation.computeIfAbsent(key, _ => f).asInstanceOf[T]
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
     * If set to `true` no new computations will be scheduled and running computations will
     * be terminated. Afterwards, the store can be queried, but no new computations can
     * be started.
     */
    @volatile var doTerminate: Boolean = false

    /**
     * Should be called when a PropertyStore is no longer going to be used to schedule
     * computations.
     *
     * Properties can still be queried after shutdown.
     */
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
    final val debug: Boolean = PropertyStore.Debug // TODO Rename to "Debug"

    final val traceFallbacks: Boolean = PropertyStore.TraceFallbacks // TODO Rename to "TraceFallbacks"

    final val traceSuppressedNotifications: Boolean = PropertyStore.TraceSuppressedNotifications // TODO Rename to "TraceSuppressedNotifications"

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
     * The number of ([[OnUpdateContinuation]]s) that were executed in response to an
     * updated property.
     */
    def scheduledOnUpdateComputationsCount: Int

    /** The number of times the property store reached quiescence. */
    def quiescenceCount: Int

    /**
     * The number of times a fallback property was computed for an entity though an (eager)
     * analysis was actually scheduled.
     */
    def fallbacksUsedForComputedPropertiesCount: Int

    private[fpcf] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit

    /**
     * Reports core statistics; this method is only guaranteed to report ''final'' results
     * if it is called while the store is quiescent.
     */
    def statistics: mutable.LinkedHashMap[String, Int] = {
        val s =
            if (debug)
                mutable.LinkedHashMap(
                    "scheduled tasks" ->
                        scheduledTasksCount,
                    "scheduled on update computations" ->
                        scheduledOnUpdateComputationsCount,
                    "computations of fallback properties for computed properties" ->
                        fallbacksUsedForComputedPropertiesCount
                )
            else
                mutable.LinkedHashMap.empty[String, Int]

        // Always available stats:
        s.put("quiescence", quiescenceCount)
        s
    }

    //
    //
    // CORE FUNCTIONALITY
    //
    //

    def MaxEvaluationDepth: Int

    /**
     * If a property is queried for which we have no value, then this information is used
     * to determine which kind of fallback is required.
     */
    protected[this] final val propertyKindsComputedInEarlierPhase: Array[Boolean] = {
        new Array(SupportedPropertyKinds)
    }

    final def alreadyComputedPropertyKindIds: IntIterator = {
        IntIterator.upUntil(0, SupportedPropertyKinds).filter(propertyKindsComputedInEarlierPhase)
    }

    protected[this] final val propertyKindsComputedInThisPhase: Array[Boolean] = {
        new Array(SupportedPropertyKinds)
    }

    /**
     * Used to identify situations where a property is queried, which is only going to be computed
     * in the future - in this case, the specification of an analysis is broken!
     */
    protected[this] final val propertyKindsComputedInLaterPhase: Array[Boolean] = {
        new Array(SupportedPropertyKinds)
    }

    protected[this] final val suppressInterimUpdates: Array[Array[Boolean]] = {
        Array.fill(SupportedPropertyKinds) { new Array[Boolean](SupportedPropertyKinds) }
    }

    /**
     * `true` if entities with a specific property kind (EP) may have dependers with suppressed
     * notifications. (I.e., suppressInteriumUpdates("depender")("EP") is `true`.)
     */
    protected[this] final val hasSuppressedDependers: Array[Boolean] = {
        Array.fill(SupportedPropertyKinds) { false }
    }

    /**
     * The order in which the property kinds will be finalized; the last phase is considered
     * the clean-up phase and will contain all remaining properties that were not explicitly
     * finalized previously.
     */
    protected[this] final var subPhaseFinalizationOrder: Array[List[PropertyKind]] = Array.empty

    /**
     * The set of computations that will only be scheduled if the result is required.
     */
    protected[this] final val lazyComputations: Array[SomeProperPropertyComputation] = {
        new Array(PropertyKind.SupportedPropertyKinds)
    }

    /**
     * The set of transformers that will only be executed when required.
     */
    protected[this] final val transformersByTargetPK: Array[( /*source*/ PropertyKey[Property], (Entity, Property) => FinalEP[Entity, Property])] = {
        new Array(PropertyKind.SupportedPropertyKinds)
    }
    protected[this] final val transformersBySourcePK: Array[( /*target*/ PropertyKey[Property], (Entity, Property) => FinalEP[Entity, Property])] = {
        new Array(PropertyKind.SupportedPropertyKinds)
    }

    protected[this] def computeFallback[E <: Entity, P <: Property](
        e:    E,
        pkId: Int
    ): FinalEP[E, P] = {
        val reason = {
            if (propertyKindsComputedInEarlierPhase(pkId) || propertyKindsComputedInThisPhase(pkId)) {
                if (debug) incrementFallbacksUsedForComputedPropertiesCounter()
                PropertyIsNotDerivedByPreviouslyExecutedAnalysis
            } else {
                PropertyIsNotComputedByAnyAnalysis
            }
        }
        val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
        if (traceFallbacks) {
            trace("analysis progress", s"used fallback $p for $e")
        }
        FinalEP(e, p.asInstanceOf[P])
    }

    /**
     * Returns `true` if the given entity is known to the property store. Here, `isKnown` can mean
     *  - that we actually have a property, or
     *  - a computation is scheduled/running to compute some property, or
     *  - an analysis has a dependency on some (not yet finally computed) property, or
     *  - that the store just eagerly created the data structures necessary to associate
     *    properties with the entity because the entity was queried.
     */
    def isKnown(e: Entity): Boolean

    /**
     * Tests if we have some (lb, ub or final) property for the entity with the respective kind.
     * If `hasProperty` returns `true` a subsequent `apply` will return an `EPS` (not an `EPK`).
     */
    final def hasProperty(epk: SomeEPK): Boolean = hasProperty(epk.e, epk.pk)

    /** See `hasProperty(SomeEPK)` for details. **/
    def hasProperty(e: Entity, pk: PropertyKind): Boolean

    /**
     * Returns an iterator of the different properties associated with the given entity.
     *
     * This method is the preferred way to get a snapshot of all properties of an entity and should
     * be used if you know that all properties are already computed.
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     *
     * @param e An entity stored in the property store.
     */
    def properties[E <: Entity](e: E): Iterator[EPS[E, Property]]

    /**
     * Returns all entities which have a property of the respective kind. The result is
     * undefined if this method is called while the property store still performs
     * (concurrent) computations.
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]]

    /**
     * Returns all entities that currently have the given property bounds based on an "==" (equals)
     * comparison. (In case of final properties the bounds are equal.)
     * If some analysis only computes an upper or a lower bound and no final results exists,
     * that entity will be ignored.
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def entities[P <: Property](lb: P, ub: P): Iterator[Entity]

    /**
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def entitiesWithLB[P <: Property](lb: P): Iterator[Entity]

    /**
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def entitiesWithUB[P <: Property](ub: P): Iterator[Entity]

    /**
     * The set of all entities which have an entity property state that passes
     * the given filter.
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def entities(propertyFilter: SomeEPS => Boolean): Iterator[Entity]

    /**
     * Returns all final entities with the given property.
     *
     * @note Only to be called when the store is quiescent.
     * @note Does not trigger lazy property computations.
     */
    def finalEntities[P <: Property](p: P): Iterator[Entity] = {
        entities((otherEPS: SomeEPS) => otherEPS.isFinal && otherEPS.asFinal.p == p)
    }

    /** @see `get(epk:EPK)` for details. */
    def get[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Option[EOptionP[E, P]]

    /**
     * Returns the property of the respective property kind `pk` currently associated
     * with the given element `e`. Does not trigger any computations.
     */
    def get[E <: Entity, P <: Property](epk: EPK[E, P]): Option[EOptionP[E, P]]

    /**
     * Associates the given property `p`, which has property kind `pk`, with the given entity
     * `e` iff `e` has no property of the respective kind. The set property is always final.
     *
     * '''Calling this method is only supported before any analysis is scheduled!'''
     *
     * One use case is an analysis that does use the property store while executing the analysis,
     * but which wants to store the results in the store. Such an analysis '''must
     * be executed before any other analysis is scheduled'''.
     * A second use case are (eager or lazy) analyses, which want to store some pre-configured
     * information in the property store; e.g., properties of natives methods which were derived
     * beforehand.
     *
     * @note   This method must not be used '''if there might be a computation (in the future) that
     *         computes the property kind `pk` for the given `e`'''.
     */
    final def set(e: Entity, p: Property): Unit = {
        if (!isIdle) {
            throw new IllegalStateException("analyses are already running")
        }
        if (propertyKindsComputedInEarlierPhase(p.key.id)) {
            throw new IllegalStateException(s"property kind (of $p) was computed in previous phase")
        }
        doSet(e, p)
    }

    protected[this] def doSet(e: Entity, p: Property): Unit

    /**
     * Associates the given entity with the newly computed intermediate property P.
     *
     * '''Calling this method is only supported before any analysis is scheduled!'''
     *
     * @param pc A function which is given the current property of kind pk associated with e and
     *           which has to compute the new '''intermediate''' property `p`.
     */
    final def preInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] => InterimEP[E, P]
    ): Unit = {
        if (!isIdle) {
            throw new IllegalStateException("analyses are already running")
        }
        if (propertyKindsComputedInEarlierPhase(pk.id)) {
            throw new IllegalStateException(s"property kind ($pk) was computed in previous phase")
        }
        doPreInitialize(e, pk)(pc)
    }

    protected[this] def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] => InterimEP[E, P]
    ): Unit

    final def setupPhase(configuration: PropertyKindsConfiguration): Unit = {
        setupPhase(
            configuration.propertyKindsComputedInThisPhase,
            configuration.propertyKindsComputedInLaterPhase,
            configuration.suppressInterimUpdates,
            configuration.collaborativelyComputedPropertyKindsFinalizationOrder
        )
    }

    protected[this] var subPhaseId: Int = 0

    protected[this] var hasSuppressedNotifications: Boolean = false

    /**
     * Needs to be called before an analysis is scheduled to inform the property store which
     * properties will be computed now and which are computed in a later phase. The
     * information is used to decide when we use a fallback and which kind of fallback.
     *
     * @note `setupPhase` even needs to be called if just fallback values should be computed; in
     *        this case `propertyKindsComputedInThisPhase` and `propertyKindsComputedInLaterPhase`
     *        have to be empty, but `finalizationOrder` have to contain the respective property
     *        kind.
     *
     * @param propertyKindsComputedInThisPhase The kinds of properties for which we will schedule
     *                                         computations.
     *
     * @param propertyKindsComputedInLaterPhase The set of property kinds which will be computed
     *        in a later phase.
     * @param suppressInterimUpdates Specifies which interim updates should not be passed to which
     *        kind of dependers.
     *        A depender will only be informed about the final update. The key of the map
     *        identifies the target of a notification about an update (the depender) and the value
     *        specifies which dependee updates should be ignored unless it is a final update.
     *        This is an optimization related to lazy computations, but also enables the
     *        implementation of transformers and the scheduling of analyses which compute different
     *        kinds of bounds unless the analyses have cyclic dependencies.
     */
    final def setupPhase(
        propertyKindsComputedInThisPhase:  Set[PropertyKind],
        propertyKindsComputedInLaterPhase: Set[PropertyKind]                    = Set.empty,
        suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]] = Map.empty,
        finalizationOrder:                 List[List[PropertyKind]]             = List.empty
    ): Unit = handleExceptions {
        if (!isIdle) {
            throw new IllegalStateException("computations are already running");
        }

        require(
            suppressInterimUpdates.forall { e =>
                val (dependerPK, dependeePKs) = e
                !dependeePKs.contains(dependerPK)
            },
            "illegal self dependency"
        )

        // Step 1
        // Copy all property kinds that were computed in the previous phase that are no
        // longer computed to the "propertyKindsComputedInEarlierPhase" array.
        // Afterwards, initialize the "propertyKindsComputedInThisPhase" with the given
        // information.
        // Note that "lazy" property computations may be executed accross several phases,
        // however, all "intermediate" values found at the end of a phase can still be executed.
        this.propertyKindsComputedInThisPhase.iterator.zipWithIndex foreach { previousPhaseComputedPK =>
            val (isComputed, pkId) = previousPhaseComputedPK
            if (isComputed && !propertyKindsComputedInThisPhase.exists(_.id == pkId)) {
                propertyKindsComputedInEarlierPhase(pkId) = true
            }
        }
        JArrays.fill(this.propertyKindsComputedInThisPhase, false)
        propertyKindsComputedInThisPhase foreach { pk =>
            this.propertyKindsComputedInThisPhase(pk.id) = true
        }

        // Step 2
        // Set the "propertyKindsComputedInLaterPhase" array to the specified values.
        JArrays.fill(this.propertyKindsComputedInLaterPhase, false)
        propertyKindsComputedInLaterPhase foreach { pk =>
            this.propertyKindsComputedInLaterPhase(pk.id) = true
        }

        // Step 3
        // Collect the information about which interim results should be suppressed.
        suppressInterimUpdates foreach { dependerDependees =>
            val (depender, dependees) = dependerDependees
            require(dependees.nonEmpty)
            dependees foreach { dependee =>
                this.suppressInterimUpdates(depender.id)(dependee.id) = true
                hasSuppressedDependers(dependee.id) = true
            }
        }

        // Step 4
        // Save the information about the finalization order (of properties which are
        // collaboratively computed).
        val cleanUpSubPhase =
            (propertyKindsComputedInThisPhase -- finalizationOrder.flatten.toSet) + AnalysisKey
        this.subPhaseFinalizationOrder =
            if (cleanUpSubPhase.isEmpty) {
                finalizationOrder.toArray
            } else {
                (finalizationOrder :+ cleanUpSubPhase.toList).toArray
            }

        subPhaseId = 0
        hasSuppressedNotifications = suppressInterimUpdates.nonEmpty

        // Step 5
        // Call `newPhaseInitialized` to enable subclasses to perform custom initialization steps
        // when a phase was setup.
        newPhaseInitialized(
            propertyKindsComputedInThisPhase,
            propertyKindsComputedInLaterPhase,
            suppressInterimUpdates,
            finalizationOrder
        )
    }

    /**
     * Called when a new phase was initialized. Intended to be overridden by subclasses if
     * special handling is required.
     */
    protected[this] def newPhaseInitialized(
        propertyKindsComputedInThisPhase:  Set[PropertyKind],
        propertyKindsComputedInLaterPhase: Set[PropertyKind],
        suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]],
        finalizationOrder:                 List[List[PropertyKind]]
    ): Unit = { /*nothing to do*/ }

    /**
     * Returns `true` if the store does not perform any computations at the time of this method
     * call.
     *
     * This method is only intended to support bug detection.
     */
    def isIdle: Boolean

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note   Querying the properties of the given entities will trigger lazy computations.
     * @note   The returned collection can be used to create an [[InterimResult]].
     *         @see `apply(epk:EPK)` for details.
     */
    final def apply[E <: Entity, P <: Property](
        es: Iterable[E],
        pk: PropertyKey[P]
    ): Iterable[EOptionP[E, P]] = {
        es.map(e => apply(EPK(e, pk)))
    }

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note  Querying the properties of the given entities will trigger lazy computations.
     * @note  The returned collection can be used to create an [[InterimResult]].
     * @see  `apply(epk:EPK)` for details.
     */
    final def apply[E <: Entity, P <: Property](
        es:  Iterable[E],
        pmi: PropertyMetaInformation { type Self <: P }
    ): Iterable[EOptionP[E, P]] = {
        apply(es, pmi.key)
    }

    /** @see `apply(epk:EPK)` for details. */
    final def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        apply(EPK(e, pk), e, pk, pk.id)
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
     * @note   In general, the returned value may change over time but only such that it
     *         is strictly more precise.
     * @note   Querying a property may trigger the (lazy) computation of the property.
     * @note   [[setupPhase]] has to be called before calling apply!
     * @note   After all computations has finished one of the "pure" query methods (e.g.,
     *         `entities` or `get` should be used.)
     *
     * @throws IllegalStateException If setup phase was not called or
     *         a previous computation result contained an epk which was not queried.
     *         (Both state are ALWAYS illegal, but are only explicitly checked for if debug
     *         is turned on!)
     * @param  epk An entity/property key pair.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *         `Final|InterimP(e,Property)` otherwise.
     */
    def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        val e = epk.e
        val pk = epk.pk
        val pkId = pk.id
        apply(epk, e, pk, pkId)
    }

    private[this] def apply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pk:   PropertyKey[P],
        pkId: Int
    ): EOptionP[E, P] = {

        if (debug && propertyKindsComputedInLaterPhase(pkId)) {
            throw new IllegalArgumentException(
                s"querying of property kind ($pk) computed in a later phase"
            )
        }

        doApply(epk, e, pkId)
    }

    protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P]

    /**
     * Enforce the evaluation of the specified property kind for the given entity, even
     * if the property is computed lazily and no "eager computation" requires the results
     * (anymore).
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
     * In general, the result can't be an `IncrementalResult`, a `PartialResult` or a `NoResult`.
     *
     * ''A lazy computation must never return a [[NoResult]]; if the entity cannot be processed an
     * exception has to be thrown or the bottom value – if defined – has to be returned.''
     *
     * '''Calling `registerLazyPropertyComputation` is only supported as long as the store is not
     * queried and no computations are already running.
     * In general, this requires that lazy property computations are scheduled before any eager
     * analysis that potentially reads the value.'''
     */
    final def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: ProperPropertyComputation[E] // TODO add definition of PropertyComputationResult that is parameterized over the kind of Property to specify that we want a PropertyComputationResult with respect to PropertyKey (pk)
    ): Unit = {
        if (!isIdle) {
            throw new IllegalStateException(
                "lazy computations can only be registered while the property store is idle"
            )
        }

        lazyComputations(pk.id) = pc
    }

    /**
     * Registers a total function that takes a given final property and computes a new final
     * property of a different kind; the function must not query the property store. Furthermore,
     * `setupPhase` must specify that notifications about interim updates have to be suppressed.
     * A transformer is conceptually a special kind of lazy analysis.
     */
    final def registerTransformer[SourceP <: Property, TargetP <: Property, E <: Entity](
        sourcePK: PropertyKey[SourceP],
        targetPK: PropertyKey[TargetP]
    )(
        pc: (E, SourceP) => FinalEP[E, TargetP]
    ): Unit = {
        if (!isIdle) {
            throw new IllegalStateException(
                "transformers can only be registered while the property store is idle"
            )
        }

        transformersByTargetPK(targetPK.id) =
            (sourcePK, pc.asInstanceOf[(Entity, Property) => FinalEP[Entity, Property]])

        transformersBySourcePK(sourcePK.id) =
            (targetPK, pc.asInstanceOf[(Entity, Property) => FinalEP[Entity, Property]])
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
     * @note A computation is guaranteed to be triggered exactly once for every e/pk pair that has
     *       a concrete property - even if the value was already associated with the e/pk pair
     *       before the registration is done.
     *
     * @param pk The property key.
     * @param pc The computation that is (potentially concurrently) called to kick-start a
     *           computation related to the given entity.
     */
    final def registerTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (!isIdle) {
            throw new IllegalStateException(
                "triggered computations can only be registered while no computations are running"
            )
        }
        doRegisterTriggeredComputation(pk, pc)
    }

    protected[this] def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit

    /**
     * Will call the given function `c` for all elements of `es` in parallel.
     *
     * @see [[scheduleEagerComputationForEntity]] for details.
     */
    final def scheduleEagerComputationsForEntities[E <: Entity](
        es: IterableOnce[E]
    )(
        c: PropertyComputation[E]
    ): Unit = {
        es.iterator.foreach(e => scheduleEagerComputationForEntity(e)(c))
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
    final def scheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {
        doScheduleEagerComputationForEntity(e)(pc)
    }

    protected[this] def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit

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
    def handleResult(r: PropertyComputationResult): Unit

    /**
     * Executes the given function at some point between now and the return of a subsequent call
     * of waitOnPhaseCompletion.
     */
    def execute(f: => Unit): Unit

    /**
     * Awaits the completion of all property computations which were previously scheduled.
     * As soon as all initial computations have finished, dependencies on E/P pairs for which
     * no value was computed, will be identified and the fallback value will be used. After that,
     * the remaining intermediate values will be made final.
     *
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
        doTerminate = true
        shutdown()
        if (!suppressError) {
            val storeId = "PropertyStore@"+System.identityHashCode(this).toHexString
            error(
                "analysis progress",
                s"$storeId: shutting down computations due to failing analysis",
                t
            )
        }
    }

    @volatile protected[this] var exception: Throwable = _ /*null*/

    protected[fpcf] def collectException(t: Throwable): Unit = {
        if (exception != null) {
            if (exception != t
                && !t.isInstanceOf[InterruptedException]
                && !t.isInstanceOf[RejectedExecutionException] // <= used, e.g., by a ForkJoinPool
                ) {
                exception.addSuppressed(t)
            }
        } else {
            // Here, we use double-checked locking... we don't care about performance if
            // everything falls apart anyway.
            this.synchronized {
                if (exception ne null) {
                    if (exception != t) {
                        exception.addSuppressed(t)
                    }
                } else {
                    exception = t
                    onFirstException(t)
                }
            }
        }
    }

    @inline protected[fpcf] def collectAndThrowException(t: Throwable): Nothing = {
        collectException(t)
        throw t;
    }

    @inline /*visibility should be package and subclasses*/ def handleExceptions[U](f: => U): U = {
        if (exception != null) throw exception;

        try {
            f
        } catch {
            case ct: ControlThrowable => throw ct;
            case t: Throwable         => collectAndThrowException(t)
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
        implicit val logContext: LogContext = GlobalLogContext
        debug =
            if (newDebug) {
                info("OPAL - new PropertyStores", s"$DebugKey: debugging support on")
                true
            } else {
                info("OPAL - new PropertyStores", s"$DebugKey: debugging support off")
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
        implicit val logContext: LogContext = GlobalLogContext
        traceFallbacks =
            if (newTraceFallbacks) {
                info(
                    "OPAL - new PropertyStores",
                    s"$TraceFallbacksKey: usages of fallbacks are reported"
                )
                true
            } else {
                info(
                    "OPAL - new PropertyStores",
                    s"$TraceFallbacksKey: fallbacks are not reported"
                )
                false
            }
    }

    final val TraceSuppressedNotificationsKey = {
        "org.opalj.fpcf.PropertyStore.TraceSuppressedNotifications"
    }

    private[this] var traceSuppressedNotifications: Boolean = {
        val initialTraceSuppressedNotifications = BaseConfig.getBoolean(TraceSuppressedNotificationsKey)
        updateTraceDependersNotificationsKey(initialTraceSuppressedNotifications)
        initialTraceSuppressedNotifications
    }

    // We think of it as a runtime constant (which can be changed for testing purposes).
    def TraceSuppressedNotifications: Boolean = traceSuppressedNotifications

    def updateTraceDependersNotificationsKey(newTraceSuppressedNotifications: Boolean): Unit = {
        implicit val logContext: LogContext = GlobalLogContext
        traceSuppressedNotifications =
            if (newTraceSuppressedNotifications) {
                info(
                    "OPAL - new PropertyStores",
                    s"$TraceSuppressedNotificationsKey: suppressed notifications are reported"
                )
                true
            } else {
                info(
                    "OPAL - new PropertyStores",
                    s"$TraceSuppressedNotificationsKey: suppressed notifications are not reported"
                )
                false
            }
    }

}
