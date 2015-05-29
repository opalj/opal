/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.fp

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ ConcurrentHashMap ⇒ JCHMap }
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.{ IdentityHashMap ⇒ JIDMap }
import java.util.{ Set ⇒ JSet }
import scala.collection.mutable.{ HashSet ⇒ HSet }
import scala.collection.mutable.{ HashMap ⇒ HMap }
import scala.collection.mutable.{ ListBuffer ⇒ Buffer }
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import org.opalj.concurrent.Locking
import org.opalj.collection.immutable.IdentityPair
import org.opalj.concurrent.Locking.{ withReadLock, withWriteLock }
import org.opalj.concurrent.ThreadPoolN
import org.opalj.log.OPALLogger
import org.opalj.concurrent.UncaughtExceptionHandler.uncaughtException
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

/**
 * The central store which manages the execution of all
 * computations that provide information about the entities of the store.
 *
 * In general, we can distinguish two kinds of computations:
 *  1. [Strictly Dependent Computations] require information about a specific property of an entity
 *      to be able to continue. For such computations it may happen that we run into a deadlock that
 *      consists of n >= 2 computations that are mutually dependent. In this case
 *      the computation of a property p of an entity e1 depends
 *      on the property p of an entity e2 that requires the property p of the entity e1.
 *      The store implements an algorithm to detect dependencies and to report a
 *      set of strongly connected computations to respective handler computations.
 *  1. [Weakly Dependent Computations] may use information about a specific property of an entity e, but
 *      they can still continue if the respective knowledge is not (yet) available and they
 *      are able to refine the results, once the knowledge becomes available.
 *
 *
 * ==Core Requirements on Property Computation Functions==
 *  - (One Function per Property Kind) A specific kind of property is always computed
 *      by only one registered `PropertyComputation` function.
 *  - (Thread-Safe) PropertyComputation functions have to be thread-safe.
 *  - (Non-Overlapping Results) [[PropertyComputation]] functions that are invoked on different
 *      entities have to compute result sets that are disjoint.
 *      For example, an analysis that performs a computation on class files and
 *      that derives properties of specific kind related to a class file's methods must ensure
 *      that no two analysis of two different class files derive information about
 *      the same method.
 *  - (Monoton) If a `PropertyComputation` function calculates (refines) a (new )property for
 *      a specific element then the result must be more specific.
 *
 * ==Thread Safety==
 * The PropertyStore is thread-safe.
 *
 * @author Michael Eichberg
 */
/*
 * The ProperStore prevents deadlocks by ensuring that updates of the store are always
 * atomic and by preventing each computation from acquiring more than one (write and/or
 * read) lock at a time.
 */
