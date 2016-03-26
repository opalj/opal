/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import scala.language.existentials

import java.util.{IdentityHashMap ⇒ JIDMap}
import java.util.{Set ⇒ JSet}
import java.util.{HashSet ⇒ JHSet}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.{ConcurrentHashMap ⇒ JCHMap}
import java.util.Collections
import scala.reflect.ClassTag
import scala.collection.mutable
import scala.collection.mutable.{HashSet ⇒ HSet}
import scala.collection.mutable.{HashMap ⇒ HMap}
import scala.collection.mutable.{ListBuffer ⇒ Buffer}
import scala.collection.mutable.StringBuilder
import scala.collection.JavaConverters._
import org.opalj.collection.mutable.ArrayMap
import org.opalj.concurrent.Locking.{withReadLock, withWriteLock, withWriteLocks}
import org.opalj.concurrent.ThreadPoolN
import org.opalj.concurrent.handleUncaughtException
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.OPALLogger.{info ⇒ logInfo}
import org.opalj.log.OPALLogger.{debug ⇒ logDebug}
import org.opalj.log.OPALLogger.{error ⇒ logError}
import org.opalj.log.LogContext

/**
 * The property store manages the execution of computations of properties related to specific
 * entities (e.g., methods and classes of a program). These computations may require and provide
 * information about other entities of the store and the property store implements the logic
 * to handle the dependencies between the entities. Furthermore, the property store parallelizes
 * the computation of the properties as far as possible without requiring users to take care of it.
 *
 * The store supports two kinds of properties: '''set properties''' and '''per entity properties'''.
 * Set properties are particularly useful if the respective property (instance) is never specialized
 * for an entity. For example, the property whether a class is immutable or not can
 * be shared across all respective classes. The property which methods are calling a specific
 * method m on the other hand is specific for each method m.
 * In general, if the concrete instance of a property may be shared by all entities it is
 * advantageous to model it as a set property. However, the more general mechanism are
 * per entity properties and is generally needed if the result of a computation may be refined.
 *
 * ==Usage==
 * The general strategy when using the PropertyStore is to always
 * continue computing the property
 * of an entity and to collect the dependencies on those elements that are relevant.
 * I.e., if some information is not or just not completely available, the analysis should
 * still continue using the provided information and (internally) records the dependency.
 * Later on, when the analysis has computed its result it reports the same and informs the framework
 * about its dependencies.
 *
 * ===Core Requirements on Property Computation Functions===
 *  - (One Function per Property Kind) A specific kind of property is always computed
 *      by only one registered `PropertyComputation` function.
 *  - (Thread-Safe) PropertyComputation functions have to be thread-safe. I.e., the function may
 *  	be executed concurrently for different entities.
 *  - (Non-Overlapping Results) [[PropertyComputation]] functions that are invoked on different
 *      entities have to compute result sets that are disjoint.
 *      For example, an analysis that performs a computation on class files and
 *      that derives properties of a specific kind related to a class file's methods must ensure
 *      that the same analysis running concurrently on two different class files do not derive
 *      information about the same method.
 *  - (Monoton) If a `PropertyComputation` function calculates (refines) a (new )property for
 *      a specific element then the result must be more specific.
 *
 * ===Cyclic Dependencies===
 * In general, it may happen that some analyses cannot make any progress, because
 * they are mutually dependent. In this case the computation of a property `p` of an entity `e1`
 * depends on the property `p` of an entity `e2` that requires the property `p` of the entity `e1`.
 * In this case the [[PropertyKey]]'s strategy is used to resolve such a cyclic dependency.
 *
 * ==Thread Safety==
 * The PropertyStore is thread-safe.
 *
 * ==Multi-Threading==
 * The PropertyStore uses its own fixed size ThreadPool with at most
 * [[org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks]] threads.
 *
 * @author Michael Eichberg
 */
/*
 * The ProperStore prevents deadlocks by ensuring that updates of the store are always
 * atomic and by ensuring that each computation acquires all necessary locks (write and/or
 * read) locks in the same order!
 * The locking strategy (w.r.t. the shared locks) is as follows:
 *  1.  Every entity is directly associated with a ReentrantReadWriteLock that
 *      is always used if a property for the respective entity is read or written.
 *      (Independent of the kind of property that is accessed.)
 *  1.  Continuation functions are never invoked concurrently.
 *  1.  Associated information (e.g., the internally created observers) also use
 *      the lock associated with the entity.
 *  1.  Each computation is potentially executed concurrently and it is required
 *      that each computation is thread-safe.
 *  1.  The store as a whole is associated with a lock to enable selected methods
 *      to get a consistent view.
 *  1.  All set properties related operation are synchronized using the set property's mutex.
 *
 *  THE LOCK ORDER IS:
 *  [1.] the global SET PROPERTY OBSERVERS related Lock (read/write)
 *  [2.] the specific SET PROPERTY related lock (mutex)
 *  [3.] the global STORE lock (accessEntity/accessStore=exclusive access)
 *  [4.] the specific ENTITY (read/write) related lock (the entity lock must only be acquired when the store lock is held
 *  	 (If multiple locks may be required at the same time, then all locks have to be acquired
 *  	  in the order of the entity id.)
 *  [5.] the global TASKS related lock (<Tasks>.synchronized)
 */
