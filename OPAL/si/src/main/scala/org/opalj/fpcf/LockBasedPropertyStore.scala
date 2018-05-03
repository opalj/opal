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

import java.util.{HashMap ⇒ DataMap}
import java.util.{Set ⇒ JSet}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.{ConcurrentHashMap ⇒ JCHMap}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.Type
import scala.collection.mutable
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{HashSet ⇒ HSet}
import scala.collection.mutable.{ListBuffer ⇒ Buffer}
import scala.collection.JavaConverters._

import net.ceedubs.ficus.Ficus._

import org.opalj.graphs.closedSCCs
import org.opalj.io.writeAndOpen
import org.opalj.collection.mutable.ArrayMap
import org.opalj.concurrent.Locking.withReadLock
import org.opalj.concurrent.Locking.withWriteLock
import org.opalj.concurrent.Locking.withWriteLocks
import org.opalj.concurrent.ThreadPoolN
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.OPALLogger.{info ⇒ logInfo}
import org.opalj.log.OPALLogger.{debug ⇒ logDebug}
import org.opalj.log.OPALLogger.{error ⇒ logError}
import org.opalj.log.OPALLogger.{warn ⇒ logWarn}
import org.opalj.log.{LogContext, OPALLogger}
import org.opalj.graphs.DefaultMutableNode
import org.opalj.collection.UID
import org.opalj.util.AnyToAnyThis

/**
 * See [[PropertyStore]] for a general description.
 *
 * ==Multi-Threading==
 * The PropertyStore uses its own fixed size ThreadPool with at most
 * [[org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks]] threads.
 *
 * @author Michael Eichberg
 */
