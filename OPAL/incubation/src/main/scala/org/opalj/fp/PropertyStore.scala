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

import java.util.{ IdentityHashMap ⇒ JIDMap }
import java.util.{ Set ⇒ JSet }
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.{ ConcurrentHashMap ⇒ JCHMap }
import scala.collection.mutable.{ HashSet ⇒ HSet }
import scala.collection.mutable.{ HashMap ⇒ HMap }
import scala.collection.mutable.{ ListBuffer ⇒ Buffer }
import scala.collection.mutable.StringBuilder
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
import org.opalj.collection.mutable.ArrayMap
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ListBuffer

/**
 * The central store which manages the execution of all
 * computations that require and provide information about the entities of the store.
 *
 * ==Usage==
 * The general strategy when using the PropertyStore is to always
 * continue computing the property
 * of an entity and to collect the dependencies on those elements that are relevant.
 * I.e., if some information is not/
 * or not completely available, the analysis should still continue using
 * the provided information and (internally) records the dependency. Later on, when
 * the analysis has computed its result it reports the same and informs the framework
 * about its dependencies.
 *
 * ===Core Requirements on Property Computation Functions===
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
 * ===Cyclic Dependencies===
 * In general, it may happen that some analyses cannot make any progress, because
 * they are mutually dependent. In this case
 * the computation of a property p of an entity e1 depends
 * on the property p of an entity e2 that requires the property p of the entity e1.
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
 * atomic and by preventing each computation from acquiring more than one (write and/or
 * read) lock at a time.
 * The locking strategy is as follows:
 *  1.  Every entity is directly associated with a ReentrantReadWriteLock that
 *      is always used if a property for the respective entity is read or written.
 *      (Independent of the kind of property that is accessed.)
 *  1.  Associated information (e.g., the internally created observers) also use
 *      the lock associated with the entity.
 *  1.  Each computation is potentially executed concurrently and it is required
 *      that each computation is thread-safe.
 *  1.  The store as a whole is associated with a lock to enable selected methods
 *      to get a consistent view.
 */
