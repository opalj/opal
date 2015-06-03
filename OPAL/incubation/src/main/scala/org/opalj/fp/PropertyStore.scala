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
import org.opalj.concurrent.handleUncaughtException
import org.opalj.log.OPALLogger
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.LogContext
import org.opalj.br.analyses.AnalysisException
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import scala.collection.mutable.StringBuilder

/**
 * The central store which manages the execution of all
 * computations that required and provide information about the entities of the store.
 *
 * In general, it may happen that we run into a deadlock that
 * consists of n >= 2 computations that are mutually dependent. In this case
 * the computation of a property p of an entity e1 depends
 * on the property p of an entity e2 that requires the property p of the entity e1.
 * The store implements an algorithm to detect cyclic dependencies.
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
        val isInterrupted: () ⇒ Boolean)(
                implicit val logContext: LogContext) { store ⇒

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
     * property computation function was strictly run before the current one – or
     * if (b) the running computation
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
        withReadLock(lock) { properties.get(pk) } match {
            case None | Some((null, _))             ⇒ None
            case Some((property, _ /*observers*/ )) ⇒ Some(property)
        }
    }

    def require(
        dependerE: Entity,
        dependerPK: PropertyKey,
        dependeeE: Entity,
        dependeePK: PropertyKey)(
            c: Continuation): PropertyComputationResult = accessEntity {

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
     * This store ensures that `pc` is never invoked more than once for the
     * same element at the same time. If `pc` is invoked again for a specific element
     * then only because a dependee has changed!
     */
    def <<(pc: PropertyComputation): Unit = {
        val it = keys.iterator()
        while (it.hasNext()) {
            if (isInterrupted())
                return ;
            scheduleComputation(it.next(), pc)
        }
    }

    /**
     * @param f A filter that selects those entities that are relevant to the analysis.
     *      For which the analysis may compute some property.
     */
    def <~<(f: Entity ⇒ Boolean, c: PropertyComputation): Unit = {
        val it = keys.iterator()
        while (it.hasNext()) {
            if (isInterrupted())
                return ;
            val e = it.next()
            if (f(e)) scheduleComputation(e, c)
        }
    }

    /**
     * Can be called by a client to await the completion of the computation of all
     * properties of all previously registered property computation functions.
     *
     * This function is only '''guaranteed''' to wait on the completion of the computation
     * of those properties that were registered by this thread.
     */
    def waitOnPropertyComputationCompletion(): Unit = {
        Tasks.waitOnCompletion()
    }

    /**
     * Returns a string representation of the stored properties.
     */
    override def toString: String = accessStore {
        val text = new StringBuilder
        val it = data.entrySet().iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val ps = entry.getValue._2.map { pkpos ⇒
                val (pk, (p, os)) = pkpos
                (
                    if (p eq null)
                        s"<Unsatisfied: ${PropertyKey.name(pk)}>"
                    else
                        p.toString
                )+"["+(if (os eq null) 0 else os.size)+"]"
            }
            if (ps.nonEmpty) {
                val s = ps.mkString("\t\t"+entry.getKey.toString+" => {", ", ", "}\n")
                text.append(s)
            }
        }
        "PropertyStore(\n"+
            s"\tentitiesCount=${data.size()},\n"+
            s"\texecutedComputations=${Tasks.executedComputations}\n"+
            "\tproperties=\n"+text+
            ")"
    }

    //
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    private[this] final val threadPool = ThreadPoolN(Math.max(NumberOfThreadsForCPUBoundTasks, 2))

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
         * Terminates all scheduled but not executing computations and afterwards
         * deregisters all observers.
         */
        private[PropertyStore] def interrupt(): Unit = {

            if (isInterrupted)
                return ;

            this.synchronized {
                if (isInterrupted)
                    return ;

                isInterrupted = true
                OPALLogger.debug("analysis progress", "cancelling scheduled computations")
                val waitingTasks = threadPool.shutdownNow()
                scheduled -= waitingTasks.size
            }

            def collectGarbage(): Unit = {

                threadPool.awaitTermination(5000l, TimeUnit.MILLISECONDS)

                OPALLogger.debug("analysis progress", "garbage collecting property computations")
                accessStore {
                    import scala.collection.JavaConversions._
                    for {
                        entry ← data.entrySet()
                        (_ /*p*/ , os) ← entry.getValue()._2.values
                    } {
                        os.clear()
                    }
                }
            }

            if (threadPool.group == Thread.currentThread().getThreadGroup) {
                val t = new Thread(new Runnable { def run(): Unit = collectGarbage() })
                t.start()
            } else {
                collectGarbage()
            }

        }

        def taskStarted() = synchronized {
            scheduled += 1
        }

        def taskRejected() = synchronized {
            scheduled -= 1
        }

        def taskCompleted() = synchronized {
            assert(scheduled > 0)
            scheduled -= 1
            executed += 1

            // When all scheduled tasks are completed, we check if there are
            // pending computations that now can be activated.
            if (scheduled == 0) {
                // Let's check if we have some potentially refineable intermediate results.
                OPALLogger.debug(
                    "analysis progress",
                    s"all $executed previously scheduled tasks are executed")

                try {
                    if (!isInterrupted) {
                        OPALLogger.debug("analysis progress", s"handling unsatisfied dependencies")
                        handleUnsatisfiedDependencies()
                    }
                } catch {
                    case t: Throwable ⇒
                        OPALLogger.error(
                            "analysis progress",
                            "handling suspended computations failed; aborting analyses",
                            t)
                        interrupt()
                        notifyAll()
                        throw t
                }

                if (scheduled == 0 /*scheduled is still === 0*/ ) {
                    OPALLogger.debug(
                        "analysis progress",
                        s"computation of all properties finished")
                    notifyAll()
                } else {
                    OPALLogger.debug(
                        "analysis progress",
                        s"(re)scheduled $scheduled property computations")
                }
            }
        }

        // Handle open computations supports both cases:
        //  1. computations that are part of a cyclic computation dependency
        //  1. computations that depend on knowledge related to a specific kind of
        //     property that was not computed (final lack of knowledge)
        private[this] def handleUnsatisfiedDependencies(): Unit = {

            println(store.toString)

            val openComputations = HSet.empty[EPK]
            val unsatisfiedDependencies = HSet.empty[EPK]

            import scala.collection.JavaConversions._
            for {
                entry ← data.entrySet()
                e = entry.getKey()
                ps = entry.getValue()._2
                (pk, (p, os)) ← ps
                if os ne null
                o ← os
                // we have an entity with unsatisfied dependencies
            } {
                openComputations += o.depender
                unsatisfiedDependencies += EPK(e, pk)
            }
            // Let's get the set of observers that will never be notified, because
            // there are no open computations related to the respective property.
            for {
                EPK(e, pk) ← unsatisfiedDependencies -- openComputations
            } {
                val defaultP = PropertyKey.defaultProperty(pk)
                OPALLogger.debug(
                    "analysis progress",
                    s"associated default property $defaultP with $e")
                scheduleHandleResult(Result(e, defaultP))
            }

            //            import scala.collection.JavaConversions._
            //            for {
            //                entry ← data.entrySet()
            //                e = entry.getKey()
            //                ps = entry.getValue()._2
            //                (pk, (p, os)) ← ps
            //                dependerO ← os
            //                // we haven an entry with observers ...
            //                dependeeEPK = EPK(e, pk)
            //                if !processedEPK.contains(dependeeEPK)
            //                dependerEPK = dependerO.depender
            //                depender = (dependerEPK, dependerO)
            //                if !processedEPK.contains(dependerEPK)
            //                // we now have observers that are not in an already
            //                // found dependent computation
            //            } {
            //                println(s"handling entity $e "+data.get(e)._2)
            //                def dependers(epk: EPK): Iterable[(EPK, PropertyObserver)] = {
            //                    for {
            //                        (pk, (p, os)) ← data.get(epk.e)._2
            //                        o ← os
            //                        dependerEPK = o.depender
            //                        if !processedEPK.contains(dependerEPK)
            //                    } yield {
            //                        (dependerEPK, o)
            //                    }
            //                }
            //
            //                // Extracts all paths, to which this entity contributes.
            //                // @return
            //                //    The first list of ePKs are those entity/property key contributing
            //                //    to the cycle, the
            //                //    second list of entities are the entities that do not belong to a
            //                //    a cycle.
            //                def extractPaths(
            //                    rootEPK: EPK,
            //                    current: (EPK, PropertyObserver)): (List[(EPK, PropertyObserver)], List[(EPK, PropertyObserver)]) = {
            //
            //                    var cyclic: List[(EPK, PropertyObserver)] = Nil
            //                    var linear: List[(EPK, PropertyObserver)] = Nil
            //                    val (currentEPK, _ /*currentO*/ ) = current
            //                    dependers(currentEPK) foreach { depender: (EPK, PropertyObserver) ⇒
            //                        val (dependerEPK, _ /*dependerO*/ ) = depender
            //                        if (dependerEPK == rootEPK) {
            //                            assert(cyclic.isEmpty)
            //                            cyclic = List(depender)
            //                        } else {
            //                            val (c, l) = extractPaths(rootEPK, depender)
            //                            if (c.nonEmpty) {
            //                                assert(cyclic.isEmpty)
            //                                cyclic = c
            //                            }
            //                            linear :::= l
            //                        }
            //                    }
            //                    if (cyclic.nonEmpty)
            //                        (current :: cyclic, linear)
            //                    else
            //                        (cyclic, current :: linear)
            //                }
            //                println(s"handling entity $e - extracting paths")
            //                val (cyclic, linear) = //try {
            //                    extractPaths(dependeeEPK, depender)
            //                //                } catch {
            //                //                    case t: Throwable ⇒
            //                //                        println(Thread.currentThread())
            //                //                        println(t.printStackTrace())
            //                //                        throw t
            //                //                }
            //                println(s"handling entity $e - paths are extracted")
            //                cyclic.foreach(epko ⇒ processedEPK += epko._1)
            //                processedEPK ++= linear.view.map(_._1)
            //
            //                /*START-DEBUG*/
            //                if (cyclic.nonEmpty)
            //                    println("entities with cyclic dependency: "+cyclic)
            //                if (linear.nonEmpty)
            //                    println("entities with linear dependency: "+linear+" >> "+dependeeEPK)
            //                println()
            //                /*DEBUG-END*/
            //
            //                if (cyclic.nonEmpty) {
            //                    // Let's pick one (arbitrary!) element and set it to the default
            //                    // property for elements involved in a cyclic dependency.
            //                    def handleCycle: Boolean = {
            //                        //                        cyclic.exists { epko ⇒
            //                        ////                            val (epk, o) = epko
            //                        ////                            val pk = epk.pk
            //                        ////                            PropertyKey.cyclicComputationHandler(cyclic) match {
            //                        ////                                case Some(fallback) ⇒
            //                        ////                                    val p = fallback(epk)
            //                        ////                                    val e = epk.e
            //                        ////                                    o(e, p)
            //                        ////                                    println(s"handling cyclic dependency: $e <= $p")
            //                        ////                                    true
            //                        ////                                case None ⇒
            //                        ////                                    false
            //                        //                            }
            //                        false
            //
            //                    }
            //                    if (!handleCycle) {
            //                        val message = s"unresolvable cyclic dependency (${cyclic.unzip._1})"
            //                        throw new AnalysisException(message)
            //                    }
            //                }
            //            }
        }

        def waitOnCompletion() = synchronized {
            while (scheduled > 0) { wait }
        }
    }

    /**
     * Schedules the handling of the result of a property computation.
     */
    private[this] def scheduleHandleResult(r: PropertyComputationResult): Unit = {
        scheduleTask(() ⇒ handleResult(r))
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
    private[this] def scheduleContinuation(
        dependeeE: Entity,
        dependeeP: Property,
        c: Continuation): Unit = {
        scheduleTask(() ⇒ handleResult(c(dependeeE, dependeeP)))
    }

    private[this] def scheduleTask(t: () ⇒ Unit): Unit = {
        if (!isInterrupted()) {
            Tasks.taskStarted()
            val task = new Runnable {
                def run(): Unit =
                    try {
                        t()
                    } catch {
                        case t: Throwable ⇒
                            handleUncaughtException(t)
                            throw t
                    } finally {
                        Tasks.taskCompleted()
                    }
            }
            try {
                threadPool.submit(task)
            } catch {
                case reh: RejectedExecutionException ⇒
                    Tasks.taskRejected()
                case t: Throwable ⇒
                    Tasks.taskRejected()
                    handleUncaughtException(t)
                    throw t
            }
        } else {
            Tasks.interrupt()
        }
    }

    // Clears all observers that were registered with other entities to compute the
    // respective property.
    private[this] def clearObservers(e: Entity, pk: PropertyKey): Unit = {
        val entitiesIterator = data.entrySet().iterator()
        while (entitiesIterator.hasNext()) {
            val (lock, properties) = entitiesIterator.next().getValue()
            val removeableO =
                withReadLock(lock) {
                    properties.get(pk).flatMap(pos ⇒ {
                        val os = pos._2
                        if (os ne null) {
                            os.find { po ⇒
                                val EPK(observerE, observerPK) = po.depender
                                (observerE eq e) && (observerPK == pk)
                            }
                        } else {
                            None
                        }
                    })
                }
            if (removeableO.isDefined) {
                withWriteLock(lock) {
                    properties.get(pk) foreach { pos ⇒
                        val os = pos._2
                        os -= removeableO.get
                    }
                }
            }
        }
    }

    /**
     * Associates / Updates the property with element e. If observers are registered
     * with the respective property then those observers will be informed about the
     * property change.
     */
    // Invariant: always only at most one function exists that will compute/update
    // the property p belonging to property kind k of an element e.
    private[this] def update(
        e: Entity,
        p: Property,
        finished: Boolean): Unit = accessEntity {
        val (lock, properties) = data.get(e)
        val pk = p.key

        if (finished) {
            clearObservers(e, pk)
        }

        val observers = withWriteLock(lock) {
            properties.get(pk) match {
                case Some((oldP, os)) ⇒
                    assert(
                        oldP != p,
                        s"$e: the old ($oldP) and the new property ($p) are identical")
                    if (finished) {
                        // All entities that observe this value are
                        // informed and then the observers are removed.
                        properties.put(p.key, (p, null /*The list of observers is no longer required!*/ ))
                        os
                    } else {
                        val newOs = os.filterNot(o ⇒ o.removeAfterNotification)
                        properties.put(p.key, (p, newOs))
                        os
                    }

                case None ⇒
                    val os = Buffer.empty[PropertyObserver]
                    properties.put(p.key, (p, os))
                    os
            }
        }
        observers foreach { o ⇒ o(e, p) }
    }
    //
    //    /**
    //     * Registers the observer, if the property is not yet available or equal to the
    //     * specified property value. If the property is already refined, the observer is
    //     * immediately invoked and not registered.
    //     *
    //     * @return `true` if an observer was registered, `false` otherwise.
    //     */
    //    private[this] def handleDependency(
    //        e: Entity,
    //        pk: PropertyKey,
    //        pOption: Option[Property],
    //        o: PropertyObserver): Unit = {
    //        // always only at most one function exists that will update the property p
    //        // of the element e
    //        val (lock, properties) = data.get(e)
    //        withWriteLock(lock) {
    //            val propertyAndObservers = properties.get(pk)
    //            if (propertyAndObservers == None) {
    //                properties.put(pk, (null, Buffer(o)))
    //                true
    //            } else {
    //                val Some((p, observers)) = propertyAndObservers
    //                if ((p eq null) || pOption.isEmpty || pOption.get == p) {
    //                    observers += o
    //                } else {
    //                    // ... the value in the store is already a more refined value
    //                    // than the value given by pOption
    //                    o (e, p)
    //                }
    //            }
    //        }
    //    }

    private[PropertyStore] def handleResult(r: PropertyComputationResult): Unit = {

        r match {
            case NoResult ⇒ // Nothing to do..

            case Result(e, p) ⇒
                update(e, p, finished = true)

            case MultiResult(results) ⇒
                results foreach { ep ⇒ val (e, p) = ep; update(e, p, finished = true) }

            case IntermediateResult(e, p, dependees: Traversable[EP], c) ⇒
                dependees foreach { ep ⇒

                    val dependeeE = ep.e
                    val dependeeP = ep.p
                    val dependeePK = dependeeP.key

                    def createObserver() = {
                        new DefaultPropertyObserver(EPK(e, p.key), false) {
                            def apply(dependeeE: Entity, dependeeP: Property): Unit = {
                                scheduleContinuation(dependeeE, dependeeP, c)
                            }
                        }
                    }

                    val (lock, properties) = data.get(dependeeE)
                    withWriteLock(lock) {
                        properties.get(dependeePK) match {
                            case None ⇒
                                properties.put(dependeePK, (null, Buffer(createObserver())))
                            case Some((null, os)) ⇒
                                os += createObserver()
                            case Some((`p`, os)) ⇒
                                os += createObserver()
                            case Some((dependeeNewP, os)) ⇒
                                os += createObserver()
                                scheduleContinuation(dependeeE, dependeeNewP, c)
                        }
                    }
                }
                update(e, p, finished = false)

            //            case IntermediateResult(results) ⇒
            //                results foreach { result ⇒ val (e, p) = result; update(e, p, false) }

            //            case result @ RefineableResult(results, dependingEntities) ⇒
            //                // 1) Store the results
            //                //
            //                results foreach { result ⇒ val (e, p) = result; update(e, p, false) }
            //                // 2) Register the observers
            //                //
            //                dependingEntities foreach { dependingEntity ⇒
            //                    val (e, pk, pOption, c) = dependingEntity
            //                    val o = new DefaultPropertyObserver(None) {
            //                        def apply(e: Entity, p: Property): Unit = {
            //                            // ... for each dependent property we have a refinement;
            //                            // let's reschedule the computation
            //                            scheduleContinuation(e, p, c)
            //                        }
            //                    }
            //
            //                    handleDependency(e, pk, pOption, o)
            //                }

            case suspended @ Suspended(dependerE, dependerPK, requiredE, requiredPK) ⇒

                // CONCEPT
                // First, let's get the property, then...
                //  - If we now have a property, let's immediately continue
                //    the computation.
                //  - If the property is still not computed, register an
                //    observer that will schedule the computation when the
                //    property was computed.

                def createPropertyObserver =
                    new DefaultPropertyObserver(EPK(dependerE, dependerPK), true) {
                        def apply(dependeeE: Entity, dependeeP: Property): Unit = {
                            val pc = (e: AnyRef) ⇒ suspended.continue(dependeeE, dependeeP)
                            scheduleComputation(dependerE, pc)
                        }
                    }

                val (lock, properties) = data.get(requiredE)
                val pc = withWriteLock(lock) {
                    properties.get(requiredPK) match {
                        case Some((requiredP, observers)) ⇒
                            if (requiredP eq null) {
                                // we have other computations that are also waiting...
                                observers += createPropertyObserver
                                null
                            } else {
                                // the property was computed in the meantime
                                (e: AnyRef) ⇒ suspended.continue(requiredE, requiredP)
                            }
                        case _ ⇒
                            // this computation is the first who is interested in the property
                            properties.put(requiredPK, (null, Buffer(createPropertyObserver)))
                            null
                    }
                }
                if (pc ne null) {
                    scheduleComputation(dependerE, pc)
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
        isInterrupted: () ⇒ Boolean)(
            implicit logContext: LogContext): PropertyStore = {

        val entitiesCount = entities.size
        val map = new JIDMap[Entity, PropertyStoreValue](entitiesCount)

        entities.foreach { e ⇒ map.put(e, (new ReentrantReadWriteLock, HMap.empty)) }

        new PropertyStore(map, isInterrupted)
    }

}