// COMMON ABBREVIATONS USED IN THE FOLLOWING:
// ==========================================
// e = Entity
// l = Entity Lock (associated with an entity)
// p = Property
// ps = Properties (the properties of an entity)
// eps = EntityProperties (the pairing of an entity's lock and its properties)
// pk = Property Key
// pc = Property Computation
// lpc = Lazy Property Computation
// dpc = Direct Property Computation
// c = Continuation (The rest of a computation if a specific, dependent property was computed.)
// (p)o = PropertyObserver
// os = PropertyObservers
// pos = PropertyAndObservers
// EPK = An Entity and a Property Key
// EP = An Entity and an associated Property
// EOptionP = An Entity and either a Property Key or (if available) a Property
class PropertyStore private (
        // type Observers = mutable.ListBuffer[PropertyObserver]
        // class PropertyAndObservers(p: Property, os: Observers)
        // type Properties = OArrayMap[PropertyAndObservers] // the content of the array may be updated
        // class EntityProperties(l: ReentrantReadWriteLock, ps: Properties) // the references are never updated
        private[this] val data:  JIDMap[Entity, EntityProperties],
        final val isInterrupted: () ⇒ Boolean,
        @volatile var debug:     Boolean
)(
        implicit
        val logContext: LogContext
) { store ⇒

    private[this] def createIdentityHashSet(): JSet[AnyRef] = {
        Collections.newSetFromMap(new JIDMap[AnyRef, java.lang.Boolean]())
    }

    /**
     * The (immutable) set of all entities.
     */
    // This set is not mutated.
    private[this] final val keys: JSet[Entity] = data.keySet()
    /**
     * The (immutable) list of all entities.
     */
    private[this] final val keysList: List[Entity] = keys.asScala.toList

    private[this] final val entries: List[(Entity, EntityProperties)] = {
        data.entrySet().asScala.map(e ⇒ (e.getKey, e.getValue)).toList
    }

    private[this] final val entitiesProperties: List[EntityProperties] = {
        data.values().asScala.toList
    }

    /**
     * Returns `true` if the store contains the respective entity.
     */
    def isKnown(e: Entity): Boolean = keys.contains(e)

    /**
     * Counts how often some observer was notified. I.e., how often an analysis reacted
     * upon the update of a value and was not able to directly use a/the value.
     */
    private[this] val propagationCount = new AtomicLong(0)

    /**
     * Counts how often a default property was effectively assigned to an entity.
     */
    private[this] val effectiveDefaultPropertiesCount = new AtomicLong(0)

    // =============================================================================================
    // STORE MANAGEMENT
    //
    //

    // Using the accessStore lock it is possible to get a consistent snapshot view of the store.
    // NOTE: The store lock's purpose is NOT to distinguish reads and write to the store, but
    //       to get (non)-exclusive access to the store's properties.
    //       The entity's read and write locks can
    //       be used to get exclusive access to an entity even if the store is accessed concurrently
    //        which is the default case.
    private[this] final val StoreLock = new ReentrantReadWriteLock
    @inline final private[this] def accessEntity[B](f: ⇒ B) = withReadLock(StoreLock)(f)
    @inline final private[this] def accessStore[B](f: ⇒ B) = withWriteLock(StoreLock)(f)

    /**
     * @param entities Conceptually a set of entities for which we will acquire the locks in order
     * 		of the locks' ids.
     */
    @inline final private[this] def withEntitiesWriteLocks[T](entities: Traversable[Entity])(f: ⇒ T): T = {
        val sortedEntities = entities.toList.sortWith((e1, e2) ⇒ data.get(e1).id < data.get(e2).id)
        withWriteLocks(sortedEntities.map(e ⇒ data.get(e).l))(f)
    }

    /**
     * Clears all properties and property computation functions.
     */
    // Locks: SetPropertyObservers (write), accessStore
    def reset(): Unit = {
        writeSetPropertyObservers {
            accessStore {
                Tasks.reset()

                // reset statistics
                propagationCount.set(0l)
                effectiveDefaultPropertiesCount.set(0l)

                // reset set property related information
                theSetPropertyObservers.clear()
                theSetProperties.clear();

                // reset entity related information
                theDirectPropertyComputations.clear()
                theLazyPropertyComputations.clear()
                theOnPropertyComputations.clear()
                observers.clear()
                entitiesProperties foreach { eps ⇒ eps.ps.clear /*delete properties*/ }
            }
        }
    }

    /**
     * Returns a snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    // Locks: accessStore
    def toString(printProperties: Boolean): String = accessStore(snapshotToString(printProperties))

    private[this] val snapshotMutex = new Object
    private[this] def snapshotToString(printProperties: Boolean): String = snapshotMutex.synchronized {

        val entitiesPerSetPropertyCount = theSetProperties map { (index, entities) ⇒
            (SetProperty.propertyName(index), entities.size)
        }
        val overallSetPropertyCount = entitiesPerSetPropertyCount.map(_._2).sum
        val setPropertiesStatistics =
            s"∑$overallSetPropertyCount: "+
                entitiesPerSetPropertyCount.map(e ⇒ e._1+":"+e._2).mkString("(", ", ", ")")

        val perPropertyKeyEntities = new Array[Int](PropertyKey.maxId + 1)
        var perEntityPropertiesCount = 0
        var unsatisfiedPropertyDependencies = 0
        var registeredObservers = 0
        val properties = new StringBuilder
        for { (e, eps) ← entries } {
            val ps = eps.ps.entries.filter(pk ⇒ pk._2 ne null).map { e ⇒
                val (pkId, pos) = e
                val p = pos.p
                val os = pos.os
                val observedByCount = if (os eq null) 0 else os.size
                registeredObservers += observedByCount
                val observingCount = {
                    if (p eq null)
                        "N/A"
                    else {
                        val observers = store.observers.get(EPK(e, p.key))
                        if (observers ne null)
                            observers.size
                        else
                            0
                    }
                }
                (
                    if (p eq null) {
                        unsatisfiedPropertyDependencies += 1
                        s"$pkId#<TBD: ${PropertyKey.name(pkId)}>"
                    } else {
                        perEntityPropertiesCount += 1
                        perPropertyKeyEntities(pkId) = perPropertyKeyEntities(pkId) + 1
                        s"$pkId#$p[${p.key}]"
                    }
                ) + s"[observedBy=$observedByCount,observing=$observingCount]"
            }
            if (printProperties && ps.nonEmpty) {
                val s = ps.mkString("\t\t"+e.toString+" => {", ", ", "}\n")
                properties.append(s)
            }
        }

        val perEntityPropertiesStatistics =
            s"∑$perEntityPropertiesCount: "+
                (perPropertyKeyEntities.zipWithIndex.filter(_._1 > 0) map { e ⇒
                    val (sum, pk) = e
                    (PropertyKey.name(pk), sum)
                }).map(e ⇒ e._1+":"+e._2).mkString("(", ", ", ")")

        "PropertyStore(\n"+
            s"\tentitiesCount=${data.size()}\n"+
            s"\t(still)scheduledComputations=${Tasks.scheduledComputations}\n"+
            s"\texecutedComputations=${Tasks.executedComputations}\n"+
            s"\tpropagations=${propagationCount.get}\n"+
            s"\tunsatisfiedPropertyDependencies=$unsatisfiedPropertyDependencies\n"+
            s"\tregisteredObservers=$registeredObservers\n"+
            s"\teffectiveDefaultPropertiesCount=$effectiveDefaultPropertiesCount\n"+
            s"\tperEntityProperties[$perEntityPropertiesStatistics]"+
            (if (printProperties) s"=\n$properties" else "\n") +
            (if (overallSetPropertyCount > 0) s"\tperSetPropertyEntities[$setPropertiesStatistics]\n" else "")+
            ")"
    }

    /**
     * Returns a short string representation of the property store related to the key figures.
     */
    override def toString: String = toString(false)

    /**
     * Checks the consistency of the store.
     *
     * Only checks related to potentially internal bugs are performed. None of the checks is
     * relevant to developers of analyses.
     */
    // REQUIRES: Lock: AccessStore (!)
    @throws[AssertionError]("if the store is inconsistent")
    private[fpcf] def validate(dependerEPKOpt: Option[SomeEPK] = None): Boolean = {
        // 1. check that the properties are stored in the correct slots.
        entries foreach { entry ⇒
            val (_ /*e*/ , eps) = entry

            val ps = eps.ps
            ps foreach { (id, pos) ⇒
                if (pos ne null) {
                    val p = pos.p
                    if ((p ne null) && !p.isBeingComputed) {
                        if (p.id != id)
                            throw new AssertionError(s"illegal property $p stored in slot $id")

                        if (p.isFinal && (pos.os ne null))
                            throw new AssertionError(s"final property $p has observers ${pos.os}")
                    }
                }
            }
        }

        // 2. check that each observer found in observers still exists
        // observers : JCHMap[SomeEPK, Buffer[(SomeEPK, PropertyObserver)]]()
        // data:  JIDMap[Entity, EntityProperties]
        for {
            dependerEPK ← dependerEPKOpt
            dependeeOss = observers.get(dependerEPK)
            if dependeeOss ne null
            (dependeeEPK, po) ← dependeeOss
        } {
            if (!data.get(dependeeEPK.e).ps(dependeeEPK.pk.id).os.contains(po)) {
                val message = s"observers contains for $dependerEPK -> $dependeeEPK "+
                    s"a dangling observer: $po "
                throw new AssertionError(message)
            }
        }

        // 3. check that every found observer is recorded in observers
        for {
            relevantDependerEPK ← dependerEPKOpt
            PropertiesOfEntity(ps) ← entitiesProperties
        } {
            ps.foreach { (id, pOss) ⇒
                val os = pOss.os
                if (os ne null) {
                    os.foreach { pos ⇒
                        val dependerEPK = pos.dependerEPK
                        if (dependerEPK == relevantDependerEPK &&
                            !observers.get(dependerEPK).exists(_._2 eq pos)) {
                            val message = s"observers does not contain observer for $dependerEPK"
                            throw new AssertionError(message)
                        }
                    }
                }
            }
        }

        true
    }

    // =============================================================================================
    // SET PROPERTIES
    //
    //

    private[this] final val theSetPropertyObserversLock = new ReentrantReadWriteLock
    // access to this field needs to be synchronized!
    private[this] final val theSetPropertyObservers = ArrayMap[List[AnyRef ⇒ Unit]](5)
    private[this] final val theSetProperties = ArrayMap[JSet[AnyRef]](5)

    private[this] def writeSetPropertyObservers[U](f: ⇒ U): U = {
        withWriteLock(theSetPropertyObserversLock)(f)
    }

    private[this] def querySetPropertyObservers[U](f: ⇒ U): U = {
        withReadLock(theSetPropertyObserversLock)(f)
    }

    /**
     * Registers the callback function `f` that is called if any entity is added to the set
     * identified by the given [[SetProperty]].
     *
     * Adds the given function `f` to the set of functions that will be called
     * when an entity `e` gets the [[SetProperty]] `sp`. For those entities that already
     * have the respective property the function `f` will immediately be scheduled.
     *
     * I.e., the function `f` has to be thread-safe as it will be executed concurrently for
     * each entity `e` that has the respective property.
     */
    // Locks: Set Property Observers (write), Set Property
    def onPropertyDerivation[E <: AnyRef](sp: SetProperty[E])(f: (E) ⇒ Unit): Unit = {
        val spIndex = sp.index
        val spMutex = sp.mutex
        writeSetPropertyObservers {
            val oldObservers = theSetPropertyObservers.getOrElse(spIndex, Nil)
            theSetPropertyObservers(spIndex) = f.asInstanceOf[AnyRef ⇒ Unit] :: oldObservers
            spMutex.synchronized {
                import scala.collection.JavaConversions._
                val spSet = theSetProperties.getOrElseUpdate(spIndex, createIdentityHashSet())
                if (!spSet.isEmpty)
                    spSet.asInstanceOf[JSet[E]] foreach { e ⇒ scheduleFforE(e, f) }
            }
        }
    }

    /**
     * Directly associates the given [[SetProperty]] `sp` with the given entity `e`.
     *
     * If the given entity already has the associated property nothing will happen;
     * if not, we will immediately schedule the execution of all functions that
     * are interested in this property.
     */
    // Locks: Set Property Observers (read), Set Property
    def add[E <: AnyRef](sp: SetProperty[E])(e: E): Unit = {
        val spIndex = sp.index
        val spMutex = sp.mutex
        querySetPropertyObservers {
            val isAdded = spMutex.synchronized {
                theSetProperties.getOrElseUpdate(spIndex, createIdentityHashSet()).add(e)
            }
            if (isAdded) {
                // ATTENTION: We must not hold the lock on the store/a store entity, because
                // scheduleFforE requires the write lock!
                theSetPropertyObservers.getOrElse(spIndex, Nil) foreach { f ⇒
                    propagationCount.incrementAndGet()
                    scheduleFforE(e, f)
                }
            }
        }
    }

    /**
     * The current set of all entities which have the given [[SetProperty]].
     *
     * This is a blocking operation w.r.t. the set property; the returned set is a copy of the
     * original set.
     */
    // Locks: Set Property
    def entities[E <: AnyRef](sp: SetProperty[E]): JSet[E] = {
        sp.mutex.synchronized {
            val entitiesSet = theSetProperties.getOrElse(sp.index, new JHSet[AnyRef]())
            val clonedEntitiesSet = new JHSet[E]()
            clonedEntitiesSet.addAll(entitiesSet.asInstanceOf[JSet[E]])
            clonedEntitiesSet
        }
    }

    // =============================================================================================
    //
    // PER ENTITY PROPERTIES
    //
    //

    // access to this field is synchronized using the store's lock
    // the map's keys are the ids of the PropertyKeys
    private[this] final val theDirectPropertyComputations = ArrayMap[(Entity) ⇒ Property](5)

    // access to this field is synchronized using the store's lock
    // the map's keys are the ids of the PropertyKeys
    private[this] final val theLazyPropertyComputations = ArrayMap[SomePropertyComputation](5)

    // access to this field is synchronized using the store's lock
    // the map's keys are the ids of the PropertyKeys
    private[this] final val theOnPropertyComputations = ArrayMap[List[(Entity, Property) ⇒ Unit]](5)

    // The list of observers used by the entity e to compute the property of kind k (EPK).
    // In other words: the mapping between a Depender and its Dependee(s)/Observers!
    // The list of observers needs to be maintained whenever:
    //  1. A computation of a property finishes. In this case all observers need to
    //     be notified and removed from this map afterwards.
    //  1. A computation of a property generates an [[IntermediatResult]]
    type ObserversMap = JCHMap[ /*Depender*/ SomeEPK, Buffer[( /*Dependee*/ SomeEPK, PropertyObserver)]]
    private[this] final val observers = new ObserversMap()

    /**
     * Returns a snapshot of the properties with the given kind associated with the given entities.
     *
     * @note Querying the properties of the given entities will trigger lazy and direct property
     * 		computations.
     *
     * @note The returned collection can be used to create an [[IntermediateResult]].
     */
    // Locks: apply(Entity,PropertyKey)
    def apply[E <: Entity, P <: Property](
        es: Traversable[E],
        pk: PropertyKey[P]
    ): Traversable[EOptionP[E, P]] = {
        es.map(e ⇒ EOptionP(e, pk, this(e, pk)))
    }

    /**
     * Returns the property of the respective property kind `pk` currently associated
     * with the given element `e`.
     *
     * This is most basic method to get some property and it is the preferred way
     * if (a) you know that the property is already available – e.g., because some
     * property computation function was strictly run before the current one – or
     * if (b) it may be possible to compute a final answer even if the property
     * of the entity is not yet available.
     *
     * @note In general, the returned value may change over time but only such that it
     *      is strictly more precise.
     *
     * @note Querying a property may trigger the computation of the property if the underlying
     * 		function is either a lazy or a direct property computation function. In general
     * 		It is preferred that clients always assume that the property is lazily computed
     * 		when calling this function!
     *
     * @param e An entity stored in the property store.
     * @param pk The kind of property.
     * @return `None` if information about the respective property is not (yet) available.
     *      `Some(Property)` otherwise.
     */
    // Locks: accessEntity, Entity (read)
    //                      Entity (write)
    def apply[P <: Property](e: Entity, pk: PropertyKey[P]): Option[P] = {
        val pkId = pk.id
        val eps = data.get(e)
        val ps = eps.ps
        val lock = eps.l

        @inline def awaitComputationResult(p: PropertyIsDirectlyComputed): Some[P] = {
            // establish the happen before relation
            p.await()
            Some(ps(pkId).p.asInstanceOf[P])
        }

        accessEntity {
            var pos = withReadLock(lock) { ps(pkId) }
            if (pos eq null) {
                // => the property is not (yet) computed;
                // let's check if we have a registered lazy or direct property computation function
                val lpc = theLazyPropertyComputations(pkId)
                if (lpc ne null) withWriteLock(lock) {
                    // pos is not null if we have a property or if the property is currently computed
                    pos = ps(pkId)
                    if (pos eq null) {
                        val pos = new PropertyAndObservers(PropertyIsLazilyComputed, new Buffer)
                        ps(pkId) = pos
                        scheduleComputation(e, lpc)
                        None
                    } else {
                        val p = pos.p
                        if (p.isBeingComputed) None else Some(p.asInstanceOf[P])
                    }
                }
                else {
                    val dpc = theDirectPropertyComputations(pkId)
                    if (dpc ne null) {
                        withWriteLock(lock) {
                            pos = ps(pkId)
                            if (pos eq null) {
                                // => no other thread is currently computing this property
                                val computationLatch = new PropertyIsDirectlyComputed
                                ps(pkId) = new PropertyAndObservers(computationLatch, null)
                                Left(computationLatch)
                            } else {
                                // => either the property is now available or some other thread
                                // is still computing it
                                Right(pos.p)
                            }
                        } match {
                            case Left(computationLatch) ⇒
                                val p = dpc(e).asInstanceOf[P]
                                handleResult(ImmediateResult(e, p))
                                computationLatch.countDown()
                                Some(p)
                            case Right(p: PropertyIsDirectlyComputed) ⇒ awaitComputationResult(p)
                            case Right(p)                             ⇒ Some(p.asInstanceOf[P])
                        }
                    } else {
                        None
                    }
                }
            } else {
                pos.p match {
                    case null | PropertyIsLazilyComputed ⇒ None
                    case p: PropertyIsDirectlyComputed   ⇒ awaitComputationResult(p)
                    case p                               ⇒ Some(p.asInstanceOf[P])
                }
            }
        }
    }

    /**
     * Returns the property associated with the respective `dependeeE`.
     *
     * The function `c` is the function that is called when the property becomes
     * available and which computes – and then returns – the property for the depender.
     *
     * Require can only be used if it is guaranteed that the computation of the property
     * dependeePK will never require the property dependerPK. Hence, it should only be used
     * in combination with properties where the most precise analysis will never requirer
     * `dependerPk`.
     *
     * @example
     * {{{
     *   val c: Continuation =
     *      (dependeeE: Entity, dependeeP: Property) ⇒
     *          if (dependeeP == EffectivelyFinal) {
     *              val nextPC = body.pcOfNextInstruction(currentPC)
     *              determinePurityCont(method, nextPC, dependees)
     *          } else {
     *              Result(method, Impure)
     *          }
     * }}}
     *
     * @param dependerE The entity for which we are currently computing a property.
     * @param dependerPK The property that is currently computed for the entity `dependerE`.
     * @param dependeeE The entity about which some information is strictly required to compute the
     * 		property `dependerPK`.
     */
    // Locks of this.apply(...): Store, Entity
    def require[DependeeP <: Property](
        dependerE:  Entity,
        dependerPK: SomePropertyKey,
        dependeeE:  Entity,
        dependeePK: PropertyKey[DependeeP]
    )(
        c: Continuation[DependeeP]
    ): PropertyComputationResult = {
        this(dependeeE, dependeePK) match {
            case Some(dependeeP) ⇒
                // dependeeP may be already updated, but it is now on the caller to make
                // a decision whether it will continue waiting for further updates or not
                c(dependeeE, dependeeP)
            case _ /*None*/ ⇒
                new SuspendedPC[DependeeP](dependerE, dependerPK, dependeeE, dependeePK) {
                    override def continue(dependeeP: DependeeP) = c(dependeeE, dependeeP)
                }
        }
    }

    /**
     * Tests if all entities have the given property. If the respective property is
     * not yet available, the computation will be suspended until the property of
     * the respective kind is available. '''Hence, it only makes sense to use this
     * function if the respective property is computed by an independent analysis or
     * if it is an inherent property of the analysis/analyses that the information about the
     * dependees is guaranteed to become available without requiring information
     * about the depender.'''
     *
     * This function eagerly tries to determine if the answer is false and only
     * suspends the computation if the (negative) answer cannot directly be computed.
     *
     * @note Calling this method only makes sense if all properties that have the same property
     * 		kind as `expectedP` are not refineable/are final or if it is certain that no
     * 		further refinement of the respective properties can happen.
     */
    def allHaveProperty(
        dependerE: Entity, dependerPK: SomePropertyKey,
        dependees: Traversable[Entity], expectedP: Property
    )(
        c: (Boolean) ⇒ PropertyComputationResult
    ): PropertyComputationResult = {
        allHaveProperty(
            dependerE, dependerPK,
            dependees, expectedP.key, (p: Property) ⇒ p == expectedP
        )(c)
    }

    /**
     * Tests if all entities satisfy the given property. If the respective properties are
     * not yet available, the computation will be suspended until the properties of
     * the respective kind are available. Hence, it only makes sense to use this
     * function if the respective property is computed by an independent analysis or
     * if it is an inherent property of the analysis/analyses that the information about the
     * dependees is guaranteed to become available without requiring information
     * about the depender.
     *
     * This function eagerly tries to determine if the answer is false and only
     * suspends the computation if the (negative) answer cannot directly be computed.
     */
    // Locks of this.apply(...): Store, Entity
    def allHaveProperty[DependeeP <: Property](
        dependerE: Entity, dependerPK: SomePropertyKey,
        dependees:  Traversable[Entity],
        dependeePK: PropertyKey[DependeeP],
        expectedP:  DependeeP ⇒ Boolean
    )(
        c: (Boolean) ⇒ PropertyComputationResult
    ): PropertyComputationResult = {
        var remainingEs = dependees
        var unavailableEs: List[Entity] = Nil
        while (remainingEs.nonEmpty) {
            // The idea is to eagerly try to determine if the answer might be false.
            val dependeeE = remainingEs.head
            remainingEs = remainingEs.tail
            val p = this(dependeeE, dependeePK)
            p match {
                case Some(dependeeP) ⇒
                    if (!expectedP(dependeeP))
                        return c(false);
                case None ⇒
                    unavailableEs = dependeeE :: unavailableEs
            }
        }
        if (unavailableEs.isEmpty) {
            // all information was available and was always as expected
            return c(true);
        }

        // Let's wait on the next result and then try to get as many results as
        // possible, by using haveProperty again... i.e., we try to minimize the
        // number of suspended computations that we need to create.
        val deependeeE = unavailableEs.head
        new SuspendedPC[DependeeP](dependerE, dependerPK, deependeeE, dependeePK) {
            override def continue(dependeeP: DependeeP): PropertyComputationResult = {
                if (!expectedP(dependeeP))
                    return c(false);

                val remainingUnavailableEs = unavailableEs.tail
                if (remainingUnavailableEs.isEmpty) {
                    c(true)
                } else {
                    allHaveProperty(
                        dependerE, dependerPK,
                        remainingUnavailableEs, dependeePK, expectedP
                    )(
                        c
                    )
                }
            }
        }
    }

    /**
     * Returns an iterator of the different properties associated with the given element.
     *
     * This method is the preferred way to get all properties of an entity and should be used
     * if you know that all properties are already computed. Using this method will not
     * trigger the computation of a property.
     *
     * @note The returned iterator operates on a snapshot and will never throw any
     * 		`ConcurrentModificatonException`.
     *
     * @param e An entity stored in the property store.
     * @return `Iterator[Property]`
     */
    // Locks: Store, Entity
    def properties(e: Entity): List[Property] = {
        val eps = data.get(e)
        val l = eps.l
        val ps = eps.ps
        accessEntity {
            withReadLock(l) { (ps.values collect ComputedProperty).toList }
        }
    }

    /**
     * Returns all entities which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * While the view is computed all other computations are blocked.
     *
     * @note Lazy/direct property computations are not triggered.
     */
    // Locks: accessStore
    def entities[P <: Property](pk: PropertyKey[P]): Traversable[EP[Entity, P]] = {
        val pkId = pk.id
        accessStore {
            entries collect {
                case (e, PropertiesOfEntity(ps)) if !isPropertyUnavailable(ps(pkId)) ⇒
                    EP(e, ps(pkId).p.asInstanceOf[P])
            }
        }
    }

    /**
     * Directly associate the given property `p` with given entity `e`.
     *
     * This method must not be used if the given entity might already be associated with
     * a property of the respective kind or '''if there might be a (schedule) computation that
     * computes the property `p` for `e`''' (e.g., a lazy property computation).
     *
     * The primary use case is an analysis that does not use the property store for
     * executing the analysis, but wants to store some results in the store (and to use the
     * store's propagation mechanism.)
     */
    // Locks: accessEntity and this.update(...): Entity
    def set(e: Entity, p: Property): Unit = {
        val pkId = p.key.id
        val eps = data.get(e)
        val el = eps.l
        val ps = eps.ps
        accessEntity {
            withWriteLock(el) {
                val pos = ps(pkId)
                // Check that there is no property and no property is currently computed.
                if ((pos eq null) || (pos.p eq null))
                    // we do not have a property...
                    handleResult(ImmediateResult(e, p))
                else if (debug) logDebug(
                    "analysis progress",
                    s"did not set the property ${p} for $e, "+
                        s"because the entity already has a property or it is currently computed"
                )
            }
        }
    }

    /**
     * Stores the properties of the respective entities in the store if the respective property
     * is not yet associated with a property of the same kind. The properties are stored as
     * final values.
     */
    // Locks: set(Entity,Property): accessEntity, Entity(write), this.update(...): Entity(write)
    def set(ps: Traversable[SomeEP]): Unit = ps foreach { ep ⇒ set(ep.e, ep.p) }

    /**
     * Registers the function `f` that is called whenever an element `e` is associated with
     * a property of the respective kind (`pk`). For those elements that are already associated
     * with a respective property `p`,  `f` will immediately be scheduled
     * (i.e., `f` will not be executed immediately.)
     */
    // Locks: Store (Exclusive) and scheduleTask
    def onPropertyChange[P <: Property](pk: PropertyKey[P])(f: (Entity, P) ⇒ Unit): Unit = {
        val pkId = pk.id
        val pf = f.asInstanceOf[(Entity, Property) ⇒ Unit]
        accessStore {
            // We need exclusive access to make sure that `f` is called exactly once for
            // each entity/property pair.

            theOnPropertyComputations(pkId) =
                // AWFULL (BUT UNAVOIDABLE) HACK:
                // The following cast would always fail if the JVM would consider generic types
                // (which it does not). However, it needs to be done to store functions that take
                // different properties as their parameters in a generic store
                pf :: theOnPropertyComputations.getOrElse(pkId, Nil)

            // call `f` for all entities with a respective property
            entities(pk) foreach { ep ⇒ scheduleRunnable { f(ep.e, ep.p) } }
        }
    }

    /**
     * Executes `f` in parallel for each group of entities which is grouped by the function
     * `groupBy`. The results are then stored in the store using `set`.
     *
     * @param groupBy A function that associates every entity E that is selected by the given
     *                entitySelector function with a specific group. If all entities
     *                are associated with the same group key then `f` will not be
     *                executed in parallel.
     * @param f The analysis.
     * @tparam GK The group key.
     * @tparam E The type of the entities that will be analyzed/passed to `f`.
     */
    def execute[GK, E <: Entity](
        entitySelector: PartialFunction[Entity, E],
        groupBy:        E ⇒ GK
    )(
        f: (GK, Seq[E]) ⇒ Traversable[SomeEP]
    ): Unit = {
        val groupedEntities = keysList.view.collect(entitySelector).groupBy(groupBy)
        for ((key, entities) ← groupedEntities if !Tasks.isInterrupted) {
            scheduleRunnable { set(f(key, entities)) }
        }
    }

    /**
     * Executes the given function `f` in parallel for all entities in the store.
     * `f` is allowed to derive any properties related to any other entity found in the store.
     * However, if `f` derives a property `pNew` of property kind `pk` and the respective entity
     * `e` already has a property `pOld` of property kind pk, then the new property will be ignored.
     *
     * The function may also access the store to query '''other properties'''; however, this should
     * in general only be done after all previously scheduled computations - that
     * compute any properties of interest - have finished
     * (cf. [[waitOnPropertyComputationCompletion]]) or are guaranteed to be computed using
     * direct property computations.
     *
     * This function cannot be used to register a function that (bi-directionally) interacts with
     * other analyses.
     *
     * @param entitySelector A partial function that selects the entities of interest.
     * @param f The function that computes the respective property.
     */
    def execute[E >: Null <: Entity](
        entitySelector: PartialFunction[Entity, E]
    )(
        f: (E) ⇒ Traversable[SomeEP]
    ): Unit = {
        val remainingEntitiesMutex = new Object
        // we use the remaining entities as a worklist
        @volatile var remainingEntities = keysList
        var i = 0
        // We use exactly ThreadCount number of threads that process all entities
        val max = ThreadCount
        while (i < max) {
            i += 1
            scheduleRunnable {
                while (!Tasks.isInterrupted && remainingEntities.nonEmpty) {
                    val nextEntity: E = remainingEntitiesMutex.synchronized {
                        if (remainingEntities.nonEmpty) {
                            val nextEntity = remainingEntities.head
                            remainingEntities = remainingEntities.tail
                            if (entitySelector.isDefinedAt(nextEntity))
                                entitySelector(nextEntity)
                            else
                                null
                        } else
                            null
                    }
                    if (nextEntity ne null) {
                        val results = f(nextEntity)
                        set(results)
                    }
                }
            }
        }
    }

    /**
     * Registers a direct property computation (dpc) function that is executed in the caller's
     * thread when the property is requested for the first time. After that the computed value
     * is cached and returned the next time the property is requested.
     *
     * I.e., compared to a lazy computation the caller can always immediately get the final
     * result and the dpc function just computes a `Property`. However, an dpc has to satisfy
     * the following constraints:
     *  - a dpc may depend on other properties that are computed
     *    using dpcs if and only if the other properties are guaranteed to never have a direct or
     *    indirect dependency on the computed property. (This in particular excludes cyclic
     *    property dependencies. However, hierarchical property dependencies are supported. For
     *    example, if the computation of property for a specific class is done using a dpc that
     *    requires only information about the subclasses (or the superclasses, but not both at the
     *    same time) then it is possible to use a dpc.
     *    (A dpc may use all properties that are fully computed before the computation is registered.)
     *  - the computation must not create dependencies (i.e., an ImmediateResult)
     *
     * @note In general, using dpcs is most useful for analyses that have no notion of more/less
     * 		precise/sound. In this case client's of properties computed using dpcs can query the
     * 		store and will get the answer; i.e., a client that wants to know the property `P`
     * 		of an entity `e` with property key `pk` computed using a dpc can write:
     * 		{{{
     *  	val ps : PropertyStore = ...
     *  	ps(e,pk).get
     * 		}}}
     */
    def <<![P <: Property](pk: PropertyKey[P], dpc: (Entity) ⇒ Property): Unit = accessStore {
        /* The framework has to handle:
         *  1. the situation that the same dpc is potentially triggered by multiple other analyses
         *     concurrently
         *
         * The framework does not have to handle the following two situations because direct
         * property computations may not depend on properties of the same kind.
         *  1. a dpc may require the calculation of a dpc that leads to a cycle
         *  2. two or more dpcs may depend on each other:
         *  	t1:	o → o1 → o2
         *                 ↙︎ ↑
         *      t2:	o → o3 → o4
         *      t1 and t2 are two threads that run concurrently; an arrow means that
         *      Now: if o2 depends on o3 to finish, but o4 is currently running then o2 will block
         *      	 but if now o4 requires the property computed by o2 it also needs to wait.
         *      	 Hence, we have a deadlock.
         */
        theDirectPropertyComputations(pk.id) = dpc
    }

    /**
     * Registers a function that lazily computes a property for an element
     * of the store if the property of the respective kind is requested.
     * Hence, a first request of such a property will always first return the result "None".
     *
     * The computation is triggered by a(n in)direct call of this store's `apply` method. I.e.,
     * the allHaveProperty and the apply mehod will trigger the computation if necessary.
     * The methods
     *
     * This store ensures that the property computation function `pc` is never invoked more
     * than once for the same element at the same time. If `pc` is invoked again for a specific
     * element then only because a dependee has changed!
     */
    def <<?[P <: Property](pk: PropertyKey[P], pc: SomePropertyComputation): Unit = accessStore {
        theLazyPropertyComputations(pk.id) = pc
    }

    /**
     * Registers a function that calculates a property for all or some elements
     * of the store.
     *
     * This store ensures that the property
     * computation function `pc` is never invoked more than once for the
     * same element at the same time. If `pc` is invoked again for a specific element
     * then only because a dependee has changed!
     */
    def <<(pc: SomePropertyComputation): Unit = bulkScheduleComputations(keysList, pc)

    /**
     * Registers a function that calculates a property for those elements
     * of the store that pass the filter `f`.
     *
     * The filter is evaluated as part of this method; i.e., the calling thread.
     *
     * @param f A filter that selects those entities that are relevant to the analysis.
     *      For which the analysis may compute some property.
     *      The filter function is performed in the context of the calling thread.
     */
    def <|<(f: Entity ⇒ Boolean, c: SomePropertyComputation): Unit = {
        val it = keys.iterator()
        var es: List[Entity] = Nil
        while (it.hasNext) {
            if (isInterrupted())
                return ;
            val e = it.next()
            if (f(e)) es = e :: es
        }
        bulkScheduleComputations(es, c)
    }

    /**
     * Registers a function `c` that calculates a property for those elements
     * of the store that are collected by the given partial function `pf`.
     *
     * The partial function is evaluated for all entities as part of this
     * method; i.e., the calling thread.
     *
     * @param pf A a partial function that is used to collect those elements that will be
     *      passed to the function`c` and for which the analysis may compute some property.
     *      The function pf is performed in the context of the calling thread.
     */
    def <||<[E <: Entity](pf: PartialFunction[Entity, E], c: PropertyComputation[E]): Unit = {
        val es = keysList.collect(pf)
        bulkScheduleComputations(es, c.asInstanceOf[Entity ⇒ PropertyComputationResult])
    }

    def <|<<[E <: Entity](es: Traversable[E], c: PropertyComputation[E]): Unit = {
        bulkScheduleComputations(es, c.asInstanceOf[Entity ⇒ PropertyComputationResult])
    }

    /**
     * Awaits the completion of the computation of all
     * properties of all previously registered property computation functions. I.e.,
     * if a second thread is used to register [[PropertyComputation]] functions then
     * no guarantees are given. In general it is recommended to schedule all
     * property computation functions using one thread.
     *
     * This function is only '''guaranteed''' to wait on the completion of the computation
     * of those properties for which a property computation function was registered by
     * the calling thread.
     */
    def waitOnPropertyComputationCompletion(
        useDefaultForIncomputableProperties: Boolean = true
    ): Unit = {
        Tasks.waitOnCompletion(useDefaultForIncomputableProperties)
    }

    /**
     * The set of all entities which have a property that passes the given filter.
     *
     * This is a blocking operation; the returned set is independent of the store.
     *
     * @note This method will not trigger lazy/direct property computations.
     */
    def entities(propertyFilter: Property ⇒ Boolean): Traversable[Entity] = {
        accessStore {
            for {
                (e, eps) ← entries
                if eps.ps.values.exists { pos ⇒
                    val p = pos.p
                    (p ne null) && !p.isBeingComputed && propertyFilter(p)
                }
            } yield {
                e
            }
        }
    }

    /**
     * The set of all entities which have a property that passes the given filter.
     *
     * This is a blocking operation; the returned set is independent of the store.
     *
     * @note This method will not trigger lazy property computations.
     */
    def collect[T](collect: PartialFunction[(Entity, Property), T]): Traversable[T] = {
        accessStore {
            for {
                (e, eps) ← entries
                ps = eps.ps
                pos ← ps.values
                p = pos.p
                if !p.isBeingComputed
                ep /*: (Entity, Property)*/ = (e, p)
                if collect.isDefinedAt(ep)
            } yield {
                collect(ep)
            }
        }
    }

    // =============================================================================================
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    val ThreadCount = Math.max(NumberOfThreadsForCPUBoundTasks, 2)
    private[this] final val threadPool = ThreadPoolN(ThreadCount)

    /**
     * @return `true` if the pool is shutdown. In this case it is no longer possible to submit
     * 		new computations.
     */
    def isShutdown(): Boolean = threadPool.isShutdown()

    /**
     * General handling of the tasks that are executed.
     */
    private[this] object Tasks {

        @volatile var useFallbackForIncomputableProperties: Boolean = false

        @volatile private[PropertyStore] var isInterrupted: Boolean = false

        // ALL ACCESSES TO "executed" and "scheduled" ARE SYNCHRONIZED
        @volatile private[this] var executed: Int = 0

        /**
         * The number of scheduled tasks. I.e., the number of tasks that are running or
         * that will run in the future.
         */
        @volatile private[this] var scheduled: Int = 0

        private[this] var cleanUpRequired = false

        private[PropertyStore] def executedComputations: Int = executed

        private[PropertyStore] def scheduledComputations: Int = scheduled

        private[PropertyStore] def reset(): Unit = {
            if (isInterrupted || isShutdown)
                throw new InterruptedException();

            this.synchronized {
                if (scheduled > 0)
                    throw new IllegalStateException("computations are still running");

                useFallbackForIncomputableProperties = false
                executed = 0
                cleanUpRequired = false
            }
        }

        /**
         * Terminates all scheduled but not executing computations and afterwards
         * deregisters all observers.
         */
        private[PropertyStore] def interrupt(): Unit = {

            if (isInterrupted)
                return ;

            this.synchronized {
                // double-checked locking idiom...
                if (isInterrupted)
                    return ;

                isInterrupted = true
                if (debug) logDebug("analysis progress", "cancelling scheduled computations")
                val waitingTasks = threadPool.shutdownNow()
                tasksAborted(waitingTasks.size)
            }

            def clearAllObservers(): Unit = {
                // We iterate over all entities and remove all related observers
                // to help to make sure that the computation can finish in due time.
                threadPool.awaitTermination(5000l, TimeUnit.MILLISECONDS)

                if (debug) logDebug("analysis progress", "garbage collecting property computations")
                accessStore {
                    // 1) clear the list of outgoing observers
                    store.observers.clear()

                    // 2) clear the list of incoming observers
                    for {
                        eps ← entitiesProperties
                        ps = eps.ps
                        (pos, pkId) ← ps.values.zipWithIndex // the property p may (still be) null
                        os = pos.os
                        if os ne null // if the property is final the observers are already cleared
                    } {
                        ps(pkId) = new PropertyAndObservers(pos.p, null) // clear the observers but keep the entity
                    }
                }
            }

            // Invoke the garbage collection either in this thread if this thread
            // is not a thread belonging to the property store's thread pool or
            // in a new thread.
            if (threadPool.group == Thread.currentThread().getThreadGroup) {
                new Thread(new Runnable { def run(): Unit = clearAllObservers() }).start()
            } else {
                clearAllObservers()
            }
        }

        def taskStarted() = this.synchronized {
            scheduled += 1
            cleanUpRequired = true
        }

        def tasksStarted(tasksCount: Int) = this.synchronized {
            scheduled += tasksCount
        }

        def tasksAborted(tasksCount: Int) = this.synchronized {
            scheduled -= tasksCount
        }

        // Locks: Tasks
        //        Store(exclusive access), Tasks, handleUnsatisfiedDependencies: Store (access), Entity and scheduleContinuation: Tasks
        def taskCompleted() = {
            assert(scheduled > 0)

            this.synchronized {
                scheduled -= 1
                executed += 1
            }

            // When all scheduled tasks are completed, we check if there are
            // pending computations that now can be activated.
            if (scheduled == 0) accessStore {
                this.synchronized {
                    assert(validate(None), s"the property store is inconsistent")

                    if (scheduled == 0 && cleanUpRequired) {
                        cleanUpRequired = false
                        // Let's check if we have some potentially refineable intermediate results.
                        if (debug) logDebug(
                            "analysis progress", s"all $executed scheduled tasks have finished"
                        )

                        try {
                            if (!isInterrupted) {
                                if (debug) logDebug(
                                    "analysis progress", s"handling unsatisfied dependencies"
                                )
                                handleUnsatisfiedDependencies()
                            }
                        } catch {
                            case t: Throwable ⇒
                                val isValid =
                                    try {
                                        validate(None)
                                    } catch {
                                        case ae: AssertionError ⇒
                                            logError(
                                                "analysis progress",
                                                "the property store is inconsistent",
                                                ae
                                            )
                                            false
                                    }
                                logError(
                                    "analysis progress",
                                    "handling unsatisfied dependencies failed "+
                                        s"${if (isValid) "(store is valid)" else ""}; "+
                                        "aborting analyses",
                                    t
                                )
                                interrupt()
                                notifyAll()
                        }

                        if (scheduled == 0 /*scheduled is still === 0*/ ) {
                            if (debug) {
                                def registeredObservers: Int = {
                                    val pss = entitiesProperties.map(_.ps)
                                    val poss = pss.map(_.values).flatten
                                    poss.map { pos ⇒
                                        val os = pos.os
                                        if (os eq null) 0 else os.count(_ ne null)
                                    }.sum
                                }

                                logDebug(
                                    "analysis progress",
                                    "computation of all properties finished"+
                                        s" (remaining observers: $registeredObservers)"
                                )
                            }
                            notifyAll()
                        } else {
                            if (debug) logDebug(
                                "analysis progress", s"(re)scheduled $scheduled property computations"
                            )
                        }
                    }
                }
            }
        }

        // THIS METHOD REQUIRES EXCLUSIVE ACCESS TO THE STORE!
        // Handle unsatisfied dependencies supports both cases:
        //  1. computations that are part of a cyclic computation dependency
        //  1. computations that depend on knowledge related to a specific kind of
        //     property that was not computed (final lack of knowledge) and for
        //     which no computation exits.
        //Locks: handleResult: Store (access), Entity and scheduleContinuation: Tasks
        private[this] def handleUnsatisfiedDependencies(): Unit = {

            /*
      		 * Returns the list of observers related to the given entity and property kind.
       		 * I.e., the list of observers on those elements that are needed to compute the
       		 * given property kind.
       		 */
            def getDependeeObservers(e: Entity, pkId: Int): Observers = {
                val eps = data.get(e)
                if (eps eq null)
                    return null; // <= the entity is unknown

                val ps = eps.ps
                val pos = ps(pkId)
                if (pos eq null)
                    null
                else
                    pos.os
            }

            val observers = store.observers

            val indirectlyIncomputableEPKs = HSet.empty[SomeEPK]

            // All those EPKs that require some information that do not depend (directly
            // or indirectly) on an incomputableEPK.
            val cyclicComputableEPKCandidates = HSet.empty[SomeEPK]

            val directlyIncomputableEPKs = HSet.empty[SomeEPK]

            /*
             * @param epks The set of EPKs which have a dependency on dependerEPK.
             * @return Those epks that are newly added to set epks. If epks is initially empty
             * 		the returned list and the given set epks contain the same elements.
             */
            def determineDependentIncomputableEPKs(
                dependerEPK: SomeEPK,
                epks:        HSet[SomeEPK]
            ): List[SomeEPK] = {
                var newDependentEPKs = List.empty[SomeEPK]
                if (epks.add(dependerEPK)) {
                    // make sure that the start epk is in the list...
                    newDependentEPKs = dependerEPK :: newDependentEPKs
                }
                var worklist: List[SomeEPK] = List(dependerEPK)
                while (worklist.nonEmpty) {
                    val dependerEPK = worklist.head
                    worklist = worklist.tail
                    val pos = data.get(dependerEPK.e).ps(dependerEPK.pk.id)
                    if ((pos ne null) && (pos.os ne null)) {
                        val os = pos.os
                        os foreach { o ⇒
                            val dependerEPK = o.dependerEPK
                            if (epks.add(dependerEPK)) {
                                newDependentEPKs = dependerEPK :: newDependentEPKs
                                worklist = dependerEPK :: worklist
                            }
                        }
                    }
                }
                newDependentEPKs
            }

            /* Let's determine all EPKs that have a dependency on an incomputableEPK
             * (They may be in a strongly connected component, but we don't care about
             * these, because they may still be subject to some refinement.)
             */
            def determineIncomputableEPKs(dependerEPK: SomeEPK): Unit = {
                cyclicComputableEPKCandidates --=
                    determineDependentIncomputableEPKs(dependerEPK, indirectlyIncomputableEPKs)
            }

            observers.entrySet().asScala foreach { e ⇒
                val dependerEPK = e.getKey
                if (!indirectlyIncomputableEPKs.contains(dependerEPK)) {
                    val dependees = e.getValue
                    dependees foreach { dependee ⇒
                        val dependeeEPK = dependee._1
                        if (!observers.containsKey(dependeeEPK)) {
                            directlyIncomputableEPKs += dependeeEPK
                            assert(
                                data.get(dependeeEPK.e).ps(dependeeEPK.pk.id).p eq null,
                                s"property propagation failed $dependeeEPK has a property("+
                                    s"${data.get(dependeeEPK.e).ps(dependeeEPK.pk.id).p}"+
                                    s"), but $dependerEPK was not notified"
                            )
                            indirectlyIncomputableEPKs += dependerEPK
                            determineIncomputableEPKs(dependerEPK)
                        } else {
                            // this EPK observes EPKs that have observers...
                            // but, is it also observed?
                            val observers = getDependeeObservers(dependerEPK.e, dependerEPK.pk.id)
                            if (observers ne null) {
                                cyclicComputableEPKCandidates += dependerEPK
                            }
                        }
                    }
                }
            }

            val epkSuccessors: (SomeEPK) ⇒ Traversable[SomeEPK] = (epk: SomeEPK) ⇒ {
                val observers = store.observers.get(epk)
                assert(
                    observers ne null,
                    s"$epk has no observers!\n"+
                        s"\tcyclicComputableEPKCandidates="+
                        {
                            cyclicComputableEPKCandidates.
                                map(c ⇒ store.observers.get(c).map(_._1)).
                                mkString("", "->", "\n")
                        } +
                        s"\tdirectlyIncomputableEPKs=$directlyIncomputableEPKs\n"+
                        s"\tindirectlyIncomputableEPKs=$indirectlyIncomputableEPKs"
                )
                observers.view.map(_._1)
            }
            val cSCCs: List[Iterable[SomeEPK]] = org.opalj.graphs.closedSCCs[SomeEPK](
                cyclicComputableEPKCandidates, epkSuccessors
            )
            if (debug && cSCCs.nonEmpty) logDebug(
                "analysis progress",
                cSCCs.
                    map(_.mkString("", " → ", " ↺")).
                    mkString("found the following cyclic computations:\n\t", "\n\t", "\n")
            )
            for (cSCC ← cSCCs) {
                val results = PropertyKey.resolveCycle(store, cSCC)
                if (results.nonEmpty) {
                    for (result ← results) {
                        handleResult(result)
                    }
                } else {
                    if (debug) {
                        val infoMessage = s"\tresolution produced no results; removing observers\n\t"
                        logInfo("analysis progress", infoMessage)
                    }
                    for (epk ← cSCC) {
                        clearAllDependeeObservers(epk)
                    }
                }
            }

            // Let's get the set of observers that will never be notified, because
            // there are no open computations related to the respective property.
            // This is also the case if no respective analysis is registered so far.
            if (directlyIncomputableEPKs.nonEmpty && useFallbackForIncomputableProperties) {
                if (debug) logDebug(
                    "analysis progress",
                    s"fallback is used for ${directlyIncomputableEPKs.size} entities: "+
                        directlyIncomputableEPKs.mkString(",")
                )
                for {
                    EPK(e, pk) ← directlyIncomputableEPKs
                } {
                    assert(
                        data.get(e).ps(pk.id).p eq null,
                        s"the entity $e already has a property ${data.get(e).ps(pk.id).p}($pk)"
                    )
                    val defaultP = PropertyKey.fallbackProperty(store, e, pk)
                    scheduleHandleFallbackResult(e, defaultP)
                }
                if (debug) logDebug(
                    "analysis progress", "created all tasks for setting the fallback properties"
                )
            }
            assert(
                validate(None),
                s"the property store is inconsistent after handling unsatisfied dependencies"
            )
        }

        // Locks: Tasks
        def waitOnCompletion(useFallbackForIncomputableProperties: Boolean): Unit = this.synchronized {
            this.useFallbackForIncomputableProperties = useFallbackForIncomputableProperties
            //noinspection LoopVariableNotUpdated
            while (scheduled > 0) {
                if (debug) logDebug("analysis progress", s"remaining tasks: $scheduled")
                wait()
            }
        }
    }

    /**
     * Schedules the handling of the result of a property computation.
     */
    // Locks of scheduleRunnable: Tasks
    private[this] def scheduleHandleFallbackResult(e: Entity, p: Property): Unit = {
        scheduleRunnable {
            handleResult(FallbackResult(e, p))
        }
    }

    /**
     * Schedules the continuation w.r.t. the entity `e`.
     */
    // Locks of scheduleRunnable: Tasks
    private[this] def scheduleContinuation(
        dependeeE:  Entity,
        dependeeP:  Property,
        updateType: UserUpdateType,
        c:          OnUpdateContinuation
    ): Unit = {
        scheduleRunnable { handleResult(c(dependeeE, dependeeP, updateType)) }
    }

    /**
     * Schedules the computation of a property w.r.t. the list of entities `es`.
     */
    // Locks of scheduleComputation: Tasks
    private[this] def bulkScheduleComputations(
        es: Traversable[_ <: Entity],
        pc: SomePropertyComputation
    ): Unit = {
        es foreach { e ⇒ if (!isInterrupted()) scheduleComputation(e, pc) }
    }

    // requires: TASKS lock
    private[this] final def scheduleFforE[E <: Entity](e: E, f: (E) ⇒ Unit): Unit = {
        scheduleRunnable { f(e) }
    }

    /**
     * Schedules the computation of a property w.r.t. the entity `e`.
     */
    // Locks of scheduleRunnable: Tasks
    private[this] def scheduleComputation(e: Entity, pc: SomePropertyComputation): Unit = {
        scheduleRunnable { handleResult(pc.asInstanceOf[Entity ⇒ PropertyComputationResult](e)) }
    }

    // Locks of scheduleTask: Tasks
    private[this] def scheduleRunnable(f: ⇒ Unit): Unit = {
        scheduleTask(new Runnable {
            override def run(): Unit = {
                try {
                    if (!isInterrupted())
                        f
                } catch {
                    case t: Throwable ⇒ handleUncaughtException(Thread.currentThread(), t)
                } finally {
                    Tasks.taskCompleted()
                }
            }
        })
    }

    /**
     * The core method that actually submits runnables to the thread pool.
     */
    // Locks: Tasks
    private[this] def scheduleTask(r: Runnable): Unit = {
        import Tasks.{taskStarted, taskCompleted}
        if (isInterrupted()) {
            Tasks.interrupt()
            return ;
        }
        taskStarted()
        try {
            threadPool.submit(r)
        } catch {
            // (Exceptions thrown by "r" do not end up here.)
            // here, we just handle threadpool exceptions
            case _: RejectedExecutionException ⇒
                taskCompleted()
            case t: Throwable ⇒
                try { handleUncaughtException(t) } finally { taskCompleted() }
        }
    }

    /**
     * Clears all observers that were registered with other entities to compute the
     * respective property of the given entity.
     * This method handles the situation where the computation of a property
     * potentially depended on some other entities and we now have a final result
     * and now need to cleanup the registered observers.
     *
     * @return `true` if some observers were removed.
     */
    // Locks: Entity (write)
    private[this] def clearAllDependeeObservers(dependerEPK: SomeEPK): Boolean = {
        // observers : JCHMap[EPK, Buffer[(EPK, PropertyObserver)]]()
        val observers = store.observers
        val dependerOs = observers.remove(dependerEPK) // outgoing ones...
        if ((dependerOs eq null) || dependerOs.isEmpty)
            return false;

        // dependerOs maybe empty if we had intermediate results so far...
        dependerOs foreach { epkos ⇒
            val (dependeeEPK, epkO) = epkos
            val eps = data.get(dependeeEPK.e)
            val dependeePs = eps.ps
            val dependeePkId = dependeeEPK.pk.id
            withWriteLock(eps.l) {
                val dependeeOs = dependeePs(dependeePkId).os
                if (dependeeOs ne null) {
                    dependeeOs -= epkO
                    // dependeeOs may be empty now, but - given that dependeeP is not final -
                    // this is perfectly ok; we generally only null out values if
                    // a final property is derived
                }
            }
        }
        true
    }

    /**
     * Processes the result.
     */
    // Locks: Store (access), Entity and scheduleContinuation: Tasks
    def handleResult(r: PropertyComputationResult): Unit = {
        handleResult(r, false)
    }

    /**
     * Processes the result.
     *
     * @param forceDependerNotification `true` if during the processing of a result (e.g., an
     * 		[[IntermediateResult]]) the property store determines that a value has changed
     * 		and immediately continues (not reschedules!) the execution of the analysis.
     */
    private[this] def handleResult(
        r:                         PropertyComputationResult,
        forceDependerNotification: Boolean
    ): Unit = {

        // Locks: Entity
        def registerObserverWithItsDepender(
            dependerEPK: SomeEPK,
            dependeeEPK: SomeEPK,
            o:           PropertyObserver
        ): Unit = {
            val observers = store.observers
            withWriteLock(data.get(dependerEPK.e).l) {
                var buffer = observers.get(dependerEPK)
                if (buffer eq null) {
                    buffer = Buffer.empty
                    observers.put(dependerEPK, buffer)
                }
                buffer += ((dependeeEPK, o))
            }
        }

        /*
     	 * Associates / Updates the property with element e. If observers are registered
     	 * with the respective property then those observers will be informed about the
     	 * property change and the observers will be removed unless the new property
     	 * is the same as the old property and the updateType is intermediate update.
     	 */
        // Invariant: always only at most one function exists that will compute/update
        // the property p belonging to property kind k of an element e.
        //
        // All calls to update have to acquire either entity access (using "accessEntity")
        // or store wide access (using "accessStore")
        // Locks: Entity (write) (=> clearAllDependeeObservers)
        def update(e: Entity, p: Property, updateType: UpdateType): Unit = {
            assert(!p.isBeingComputed)
            val pk = p.key
            val pkId = pk.id

            // update the property and schedule(!) all interested onPropertyComputations
            val onPropertyComputations = theOnPropertyComputations.getOrElse(pkId, Nil)

            val updateTypeId = updateType.id
            val eps = data.get(e)
            val ps = eps.ps

            val os = withWriteLock(eps.l) {
                // we first need to acquire the lock to avoid that a scheduled "on property change
                // computation" is run before the property is actually updated
                onPropertyComputations foreach { opc ⇒ scheduleRunnable { opc(e, p) } }

                val pos = ps(pkId)
                if (pos eq null) {
                    // No one was interested in this property so far...
                    (updateTypeId: @scala.annotation.switch) match {
                        case OneStepFinalUpdate.id | FinalUpdate.id ⇒
                            // Note that it is possible to have a Final Update though the underlying
                            // property is refineable. This is the case whenever the analysis knows
                            // that no further refinement may happen (given the current program).
                            ps(pkId) = new PropertyAndObservers(p, null)

                        case IntermediateUpdate.id ⇒
                            assert(p.isRefineable, s"$e: intermediate update of a final property $p")
                            ps(pkId) = new PropertyAndObservers(p, Buffer.empty)

                        case FallbackUpdate.id ⇒
                            val m = s"fallback property $p assigned to entity $e without dependers"
                            throw new UnknownError(m)

                    }
                    Nil // <=> we have no observers
                } else {
                    // USELESS INTERMEDIATE UPDATES CAN HAPPEN IF:
                    // a -> b and a -> c
                    // 1. b is updated such that a has a new property P
                    // 2. c is updated such that a has still the property P
                    // But given that c was triggered it need to reregister the listeners!

                    // We are either updating or setting a property or changing the state of the
                    // property => intermediate result => final result
                    val oldP = pos.p
                    var os: Seq[PropertyObserver] = pos.os

                    (updateTypeId: @scala.annotation.switch) match {
                        case OneStepFinalUpdate.id ⇒
                            assert(
                                (oldP eq null) || oldP.isBeingComputed || (oldP.isRefineable && (os ne null)),
                                s"$e: the old property $oldP is already a final property and refinement to $p is not supported"
                            )
                            assert(clearAllDependeeObservers(EPK(e, pk)) == false)
                            // The computation did not create any (still living) dependencies!
                            ps(pkId) = new PropertyAndObservers(p, null /* there will be no further observers */ )
                            if (PropertyIsDirectlyComputed(oldP)) os = Nil /* => there are no observers */
                            if (oldP.isInstanceOf[PropertyIsDirectlyComputed]) os = Nil /* => there are no observers */

                        case FinalUpdate.id ⇒
                            assert(
                                (oldP eq null) || oldP.isBeingComputed || (oldP.isRefineable && (os ne null)),
                                s"$e: the old property $oldP is already a final property and refinement to $p is not supported"
                            )
                            clearAllDependeeObservers(EPK(e, pk))
                            ps(pkId) = new PropertyAndObservers(p, null /* there will be no
                            further observers  */ )

                        case IntermediateUpdate.id ⇒
                            assert(
                                (oldP eq null) || oldP.isBeingComputed || (oldP.isRefineable && (os ne null)),
                                s"$e: impossible intermediate update of the old property $oldP with $p"
                            )
                            assert(
                                p.isRefineable,
                                s"$e: intermediate update using a final property $p"
                            )
                            if (oldP != p) {
                                ps(pkId) = new PropertyAndObservers(p, Buffer.empty)
                            } else if (!forceDependerNotification) {
                                //if (debug)
                                logDebug(
                                    "analysis progress",
                                    s"useless intermediate update $e($oldP): $p"
                                )
                                os = Nil // os will be non-null if we force depender notification
                            }

                        case FallbackUpdate.id ⇒
                            // Fallback updates are only used in case a property of an entity
                            // is required by a dependent computation and no more computation is
                            // running that could compute this property.
                            assert(p.isFinal, "fallback properties need to be final")
                            assert(
                                oldP eq null,
                                s"$e already has a property $oldP; no fallback required"
                            )
                            assert(
                                os ne null,
                                s"the fallback property $p for $e is not required"
                            )
                            assert(
                                clearAllDependeeObservers(EPK(e, pk)) == false,
                                s"assigning fallback property $p to $e which has unsatisfied dependencies"
                            )

                            if (debug) logDebug(
                                "analysis progress",
                                s"using default property $p for $e"
                            )

                            effectiveDefaultPropertiesCount.incrementAndGet()

                            // We may still observe other entities... we have to clear
                            // these dependencies (e.g., if this is a fallback update)
                            ps(pkId) = new PropertyAndObservers(p, null)
                    }
                    os
                }
            }
            if (os.nonEmpty) {
                // inform all (previously registered) observers about the value
                scheduleRunnable { os foreach { o ⇒ o(e, p, updateType) } }
            }
        }

        // Stores a property in the store if and only if the property was not computed before
        // or if there can be a future update.
        //
        // The whole purpose of this method is to store an intermediate property in the store, where
        // the store knows right away that a new property is currently computed!
        // (I.e. another result with respect to epk)
        //
        // All calls to store have to acquire either entity access (using "accessEntity")
        // or store wide access (using "accessStore") and also the entity's write lock
        // Locks: Entity (write) (=> clearAllDependeeObservers)
        def storeIntermediateProperty(e: Entity, p: Property): Unit = {
            assert(!p.isBeingComputed)
            assert(p.isRefineable, s"$e: the final property $p is stored as an intermediate one")

            val pkId = p.key.id
            val ps = data.get(e).ps

            if (debug) logDebug("analysis progress", s"storing intermediate property $e($p)")

            // implicitly locked: withWriteLock(eps.l) {
            val pos = ps(pkId)
            if (pos eq null) {
                ps(pkId) = new PropertyAndObservers(p, Buffer.empty)
            } else {
                val oldP = pos.p
                assert(
                    (oldP eq null) || oldP.isRefineable,
                    s"the old property $oldP is already a final property and refinement to $p is not supported"
                )
                val os = pos.os
                assert(
                    (os ne null),
                    s"$e is effectively final; the old property was ($oldP) and the new property is $p"
                )
                if (oldP != p) {
                    ps(pkId) = new PropertyAndObservers(p, os)
                }
            }
            // }
        }

        //
        // PROCESSING RESULTS
        //

        if (debug) logDebug("analysis progress", s"analysis result $r($forceDependerNotification)")
        val resultId = r.id
        accessEntity {
            (resultId: @scala.annotation.switch) match {

                case ImmediateResult.id ⇒
                    val ImmediateResult(e, p) = r
                    update(e, p, OneStepFinalUpdate)

                case ImmediateMultiResult.id ⇒
                    val ImmediateMultiResult(results) = r
                    results foreach { ep ⇒ update(ep.e, ep.p, OneStepFinalUpdate) }

                case Result.id ⇒
                    val Result(e, p) = r
                    update(e, p, FinalUpdate)

                case MultiResult.id ⇒
                    val MultiResult(results) = r
                    results foreach { ep ⇒ update(ep.e, ep.p, FinalUpdate) }

                case FallbackResult.id ⇒
                    val FallbackResult(e, p) = r
                    update(e, p, FallbackUpdate)

                case IncrementalResult.id ⇒
                    val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                    handleResult(ir)
                    npcs foreach { npc ⇒ val (pc, e) = npc; scheduleComputation(e, pc) }

                case IntermediateResult.id ⇒
                    val IntermediateResult(e, p, dependees: Traversable[SomeEOptionP], c) = r
                    assert(dependees.nonEmpty, s"the intermediate result $r has no dependencies")
                    assert(p.isRefineable, s"intermediate result $r used to store final property $p")

                    val accessedEntities = dependees.map(_.e) ++ Set(e) // make dependees a Seq
                    val boundC = withEntitiesWriteLocks(accessedEntities) {

                        val pk = p.key
                        val dependerEPK = EPK(e, pk)
                        // we use ONE observer to make sure that the continuation function
                        // is called at most once - independent of how many entities are
                        // actually observed
                        val o = new DependeePropertyObserver(dependerEPK, clearAllDependeeObservers) {
                            def propertyChanged(e: Entity, p: Property, u: UpdateType): Unit = {
                                propagationCount.incrementAndGet()
                                scheduleContinuation(e, p, u.asUserUpdateType, c)
                            }
                        }
                        var boundC: () ⇒ PropertyComputationResult = null
                        dependees exists /*where the property already has changed...*/ { eOptionP ⇒

                            val dependeeE = eOptionP.e
                            val dependeePK = eOptionP.pk
                            val dependeePKId = dependeePK.id
                            val dependeeEPK = EPK(dependeeE, dependeePK)
                            val dependeeEPs = data.get(dependeeE)
                            val dependeePs = dependeeEPs.ps
                            dependeePs(dependeePKId) match {
                                case null ⇒
                                    // => the dependee's property definitively has not changed
                                    dependeePs(dependeePKId) = new PropertyAndObservers(null, Buffer(o))
                                    registerObserverWithItsDepender(dependerEPK, dependeeEPK, o)
                                    false

                                case PropertyAndObservers(null, dependeeOs) ⇒
                                    // => the dependee's property definitively has not changed
                                    dependeeOs += o
                                    registerObserverWithItsDepender(dependerEPK, dependeeEPK, o)
                                    false

                                case PropertyAndObservers(currentDependeeP, dependeeOs) ⇒
                                    if (dependeeOs eq null) {
                                        // => the state of the property has changed => final
                                        boundC = new (() ⇒ PropertyComputationResult) {
                                            def apply(): PropertyComputationResult = {
                                                c(dependeeE, currentDependeeP, FinalUpdate)
                                            }
                                            override def toString: String = {
                                                val ep = EP(dependeeE, currentDependeeP)
                                                s"Continuation(dependeeEP=$ep,FinalUpdate)"
                                            }
                                        }
                                        true
                                    } else if (!eOptionP.hasProperty || eOptionP.p != currentDependeeP) {
                                        // => a/the property is now available or has changed!
                                        boundC = new (() ⇒ PropertyComputationResult) {
                                            def apply(): PropertyComputationResult = {
                                                c(dependeeE, currentDependeeP, IntermediateUpdate)
                                            }
                                            override def toString: String = {
                                                val ep = EP(dependeeE, currentDependeeP)
                                                s"Continuation(dependeeEP=$ep,IntermediateUpdate)"
                                            }
                                        }
                                        true
                                    } else {
                                        // nothing has changed compared to the time where
                                        // the entity dependeeE was queried
                                        registerObserverWithItsDepender(dependerEPK, dependeeEPK, o)
                                        dependeeOs += o
                                        false
                                    }
                            }
                        }
                        if (boundC ne null) {
                            // These two steps have to be done while we still hold all locks:
                            o.deregister()
                            storeIntermediateProperty(e, p)
                        } else {
                            update(e, p, IntermediateUpdate)
                        }
                        boundC
                    } // End of write lock for all written entities
                    if (boundC ne null) {
                        try {
                            val newResult = boundC()

                            assert(
                                newResult.id != IntermediateUpdate.id || {
                                    newResult.asInstanceOf[IntermediateResult].dependees.forall { newEP ⇒
                                        dependees.exists { oldEP ⇒
                                            (newEP.e eq oldEP.e) && (oldEP.p.isRefineable) &&
                                                (!oldEP.p.isInstanceOf[Ordered[_]] ||
                                                    oldEP.p.asInstanceOf[Ordered[Property]].compare(newEP.p) <= 0)
                                        }
                                    }
                                },
                                s"the new result $newResult referes to older results than the old result $r"
                            )

                            if (debug) logDebug(
                                "analysis progress",
                                s"immediately continued computation of $e(${p}) => \n\t$boundC\n"+
                                    s"\told and new result are equal: ${newResult == r} "+
                                    s"(forceDependerNotification=$forceDependerNotification)\n"+
                                    s"\told result: $r\n\tnew result: $newResult"
                            )
                            val newForceDependerNotification =
                                (forceDependerNotification || newResult != r)
                            handleResult(newResult, newForceDependerNotification)
                        } catch {
                            case soe: StackOverflowError ⇒
                                val message =
                                    s"the analysis which computed $e($p) failed miserably "+
                                        "with a StackOverflowError; possible reasons: "+
                                        "1. the computed properties do not implement the equals "+
                                        "method correctly (structural equality) or 2. the "+
                                        s"dependee information ${dependees.mkString(", ")} is not "+
                                        "correctly updated in each round"
                                logError("implementation error", message)
                                throw new Error(message, soe)
                        }
                    }

                case SuspendedPC.id ⇒
                    val suspended @ SuspendedPC(dependerE, dependerPK, dependeeE, dependeePK) = r
                    // CONCEPT
                    // First, let's get the property, then...
                    //  - If we now have a property, let's immediately continue
                    //    the computation.
                    //  - If the property is still not computed, register an
                    //    observer that will schedule the computation when the
                    //    property was computed.

                    def createAndRegisterObserver(): PropertyObserver = {
                        val dependerEPK = EPK(dependerE, dependerPK)
                        val dependeeEPK = EPK(dependeeE, dependeePK)
                        val o = new DependeePropertyObserver(dependerEPK, clearAllDependeeObservers) {
                            def propertyChanged(e: Entity, p: Property, u: UpdateType): Unit = {
                                propagationCount.incrementAndGet()
                                val suspendedPC = suspended.asInstanceOf[SuspendedPC[Property]]
                                val pc = (e: AnyRef) ⇒ suspendedPC.continue(p)
                                scheduleComputation(dependerE, pc)
                            }
                        }
                        registerObserverWithItsDepender(dependerEPK, dependeeEPK, o)
                        o
                    }

                    val eps = data.get(dependeeE)
                    val dependeeLock = eps.l
                    val dependeePS = eps.ps
                    val dependeePKId = dependeePK.id
                    val p = withWriteLock(dependeeLock) {
                        dependeePS(dependeePKId) match {
                            case null ⇒
                                // this computation is the first one which is interested
                                // in the property
                                val os = Buffer(createAndRegisterObserver())
                                dependeePS(dependeePKId) = new PropertyAndObservers(null, os)
                                null

                            case PropertyAndObservers(dependeeP, dependeeOs) ⇒
                                if ((dependeeP eq null) || dependeeP.isBeingComputed) {
                                    // we have other computations that are also waiting...
                                    dependeeOs += createAndRegisterObserver()
                                    null
                                } else {
                                    // the property was computed in the meantime...
                                    // but we don't want to call the continuation while we
                                    // still hold the lock on dependee
                                    if (debug) logDebug(
                                        "analysis progress",
                                        "immediately continued the suspended computation of "+
                                            s"$dependerE($dependerPK) using $dependeeE($dependeeP)"
                                    )
                                    dependeeP
                                }
                        }
                    }
                    if (p ne null) {
                        /* immediately exec*/
                        val suspendedPC = suspended.asInstanceOf[SuspendedPC[Property]]
                        handleResult(suspendedPC.continue(p))
                    }
            }
        }
    }

    private[this] def isPropertyUnavailable(pos: PropertyAndObservers): Boolean = {
        (pos eq null) || { val p = pos.p; (p eq null) || p.isBeingComputed }
    }
}