// COMMON ABBREVIATONS USED IN THE FOLLOWING:
// ==========================================
// e = ENTITY
// p = Property
// ps = Properties
// pk = PropertyKey
// pc = (Property)Computation
// c = Continuation (The rest of a computation if a specific, dependend property was computed.)
// o = (Property)Observer
// os = (Property)Observers
// EPK = An entity and a property key
// EP = An entity and an associated property
class PropertyStore private (
        private[this] val data: JIDMap[Entity, PropertyStoreValue],
        val isInterrupted: () ⇒ Boolean)(
                implicit val logContext: LogContext) { store ⇒

    import UpdateTypes.FinalUpdate
    import UpdateTypes.OneStepFinalUpdate
    import UpdateTypes.IntermediateUpdate

    private[this] val propagationCount = new java.util.concurrent.atomic.AtomicLong(0)

    // We want to be able to make sure that methods that access the store as
    // a whole always get a consistent snapshot view
    private[this] val storeLock = new ReentrantReadWriteLock
    @inline private[this] def accessEntity[B](f: ⇒ B) = Locking.withReadLock(storeLock)(f)
    @inline private[this] def accessStore[B](f: ⇒ B) = Locking.withWriteLock(storeLock)(f)

    // The list of observers used by the entity to compute the property of kind k (EPK).
    // In other words: the mapping between a Depender and its Observers!
    // The list of observers needs to be maintained whenever:
    //  1. A computation of a property finishes. In this kind all observers need to
    //     be notified and removed from this map afterwards.
    //  1. A computation of a property generates an [[IntermediatResult]], but the
    //     the observer is one-time observer. (Such observers are only used internally.
    private[this] final val observers = new JCHMap[EPK, Buffer[(EPK, PropertyObserver)]]()

    /**
     * The final set of all stored elements.
     */
    // This set is not mutated.
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
    def apply(e: Entity, pk: PropertyKey): Option[Property] = {
        accessEntity {
            val (lock, properties) = data.get(e)
            withReadLock(lock) { properties(pk.id) }
        } match {
            case null                         ⇒ None
            case (property, _ /*observers*/ ) ⇒ Option(property)
        }
    }

    def require(
        dependerE: Entity,
        dependerPK: PropertyKey,
        dependeeE: Entity,
        dependeePK: PropertyKey)(
            c: Continuation): PropertyComputationResult = accessEntity {

        @inline def suspend = new Suspended(dependerE, dependerPK, dependeeE, dependeePK) {
            def continue(dependeeE: Entity, dependeeP: Property) = c(dependeeE, dependeeP)
        }

        this(dependeeE, dependeePK) match {
            case Some(dependeeP) ⇒ c(dependeeE, dependeeP)
            case None            ⇒ suspend
        }
    }

    /**
     * Tests if all entities have the given property. If the respective property is
     * not yet available, the computation will be suspended until the property of
     * the respective kind is available. Hence, it only makes sense to use this
     * function if the respective property is computed by an independent analysis or
     * if it is an inherent property of the analysis that the information about the
     * dependees is guaranteed to become available without requiring information
     * about the depender.
     */
    def allHaveProperty(
        dependerE: Entity, dependerPK: PropertyKey,
        dependees: Traversable[Entity], expectedP: Property)(
            c: (Boolean) ⇒ PropertyComputationResult): PropertyComputationResult = {

        // The idea is to eagerly try to determine if the answer might be false.
        val dependeePK = expectedP.key
        var remainingEs = dependees
        var unavailableEs: List[Entity] = Nil
        while (remainingEs.nonEmpty) {
            val dependeeE = remainingEs.head
            remainingEs = remainingEs.tail
            this(dependeeE, dependeePK) match {
                case Some(dependeeP) ⇒
                    if (expectedP != dependeeP)
                        return c(false);
                case None ⇒
                    unavailableEs = dependeeE :: unavailableEs
            }
        }

        if (unavailableEs.nonEmpty) {
            // Let's wait on the next result and then try to get as many results as
            // possible, by using haveProperty again... i.e., we try to minimize the
            // number of suspended objects that we need to create.
            val deependeeE = unavailableEs.head
            new Suspended(dependerE, dependerPK, deependeeE, dependeePK) {
                def continue(
                    dependeeE: Entity,
                    dependeeP: Property): PropertyComputationResult = {
                    if (expectedP != dependeeP)
                        c(false);
                    else {
                        allHaveProperty(
                            dependerE, dependerPK,
                            unavailableEs.tail,
                            expectedP)(c)
                    }
                }
            }
        } else {
            // all information was available and was always as expected
            c(true)
        }
    }

    /**
     * Associate the given property `p` with given entity `e`.
     *
     * This method must not be used if the given entity might already be associated with
     * a property of the respective kind or if there might be a computation that
     * computes the property p.
     *
     * The primary use case is an analysis that does not use the property store for
     * executing the analysis, but wants to store some results in the store.
     */
    def set(e: Entity, p: Property): Unit = update(e, p, OneStepFinalUpdate)

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
                properties(pk.id) match {
                    case null | (null, _) ⇒ false
                    case _                ⇒ true
                }
            }
        valuesWithProperty map { v ⇒ (v.getKey, v.getValue._2(pk.id)._1) }
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
    def <<(pc: PropertyComputation): Unit = {
        import scala.collection.JavaConverters._
        bulkScheduleComputations(keys.asScala, pc)
    }

    /**
     * Registers a function that calculates a property for those elements
     * of the store that pass the filter `f`.
     *
     * @param f A filter that selects those entities that are relevant to the analysis.
     *      For which the analysis may compute some property.
     */
    def <|<(f: Entity ⇒ Boolean, c: PropertyComputation): Unit = {
        val it = keys.iterator()
        var es: List[Entity] = Nil
        while (it.hasNext()) {
            if (isInterrupted())
                return ;
            val e = it.next()
            if (f(e)) es = e :: es
        }
        bulkScheduleComputations(es, c)
    }

    def <||<[E <: Entity](
        pf: PartialFunction[Entity, E],
        c: E ⇒ PropertyComputationResult): Unit = {
        import scala.collection.JavaConverters._
        val es = keys.iterator().asScala.collect(pf).toSeq
        bulkScheduleComputations(es, c.asInstanceOf[Object ⇒ PropertyComputationResult])
    }

    /**
     * Registers a function that calculates a property for those elements
     * of the store that pass the filter and which are sorted by the given function `s`.
     * The given function `s`
     *
     * @param pf A filter that selects those entities that are relevant to the analysis.
     *      For which the analysis may compute some property.
     * @param s An ordering defined on the selected entities.
     */
    def <||~<[E <: Entity](
        pf: PartialFunction[Entity, E],
        s: Ordering[E],
        c: E ⇒ PropertyComputationResult): Unit = {
        import scala.collection.JavaConverters._
        val es = keys.iterator().asScala.collect(pf).toSeq.sorted(s)
        bulkScheduleComputations(es, c.asInstanceOf[Object ⇒ PropertyComputationResult])
    }

    /**
     * Awaits the completion of the computation of all
     * properties of all previously registered property computation functions. I.e.,
     * if a second thread is used to register [[PropertyComputation]] functions then
     * no guarantees are given. In general it is recommended to schedule all
     * property computation functions using one thread.
     *
     * This function is only '''guaranteed''' to wait on the completion of the computation
     * of those properties that were registered by this thread.
     */
    def waitOnPropertyComputationCompletion(
        useDefaultForIncomputableProperties: Boolean = true): Unit = {
        Tasks.waitOnCompletion(useDefaultForIncomputableProperties)
    }

    /**
     * Returns a string representation of the stored properties.
     */
    def toString(printProperties: Boolean): String = accessStore /* <=> Exclusive Access*/ {
        val properties = new StringBuilder
        var propertiesCount = 0
        var unsatisfiedPropertyDependencies = 0
        val it = data.entrySet().iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val ps = entry.getValue._2.map { (pk, pos) ⇒
                val (p, os) = pos
                (
                    if (p eq null) {
                        unsatisfiedPropertyDependencies += 1
                        s"<Unsatisfied: ${PropertyKey.name(pk)}>"
                    } else {
                        propertiesCount += 1
                        p.toString
                    }
                )+"["+(if (os eq null) 0 else os.size)+"]"
            }
            if (printProperties && ps.nonEmpty) {
                val s = ps.mkString("\t\t"+entry.getKey.toString+" => {", ", ", "}\n")
                properties.append(s)
            }
        }
        "PropertyStore(\n"+
            s"\tentitiesCount=${data.size()},\n"+
            s"\texecutedComputations=${Tasks.executedComputations}\n"+
            s"\tpropagations=${propagationCount.get}\n"+
            s"\tunsatisfiedPropertyDependencies=$unsatisfiedPropertyDependencies\n"+
            s"\tproperties[$propertiesCount]"+
            (if (printProperties) s"=\n$properties)" else ")")
    }

    override def toString: String = toString(false)

    //
    //
    // INTERNAL IMPLEMENTATION
    //
    //

    private[this] final val threadPool = ThreadPoolN(Math.max(NumberOfThreadsForCPUBoundTasks, 2))

    /**
     * General handling of the tasks that are executed.
     */
    private[this] object Tasks {

        @volatile var useFallbackForIncomputableProperties: Boolean = false

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

            def clearAllObservers(): Unit = {

                threadPool.awaitTermination(5000l, TimeUnit.MILLISECONDS)

                OPALLogger.debug("analysis progress", "garbage collecting property computations")
                accessStore {
                    observers.clear()
                    import scala.collection.JavaConversions._
                    for {
                        entry ← data.entrySet()
                        (_ /*p*/ , os) ← entry.getValue()._2.values
                    } {
                        os.clear()
                    }
                }
            }

            // Invoke the garbage collector either in this thread if this thread
            // is not a thread belonging to the property store's thread pool or
            // in a new thread.
            if (threadPool.group == Thread.currentThread().getThreadGroup) {
                val t = new Thread(new Runnable { def run(): Unit = clearAllObservers() })
                t.start()
            } else {
                clearAllObservers()
            }

        }

        def taskStarted() = synchronized {
            scheduled += 1
        }

        def tasksStarted(tasksCount: Int) = synchronized {
            scheduled += tasksCount
        }

        def tasksAborted(tasksCount: Int) = synchronized {
            scheduled -= tasksCount
        }

        private[this] def registeredObservers: Int = {
            import scala.collection.JavaConverters._
            val ps = data.values().asScala.map(_._2)
            val poss = ps.map(_.values).flatten
            poss.map(pos ⇒ if (pos._2 eq null) 0 else pos._2.filter(_ ne null).size).sum
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
                }

                if (scheduled == 0 /*scheduled is still === 0*/ ) {
                    OPALLogger.debug(
                        "analysis progress",
                        "computation of all properties finished"+
                            s" (remaining computations: $registeredObservers)")
                    notifyAll()
                } else {
                    OPALLogger.debug(
                        "analysis progress",
                        s"(re)scheduled $scheduled property computations")
                }
            }
        }

        @inline private[this] def getObservers(e: Entity, pkId: Int): Observers = {
            val value = data.get(e)
            if (value eq null)
                return null;

            val (_, properties) = value
            if (properties eq null)
                return null;

            val (_, observers) = properties(pkId)
            observers
        }

        // THIS METHOD REQUIRES EXCLUSIVE ACCESS TO THE STORE!
        // Handle unsatisfied dependencies supports both cases:
        //  1. computations that are part of a cyclic computation dependency
        //  1. computations that depend on knowledge related to a specific kind of
        //     property that was not computed (final lack of knowledge) and for
        //     which no computation exits.
        private[this] def handleUnsatisfiedDependencies(): Unit = {
            import scala.collection.JavaConverters._
            // GIVEN: data: JIDMap[Entity, PropertyStoreValue]
            // GIVEN: observers: new JCHMap[EPK, Buffer[(EPK, PropertyObserver)]]()
            val observers = store.observers

            val indirectlyIncomputableEPKs = HSet.empty[EPK]

            // All those EPKs that require some information that do not depend (directly
            // or indirectly) on an incomputableEPK. However, this set also includes
            // those EPKs that may depend on another strongly connected component which
            // is a knot (which has no outgoing dependency).
            val cyclicComputableEPKCandidates = HSet.empty[EPK]

            // Let's determine all EPKs that have a dependency on an incomputableEPK
            // (They may be in a strongly connected component, but we don't care about
            // these, because they may still be subject to some refinement.)
            def determineIncomputableEPKs(dependerEPK: EPK): Unit = {
                var worklist = List(dependerEPK)
                while (worklist.nonEmpty) {
                    val dependerEPK = worklist.head
                    worklist = worklist.tail
                    val ps = data.get(dependerEPK.e)._2(dependerEPK._2.id)
                    if ((ps ne null) && (ps._2 ne null)) {
                        val os = ps._2
                        os foreach { o ⇒
                            val dependerEPK = o.depender
                            if (indirectlyIncomputableEPKs.add(dependerEPK)) {
                                cyclicComputableEPKCandidates -= dependerEPK
                                worklist = dependerEPK :: worklist
                            }
                        }
                    }
                }
            }

            val directlyIncomputableEPKs = HSet.empty[EPK]
            observers.entrySet().asScala foreach { e ⇒
                val dependerEPK = e.getKey
                if (!indirectlyIncomputableEPKs.contains(dependerEPK)) {
                    val dependees = e.getValue
                    dependees foreach { dependee ⇒
                        val dependeeEPK = dependee._1
                        if (!observers.containsKey(dependeeEPK)) {
                            directlyIncomputableEPKs += dependeeEPK
                            indirectlyIncomputableEPKs += dependerEPK
                            determineIncomputableEPKs(dependerEPK)
                        } else {
                            // this EPK observers EPKs that have observers...
                            // but, is it also observed?
                            val observers = getObservers(dependerEPK.e, dependerEPK.pk.id)
                            if ((observers ne null) && observers.nonEmpty) {
                                cyclicComputableEPKCandidates += dependerEPK
                            }
                        }
                    }
                }
            }

            println("Directly..."+directlyIncomputableEPKs)
            println("Indirectly..."+indirectlyIncomputableEPKs)
            println("Cyclic..."+cyclicComputableEPKCandidates)

            // Now

            // Let's get the set of observers that will never be notified, because
            // there are no open computations related to the respective property.
            // This is also the case if no respective analysis is registered so far.
            if (useFallbackForIncomputableProperties) {
                for {
                    EPK(e, pk) ← directlyIncomputableEPKs
                } {
                    val defaultP = PropertyKey.fallbackProperty(pk.id)
                    OPALLogger.debug(
                        "analysis progress",
                        s"associated default property $defaultP with $e")
                    scheduleHandleResult(Result(e, defaultP))
                }
            }
        }

        def waitOnCompletion(useFallbackForIncomputableProperties: Boolean): Unit =
            synchronized {
                this.useFallbackForIncomputableProperties = useFallbackForIncomputableProperties
                while (scheduled > 0) { wait }
            }
    }

    /**
     * Schedules the handling of the result of a property computation.
     */
    private[this] def scheduleHandleResult(r: PropertyComputationResult): Unit = {
        scheduleTask(new Runnable {
            def run() = {
                try {
                    handleResult(r)
                } catch {
                    case t: Throwable ⇒ handleUncaughtException(Thread.currentThread(), t)
                } finally {
                    Tasks.taskCompleted()
                }
            }
        })
    }

    /**
     * Schedules the continuation w.r.t. the entity `e`.
     */
    private[this] def scheduleContinuation(
        dependeeE: Entity,
        dependeeP: Property,
        c: Continuation): Unit = {
        scheduleTask(new Runnable {
            def run() = {
                try {
                    handleResult(c(dependeeE, dependeeP))
                } catch {
                    case t: Throwable ⇒ handleUncaughtException(Thread.currentThread(), t)
                } finally {
                    Tasks.taskCompleted()
                }
            }
        })
    }

    /**
     * Schedules the computation of a property w.r.t. the entity `e`.
     */
    private[this] def scheduleComputation(e: Entity, pc: PropertyComputation): Unit = {
        scheduleTask(new Runnable {
            def run() = {
                try {
                    handleResult(pc(e))
                } catch {
                    case t: Throwable ⇒ handleUncaughtException(Thread.currentThread(), t)
                } finally {
                    Tasks.taskCompleted()
                }
            }
        })
    }

    /**
     * Schedules the computation of a property w.r.t. the entity `e`.
     */
    private[this] def bulkScheduleComputations(
        es: Traversable[_ <: Entity],
        pc: PropertyComputation): Unit = {
        val tasks = es.map { e ⇒
            new Runnable {
                def run() = {
                    try {
                        handleResult(pc(e))
                    } catch {
                        case t: Throwable ⇒ handleUncaughtException(Thread.currentThread(), t)
                    } finally {
                        Tasks.taskCompleted()
                    }
                }
            }
        }
        if (isInterrupted())
            return ;

        scheduleTasks(tasks)
    }

    //    private[this] def scheduleTask(t: () ⇒ Unit): Unit = {
    //        scheduleTask(
    //            new Runnable {
    //                def run(): Unit =
    //                    try {
    //                        t()
    //                    } catch {
    //                        case t: Throwable ⇒
    //                            handleUncaughtException(t)
    //                            throw t
    //                    } finally {
    //                        Tasks.taskCompleted()
    //                    }
    //            }
    //        )
    //    }

    private[this] def scheduleTask(r: Runnable): Unit = {
        if (isInterrupted()) {
            Tasks.interrupt()
            return ;
        }

        Tasks.taskStarted()
        try {
            threadPool.submit(r)
        } catch {
            case reh: RejectedExecutionException ⇒
                Tasks.taskCompleted()

            case t: Throwable ⇒
                Tasks.taskCompleted()
                handleUncaughtException(t);
        }
    }

    private[this] def scheduleTasks(rs: Traversable[Runnable]): Unit = {
        if (isInterrupted()) {
            Tasks.interrupt()
            return ;
        }
        val allTasksCount = rs.size
        Tasks.tasksStarted(allTasksCount)
        var startedTasksCount = 0
        try {
            rs foreach { r ⇒ threadPool.execute(r); startedTasksCount += 1 }
        } catch {
            case reh: RejectedExecutionException ⇒
                Tasks.tasksAborted(allTasksCount - startedTasksCount)

            case t: Throwable ⇒
                Tasks.tasksAborted(allTasksCount - startedTasksCount)
                handleUncaughtException(t);
        }
    }

    // Clears all observers that were registered with other entities to compute the
    // respective property of the given entity.
    // This method handles the situation where the computation of a property
    // potentially depended on some other entities and we now have a final result
    // and now need to cleanup the registered observers.
    private[this] def clearAllObservers(dependerE: Entity, dependerPK: PropertyKey): Unit = {
        // observers : JCHMap[EPK, Buffer[(EPK, PropertyObserver)]]()
        val dependerEPK = EPK(dependerE, dependerPK)
        val dependerOs = observers.get(dependerEPK)
        if (dependerOs ne null) {
            dependerOs foreach { epkos ⇒
                val (dependeeEPK, dependerO) = epkos
                val (lock, dependeePs) = data.get(dependeeEPK.e)
                withWriteLock(lock) {
                    val dependeeOs = dependeePs(dependeeEPK.pk.id)._2
                    if (dependeeOs ne null)
                        dependeeOs -= dependerO
                }
            }

            observers.remove(dependerEPK)
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
        updateType: UpdateType): Unit = accessEntity {
        val (lock, properties) = data.get(e)
        val pk = p.key

        var obsoleteOs: List[PropertyObserver] = Nil
        val os = withWriteLock(lock) {
            properties(pk.id) match {
                case null ⇒
                    // No one was interested in this property so far...
                    val os = Buffer.empty[PropertyObserver]
                    properties(p.key.id) = (p, os)
                    return ;

                case (oldP, os) ⇒
                    assert(
                        oldP != p,
                        s"$e: the old ($oldP) and the new property ($p) are identical")

                    updateType match {

                        case OneStepFinalUpdate ⇒
                            // The computation did not create any (still living) dependencies!
                            properties(pk.id) = (p, null /*The list of observers is no longer required!*/ )

                        case FinalUpdate ⇒
                            // We still may observe other entities... we have to clear
                            // these dependencies.
                            clearAllObservers(e, pk)
                            properties(pk.id) = (p, null /*The list of observers is no longer required!*/ )

                        case IntermediateUpdate ⇒
                            // We still continue observing all other entities;
                            // hence, we only need to clear our one-time observers.
                            val newOs = os.filterNot { o ⇒
                                if (o.removeAfterNotification) {
                                    obsoleteOs = o :: obsoleteOs
                                    true
                                } else {
                                    false
                                }
                            }
                            properties(p.key.id) = (p, newOs)
                    }
                    os
            }
        }
        // ... non-exclusive access
        if (obsoleteOs.nonEmpty) {
            val dependeeEPK = EPK(e, pk)
            obsoleteOs foreach { o ⇒
                val depender = o.depender
                val (lock, _) = store.data.get(depender.e)
                withWriteLock(lock) {
                    val dependerOs = store.observers.get(depender)
                    if (dependerOs ne null) {
                        dependerOs -= ((dependeeEPK, o))
                    }
                }
            }
        }
        os foreach { o ⇒ o(e, p) }
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
            case NoResult              ⇒ // Nothing to do..

            case ImmediateResult(e, p) ⇒ update(e, p, OneStepFinalUpdate)

            case Result(e, p)          ⇒ update(e, p, FinalUpdate)

            case ImmediateMultiResult(results) ⇒
                results foreach { ep ⇒ val (e, p) = ep; update(e, p, OneStepFinalUpdate) }

            case MultiResult(results) ⇒
                results foreach { ep ⇒ val (e, p) = ep; update(e, p, FinalUpdate) }

            case IntermediateResult(e, p, dependees: Traversable[EP], c) ⇒
                val dependerEPK = EPK(e, p.key)
                dependees foreach { ep ⇒

                    val dependeeE = ep.e
                    val dependeeP = ep.p
                    val dependeePK = dependeeP.key.id
                    val dependeeEPK = EPK(dependeeE, dependeeP.key)
                    val (dependeeLock, properties) = data.get(dependeeE)

                    val o = {
                        new DefaultPropertyObserver(dependerEPK, false) {
                            def apply(dependeeE: Entity, dependeeP: Property): Unit = {
                                propagationCount.incrementAndGet()
                                scheduleContinuation(dependeeE, dependeeP, c)
                            }
                        }
                    }

                    withWriteLock(dependeeLock) {
                        properties(dependeePK) match {
                            case null       ⇒ properties(dependeePK) = (null, Buffer(o))
                            case (null, os) ⇒ os += o
                            case (`p`, os)  ⇒ os += o
                            case (dependeeNewP, os) ⇒
                                os += o
                                scheduleContinuation(dependeeE, dependeeNewP, c)
                        }
                    }
                    val (dependerLock, _) = data.get(e)
                    withWriteLock(dependerLock) {
                        var buffer = observers.get(dependerEPK)
                        if (buffer eq null) {
                            buffer = ListBuffer.empty
                            observers.put(dependerEPK, buffer)
                        }
                        buffer += ((dependeeEPK, o))
                    }
                }
                update(e, p, IntermediateUpdate)

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

            case suspended @ Suspended(dependerE, dependerPK, dependeeE, dependeePK) ⇒

                // CONCEPT
                // First, let's get the property, then...
                //  - If we now have a property, let's immediately continue
                //    the computation.
                //  - If the property is still not computed, register an
                //    observer that will schedule the computation when the
                //    property was computed.

                def createPropertyObserver(): PropertyObserver =
                    new DefaultPropertyObserver(EPK(dependerE, dependerPK), true) {
                        def apply(dependeeE: Entity, dependeeP: Property): Unit = {
                            propagationCount.incrementAndGet()
                            val pc = (e: AnyRef) ⇒ suspended.continue(dependeeE, dependeeP)
                            scheduleComputation(dependerE, pc)
                        }
                    }

                val (lock, properties) = data.get(dependeeE)
                val pc = withWriteLock(lock) {
                    properties(dependeePK.id) match {
                        case null ⇒
                            // this computation is the first who is interested in the property
                            properties(dependeePK.id) = (null, Buffer(createPropertyObserver()))
                            null

                        case (dependeeP, observers) ⇒
                            if (dependeeP eq null) {
                                // we have other computations that are also waiting...
                                observers += createPropertyObserver()
                                null
                            } else {
                                // the property was computed in the meantime
                                (e: AnyRef) ⇒ suspended.continue(dependeeE, dependeeP)
                            }
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

        entities.foreach { e ⇒ map.put(e, (new ReentrantReadWriteLock, ArrayMap.empty)) }

        new PropertyStore(map, isInterrupted)
    }

}