class PropertyStore private (
        private[this] val data: JIDMap[Entity, PropertyStoreValue],
        val isInterrupted: () ⇒ Boolean) { store ⇒

    // COMMON ABBREVIATONS USED IN THE FOLLOWING:
    // ==========================================
    // e = ENTITY
    // p = Property
    // ps = Properties
    // pk = PropertyKey
    // pc = (Property)Computation
    // c = Continuation (The rest of the computation of a property if a specific information becomes available.)
    // o = (Property)Observer
    // so = Strict(Property)Observer (An observer which encapsulates a computation that strictly needs the knowledge about the dependee to be able to continue)
    // wo = Weak(Property)Observer ((An observer which encapsulates a computation that may benefit from more precise information about dependee)
    // EPK = An entity and a property key

    // We want to be able to make sure that methods that access the store as
    // a whole always get a consistent snapshot view
    private[this] val storeLock = Locking()
    import storeLock.{ withReadLock ⇒ accessEntity }
    import storeLock.{ withWriteLock ⇒ accessStore }

    /**
     * The set of all stored elements.
     *
     * This set is not mutated.
     */
    private[this] final val keys: JSet[Entity] = data.keySet()

    /**
     * Returns the property of the respective property kind currently associated
     * with the given element.
     *
     * This is most basic method to get some property and it is the preferred way
     * if (a) you know that the property is already available – e.g., because some
     * property computation function was strictly run before the current one – or if (b) the running computation
     * has a huge, complex state that is not completely required if the computation
     * needs to be suspended because the property is not (yet) available. In the latter
     * case it may be beneficial to only store the strictly necessary information and
     *
     * @note The returned value may change over time but only such that it
     *      is strictly more precise.
     *
     * @param e An entity stored in the property store.
     * @param pk The kind of property.
     * @return `None` if no information about the respective property is (yet) available.
     *      `Some(Property)` otherwise.
     */
    def apply(e: Entity, pk: PropertyKey): Option[Property] = accessEntity {
        val (lock, properties) = data.get(e)
        withReadLock(lock) {
            properties.get(pk) match {
                case None | Some((null, _))             ⇒ None
                case Some((property, _ /*observers*/ )) ⇒ Some(property)
            }
        }
    }

    def require(
        dependerE: Entity,
        dependerPK: PropertyKey,
        dependeeE: Entity,
        dependeePK: PropertyKey)(
            c: Continuation): PropertyComputationResult = {

        @inline def suspend: Suspended =
            new Suspended(dependerE, dependerPK, dependeeE, dependeePK) {
                def continue(
                    dependeeE: Entity,
                    dependeeP: Property): PropertyComputationResult =
                    c(dependeeE, dependeeP)
            }

        apply(dependeeE, dependeePK) match {
            case Some(dependeeP) ⇒ c(dependeeE, dependeeP)
            case None            ⇒ suspend
        }
    }

    /**
     * Returns all elements which have a property of the respective kind. This method
     * returns a consistent snapshot view of the store w.r.t. the given
     * [[PropertyKey]].
     *
     * While the view is computed all other computations are blocked.
     */
    def apply(pk: PropertyKey): Traversable[(Entity, Property)] = accessStore {
        import scala.collection.JavaConversions._
        val valuesWithProperty =
            data.entrySet() filter { v ⇒
                val (_ /*lock*/ , properties) = v.getValue()
                properties.get(pk) match {
                    case Some((null, _)) | None ⇒ false
                    case _                      ⇒ true
                }
            }
        valuesWithProperty map { v ⇒ (v.getKey, v.getValue._2(pk)._1) }
    }

    /**
     * Registers a function that calculates a property for all or some elements
     * of the store.
     *
     * This store ensures that `f` is never invoked more than once for the
     * same element at the same time. If `f` is invoked again for a specific element
     * then only because a dependee has changed!
     */
    def <<=(c: PropertyComputation): Unit = accessEntity {
        if (!isInterrupted()) {
            val it = keys.iterator()
            while (it.hasNext()) {
                scheduleComputation(it.next(), c)
            }
        }
    }

    /**
     * Can be called by a client to await the completion of the computation of all
     * properties of all previously registered property computation functions.
     *
     * This function is only '''guaranteed''' to wait on the completion of the computation
     * of those properties that were registered by this thread.
     */
    def waitOnPropertyComputationCompletion(): Unit = Tasks.waitOnCompletion()

    override def toString: String = accessStore {
        "PropertyStore("+
            s"entitiesCount=${data.size()}, "+
            s"executedComputations=${Tasks.executedComputations}"+
            ")"
    }

    //
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    private[this] final val threadPool = ThreadPoolN(NumberOfThreadsForCPUBoundTasks)

    /**
     * General information about the tasks that are executed.
     */
    private[this] object Tasks {

        // ALL ACCESSES ARE SYNCHRONIZED
        private[this] var executed = 0

        private[PropertyStore] def executedComputations: Int = synchronized { executed }

        private[this] var isInterrupted: Boolean = false

        /**
         * The number of scheduled tasks. I.e., the number of tasks that are running or
         * that will run in the future.
         */
        private[this] var scheduled = 0

        /**
         * @return `true` if the state was newly set to `true`.
         */
        private[PropertyStore] def interrupt(): Unit = synchronized {
            if (!isInterrupted) {
                isInterrupted = true
                val waitingTasks = threadPool.shutdownNow()
                scheduled -= waitingTasks.size
            }
        }

        def taskStarted() = synchronized {
            executed += 1
            scheduled += 1
        }

        def taskCompleted() = synchronized {
            assert(scheduled > 0)
            scheduled -= 1

            // When all scheduled tasks are completed, we check if there are
            // pending computations that now can be activated.
            if (scheduled == 0) {
                // Let's check if we have some potentially refineable intermediate results.

                if (!isInterrupted) handleOpenComputations()

                if (scheduled == 0 /*scheduled is still === 0*/ )
                    // Well... it seems as if we are done
                    notifyAll()
            }
        }

        // Handle open computations supports both cases:
        //  1. computations that are part of a cyclic computation dependency
        //  1. computations that depend on knowledge related to a specific kind of
        //     property that was not computed (final lack of knowledge)
        // @return The set of strict PropertyObservers.
        private[this] def handleOpenComputations(): Traversable[PropertyObserver] = {
            // Based on the set of required core properties, each computation can
            // at most be found in one cyclic strictly dependent computation;
            // however, an epk can have multiple observers!

            var strictPS: List[PropertyObserver] = Nil
            val processedEPK = HSet.empty[EPK]

            import scala.collection.JavaConversions._
            for {
                entry ← data.entrySet()
                e = entry.getKey()
                ps = entry.getValue()._2
                (pk, (p, os)) ← ps
                o ← os
                // we have observers ...
                dependeeEPK = EPK(e, pk)
                if !processedEPK.contains(dependeeEPK)
                dependerEPK ← o.depender
                if !processedEPK.contains(dependerEPK)
                // we now have only "strict observers" that are not in an already
                // found strictly dependent computation
            } {
                strictPS ::= o

                def dependers(epk: EPK): Iterable[EPK] = {
                    for {
                        (pk, (p, os)) ← data.get(epk.e)._2
                        o ← os
                        dependerEPK ← o.depender
                        if !processedEPK.contains(dependerEPK)
                    } yield {
                        dependerEPK
                    }
                }

                // Extracts all paths, to which this entity contributes
                // @return The first list of ePKs are those entity/property key contributing to the cycle, the
                //        second list of entities are the entities that do not belong to a
                //        a cycle.
                def extractPaths(rootEPK: EPK, currentEPK: EPK): (List[EPK], List[EPK]) = {
                    var cyclic: List[EPK] = Nil
                    var linear: List[EPK] = Nil
                    dependers(currentEPK) foreach { dependerEPK: EPK ⇒
                        if (dependerEPK == rootEPK) {
                            assert(cyclic.isEmpty)
                            cyclic = List(rootEPK)
                        } else {
                            val (c, l) = extractPaths(rootEPK, dependerEPK)
                            if (c.nonEmpty) {
                                assert(cyclic.isEmpty)
                                cyclic = c
                            }
                            linear :::= l
                        }
                    }
                    if (cyclic.nonEmpty)
                        (currentEPK :: cyclic, linear)
                    else
                        (cyclic, currentEPK :: linear)
                }

                val (cyclic, initialLinear) = extractPaths(dependeeEPK, dependerEPK)
                val linear = if (cyclic.isEmpty) dependeeEPK :: initialLinear else initialLinear
                println("entities with cyclic dependency: "+cyclic)
                println("entities with linear dependency: "+linear)
                processedEPK ++= cyclic
                processedEPK ++= linear
                if (cyclic.nonEmpty) {

                }
            }
            strictPS
        }

        def waitOnCompletion() = synchronized {
            while (scheduled > 0) { wait }
        }
    }

    /**
     * Schedules the computation of a property w.r.t. the entity `e`.
     */
    private[this] def scheduleComputation(e: Entity, pc: PropertyComputation): Unit = {
        scheduleTask(() ⇒ handleResult(pc(e)))
    }

    /**
     * Schedules the continuation w.r.t. the entity `e`.
     */
    private[this] def scheduleContinuation(e: Entity, p: Property, c: Continuation): Unit = {
        scheduleTask(() ⇒ handleResult(c(e, p)))
    }

    private[this] def scheduleTask(t: () ⇒ Unit): Unit = {
        if (!isInterrupted()) {
            Tasks.taskStarted()
            threadPool.submit(new Runnable {
                def run(): Unit = {
                    try {
                        t()
                    } catch {
                        case t: Throwable ⇒ uncaughtException(Thread.currentThread(), t)
                    } finally {
                        Tasks.taskCompleted()
                    }
                }
            })
        } else {
            Tasks.interrupt()
        }
    }

    /**
     * Associates / Updates the property with element e. If observers are registered
     * with the respective property then those observers will be informed about the
     * property change.
     */
    // Invariant: always only at most one function exists that will compute/update
    // the property p belonging to property kind k of an element e.
    private[this] def update(e: Entity, p: Property): Unit = accessEntity {
        val (lock, properties) = data.get(e)
        withWriteLock(lock) {
            properties.put(p.key, (p, Buffer.empty)) match {

                case Some((oldP, observers)) ⇒
                    assert(oldP != p, "the old and the new property are identical")
                    observers.foreach { o ⇒ o(e, p) }

                case None ⇒
                // Nothing to do ...
            }
        }
    }

    /**
     * Registers the observer, if the property is not yet available or equal to the
     * specified property value. If the property is already refined, the observer is
     * immediately invoked and not registered.
     *
     * @return `true` if an observer was registered, `false` otherwise.
     */
    private[this] def handleDependency(
        e: Entity,
        pk: PropertyKey,
        pOption: Option[Property],
        o: PropertyObserver): Unit = {
        // always only at most one function exists that will update the property p
        // of the element e
        val (lock, properties) = data.get(e)
        withWriteLock(lock) {
            val propertyAndObservers = properties.get(pk)
            if (propertyAndObservers == None) {
                properties.put(pk, (null, Buffer(o)))
                true
            } else {
                val Some((p, observers)) = propertyAndObservers
                if ((p eq null) || pOption.isEmpty || pOption.get == p) {
                    observers += o
                } else {
                    // ... the value in the store is already a more refined value
                    // than the value given by pOption
                    o(e, p)
                }
            }
        }
    }

    private[PropertyStore] def handleResult(r: PropertyComputationResult): Unit = {
        r match {
            case NoResult ⇒
            // Nothing to do..

            case Result(results) ⇒
                results foreach { result ⇒ val (e, p) = result; update(e, p) }

            case result @ IntermediateResult(results, dependingEntities) ⇒

                // 1) Store the results
                results foreach { result ⇒ val (e, p) = result; update(e, p) }

                // 2) Register the observers
                dependingEntities foreach { dependingEntity ⇒
                    val (e, pk, pOption, c) = dependingEntity
                    val o = new DefaultPropertyObserver(None) {
                        def apply(e: Entity, p: Property): Unit = {
                            // ... for each dependent property we have a refinement;
                            // let's reschedule the computation
                            scheduleContinuation(e, p, c)
                        }
                    }

                    handleDependency(e, pk, pOption, o)
                }

            case suspended @ Suspended(e, pk, requiredE, requiredPK) ⇒

                // CONCEPT
                // First, let's get the property, then...
                //  - If we now have a property, let's immediately continue
                //    the computation.
                //  - If the property is still not computed, register an
                //    observer that will schedule the computation when the
                //    property was computed.

                def createPropertyObserver =
                    new DefaultPropertyObserver(Some(EPK(e, pk))) {
                        def apply(dependeeE: Entity, dependeeP: Property): Unit = {
                            val pc = (e: AnyRef) ⇒ suspended.continue(dependeeE, dependeeP)
                            scheduleComputation(e, pc)
                        }
                    }

                val (lock, properties) = data.get(requiredE)
                withWriteLock(lock) {
                    properties.get(requiredPK) match {
                        case Some((requiredP, observers)) ⇒
                            if (requiredP eq null) {
                                // we have other computations that are also waiting...
                                observers += createPropertyObserver
                            } else {
                                // the property was computed in the meantime
                                val pc = (e: AnyRef) ⇒ suspended.continue(requiredE, requiredP)
                                scheduleComputation(e, pc)
                            }
                        case _ ⇒
                            // this computation is the first who is interested in the property
                            properties.put(requiredPK, (null, Buffer(createPropertyObserver)))
                    }
                }
        }
    }

}
/**
 * Factory to create [[PropertyStore]]s.
 */
object PropertyStore {

    def apply(
        entities: Traversable[Entity],
        isInterrupted: () ⇒ Boolean): PropertyStore = {

        val entitiesCount = entities.size
        val map = new JIDMap[Entity, PropertyStoreValue](entitiesCount)

        entities.foreach { e ⇒ map.put(e, (new ReentrantReadWriteLock, HMap.empty)) }

        new PropertyStore(map, isInterrupted)
    }

}