/**
 * Factory to create [[PropertyStore]]s.
 */
object PropertyStore {

    /**
     * Creates a new [[PropertyStore]] for the given set of entities.
     *
     * @param entities The entities which will be stored in the property store and with which it is
     *                 possible to associate properties. E.g., the set of all class files, methods
     *                 and fields of a program.
     * @param isInterrupted A function that is called by the framework to test if
     *          the running/scheduled computations should be aborted.
     *          It is important that this function is efficient as it is frequently called.
     * @param debug `true` if debug output should be generated.
     * @param logContext The [[org.opalj.log.LogContext]] that will be used for debug etc. messages.
     * @return The newly created [[PropertyStore]].
     */
    def apply(
        entities:      Traversable[Entity],
        isInterrupted: () ⇒ Boolean,
        debug:         Boolean
    )(
        implicit
        logContext: LogContext
    ): PropertyStore = {

        val entitiesCount = entities.size
        val data = new JIDMap[Entity, EntityProperties](entitiesCount)
        var entityId = 0
        entities foreach { e ⇒
            if (data.put(e, new EntityProperties(entityId)) ne null) {
                throw new IllegalArgumentException(
                    s"the list of entities contains duplicates; e.g. $e"
                )
            }
            entityId += 1
        }
        new PropertyStore(data, isInterrupted, debug)
    }