class LockBasedPropertyStore private (
        // type Observers = mutable.ListBuffer[PropertyObserver]
        // class PropertyAndObservers(p: Property, os: Observers)
        // type Properties = OArrayMap[PropertyAndObservers] // the content of the array may be updated
        // class EntityProperties(l: ReentrantReadWriteLock, ps: Properties) // the references are never updated
        private[this] val data:     DataMap[Entity, EntityProperties],
        final val ctx:              Map[Type, AnyRef],
        final val ParallelismLevel: Int
)(
        implicit
        val logContext: LogContext
) extends PropertyStore { store ⇒
    /*
     * The LockBasedProperStore prevents deadlocks by ensuring that updates of the store are always
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
     *  [4.] the specific ENTITY (read/write) related lock (the entity lock must only be acquired
     *       when the store lock (accessEntity/accessStore) is held.
     *       If multiple locks are required at the same time, then all locks are acquired
     *       in the order of the entity id; i.e., we are using a globally consistent order.
     *  [5.] the global TASKS related lock (<Tasks>.synchronized)
     */
    // COMMON ABBREVIATONS USED IN THE FOLLOWING:
    // ==========================================
    // e =         Entity
    // l =         Entity Lock (associated with an entity)
    // p =         Property
    // ps =        Properties (the properties of an entity)
    // eps =       EntityProperties (the pairing of an entity's lock and its properties)
    // pk =        Property Key
    // pc =        Property Computation
    // lpc =       Lazy Property Computation
    // c =         Continuation (The rest of a computation if a specific, dependent property was computed.)
    // (p)o =      PropertyObserver
    // os =        PropertyObservers
    // pos =       PropertyAndObservers
    // EPK =       Entity and a PropertyKey
    // EP =        Entity and an associated Property
    // EOptionP =  Entity and either a PropertyKey or (if available) a Property

    private[this] final val ValidateConsistency = LockBasedPropertyStore.ValidateConsistency

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

    private[this] final val entitiesProperties: Traversable[EntityProperties] = {
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
     * @param sortedEntityProperties Conceptually a set of entities for which we will
     *      acquire the locks in order of the locks' ids.
     */
    //    @inline final private[this] def withEntitiesWriteLocks[T](epss: List[EntityProperties])(f: ⇒ T): T = {
    //        val sortedEntities = epss.sortWith((e1, e2) ⇒ e1.id < e2.id)
    //        val entityLocks = sortedEntities.view.map(e ⇒ e.l)
    //        withWriteLocks(entityLocks)(f)
    //    }
    //        @inline final private[this] def withEntitiesWriteLocks[T](
    //            sortedEntityProperties: SortedSet[EntityProperties]
    //        )(
    //            f: ⇒ T
    //        ): T = {
    //            val entityLocks = sortedEntityProperties.view.map(e ⇒ e.l)
    //            withWriteLocks(entityLocks)(f)
    //        }
    @inline final private[this] def withEntitiesWriteLocks[T](
        sortedEntityProperties: SortedSet[EntityProperties]
    )(
        f: ⇒ T
    ): T = {
        val entityLocks = sortedEntityProperties.toIterator.map(e ⇒ e.l)
        withWriteLocks(entityLocks)(f)
    }

    /**
     * Clears all properties and property computation functions.
     */
    // Locks: accessStore
    def reset(): Unit = {

        accessStore {
            Tasks.reset()

            // reset statistics
            propagationCount.set(0L)
            effectiveDefaultPropertiesCount.set(0L)

            // reset entity related information
            theLazyPropertyComputations.clear()
            theOnPropertyComputations.clear()
            observers.clear()
            entitiesProperties foreach { eps ⇒ eps.ps.clear() /*delete properties*/ }
        }

    }

    /**
     * Returns a graphviz/dot representation of the current dependencies between the per-entity
     * properties.
     *
     * @note This is generally only useful if the number of dependencies is small!
     */
    def visualizeDependencies(): String = accessStore {
        val epkNodes = mutable.Map.empty[SomeEPK, DefaultMutableNode[SomeEPK]]

        def getNode(epk: SomeEPK): DefaultMutableNode[SomeEPK] = {
            epkNodes.getOrElseUpdate(
                epk,
                { new DefaultMutableNode[SomeEPK](epk, (epk) ⇒ epk.e.toString+"\n"+epk.pk) }
            )
        }

        observers.entrySet().asScala foreach { e ⇒
            val dependerEPK = e.getKey
            val dependeeEPKs = e.getValue.map(_._1)
            val dependerNode = getNode(dependerEPK)
            val dependeeNodes = dependeeEPKs.map(getNode(_))
            dependerNode.addChildren(dependeeNodes.toList)
        }

        org.opalj.graphs.toDot(epkNodes.valuesIterator.toSet)
    }

    /**
     * Returns a consistent snapshot of the stored properties.
     *
     * @note Some computations may still be running.
     */
    // Locks: accessStore
    def toString(printProperties: Boolean): String = accessStore(snapshotToString(printProperties))

    private[this] val snapshotMutex = new Object
    private[this] def snapshotToString(printProperties: Boolean): String = snapshotMutex.synchronized {

        val perPropertyKeyEntities = new Array[Int](PropertyKey.maxId + 1)
        var perEntityPropertiesCount = 0
        var unsatisfiedPropertyDependencies = 0
        var registeredObservers = 0
        val properties = new java.lang.StringBuilder()
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
            (
                if (printProperties)
                    s"\tperEntityProperties[$perEntityPropertiesStatistics]"+"\n"+properties
                else
                    ""
            )+
                ")"
    }

    /**
     * Returns a short string representation of the property store related to the key figures.
     */
    override def toString: String = toString(false)

    /**
     * Checks the consistency of the store.
     *
     * @note Only checks related to potentially internal bugs are performed. None of the checks is
     *      relevant to developers of analyses. However, even if some checks fail, they can still
     *      be caused by failures in user code.
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
        // data:  DataMap[Entity, EntityProperties]
        for {
            dependerEPK ← dependerEPKOpt
            dependeeOss = observers.get(dependerEPK)
            if dependeeOss ne null
            (dependeeEPK, po) ← dependeeOss
        } {
            if (!data.get(dependeeEPK.e).ps(dependeeEPK.pk.id).os.contains(po)) {
                val message = s"observers contains for $dependerEPK → $dependeeEPK "+
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
    //
    // PER ENTITY PROPERTIES
    //
    //

    // Access to this field is synchronized using the store's lock;
    // the map's keys are the ids of the PropertyKeys.
    private[this] final val theLazyPropertyComputations = ArrayMap[SomePropertyComputation](5)

    // Access to this field is synchronized using the store's lock;
    // the map's keys are the ids of the PropertyKeys.
    private[this] final val theOnPropertyComputations = ArrayMap[List[(Entity, Property) ⇒ Unit]](5)

    // The list of observers used by the entity `e` to compute the property of kind `k` (EPK).
    // In other words: the mapping between a Depender and its Dependee(s)!
    // The list of observers needs to be maintained whenever:
    //  1. A computation of a property finishes. In this case all observers need to
    //     be notified and removed from this map afterwards.
    //  1. A computation of a property generates an [[IntermediatResult]]
    private type DependerEPK = SomeEPK
    private type DependeeEPK = SomeEPK
    private type ObserversMap = JCHMap[DependerEPK, Buffer[(DependeeEPK, PropertyObserver)]]
    private[this] final val observers: ObserversMap = new JCHMap()

    def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[epk.e.type, P] = {
        val EPK(e, pk) = epk
        apply(e, pk).asInstanceOf[EOptionP[epk.e.type, P]]
    }

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
     *      function is either a lazy or a direct property computation function. In general
     *      It is preferred that clients always assume that the property is lazily computed
     *      when calling this function!
     * @param e An entity stored in the property store.
     * @param pk The kind of property.
     * @return `EPK(e,pk)` if information about the respective property is not (yet) available.
     *      `EP(e,Property)` otherwise; in the later case `EP` may encapsulate a property that
     *      is the final result of a computation `ep.isPropertyFinal === true` even though the
     *      property as such is in general refinable. Hence, to determine if the property in
     *      the current analysis context is final it is necessary to call the `EP` object's
     *      `isPropertyFinal` method.
     */
    // Locks: accessEntity, Entity (read)
    //                      Entity (write)
    def apply[P <: Property](e: Entity, pk: PropertyKey[P]): EOptionP[e.type, P] = {
        val pkId = pk.id
        val eps = data.get(e)
        val ps = eps.ps
        val lock = eps.l

        accessEntity {
            // Thread safety: We use double checked locking w.r.t. the entity to ensure that
            // we minimize the time we have to keep some lock.

            var pos = withReadLock(lock) { ps(pkId) }
            if (pos eq null) {
                // => the property is not (yet) computed;
                // let's check if we have a registered lazy property computation function
                val lpc = theLazyPropertyComputations(pkId)
                if (lpc ne null) withWriteLock(lock) {
                    // pos is not null if we have a property or if the property is currently computed
                    // quick path without locks... this path is only guaranteed to work if the property
                    // is final(!)
                    {
                        val pos = ps.apply(pkId)
                        if (pos ne null) {
                            val p = pos.p
                            if ((p ne null) && !p.isBeingComputed && p.isFinal) {
                                // println("quick check succeeded")
                                return EP(e, p.asInstanceOf[P]);
                            }
                        }
                    }

                    pos = ps(pkId)
                    if (pos eq null) {
                        val pos = new PropertyAndObservers(PropertyIsLazilyComputed, new Buffer)
                        ps(pkId) = pos
                        scheduleComputation(e, lpc)
                        EPK(e, pk)
                    } else {
                        val p = pos.p
                        if (p.isBeingComputed)
                            EPK(e, pk)
                        else if (pos.os eq null)
                            FinalEP(e, p.asInstanceOf[P])
                        else
                            EP(e, p.asInstanceOf[P])
                    }
                }
                else {
                    // So far no one wanted to compute a result, but this may still change.
                    EPK(e, pk)
                }
            } else {
                pos.p match {
                    case null | PropertyIsLazilyComputed ⇒ EPK(e, pk)
                    case p ⇒
                        val theP = p.asInstanceOf[P]
                        if (pos.os eq null)
                            FinalEP(e, theP)
                        else
                            EP(e, theP)
                }
            }
        }
    }

    /**
     * Returns an iterator of the different properties associated with the given element.
     *
     * This method is the preferred way to get a snapshot all properties of an entity and should
     * be used if you know that all properties are already computed. Using this method '''will not
     * trigger''' the computation of a property.
     *
     * @note The returned iterator operates on a snapshot and will never throw any
     *      `ConcurrentModificatonException`.
     * @param e An entity stored in the property store.
     * @return `Iterator[Property]`
     */
    // Locks: accessEntity, Entity
    def properties(e: Entity): List[Property] = {
        val eps = data.get(e)
        val l = eps.l
        val ps = eps.ps
        accessEntity {
            withReadLock(l) { (ps.valuesIterator collect ComputedProperty).toList }
        }
    }

    /**
     * Returns all entities which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * While the view is computed all other computations are blocked.
     *
     * Lazy/direct property computations are not triggered.
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
     * Returns all entities that have a specific property.
     */
    // Locks: accessStore
    def entities[P <: Property](p: P): Traversable[Entity] = {
        val pkId = p.key.id
        object PropertyP {
            def unapply(eps: EntityProperties): Boolean = {
                val ps = eps.ps
                val psPKId = ps(pkId)
                !isPropertyUnavailable(psPKId) && psPKId.p == p
            }
        }
        accessStore {
            entries collect { case (e, PropertyP()) ⇒ e }
        }
    }

    // TODO FOREACH

    /*
     * @see [[put]] For further information regarding the usage of `set` and `put`.
     */
    // Locks: accessEntity and this.update(...): Entity
    def set(e: Entity, p: Property): Unit = {
        val pkId = p.key.id
        val eps = data.get(e)
        val el = eps.l
        val ps = eps.ps
        val posP = accessEntity {
            withWriteLock(el) {
                val pos = ps(pkId)
                // Check that there is no property and no property is currently computed.
                if ((pos eq null) || (pos.p eq null)) {
                    // we do not have a property...
                    handleResult(Result(e, p))
                    return ;
                }
                pos.p
            }
        }
        if (posP == p) {
            // ignore
        } else {
            val message = s"cannot set $e to $p becaus it already has the value $posP"
            throw new IllegalArgumentException(message)
        }
    }

    /**
     * Stores the properties of the respective entities in the store if the respective property
     * is not yet associated with a property of the same kind. The properties are stored as
     * final values.
     *
     * @see `set(e:Entity,p:Property):Unit` for further details.
     */
    // Locks (indirectly): set(Entity,Property): accessEntity, Entity(write), this.update(...): Entity(write)
    def set(ps: Traversable[SomeEP]): Unit = ps foreach { ep ⇒ set(ep.e, ep.p) }

    /**
     * Associates the given entity `e` with the given property `p`.
     *
     * It is generally not possible to use `put` for those properties for which the
     * property store manages the property computations; i.e., where there might be
     * another computation that assumes that it is the only one potentially deriving this
     * property. In other words, all computations that derive this property have to
     * use `put` or none.
     *
     * @note    The property store offers two methods to directly associate a property with
     *          an entity: `set` and `put`.
     *          `set` is intended to be used if the respective
     *          property is computed independent of the computations managed
     *          by the store. Therefore, setting an already set property will throw an
     *          exception!
     *          `put` is intended to be used if the respective property is potentially
     *          computed concurrently by multiple (independent) computations and if
     *          all computations are guaranteed to derive the same property!
     *
     * @param   e An entity stored in the property store.
     *          If the entity `e` is unknown the behavior and state of the property
     *          store is undefined after calling this method. Furthermore, the current
     *          behavior in this special case may change arbitrarily.
     * @param   p Some arbitrary property. (The property `p` must not be `final`; however any
     *          further updates cannot be done using `put` to prevent some very
     *          nasty concurrency bugs.)
     * @return  `true` if the property was associated with the entity `e` and `false` if the
     *          property (the same object) was already associated with the entity.
     */
    def put(e: Entity, p: Property): Boolean = {
        val pkId = p.key.id
        val eps = data.get(e)
        val el = eps.l
        val ps = eps.ps
        accessEntity {
            withWriteLock(el) {
                val pos = ps(pkId)
                // Check that there is no property and no property is currently computed.
                if (pos eq null) {
                    handleResult(Result(e, p))
                    true
                } else {
                    val currentP = pos.p
                    if (currentP eq null) {
                        handleResult(Result(e, p))
                        true
                    } else if (currentP ne p) {
                        val message = s"$e:illegal property update: $currentP => $p"
                        throw new AssertionError(message)
                    } else {
                        // ... the property is the same; let's ignore it (idempotent update)
                        false
                    }
                }
            }
        }
    }

    /**
     * Registers the function `f` that is called whenever an element `e` is associated with
     * a property of the respective kind (`pk`). For those elements that are already associated
     * with a respective property `p`,  `f` will immediately be scheduled
     * (i.e., `f` will not be executed immediately.)
     *
     * If the entity `e` is updated multiple times no guarantee is given in which order `f`
     * is called with the new properties.
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
     * `e` already has a property `pOld` of property kind `pk`, then the new property will be
     * ignored.
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
     * @see `set(e:Entity,p:Property):Unit` for further details.
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
        // We use exactly ThreadCount number of threads that process all entities.
        val max = ParallelismLevel
        while (i < max) {
            i += 1
            scheduleRunnable {
                while (!Tasks.isInterrupted && remainingEntities.nonEmpty) {
                    val nextEntity: E = remainingEntitiesMutex.synchronized {
                        if (remainingEntities.nonEmpty) {
                            val nextEntity = remainingEntities.head
                            remainingEntities = remainingEntities.tail
                            val r: Any = entitySelector.applyOrElse(nextEntity, AnyToAnyThis)
                            if (r.asInstanceOf[AnyRef] ne AnyToAnyThis) {
                                r.asInstanceOf[E]
                            } else {
                                null
                            }
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
    ): Unit = accessStore {
        theLazyPropertyComputations(pk.id) = pc
    }

    /**
     * Registers a property computation function that is called for all known entities.
     *
     * This store ensures that the property
     * computation function `pc` is never invoked more than once for the
     * same element at the same time. If `pc` is invoked again for a specific element
     * then only because a dependee has changed!
     */
    def schedule(pc: SomePropertyComputation): Unit = bulkScheduleComputations(keysList, pc)

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
    def scheduleForFiltered(f: Entity ⇒ Boolean)(c: SomePropertyComputation): Unit = {
        val it = keys.iterator()
        var es: List[Entity] = Nil
        while (it.hasNext) {
            if (checkIsInterrupted)
                return ;
            val e = it.next()
            if (f(e)) es = e :: es
        }
        bulkScheduleComputations(es, c)
    }

    /**
     * Registers a function `c` that computes a property for those elements
     * of the store that are collected by the given partial function `pf`.
     *
     * The partial function is evaluated for all entities as part of this
     * method; i.e., the calling thread.
     *
     * @param  pf A a partial function that is used to collect those elements that will be
     *         passed to the function`c` and for which the analysis may compute some property.
     *         The function pf is performed in the context of the calling thread.
     */
    def scheduleForCollected[E <: Entity](
        pf: PartialFunction[Entity, E]
    )(
        c: PropertyComputation[E]
    ): Unit = {
        val es = keysList.collect(pf)
        if (es.isEmpty) {
            logWarn("project", s"the entity selector function $pf did not select any entity")
        }
        bulkScheduleComputations(es, c.asInstanceOf[Entity ⇒ PropertyComputationResult])
    }

    /**
     * Schedules the execution of the given PropertyComputation function for the given entity.
     * This is of particular interest to start an incremental computation
     * (cf. [[IncrementalResult]]) which, e.g., processes the class hierachy in a top-down manner.
     */
    def scheduleForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {
        scheduleComputation(e, pc)
    }

    /**
     * Awaits the completion of all property computation functions which were previously registered.
     * If a second thread is used to register [[PropertyComputation]] functions
     * no guarantees are given and it is recommended to schedule all property computation functions
     * using one thread and using that thread to call this method.
     *
     * This function is only '''guaranteed''' to wait on the completion of the computation
     * of those properties for which a property computation function was registered by
     * the calling thread.
     */
    def waitOnPropertyComputationCompletion(
        resolveCycles:                         Boolean = true,
        useFallbacksForIncomputableProperties: Boolean = true
    ): Unit = {
        Tasks.waitOnCompletion(resolveCycles, useFallbacksForIncomputableProperties)
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
                if eps.ps.valuesIterator.exists { pos ⇒
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
     * @note This method will not trigger lazy or direct property computations.
     */
    def collect[T](pf: PartialFunction[(Entity, Property), T]): Traversable[T] = {
        accessStore {
            for {
                (e, eps) ← entries
                ps = eps.ps
                pos ← ps.valuesIterator
                p = pos.p
                if p ne null
                if !p.isBeingComputed
                ep /*: (Entity, Property)*/ = (e, p)
                // IMPROVE Rewrite the split isDefinedAt + pf.apply using pf.applyOrElse
                if pf.isDefinedAt(ep)
            } yield {
                pf(ep)
            }
        }
    }

    // =============================================================================================
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    private[this] final val threadPool = ThreadPoolN(ParallelismLevel)

    /**
     * @return `true` if the pool is shutdown. In this case it is no longer possible to submit
     *      new computations.
     */
    def isShutdown: Boolean = threadPool.isShutdown

    /**
     * General handling of the tasks that are executed.
     */
    private[this] object Tasks {

        @volatile private[LockBasedPropertyStore] var isInterrupted: Boolean = false

        // ALL ACCESSES TO "executed" and "scheduled" ARE SYNCHRONIZED
        @volatile private[this] var executed: Int = 0

        /**
         * The number of scheduled tasks. I.e., the number of tasks that are running or
         * that will run in the future.
         */
        @volatile private[this] var scheduled: Int = 0

        private[this] var cleanUpRequired = false

        private[LockBasedPropertyStore] def executedComputations: Int = executed

        private[LockBasedPropertyStore] def scheduledComputations: Int = scheduled

        private[LockBasedPropertyStore] def reset(): Unit = {
            if (isInterrupted || isShutdown)
                throw new InterruptedException();

            this.synchronized {
                if (scheduled > 0)
                    throw new IllegalStateException("computations are still running");

                executed = 0
                cleanUpRequired = false
            }
        }

        /**
         * Terminates all scheduled but not executing computations and afterwards
         * deregisters all observers.
         */
        private[LockBasedPropertyStore] def interrupt(): Unit = {

            if (isInterrupted)
                return ;

            this.synchronized {
                // double-checked locking idiom...
                if (checkIsInterrupted)
                    return ;

                isInterrupted = true
                if (ValidateConsistency) logDebug("analysis progress", "cancelling scheduled computations")
                val waitingTasks = threadPool.shutdownNow()
                tasksAborted(waitingTasks.size)
            }

            def clearAllObservers(): Unit = {
                // We iterate over all entities and remove all related observers
                // to help to make sure that the computation can finish in due time.
                threadPool.awaitTermination(5000L, TimeUnit.MILLISECONDS)

                if (ValidateConsistency) logDebug("analysis progress", "garbage collecting property computations")
                accessStore {
                    // 1) clear the list of outgoing observers
                    store.observers.clear()

                    // 2) clear the list of incoming observers
                    for {
                        eps ← entitiesProperties
                        ps = eps.ps
                        (pos, pkId) ← ps.valuesIterator.zipWithIndex // the property p may (still be) null
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

        def taskStarted(): Unit = this.synchronized {
            scheduled += 1
            cleanUpRequired = true
        }

        def tasksStarted(tasksCount: Int): Unit = this.synchronized {
            scheduled += tasksCount
        }

        def tasksAborted(tasksCount: Int): Unit = this.synchronized {
            scheduled -= tasksCount
        }

        // Locks: Tasks
        //        Store(exclusive access), Tasks, handleUnsatisfiedDependencies: Store (access), Entity and scheduleContinuation: Tasks
        def taskCompleted(): Unit = {
            /*internal*/ // assert(scheduled > 0)

            this.synchronized {
                scheduled -= 1
                executed += 1
            }

            // When all scheduled tasks are completed, we check if there are
            // pending computations that now can be activated.
            if (scheduled == 0) accessStore {
                this.synchronized {
                    /*internal*/ // assert(validate(None), s"the property store is inconsistent")

                    if (scheduled == 0 && cleanUpRequired) {
                        cleanUpRequired = false
                        if (ValidateConsistency) {
                            val message = s"all $executed scheduled tasks have finished"
                            logDebug("analysis progress", message)
                        }
                        // Do not call handleUnsatisfiedDependencies in the following!
                        // We want to give clients the full control over resolving
                        // cycles and using default values.
                        if (scheduled == 0 /*scheduled is still === 0*/ ) {
                            if (ValidateConsistency) {
                                def registeredObservers: Int = {
                                    val pss = entitiesProperties.map(_.ps)
                                    val poss = pss.flatMap(_.valuesIterator)
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
                            if (ValidateConsistency) logDebug(
                                "analysis progress",
                                s"(re)scheduled $scheduled property computations"
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
        private[this] def handleUnsatisfiedDependencies(
            resolveCycles:                         Boolean,
            useFallbacksForIncomputableProperties: Boolean
        ): Boolean = {
            var storeUpdated: Boolean = false
            val observers = store.observers

            val directlyIncomputableEPKs = HSet.empty[SomeEPK]
            val indirectlyIncomputableEPKs = HSet.empty[SomeEPK]
            // All those EPKs that require some information that do not depend (directly
            // or indirectly) on an incomputableEPK.
            val cyclicComputableEPKCandidates = HSet.empty[SomeEPK]

            /*
             * @param epks The set of EPKs which have a dependency on dependerEPK.
             * @return Those epks that are newly added to the set epks. If epks is initially empty
             *      the returned list and the given set epks contain the same elements.
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

            observers.entrySet().asScala foreach { e ⇒
                val dependerEPK = e.getKey
                if (!indirectlyIncomputableEPKs.contains(dependerEPK)) {
                    cyclicComputableEPKCandidates += dependerEPK
                    val dependees = e.getValue
                    dependees foreach { dependee ⇒
                        val dependeeEPK = dependee._1
                        if (!observers.containsKey(dependeeEPK)) {
                            directlyIncomputableEPKs += dependeeEPK
                            indirectlyIncomputableEPKs += dependerEPK
                            determineDependentIncomputableEPKs(dependerEPK, indirectlyIncomputableEPKs)
                        } else {
                            cyclicComputableEPKCandidates += dependeeEPK
                        }
                    }
                }
            }
            cyclicComputableEPKCandidates --= directlyIncomputableEPKs
            cyclicComputableEPKCandidates --= indirectlyIncomputableEPKs

            if (resolveCycles) {
                val epkSuccessors: (SomeEPK) ⇒ Traversable[SomeEPK] = (epk: SomeEPK) ⇒ {
                    val observers = store.observers.get(epk)
                    /*internal*/ assert(
                        observers ne null,
                        {
                            writeAndOpen(visualizeDependencies(), "PropertyStoreDependencies", ".dot")
                            s"$epk has no dependees!\n"+
                                cyclicComputableEPKCandidates.
                                map(_.toString.replace("\n", "\n\t\t")).
                                mkString("\tcyclicComputableEPKCandidates=\n\t\t", " ->\n\t\t", "\n") +
                                directlyIncomputableEPKs.
                                map(_.toString.replace("\n", "\n\t\t")).
                                mkString("\tdirectlyIncomputableEPKs=\n\t\t", "\n\t\t", "\n") +
                                indirectlyIncomputableEPKs.
                                map(_.toString.replace("\n", "\n\t\t")).
                                mkString("\tindirectlyIncomputableEPKs=\n\t\t", "\n\t\t", "\n")
                        }
                    )
                    observers.view.map(_._1)
                }
                val cSCCs: List[Iterable[SomeEPK]] =
                    closedSCCs(cyclicComputableEPKCandidates, epkSuccessors)
                if (ValidateConsistency && cSCCs.nonEmpty) logDebug(
                    "analysis progress",
                    cSCCs.
                        map(_.mkString("", " → ", " ↺")).
                        mkString("found the following cyclic computations:\n\t", "\n\t", "\n")
                )
                for (cSCC ← cSCCs) {
                    // in the following, either the observer relations are updated or one or more
                    // property is updated
                    storeUpdated = true
                    val results = PropertyKey.resolveCycle(store, cSCC)
                    if (results.nonEmpty) {
                        if (ValidateConsistency) {
                            val cycle = cSCC.mkString("", " → ", " ↺")
                            logInfo(
                                "analysis progress",
                                s"resolving the cycle $cycle resulted in $results"
                            )
                        }
                        for (result ← results) {
                            // TODO validate that we have indeed changed a property!
                            handleResult(result)
                        }
                    } else {
                        // The following handles the case of a cycle which could not be resolved.
                        if (ValidateConsistency) {
                            val cycle = cSCC.mkString("", " → ", " ↺")
                            val infoMessage =
                                s"resolution of $cycle produced no results; removing observers"
                            logInfo("analysis progress", infoMessage)
                        }
                        for (epk ← cSCC) { clearAllDependeeObservers(epk) }
                    }
                }
            }

            // Let's get the set of observers that will never be notified, because
            // there are no open computations related to the respective property.
            // This is also the case if no respective analysis is registered so far.
            if (directlyIncomputableEPKs.nonEmpty && useFallbacksForIncomputableProperties) {
                storeUpdated = true
                for (EPK(e, pk) ← directlyIncomputableEPKs) {
                    /*internal*/ /* assert(
                        data.get(e).ps(pk.id).p eq null,
                        s"the entity $e already has a property ${data.get(e).ps(pk.id).p}($pk)"
                    ) */
                    val defaultP = PropertyKey.fallbackProperty(store, e, pk)
                    effectiveDefaultPropertiesCount.incrementAndGet()
                    logInfo(
                        "analysis progress",
                        s"using default property ${PropertyKey.name(pk.id)}:$defaultP for $e"
                    )
                    set(e, defaultP)
                }

                if (ValidateConsistency) logDebug(
                    "analysis progress", "created all tasks for setting the fallback properties"
                )
            }
            /*internal*/ /* assert(
                validate(None),
                s"the property store is inconsistent after handling unsatisfied dependencies"
            ) */
            storeUpdated
        }

        private[this] def doWaitOnCompletion(): Unit = {
            //noinspection LoopVariableNotUpdated
            while (scheduled > 0) {
                if (ValidateConsistency)
                    logDebug("analysis progress", s"remaining tasks: $scheduled")
                wait()
            }
        }

        // Locks: Tasks
        def waitOnCompletion(
            resolveCycles:                        Boolean,
            useFallbackForIncomputableProperties: Boolean
        ): Unit = this.synchronized {
            doWaitOnCompletion()
            // If all computations were finished already `scheduled` would have been "0" already
            // and no fallback computations would have been triggered
            if (useFallbackForIncomputableProperties || resolveCycles) {
                while (handleUnsatisfiedDependencies(
                    resolveCycles,
                    useFallbackForIncomputableProperties
                )) {
                    doWaitOnCompletion()
                }
            }
        }
    }

    /**
     * Schedules the continuation w.r.t. the entity `e`.
     */
    // Locks of scheduleRunnable: Tasks
    private[this] def scheduleContinuation(
        dependeeE:  Entity,
        dependeeP:  Property,
        updateType: UpdateType,
        c:          OnUpdateContinuation
    ): Unit = {
        scheduleRunnable { handleResult(c(dependeeE, dependeeP, updateType)) }
    }

    /**
     * Schedules the computation of a property w.r.t. the list of entities `es`.
     */
    // Locks of scheduleComputation: Tasks
    private[this] def bulkScheduleComputations(
        es: TraversableOnce[_ <: Entity],
        pc: SomePropertyComputation
    ): Unit = {
        es foreach { e ⇒ scheduleComputation(e, pc) }
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
        import Tasks.{taskStarted, taskCompleted}

        /*
         * The core method that actually submits runnables to the thread pool.

         * @note    tastStarted is called immediately and taskCompleted is called when the
         *          task has finished.
         */
        // Locks: Tasks
        def scheduleTask(task: Runnable): Unit = {
            if (checkIsInterrupted) {
                Tasks.interrupt()
                return ;
            }
            taskStarted()
            try {
                threadPool.submit(task)
            } catch {
                // (Exceptions thrown by the task do not end up here.)
                // Here, we just handle threadpool exceptions.
                case ree: RejectedExecutionException ⇒
                    logError("analysis progress", s"submitting the next task failed", ree)
                    taskCompleted()

                case t: Throwable ⇒
                    logError("analysis progress", s"submitting the next task failed", t)
                    taskCompleted()
            }
        }
        val task = new Runnable {
            override def run(): Unit = {
                try {
                    if (!checkIsInterrupted) f
                } catch {
                    case t: Throwable ⇒
                        t.printStackTrace()
                        logError("analysis progress", s"an analysis failed", t)
                } finally {
                    // We have finished processing the task!
                    taskCompleted()
                }
            }
        }
        if (!checkIsInterrupted)
            scheduleTask(task)
    }

    /**
     * Clears all observers that were registered with other entities to compute the
     * respective property (dependerEPK) of the given entity.
     * This method handles the situation where the computation of a property
     * potentially depended on some other entities and we now have '''a final result'''
     * and need to cleanup the registered observers.
     *
     * @return `true` if some observers were removed.
     */
    // Locks: Entity (write)
    private[this] def clearAllDependeeObservers(dependerEPK: SomeEPK): Boolean = {
        // observers : JCHMap[/*Depender*/EPK, Buffer[(/*Dependee*/EPK, PropertyObserver)]]()
        val observers = store.observers
        val dependerEPKOs = observers.remove(dependerEPK) // outgoing ones...
        if ((dependerEPKOs eq null) || dependerEPKOs.isEmpty)
            return false;

        // dependerOs maybe empty if we had intermediate results so far...
        dependerEPKOs foreach { epkos ⇒
            val (dependeeEPK, dependeeO) = epkos
            val dependeeEPs = data.get(dependeeEPK.e)
            val dependeePs = dependeeEPs.ps
            val dependeePkId = dependeeEPK.pk.id
            withWriteLock(dependeeEPs.l) {
                val dependeeOs = dependeePs(dependeePkId).os
                if (dependeeOs ne null) {
                    dependeeOs -= dependeeO
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
    def handleResult(r: PropertyComputationResult): Unit = {

        // Locks: Entity
        def registerDependeeObserverWithItsDepender(
            dependeeEPK: SomeEPK,
            o:           PropertyObserver
        ): Unit = {
            val dependerEPK = o.dependerEPK
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
            /*internal*/ // assert(!p.isBeingComputed)
            val pk = p.key
            val pkId = pk.id

            // update the property and schedule(!) all interested onPropertyComputations
            val onPropertyComputations = theOnPropertyComputations.getOrElse(pkId, Nil)

            val updateTypeId = updateType.id
            val eps = data.get(e)

            val os = withWriteLock(eps.l) {

                val ps = eps.ps

                // we first need to acquire the lock to avoid that a scheduled "on property change
                // computation" is run before the property is actually updated
                onPropertyComputations foreach { opc ⇒ scheduleRunnable { opc(e, p) } }

                val pos = ps(pkId)
                if (pos eq null) {
                    // No one was interested in this property so far...
                    (updateTypeId: @scala.annotation.switch) match {
                        case FinalUpdate.id ⇒
                            // Note that it is possible to have a Final Update though the underlying
                            // property is refinable. This is the case whenever the analysis knows
                            // that no further refinement may happen (given the current program).
                            ps(pkId) = new PropertyAndObservers(p, null)
                        /*internal*/ /* assert(
                                ps(p.key.id).p == p,
                                s"the property store $pos does not contain the new property $p"
                            ) */

                        case IntermediateUpdate.id ⇒
                            assert(p.isRefinable, s"$e: intermediate update of final property $p")
                            ps(pkId) = new PropertyAndObservers(p, Buffer.empty)
                        /*internal*/ /* assert(
                                ps(p.key.id).p == p,
                                s"the property store $pos does not contain the new property $p"
                            ) */
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
                    if (ValidateConsistency && (oldP ne null) && p.isOrdered) {
                        val isValid = p.asOrderedProperty.isValidSuccessorOf(oldP.asOrderedProperty)
                        isValid.foreach(s ⇒ throw new AssertionError(s"$e: $s"))
                    }
                    /*internal*/ /* assert(
                        (oldP eq null) || oldP.isBeingComputed || oldP.key == pk,
                        s"the key of the old property ${oldP.key} and the new property ${pk} are different"
                    ) */
                    var os: Seq[PropertyObserver] = pos.os

                    (updateTypeId: @scala.annotation.switch) match {
                        case FinalUpdate.id ⇒
                            if (ValidateConsistency) assert(
                                (oldP eq null) || oldP.isBeingComputed || (oldP.isRefinable && (os ne null)),
                                s"$e: the old property $oldP is final; refinement to $p is not supported"
                            )
                            clearAllDependeeObservers(EPK(e, pk))
                            ps(pkId) = new PropertyAndObservers(p, null /* <=> p is final  */ )
                        /*internal*/ /* assert(
                                ps(p.key.id).p == p,
                                s"the property store $pos does not contain the new property $p"
                            ) */

                        case IntermediateUpdate.id ⇒
                            if (ValidateConsistency) assert(
                                (oldP eq null) || oldP.isBeingComputed || (oldP.isRefinable && (os ne null)),
                                s"$e: impossible intermediate update of the old property $oldP with $p (os=$os)"
                            )
                            if (ValidateConsistency) assert(
                                p.isRefinable, s"$e: intermediate update using a final property $p"
                            )
                            if (oldP != p) {
                                if (ValidateConsistency) assert(p != oldP, s"equals is not reflexive: $p <=> $oldP")
                                ps(pkId) = new PropertyAndObservers(p, Buffer.empty)
                                /*internal*/ /* assert(
                                    ps(p.key.id).p == p,
                                    s"the property store $pos does not contain the new property $p"
                                ) */
                            } else {
                                if (ValidateConsistency) logDebug(
                                    "analysis progress", s"$e: ignoring useless update $oldP => $p"
                                )
                                os = Nil
                            }

                    }
                    os
                }
            }
            /*internal*/ //  assert(os ne null, s"$e($updateType => $p): os is null")
            if (os != null && os.nonEmpty) {
                // inform all (previously registered) observers about the value
                scheduleRunnable { os foreach { o ⇒ o(e, p, updateType) } }
            }
        }

        //
        // PROCESSING RESULTS
        //

        if (ValidateConsistency && r.id != Results.id && r.id != IncrementalResult.id) logDebug(
            "analysis progress", s"analysis result $r"
        )

        val resultId = r.id
        accessEntity {
            (resultId: @scala.annotation.switch) match {

                case NoResult.id ⇒ /* nothing to do */

                case Result.id ⇒
                    val Result(e, p) = r
                    update(e, p, FinalUpdate)
                /*internal*/ /* assert(
                        { val os = observers.get(EPK(e, p.key)); (os eq null) || (os.isEmpty) },
                        s"observers of ${EPK(e, p.key)} should be empty, but contains ${observers.get(EPK(e, p.key))}"
                    ) */

                case RefinableResult.id ⇒
                    val RefinableResult(e, p) = r
                    update(e, p, IntermediateUpdate)
                /*internal*/ /* assert(
                            { val os = observers.get(EPK(e, p.key)); (os eq null) || (os.isEmpty) },
                            s"observers of ${EPK(e, p.key)} should be empty, but contains ${observers.get(EPK(e, p.key))}"
                        ) */

                case IncrementalResult.id ⇒
                    val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                    handleResult(ir)
                    npcs foreach { npc ⇒ val (pc, e) = npc; scheduleComputation(e, pc) }

                case IntermediateResult.id ⇒
                    val IntermediateResult(dependerEOptP, dependees /*: Traversable[SomeEOptionP]*/ , c) = r
                    val dependerE = dependerEOptP.e
                    val dependerPK = dependerEOptP.pk

                    if (ValidateConsistency) assert(
                        { val epk = EPK(dependerE, dependerPK); !dependees.exists(_ == epk) },
                        s"$dependerE: self-recursive computation of $dependerPK"
                    )

                    //val accessedEPs = dependees.map(_.e) ++ Set(dependerE) // make dependees a Seq
                    //val accessedEPs = data.get(dependerE) :: dependees.view.map(d ⇒ data.get(d.e)).toList
                    //val accessedEPs =
                    //    SortedSet(data.get(dependerE))(EntityPropertiesOrdering) ++
                    //        dependees.view.map(d ⇒ data.get(d.e))
                    // val dependerEP = UIDSet(data.get(dependerE)) !!!!! UID SETS ARE NO LONGER SORTED !!!!
                    //val accessedEPs = dependees.foldLeft(dependerEP)((c, d) ⇒ c + data.get(d.e))
                    val accessedEPs =
                        dependees.
                            foldLeft(SortedSet(data.get(dependerE))(EntityPropertiesOrdering)) { (c, d) ⇒
                                c + data.get(d.e)
                            }
                    withEntitiesWriteLocks(accessedEPs) {
                        /*internal*/ /* assert(
                            { val os = observers.get(dependerEPK); (os eq null) || (os.isEmpty) },
                            s"observers of $dependerEPK should be empty, but contains ${observers.get(dependerEPK)}"
                        ) */
                        /*internal*/ /* assert(
                            entitiesProperties.forall { eps ⇒
                                val pos = eps.ps(dependerPK.id)
                                (pos eq null) || {
                                    val os = pos.os
                                    (os eq null) || os.forall(_.dependerEPK != dependerEPK)
                                }
                            },
                            s"found dangling property observer"
                        ) */
                        /*internal*/ /* assert(
                            validate(Some(dependerEPK)),
                            s"the property store is inconsistent before intermediate update"
                        ) */

                        var dependeeEPKs = List.empty[SomeEPK]

                        val updatedDependee = dependees.exists { eOptionP ⇒

                            val dependeeE = eOptionP.e
                            val dependeePK = eOptionP.pk
                            val dependeePKId = dependeePK.id

                            val dependeeCurrentEPs = data.get(dependeeE)
                            val dependeeCurrentPs = dependeeCurrentEPs.ps
                            val dependeeCurrentPOs = dependeeCurrentPs(dependeePKId)
                            if ((dependeeCurrentPOs eq null) ||
                                (dependeeCurrentPOs.p eq null) ||
                                (eOptionP.hasProperty &&
                                    dependeeCurrentPOs.p == eOptionP.p &&
                                    (dependeeCurrentPOs.os ne null))) {
                                // => the dependee's property and status (!) has not changed
                                dependeeEPKs = EPK(dependeeE, dependeePK) :: dependeeEPKs
                                false
                            } else {
                                val updateType =
                                    if (dependeeCurrentPOs.os eq null)
                                        FinalUpdate
                                    else
                                        IntermediateUpdate

                                if (ValidateConsistency) logDebug(
                                    "analysis progress",
                                    s"scheduled continuation of $dependerE($dependerPK): "+
                                        s"$dependeeE("+
                                        s"oldP=${if (eOptionP.hasProperty) eOptionP.p.toString else "<none>"}, "+
                                        s"currentP=${dependeeCurrentPOs.p}, "+
                                        s"updateType=$updateType)"
                                )
                                // println("potential for savings.....")
                                scheduleContinuation(dependeeE, dependeeCurrentPOs.p, updateType, c)
                                true
                            }
                        }

                        if (!updatedDependee) {
                            // we use ONE observer to make sure that the continuation function
                            // is called at most once - independent of how many entities are
                            // actually observed
                            val dependerEPK = EPK(dependerE, dependerPK)
                            val o = new DependeePropertyObserver(dependerEPK, clearAllDependeeObservers) {
                                def propertyChanged(e: Entity, p: Property, u: UpdateType): Unit = {
                                    /*internal*/ /* assert(
                                        { val os = observers.get(dependerEPK); (os eq null) || os.isEmpty },
                                        s"failed to delete all observers for $dependerEPK"
                                    ) */
                                    propagationCount.incrementAndGet()
                                    scheduleContinuation(e, p, u, c)
                                }
                            }

                            for { dependeeEPK ← dependeeEPKs } {
                                val EPK(dependeeE, dependeePK) = dependeeEPK
                                val dependeeCurrentEPs = data.get(dependeeE)
                                val dependeeCurrentPs = dependeeCurrentEPs.ps
                                val dependeePKId = dependeePK.id

                                val dependeeCurrentPOs = dependeeCurrentPs(dependeePKId)
                                if (dependeeCurrentPOs eq null) {
                                    val dependeeOs: Buffer[PropertyObserver] = Buffer(o)
                                    dependeeCurrentPs(dependeePKId) = new PropertyAndObservers(null, dependeeOs)
                                } else {
                                    dependeeCurrentPOs.os += o
                                }
                                registerDependeeObserverWithItsDepender(dependeeEPK, o)
                            }
                        }
                        /*internal*/ /* assert(
                            validate(Some(dependerEPK)),
                            s"the property store is inconsistent (after intermediate update)"
                        ) */

                        if (dependerEOptP.hasProperty)
                            update(dependerE, dependerEOptP.p, IntermediateUpdate)
                    }

                case Results.id ⇒
                    val Results(results) = r
                    results.foreach(handleResult(_))

                case MultiResult.id ⇒
                    val MultiResult(results) = r
                    results foreach { ep ⇒
                        val e = ep.e
                        val p = ep.p
                        update(e, p, FinalUpdate)
                        /*internal*/ /* assert(
                                    { val os = observers.get(EPK(e, p.key)); (os eq null) || (os.isEmpty) },
                                    s"observers of ${EPK(e, p.key)} should be empty, but contains ${observers.get(EPK(e, p.key))}"
                                ) */
                    }

            }
        }
    }

    private[this] def isPropertyUnavailable(pos: PropertyAndObservers): Boolean = {
        (pos eq null) || { val p = pos.p; (p eq null) || p.isBeingComputed }
    }
}

/**
 * Factory to create [[LockBasedPropertyStore]]s.
 */
object LockBasedPropertyStore {

    final val ConfigKey = "org.opalj.debug.fpcf.LockBasedPropertyStore.consistency"
    final val ValidateConsistency = BaseConfig.as[Option[Boolean]](ConfigKey).getOrElse(false)

    def apply[T <: AnyRef: TypeTag](
        entities:      Traversable[Entity],
        isInterrupted: () ⇒ Boolean,
        context:       T
    )(
        implicit
        logContext: LogContext
    ): LockBasedPropertyStore = {
        apply(
            entities,
            isInterrupted,
            Math.max(NumberOfThreadsForCPUBoundTasks, 2),
            PropertyStoreContext[T](context)
        )
    }

    def apply(
        entities:      Traversable[Entity],
        isInterrupted: () ⇒ Boolean,
        context:       PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): LockBasedPropertyStore = {
        apply(
            entities,
            isInterrupted,
            Math.max(NumberOfThreadsForCPUBoundTasks, 2),
            context: _*
        )
    }

    /**
     * Creates a new [[PropertyStore]] for the given set of entities.
     *
     * @param entities The entities which will be stored in the property store and with which it is
     *                 possible to associate properties. E.g., the set of all class files, methods
     *                 and fields of a program.
     * @param isInterrupted A function that is called by the framework to test if
     *          the running/scheduled computations should be aborted.
     *          It is important that this function is efficient as it is frequently called.
     * @param context A collection of objects which are of different types and which
     *        can be queried later on to get information about the property store's
     *        context. For example, in case of OPAL the project to which this property store
     *        belongs is stored as context information.
     *
     * @param logContext The [[org.opalj.log.LogContext]] that will be used for debug etc. messages.
     * @return The newly created [[PropertyStore]].
     */
    def apply(
        entities:         Traversable[Entity],
        isInterrupted:    () ⇒ Boolean,
        parallelismLevel: Int,
        context:          PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): LockBasedPropertyStore = {

        assert(parallelismLevel > 0)

        val entitiesCount = entities.size
        val data = new DataMap[Entity, EntityProperties](entitiesCount)
        var entityId = 0
        entities foreach { e ⇒
            if (data.put(e, new EntityProperties(entityId)) ne null) {
                OPALLogger.error("internal - non-critical", s"duplicate entity: $e")
            }
            entityId += 1
        }

        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
        val ps = new LockBasedPropertyStore(data, contextMap, parallelismLevel)
        ps.setIsInterrupted(isInterrupted)
        ps
    }

    /**
     * Creates an entity selector for a specific type of entities.
     *
     * Usage:
     * {{{
     *   entitySelector[Method]
     * }}}
     */
    def entitySelector[T <: Entity: ClassTag]: PartialFunction[Entity, T] = {
        new PartialFunction[Entity, T] {

            def apply(x: Entity): T = x.asInstanceOf[T]

            def isDefinedAt(x: Entity): Boolean = {
                val ct = implicitly[ClassTag[T]]
                ct.runtimeClass.isAssignableFrom(x.getClass)
            }

            override def toString: String = {
                s"EntitySelector(${implicitly[ClassTag[T]].runtimeClass.getName})"
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
        (pos ne null) && { val p = pos.p; (p ne null) && !p.isBeingComputed }
    }

    def apply(pos: PropertyAndObservers): Property = pos.p
}

/**
 * @param id A property store wide unique id that is used to sort all entities.
 *          This global order is then used to acquire locks related to multiple entities in a
 *          '''globally''' consistent order.
 * @param ps A mutable map of the entities properties; the key is the id of the property's kind.
 */
private[fpcf] final class EntityProperties(
        final val id: Int,
        final val l:  ReentrantReadWriteLock = new ReentrantReadWriteLock,
        final val ps: Properties             = ArrayMap(sizeHint = Math.max(3, PropertyKey.maxId))
) extends UID

private[fpcf] object PropertiesOfEntity {

    def unapply(eps: EntityProperties): Some[Properties] = Some(eps.ps)

}

private[fpcf] object EntityPropertiesOrdering extends Ordering[EntityProperties] {
    def compare(x: EntityProperties, y: EntityProperties): Int = x.id - y.id
}