    /**
     * Creates an entity selector for a specific type of entities.
     */
    def entitySelector[T <: Entity: ClassTag](): PartialFunction[Entity, T] = {
        new PartialFunction[Entity, T] {
            def apply(v1: Entity): T = {
                if (isDefinedAt(v1))
                    v1.asInstanceOf[T]
                else
                    throw new IllegalArgumentException
            }

            def isDefinedAt(x: Entity): Boolean = {
                val ct = implicitly[ClassTag[T]]
                x.getClass.isInstance(ct.runtimeClass)
            }
        }
    }

}

//--------------------------------------------------------------------------------------------------
//
// HELPER DATA STRUCTURES TO FACILITATE COMPREHENSION OF THE PROPERTYSTORE'S IMPLEMENTATION
//
//--------------------------------------------------------------------------------------------------

private[fpcf] final class PropertyAndObservers(
    final val p:  Property  = null,
    final val os: Observers = null
)

private[fpcf] object PropertyAndObservers {
    def unapply(pos: PropertyAndObservers): Option[(Property, Observers)] = {
        if (pos eq null)
            None
        else
            Some((pos.p, pos.os))
    }
}

/**
 * A partial function that can be used to collect all properties that are computed.
 */
private[fpcf] object ComputedProperty extends PartialFunction[PropertyAndObservers, Property] {

    def isDefinedAt(pos: PropertyAndObservers): Boolean = {
        (pos ne null) && {
            val p = pos.p
            (p ne null) && !p.isBeingComputed
        }
    }

    def apply(pos: PropertyAndObservers): Property = pos.p
}

/**
 * @param id A globally unique id that is used to sort entities to acquire locks related to
 * 			multiple entities in a globally consistent order.
 * @param ps A mutable map of the entities properties; the key is the id of the property's kind.
 */
private[fpcf] final class EntityProperties(
        final val id: Int,
        final val l:  ReentrantReadWriteLock,
        final val ps: Properties
) {
    def this(id: Int) {
        this(id, new ReentrantReadWriteLock, ArrayMap.empty)
    }
}

private[fpcf] object PropertiesOfEntity {

    def unapply(eps: EntityProperties): Some[Properties] = Some(eps.ps)

}
