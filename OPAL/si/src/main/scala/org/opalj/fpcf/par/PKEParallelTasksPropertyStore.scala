/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.lang.System.identityHashCode
import java.util.{HashMap ⇒ JHashMap}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.collection.{Map ⇒ SomeMap}

import org.opalj.control.foreachValue
import org.opalj.collection.mutable.RefAccumulator
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.GlobalLogContext
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.fastTrackPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.notComputedPropertyBasedOnPKId
import org.opalj.fpcf.PropertyKey.isPropertyKeyForSimplePropertyBasedOnPKId

/**
 * A concurrent implementation of the property store which parallels the execution of the scheduled
 * computations.
 *
 * Entities are stored after computation.
 *
 * ==Implementation==
 * The idea is to use one specific thread (the `store updates thread`) for processing updates
 * to the store. This enables us to avoid any synchronization w.r.t. updating the
 * depender/dependee relations.
 *
 * We use `NumberOfThreadsForProcessingPropertyComputations` threads for processing the
 * scheduled computations.
 *
 * @author Michael Eichberg
 */
final class PKEParallelTasksPropertyStore private (
        val ctx:                                              Map[Class[_], AnyRef],
        val NumberOfThreadsForProcessingPropertyComputations: Int,
        val tracer:                                           Option[PropertyStoreTracer]
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore {
    store ⇒

    // --------------------------------------------------------------------------------------------
    //
    // CAPABILITIES
    //
    // --------------------------------------------------------------------------------------------

    final def supportsFastTrackPropertyComputations: Boolean = true

    // --------------------------------------------------------------------------------------------
    //
    // STATISTICS
    //
    // --------------------------------------------------------------------------------------------

    // Tasks are allowed to schedule further tasks... therefore, the scheduled tasks counter
    // has to be thread-safe.
    private[this] val scheduledTasksCounter: AtomicInteger = new AtomicInteger(0)
    def scheduledTasksCount: Int = scheduledTasksCounter.get

    private[this] val directInTaskThreadPropertyComputationsCounter: AtomicInteger = new AtomicInteger(0)
    def directInTaskThreadPropertyComputationsCount: Int = {
        directInTaskThreadPropertyComputationsCounter.get
    }

    // Fast-track properties are eagerly computed in the thread requiring the values
    // and are stored using idempotent results
    private[this] val fastTrackPropertiesCounter: AtomicInteger = new AtomicInteger(0)
    def fastTrackPropertiesCount: Int = fastTrackPropertiesCounter.get

    private[this] var redundantIdempotentResultsCounter = 0
    def redundantIdempotentResultsCount: Int = redundantIdempotentResultsCounter

    private[this] var uselessPartialResultComputationCounter = 0
    def uselessPartialResultComputationCount: Int = uselessPartialResultComputationCounter

    private[this] var scheduledLazyTasksCounter = 0
    def scheduledLazyTasksCount: Int = scheduledLazyTasksCounter

    private[this] val fallbacksUsedCounter: AtomicInteger = new AtomicInteger(0)
    def fallbacksUsedCount: Int = fallbacksUsedCounter.get

    private[this] var scheduledOnUpdateComputationsCounter = 0
    def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputationsCounter

    private[this] var scheduledDependeeUpdatesCounter = 0
    /** Computations of dependees which are scheduled immediately. */
    def scheduledDependeeUpdatesCount: Int = scheduledDependeeUpdatesCounter

    private[this] var directDependerOnUpdateComputationsCounter = 0
    /** Computations which are executed immediately and which are not scheduled. */
    def directDependerOnUpdateComputationsCount: Int = directDependerOnUpdateComputationsCounter

    private[this] var directDependeeUpdatesCounter = 0
    def directDependeeUpdatesCount: Int = directDependeeUpdatesCounter

    def immediateOnUpdateComputationsCount: Int = {
        directDependeeUpdatesCounter + scheduledDependeeUpdatesCounter
    }

    private[this] val maxTasksQueueSize: AtomicInteger = new AtomicInteger(-1)

    private[this] var updatesCounter = 0
    private[this] var oneStepFinalUpdatesCounter = 0

    private[this] var resolvedCSCCsCounter = 0
    def resolvedCSCCsCount: Int = resolvedCSCCsCounter

    private[this] var quiescenceCounter = 0
    def quiescenceCount: Int = quiescenceCounter

    def statistics: SomeMap[String, Int] = {
        val statistics = mutable.LinkedHashMap[String, Int]()

        if (debug) statistics += "scheduled tasks" -> scheduledTasksCount
        statistics += "scheduled lazy tasks (fast track computations of lazy properties are not counted)" -> scheduledLazyTasksCount
        if (debug) statistics += "max tasks queue size" -> maxTasksQueueSize.get
        if (debug) statistics += "fast-track properties computations" -> fastTrackPropertiesCount
        if (debug) statistics += "computations of fallback properties (queried but not computed properties)" -> fallbacksUsedCount
        statistics += "property store updates" -> updatesCounter
        statistics += "computations which in one step computed a final result" -> oneStepFinalUpdatesCounter
        statistics += "redundant fast-track/fallback property computations" -> redundantIdempotentResultsCount
        statistics += "useless partial result computations" -> uselessPartialResultComputationCount
        statistics += "scheduled reevaluation of dependees due to updated dependers" -> scheduledDependeeUpdatesCount
        if (debug) statistics += "direct in task-thread property computations (cheap property computation or tasks queue is full enough)" -> directInTaskThreadPropertyComputationsCount
        statistics += "direct evaluation of dependers (cheap property computation)" -> directDependerOnUpdateComputationsCount
        statistics += "direct reevaluations of dependee due to updated dependers (cheap property computation)" -> directDependeeUpdatesCount
        statistics += "number of times the store reached quiescence" -> quiescenceCount
        statistics += "resolved cSCCs" -> resolvedCSCCsCount

        statistics
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE DATA STRUCTURES
    //
    // Please note, that all data-structures are organized based on the property kind first; i.e.,
    // the property kind id is the index in the underlying array.
    //
    // --------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    // The following  data-structures are potentially read concurrently.
    //

    // Read by many threads, updated only by the store updates thread.
    // ONLY contains `true` intermediate and final properties; i.e., the value is never null.
    private[this] val properties: Array[ConcurrentHashMap[Entity, SomeEPS]] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    // The following "var"s/"arrays" do not need to be volatile, because the updates – which are
    // only done while the store is quiescent – are done within the driver thread andF are guaranteed
    // to be visible to all relevant threads. The (tasks/storeupdates)semaphores.release methods,
    // which necessarily will be called to start some computations/process some result –
    // do establish the required happens-before relation.

    /** Those computations that will only be scheduled if the property is required. */
    private[this] val lazyComputations: Array[SomePropertyComputation] = {
        new Array(SupportedPropertyKinds)
    }

    /** Computations that will be triggered when a new property becomes available. */
    private[this] val triggeredComputations: Array[Array[SomePropertyComputation]] = {
        new Array(SupportedPropertyKinds)
    }

    // Contains all those EPKs that should be computed until we have a final result.
    // When we have computed a final result, the epk will be deleted.
    private[this] val forcedComputations: Array[ConcurrentHashMap[Entity, Entity] /*...a set*/ ] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    private[this] var previouslyComputedPropertyKinds: Array[Boolean] = {
        new Array[Boolean](SupportedPropertyKinds)
    }

    private[this] var computedPropertyKinds: Array[Boolean] = _ /*null*/

    private[this] var delayedPropertyKinds: Array[Boolean] = _ /*null*/

    // ---------------------------------------------------------------------------------------------
    // The following three data-structures are never read or updated concurrently;
    // they are only read/updated by the store updates thread.
    //
    private[this] val dependers: Array[JHashMap[Entity, JHashMap[SomeEPK, (OnUpdateContinuation, PropertyComputationHint)]]] = {
        Array.fill(SupportedPropertyKinds) { new JHashMap() }
    }
    private[this] val dependees: Array[JHashMap[Entity, Traversable[SomeEOptionP]]] = {
        Array.fill(SupportedPropertyKinds) { new JHashMap() }
    }
    private[this] val triggeredLazyComputations: Array[mutable.Set[Entity]] = {
        Array.fill(SupportedPropertyKinds) { mutable.HashSet.empty }
    }

    // ---------------------------------------------------------------------------------------------
    // Helper data-structure to control the overall progress.
    //

    // The latch is initialized whenever the number of jobs goes from "0" to "1".
    // Note that the setup thread will always be responsible to - at least - schedule an initial
    // task/force an initial evaluation.
    @volatile private[this] var latch: CountDownLatch = _

    /**
     * The number of property computations and results which have not been completely processed.
     */
    private[this] val openJobs = new AtomicInteger(0)
    /**
     * MUST BE CALLED BEFORE the job is actually processed.
     */
    private[this] def incOpenJobs(): Unit = {
        val v = openJobs.getAndIncrement()
        if (v == 0) { latch = new CountDownLatch(0) }
    }
    /**
     * MUST BE CALLED AFTER the respective task was processed and all effects have be applied.
     */
    private[this] def decOpenJobs(): Unit = {
        val v = openJobs.decrementAndGet()
        if (v == 0) { latch.countDown() }
    }

    // --------------------------------------------------------------------------------------------
    //
    // Handling property computations.
    //
    // --------------------------------------------------------------------------------------------

    private[this] val propertyStoreThreads: ThreadGroup = {
        new ThreadGroup(s"OPAL - Property Store ${store.hashCode().toHexString} Threads")
    }

    private[this] final val TaskQueues = 12
    private[this] final val DefaultTaskQueue = 0
    /**
     * Array of lists of scheduled (on update) property computations - they will be processed
     * in parallel, giving lists stored at the beginning of the tasks array priority.
     */
    private[this] val tasks = {
        Array.fill(TaskQueues)(new ConcurrentLinkedQueue[QualifiedTask[_ <: Entity]]())
    }
    private[this] val tasksSemaphore = new Semaphore(0)

    private[this] def appendTask(task: QualifiedTask[_ <: Entity]): Unit = {
        incOpenJobs()
        tasks(DefaultTaskQueue).offer(task)
        tasksSemaphore.release()
    }

    private[this] def appendTask(queueId: Int, task: QualifiedTask[_ <: Entity]): Unit = {
        incOpenJobs()
        tasks(Math.min(queueId, TaskQueues - 1)).offer(task)
        tasksSemaphore.release()
    }

    @volatile private[this] var tasksProcessors: ThreadGroup = {

        @inline def processTask(): Unit = {
            if (debug) {
                var currentMaxTasksQueueSize = maxTasksQueueSize.get
                var newMaxTasksQueueSize = Math.max(maxTasksQueueSize.get, tasksSemaphore.availablePermits())
                while (currentMaxTasksQueueSize < newMaxTasksQueueSize &&
                    !maxTasksQueueSize.compareAndSet(currentMaxTasksQueueSize, newMaxTasksQueueSize)) {
                    currentMaxTasksQueueSize = maxTasksQueueSize.get
                    newMaxTasksQueueSize = Math.max(maxTasksQueueSize.get, tasksSemaphore.availablePermits())
                }
            }

            tasksSemaphore.acquire()
            // we know that we will eventually find a task in some queue
            var task: QualifiedTask[_ <: Entity] = null
            var qid = 0
            do { task = tasks(qid).poll(); qid = (qid + 1) % TaskQueues } while (task eq null)
            try {
                // As a sideeffect of processing a task, we may have (implicit) calls to schedule
                // and also implicit handleResult calls; both will increase openJobs.
                if (task.isInitialTask) {
                    task.apply()
                } else {
                    // TODO check if required; i.e., if we are forced or have dependees.
                    task.apply()
                }
            } finally {
                decOpenJobs()
            }
        }

        val tg = new ThreadGroup(propertyStoreThreads, "OPAL - Property Computations Processors")
        for { i ← 1 to NumberOfThreadsForProcessingPropertyComputations } {
            val t = new Thread(tg, s"OPAL - Property Computations Processor $i") {
                override def run(): Unit = gatherExceptions {
                    do {
                        while (!store.isSuspended()) {
                            processTask()
                            if (exception != null)
                                return ;
                        }
                        // The store is suspended; hence, we want to keep the thread alive.
                        Thread.sleep(1000)
                    } while (exception == null)
                }
            }
            t.setDaemon(true)
            t.start()
        }
        tg
    }

    // --------------------------------------------------------------------------------------------
    //
    // Handling PropertyStore updates.
    //
    // --------------------------------------------------------------------------------------------

    /**
     * The jobs which update the store.
     */
    private[this] final val StoreUpdateQueues = 5
    private[this] val storeUpdates = {
        Array.fill(StoreUpdateQueues)(new ConcurrentLinkedQueue[StoreUpdate]())
    }
    private[this] val storeUpdatesSemaphore = new Semaphore(0)

    private[this] def appendStoreUpdate(queueId: Int, update: StoreUpdate): Unit = {
        incOpenJobs()
        storeUpdates(Math.min(queueId, StoreUpdateQueues - 1)).offer(update)
        storeUpdatesSemaphore.release()
    }

    @volatile private[this] var storeUpdatesProcessor: Thread = {

        @inline def processUpdate(): Unit = {
            storeUpdatesSemaphore.acquire()
            // we know that we will eventually find a task...
            var update: StoreUpdate = null
            var queueId = 0
            do {
                update = storeUpdates(queueId).poll()
                queueId = (queueId + 1) % StoreUpdateQueues
            } while (update eq null)
            try {
                update match {
                    case NewProperty(r, forceEvaluation, forceDependersNotifications) ⇒
                        doHandleResult(r, forceEvaluation, forceDependersNotifications)

                    case TriggeredLazyComputation(e, pkId, triggeredByForce, lc) ⇒
                        // Recall, that -- once we have a final result -- all meta data
                        // is deleted; in particular information about triggeredLazyComputations.
                        val currentP = properties(pkId).get(e)
                        if (currentP == null) {
                            if (triggeredLazyComputations(pkId).add(e)) {
                                if (tracer.isDefined) tracer.get.schedulingLazyComputation(e, pkId)

                                val alsoComputedPKIds = simultaneouslyLazilyComputedPropertyKinds(pkId)
                                alsoComputedPKIds foreach { computedPKId ⇒
                                    if (!triggeredLazyComputations(computedPKId).add(e)) {
                                        throw new UnknownError(
                                            "a simultaneously computed property kind was already triggered"
                                        )
                                    }
                                }

                                scheduledLazyTasksCounter += 1
                                appendTask(new PropertyComputationTask[Entity](store, e, pkId, lc))
                            }
                        } else if (triggeredByForce && currentP.isFinal) {
                            // it maybe the case that an epk is already final; e.g., if a value
                            // is first set/computed and then forced; in this case, we have
                            // to ensure that the meta-information is really deleted
                            forcedComputations(pkId).remove(e)
                        }
                }
            } finally {
                decOpenJobs()
            }
        }

        val t = new Thread(propertyStoreThreads, "OPAL - Property Updates Processor") {
            override def run(): Unit = gatherExceptions {
                do {
                    while (!store.isSuspended()) {
                        if (exception != null)
                            return ;
                        processUpdate()
                    }
                    // The store is suspended; hence, we want to keep the thread alive.
                    Thread.sleep(1000)
                } while (exception == null)
            }
        }
        t.setDaemon(true)
        t.setPriority(8)
        t.start()
        t
    }

    // --------------------------------------------------------------------------------------------
    //
    // Shutdown/Failure handling
    //
    // --------------------------------------------------------------------------------------------

    @inline protected[this] def gatherExceptions(f: ⇒ Unit): Unit = {
        try {
            f
        } catch {
            case _: InterruptedException if storeUpdatesProcessor == null ⇒ // ignore; shutting down
            case t: Throwable                                             ⇒ collectException(t)
        }
    }

    def shutdown(): Unit = this.synchronized {
        if (storeUpdatesProcessor == null)
            return ;

        // We use the "Thread"s' interrupt method to finally abort the threads...
        val oldStoreUpdatesProcessor = storeUpdatesProcessor
        storeUpdatesProcessor = null
        oldStoreUpdatesProcessor.interrupt()
        val oldTasksProcessors = tasksProcessors
        tasksProcessors = null
        oldTasksProcessors.interrupt()

        if (latch != null) latch.countDown()

        info(
            "analysis progress",
            "shutting down PropertyStore@"+System.identityHashCode(this).toHexString
        )(GlobalLogContext)
    }
    protected[this] override def onFirstException(t: Throwable): Unit = {
        if (tracer.isDefined) tracer.get.firstException(t)
        super.onFirstException(t)
    }

    override def finalize(): Unit = {
        // DEPRECATED: super.finalize()
        shutdown()
    }

    // --------------------------------------------------------------------------------------------
    //
    // Core lazy/triggered computations related functionality
    //
    // --------------------------------------------------------------------------------------------

    override def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk:       PropertyKey[P],
        pc:       PropertyComputation[E],
        finalEPs: TraversableOnce[FinalEP[E, P]]
    ): Unit = {
        if (openJobs.get() > 0) {
            throw new IllegalStateException(
                "lazy computations can only be registered while no computations are running"
            )
        }
        // By contract, this method is never executed concurrently and no computations
        // are running. Furthermore, the store is either newly created or waitOnPhaseCompletion
        // was called and the happens-before relation w.r.t. "dependers" is established and
        // we can check that we have no unexpected dependers

        finalEPs.foreach { finalEP ⇒
            val pkId = finalEP.pk.id
            val e = finalEP.e
            val theDependers = dependers(pkId).get(e)
            if (theDependers != null) {
                throw new IllegalStateException(s"$e: unexpected dependers exists: $theDependers")
            }
            val oldP = properties(pkId).putIfAbsent(e, finalEP)
            if (oldP != null) {
                throw new IllegalArgumentException(
                    s"$e: a property ($oldP) was already set; ignoring ${finalEP.p}"
                )
            }
        }
        lazyComputations(pk.id) = pc
    }

    override def registerTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (openJobs.get() > 0) {
            throw new IllegalStateException(
                "triggered computations can only be registered while no computations are running"
            )
        }
        val pkId = pk.id
        var oldComputations: Array[SomePropertyComputation] = null
        var newComputations: Array[SomePropertyComputation] = null

        oldComputations = triggeredComputations(pkId)
        if (oldComputations == null) {
            newComputations = Array[SomePropertyComputation](pc)
        } else {
            newComputations = java.util.Arrays.copyOf(oldComputations, oldComputations.length + 1)
            newComputations(oldComputations.length) = pc
        }
        triggeredComputations(pkId) = newComputations

    }

    // --------------------------------------------------------------------------------------------
    //
    // Core properties/results related functionality
    //
    // --------------------------------------------------------------------------------------------

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val ps = for {
                (eEPSs, pkId) ← properties.iterator.zipWithIndex.take(PropertyKey.maxId + 1)
                propertyKindName = PropertyKey.name(pkId)
                eEPS ← eEPSs.entrySet().asScala.iterator
            } yield {
                val e = eEPS.getKey
                val p = eEPS.getValue
                s"$e -> $propertyKindName[$pkId]=$p"
            }
            ps.mkString("PropertyStore(\n\t", "\n\t", "\n")
        } else {
            s"PropertyStore(properties=${properties.iterator.map(_.size).sum})"
        }
    }

    override def isKnown(e: Entity): Boolean = properties.exists(_.containsKey(e))

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        properties(pk.id).containsKey(e)
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        for {
            propertiesOfEntity ← properties.iterator
            property = propertiesOfEntity.get(e)
            if property != null
        } yield {
            property.asInstanceOf[EPS[E, Property]]
        }
    }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        val entities = ArrayBuffer.empty[Entity]
        val max = properties.length
        var i = 0
        while (i < max) {
            properties(i) forEach { (e, eps) ⇒ if (propertyFilter(eps)) entities += e }
            i += 1
        }
        entities.toIterator
    }

    /**
     * Returns all entities which have the given (regular / not simple) property bounds based
     * on an "==" (equals) comparison.
     */
    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        properties.iterator.zipWithIndex.
            filter { propertiesPerKind ⇒
                val (_ /*propertiesPerEntity*/ , pkId) = propertiesPerKind
                !PropertyKey.isPropertyKeyForSimplePropertyBasedOnPKId(pkId)
            }.
            flatMap { propertiesPerKind ⇒
                val (propertiesPerEntity, _ /*pkId*/ ) = propertiesPerKind
                propertiesPerEntity.entrySet().iterator().asScala.filter { mapEntry ⇒
                    val eps = mapEntry.getValue
                    eps.lb == lb && eps.ub == ub
                }.map(_.getKey)
            }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        properties(pk.id).values().iterator().asScala.asInstanceOf[Iterator[EPS[Entity, P]]]
    }

    // Thread Safe!
    private[this] def scheduleComputationForEntity[E <: Entity](
        e:               E,
        pc:              PropertyComputation[E],
        forceEvaluation: Boolean
    ): Unit = {
        if (debug) scheduledTasksCounter.incrementAndGet()
        appendTask(new InitialPropertyComputationTask[E](this, e, pc, forceEvaluation))
    }

    // Thread Safe!
    private[this] def computeOrScheduleLazyComputationForEntity[E <: Entity, P <: Property](
        e:                E,
        pk:               PropertyKey[P],
        triggeredByForce: Boolean,
        pc:               PropertyComputation[E]
    ): EOptionP[E, P] = {
        // Currently, we do not support eagerly scheduled computations and fasttrack properties.
        // In that case, we could have a scheduled computation and "in parallel" a request by
        // another thread. This would trigger the fasttrack evaluation and then result in the
        // situation where we already have a (final) result and we then get
        // the result of the scheduled computation.
        val pkId = pk.id
        if (!isPropertyKeyForSimplePropertyBasedOnPKId(pkId) &&
            useFastTrackPropertyComputations &&
            (computedPropertyKinds(pkId) || delayedPropertyKinds(pkId))) {
            val p = fastTrackPropertyBasedOnPKId(this, e, pkId)
            if (p.isDefined) {
                if (debug) fastTrackPropertiesCounter.incrementAndGet()
                val finalEP = FinalEP(e, p.get.asInstanceOf[P])
                appendStoreUpdate(queueId = 0, NewProperty(IdempotentResult(finalEP)))
                return finalEP;
            }
        }

        appendStoreUpdate(queueId = 1, TriggeredLazyComputation(e, pkId, triggeredByForce, pc))
        EPK(e, pk)
    }

    // Thread Safe!
    override def scheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {
        scheduleComputationForEntity(e, pc, forceEvaluation = true)
    }

    // Thread Safe!
    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        val pkId = pk.id
        properties(pkId).get(e) match {
            case null ⇒
                // the entity is unknown ...
                if (computedPropertyKinds == null) {
                    /*&& delayedPropertyKinds ne null (not necessary)*/
                    throw new IllegalStateException("setup phase was not called")
                }

                lazyComputations(pkId) match {
                    case null ⇒
                        if (!computedPropertyKinds(pkId) && !delayedPropertyKinds(pkId)) {
                            // ... a property is queried that is not going to be computed;
                            // we directly compute the property and store it to make
                            // it accessible later on.
                            //
                            // In case of a "simple property" the lookup of the fallback will
                            // throw an appropriate exception; hence, we have to handle this
                            // case specifically!
                            val reason = {
                                if (previouslyComputedPropertyKinds(pkId))
                                    PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                else
                                    PropertyIsNotComputedByAnyAnalysis
                            }
                            val p =
                                if (isPropertyKeyForSimplePropertyBasedOnPKId(pkId)) {
                                    notComputedPropertyBasedOnPKId(pkId)
                                } else {
                                    fallbackPropertyBasedOnPKId(store, reason, e, pkId)
                                }
                            if (traceFallbacks) {
                                val message = s"used fallback $p (reason=$reason) for $e"
                                trace("analysis progress", message)
                            }
                            if (debug) fallbacksUsedCounter.incrementAndGet()
                            val finalEP = FinalEP(e, p.asInstanceOf[P])
                            val r = IdempotentResult(finalEP)
                            appendStoreUpdate(queueId = 0, NewProperty(r))
                            finalEP
                        } else {
                            EPK(e, pk)
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        computeOrScheduleLazyComputationForEntity(e, pk, triggeredByForce = false, lc)
                }

            case eps ⇒
                eps.asInstanceOf[EOptionP[E, P]]
        }
    }

    // Thread Safe!
    final def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = {
        apply(epk.e, epk.pk)
    }

    // Thread Safe!
    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        val pkId = pk.id
        if (forcedComputations(pkId).put(e, e) == null) {
            val lc = lazyComputations(pkId).asInstanceOf[PropertyComputation[E]]
            if (lc != null) {
                if (properties(pkId).get(e) == null) {
                    if (tracer.isDefined) tracer.get.force(e, pkId)
                    computeOrScheduleLazyComputationForEntity(e, pk, triggeredByForce = true, lc)
                } else {
                    // the property was already computed or set...
                    if (tracer.isDefined) tracer.get.forceForComputedEPK(e, pkId)
                    forcedComputations(pkId).remove(e)
                }
            }
        }
    }

    // Thread safe!
    override def set(e: Entity, p: Property): Unit = handleExceptions {
        if (debug && lazyComputations(p.key.id) != null) {
            throw new IllegalStateException(
                s"$e: setting $p is not supported; lazy computation is (already) registered"
            )
        }
        val r = ExternalResult(e, p)
        appendStoreUpdate(queueId = 0, NewProperty(r))
    }

    // Thread safe!
    final override def handleResult(
        r:               PropertyComputationResult,
        forceEvaluation: Boolean
    ): Unit = {
        handleResult(r, forceEvaluation, Set.empty)
    }

    // Thread safe!
    private[par] def handleResult(
        r:                           PropertyComputationResult,
        forceEvaluation:             Boolean,
        forceDependersNotifications: Set[SomeEPK]
    ): Unit = {
        // In the following, we have to ensure that we do not get multiple notifications
        // of dependers due to forceDependersNotifications; i.e.; we cannot pass
        // forceDependersNotifications to multiple "handleResult" methods!

        r.id match {

            case NoResult.id ⇒ {
                // A computation reported no result; i.e., it is not possible to
                // compute a/some property/properties for a given entity.
            }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs, propertyComputationsHint) = r
                handleResult(ir, forceEvaluation, forceDependersNotifications)
                if (propertyComputationsHint == CheapPropertyComputation) {
                    npcs /*: Traversable[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                        if (debug)
                            directInTaskThreadPropertyComputationsCounter.incrementAndGet()
                        val (pc, e) = npc
                        handleResult(pc(e), forceEvaluation, Set.empty)
                    }
                } else {
                    npcs /*: Traversable[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                        val (pc, e) = npc
                        // check if we can/should handle the computations immediately in
                        // this thread, because there is still enough to do for the other
                        // threads
                        if (tasksSemaphore.availablePermits() > NumberOfThreadsForProcessingPropertyComputations * 2) {
                            if (debug)
                                directInTaskThreadPropertyComputationsCounter.incrementAndGet()
                            val (pc, e) = npc
                            handleResult(pc(e), forceEvaluation, Set.empty)
                        } else {
                            scheduleComputationForEntity(e, pc, forceEvaluation)
                        }
                    }
                }

            case IntermediateResult.id ⇒
                val queueId = r.asIntermediateResult.dependees.size / 4 + 1
                val update = NewProperty(r, forceEvaluation, forceDependersNotifications)
                appendStoreUpdate(queueId, update)

            case SimplePIntermediateResult.id ⇒
                val queueId = r.asSimplePIntermediateResult.dependees.size / 4 + 1
                val update = NewProperty(r, forceEvaluation, forceDependersNotifications)
                appendStoreUpdate(queueId, update)

            case PartialResult.id ⇒
                // TODO Implement some special logic to accumulate partial results to minimize depender notifications
                val update = NewProperty(r, forceEvaluation, forceDependersNotifications)
                appendStoreUpdate(queueId = 1, update)

            case _ /*nothing special...*/ ⇒
                // "final" results are prepended
                val update = NewProperty(r, forceEvaluation, forceDependersNotifications)
                appendStoreUpdate(queueId = 0, update)
        }
    }

    /**
     * Removes the e/pk from `dependees` and also removes it from the dependers of the
     * e/pk's dependees.
     */
    private[this] def clearDependees(epk: SomeEPK): Int = {
        val pkId = epk.pk.id
        val dependeesOfEntity = this.dependees(pkId)
        var dependeesCount = 0
        val oldDependees = dependeesOfEntity.remove(epk.e) // <= the old ones
        if (oldDependees != null) {
            oldDependees.foreach { oldDependee ⇒
                val EOptionP(oldDependeeE, oldDependeePK) = oldDependee
                val dependersOfOldDependee = dependers(oldDependeePK.id).get(oldDependeeE)
                if (dependersOfOldDependee != null) {
                    dependeesCount += 1
                    dependersOfOldDependee.remove(epk)
                }
            }
        }
        dependeesCount
    }

    private[this] def notifyDependers(
        newEPS: SomeEPS,
        pcrs:   RefAccumulator[PropertyComputationResult]
    ): Unit = {
        val e = newEPS.e
        val pkId = newEPS.pk.id
        val isFinal = newEPS.isFinal
        // 3.1. notify dependers
        val oldDependersOption = this.dependers(pkId).remove(e)
        if (oldDependersOption != null) {
            oldDependersOption forEach { (oldDependerEPK, updateFunction) ⇒
                val (c, onUpdateContinuationHint) = updateFunction
                if (tracer.isDefined) tracer.get.notification(newEPS, oldDependerEPK)

                // Clear depender => dependee lists.
                // Given that we will trigger the depender, we now have to remove the
                // respective onUpdateContinuation from all dependees of the respective
                // depender to avoid that the onUpdateContinuation is triggered multiple times!
                val oldDependerDependeesCount = clearDependees(oldDependerEPK)
                if (onUpdateContinuationHint == CheapPropertyComputation) {
                    directDependerOnUpdateComputationsCounter += 1
                    pcrs += c(newEPS)
                } else {
                    scheduledOnUpdateComputationsCounter += 1
                    if (isFinal) {
                        val t = new OnFinalUpdateComputationTask(this, newEPS.asFinal, c)
                        appendTask(oldDependerDependeesCount, t)
                    } else {
                        val t = new OnUpdateComputationTask(this, newEPS.toEPK, c)
                        appendTask(oldDependerDependeesCount, t)
                    }
                }
            }
        }

        // 3.2. perform clean-up if necessary/possible
        if (isFinal) {
            if (tracer.isDefined) tracer.get.metaInformationDeleted(newEPS.asFinal)
            forcedComputations(pkId).remove(e)
            triggeredLazyComputations(pkId).remove(e)
        }
    }

    private[this] def handleInitialProperty(e: Entity, pkId: Int, isFinal: Boolean): Unit = {
        if (isFinal) oneStepFinalUpdatesCounter += 1

        val computationsToStart = this.triggeredComputations(pkId)
        if (computationsToStart ne null) {
            foreachValue[SomePropertyComputation](computationsToStart) { (_ /*index*/ , pc) ⇒
                scheduleEagerComputationForEntity(e)(pc.asInstanceOf[PropertyComputation[Entity]])
            }
        }
    }

    private[this] def handleUpdate(
        oldEPS:                              SomeEPS,
        newEPS:                              SomeEPS,
        isFinal:                             Boolean,
        notifyDependersAboutNonFinalUpdates: Boolean,
        pcrs:                                RefAccumulator[PropertyComputationResult]
    ): UpdateAndNotifyState = {

        // 3. handle relevant updates
        val relevantUpdate = newEPS != oldEPS // always true if newEPS is a final EPS...
        if (tracer.isDefined && relevantUpdate) tracer.get.update(oldEPS, newEPS)
        var notificationRequired = relevantUpdate // required if relevant, but not notified...
        if (isFinal || (notifyDependersAboutNonFinalUpdates && relevantUpdate)) {
            notificationRequired = false
            notifyDependers(newEPS, pcrs)
        }

        // 4. report result
        if (relevantUpdate) {
            if (notificationRequired) {
                RelevantUpdateButNoNotification
            } else {
                RelevantUpdateAndNotification
            }
        } else {
            NoRelevantUpdate
        }
    }

    private[this] def assertNonFinal(e: Entity, oldEPS: SomeEOptionP): Unit = {
        if (oldEPS.isFinal) {
            throw new IllegalStateException(
                s"$e@${identityHashCode(e).toHexString}: already final: $oldEPS"
            )
        }
    }

    private[this] def assertRelation(e: Entity, lb: Property, ub: Property): Unit = {
        try {
            val lbAsOP = lb.asOrderedProperty
            val ubAsOP = ub.asOrderedProperty
            ubAsOP.checkIsEqualOrBetterThan(e, lbAsOP.asInstanceOf[ubAsOP.Self])
        } catch {
            case t: Throwable ⇒
                throw new IllegalArgumentException(
                    s"$e: illegal update: lb=$lb > ub=$ub; cause="+t.getMessage,
                    t
                )
        }
    }

    /**
     * Updates the entity and optionally notifies all dependers.
     *
     * @return true iff the dependers were not notified but a notification is required!
     */
    private[this] def updateAndNotifyForRegularP(
        e:                                   Entity,
        lb:                                  Property,
        ub:                                  Property,
        notifyDependersAboutNonFinalUpdates: Boolean                                   = true,
        pcrs:                                RefAccumulator[PropertyComputationResult]
    ): UpdateAndNotifyState = {
        updatesCounter += 1
        assert(
            Thread.currentThread() == storeUpdatesProcessor || storeUpdatesProcessor == null,
            "only to be called by the store updates processing thread"
        )

        val pk = ub.key
        val pkId = pk.id
        val newEPS = EPS[Entity, Property](e, lb, ub)
        val isFinal = newEPS.isFinal
        val propertiesOfEntity = properties(pkId)

        // 1. update property
        val oldEPS = propertiesOfEntity.put(e, newEPS)

        // 2. check if update was ok
        if (oldEPS == null) {
            handleInitialProperty(e, pkId, isFinal)
        } else if (debug /*&& oldEPS != null*/ ) {
            // The entity is known and we have a property value for the respective
            // kind; i.e., we may have (old) dependees and/or also dependers.
            val oldLB = oldEPS.lb
            val oldUB = oldEPS.ub
            assertNonFinal(e, oldEPS)
            if (lb.isOrderedProperty) {
                assertRelation(e, oldLB, lb)
                assertRelation(e, ub, oldUB)
            }
        }

        // 3. check if the property is updated and generate the corresponding result
        handleUpdate(oldEPS, newEPS, isFinal, notifyDependersAboutNonFinalUpdates, pcrs)
    }

    /**
     * Updates the entity and optionally notifies all dependers.
     *
     * @return true iff the dependers were not notified but a notification is required!
     */
    private[this] def updateAndNotifyForSimpleP(
        e:                                   Entity,
        ub:                                  Property,
        isFinal:                             Boolean,
        notifyDependersAboutNonFinalUpdates: Boolean                                   = true,
        pcrs:                                RefAccumulator[PropertyComputationResult]
    ): UpdateAndNotifyState = {
        updatesCounter += 1
        assert(
            Thread.currentThread() == storeUpdatesProcessor || storeUpdatesProcessor == null,
            "only to be called by the store updates processing thread"
        )

        val pk = ub.key
        val pkId = pk.id
        val newEPS = ESimplePS[Entity, Property](e, ub, isFinal)
        val propertiesOfEntity = properties(pkId)

        // 1. update property
        val oldEPS = propertiesOfEntity.put(e, newEPS)

        // 2. check if update was ok
        if (oldEPS == null) {
            handleInitialProperty(e, pkId, isFinal)
        } else if (debug /*&& oldEPS != null*/ ) {
            // The entity is known and we have a property value for the respective
            // kind; i.e., we may have (old) dependees and/or also dependers.
            val oldUB = oldEPS.ub
            assertNonFinal(e, oldEPS)
            if (ub.isOrderedProperty) {
                assertRelation(e, ub, oldUB)
            }
        }

        // 3. check if the property is updated and generate the corresponding result
        handleUpdate(oldEPS, newEPS, isFinal, notifyDependersAboutNonFinalUpdates, pcrs)
    }

    private[this] def finalUpdate(
        e:    Entity,
        p:    Property,
        pcrs: RefAccumulator[PropertyComputationResult]
    ): UpdateAndNotifyState = {
        if (isPropertyKeyForSimplePropertyBasedOnPKId(p.key.id)) {
            updateAndNotifyForSimpleP(e, p, isFinal = true, true /*actually irrelevant*/ , pcrs)
        } else {
            updateAndNotifyForRegularP(e, p, p, true /*actually irrelevant*/ , pcrs)
        }
    }

    private[this] def assertNoDependees(e: Entity, pkId: Int): Unit = {
        // Given that "on notification" dependees are eagerly killed, clearing
        // dependees is not necessary!
        if (debug && dependees(pkId).containsKey(e)) {
            throw new IllegalStateException(
                s"$e: ${properties(pkId).get(e)} has (unexpected) dependees: \n\t"+
                    s"${dependees(pkId).get(e).mkString(", ")}\n"+
                    "this happens, e.g., if computations are started eagerly while "+
                    "also a respective lazy property computation is scheduled; "+
                    "in this case use force instead!"
            )
        }
    }

    private[this] def doHandleResult(
        r:                                  PropertyComputationResult,
        forceEvaluation:                    Boolean,
        initialForceDependersNotifications: Set[SomeEPK]
    ): Unit = {

        // Used to store immediate results, which need to be handled immediately
        val pcrs: RefAccumulator[PropertyComputationResult] = RefAccumulator(r)
        var forceDependersNotifications = initialForceDependersNotifications

        def processResult(r: PropertyComputationResult): Unit = {
            assert(
                Thread.currentThread() == storeUpdatesProcessor || storeUpdatesProcessor == null,
                "only to be called by the store updates processing thread"
            )

            if (tracer.isDefined)
                tracer.get.handlingResult(r, forceEvaluation, forceDependersNotifications)

            r.id match {

                case NoResult.id ⇒ {
                    // A computation reported no result; i.e., it is not possible to
                    // compute a/some property/properties for a given entity.
                }

                //
                // Result containers
                //

                case Results.id ⇒
                    val Results(furtherResults) = r
                    pcrs ++= furtherResults

                case IncrementalResult.id ⇒
                    val IncrementalResult(ir, npcs, propertyComputationsHint) = r
                    pcrs += ir
                    if (propertyComputationsHint == CheapPropertyComputation) {
                        pcrs ++= npcs /*: Iterator[(PropertyComputation[e],e)]*/ map { npc ⇒
                            val (pc, e) = npc
                            pc(e)
                        }
                    } else {
                        npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                            val (pc, e) = npc
                            scheduleComputationForEntity(e, pc, forceEvaluation)
                        }
                    }

                //
                // Methods which actually store results...
                //

                case Result.id ⇒
                    val Result(e, p) = r
                    val pk = p.key
                    val epk = EPK(e, pk)
                    clearDependees(epk)
                    forceDependersNotifications -= epk
                    finalUpdate(e, p, pcrs = pcrs)

                case MultiResult.id ⇒
                    val MultiResult(results) = r
                    results foreach { ep ⇒
                        val epk = ep.toEPK
                        clearDependees(epk)
                        forceDependersNotifications -= epk
                        finalUpdate(ep.e, ep.p, pcrs = pcrs)
                    }

                case IdempotentResult.id ⇒
                    val IdempotentResult(ep @ FinalEP(e, p)) = r
                    val pkId = p.key.id
                    val epk = ep.toEPK
                    val propertiesOfEntity = properties(pkId)
                    assert(!dependees(pkId).containsKey(e))
                    forceDependersNotifications -= epk
                    if (!propertiesOfEntity.containsKey(e)) {
                        finalUpdate(e, p, pcrs)
                    } else {
                        /*we already have a value*/
                        redundantIdempotentResultsCounter += 1
                        if (debug) {
                            val oldEP = propertiesOfEntity.get(e)
                            if (oldEP != ep) {
                                throw new IllegalArgumentException(s"$e: unexpected update $oldEP => $ep")
                            }
                        }
                    }

                case PartialResult.id ⇒
                    val PartialResult(e, pk, u) = r
                    type E = e.type
                    type P = Property
                    val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
                    val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                    if (newEPSOption.isDefined) {
                        val newEPS = newEPSOption.get
                        val epk = newEPS.toEPK
                        if (clearDependees(epk) > 0) {
                            throw new IllegalStateException(
                                s"partial result ($r) for property with dependees (and continuation function)"
                            )
                        }
                        forceDependersNotifications -= epk
                        if (isPropertyKeyForSimplePropertyBasedOnPKId(pk.id))
                            updateAndNotifyForSimpleP(newEPS.e, newEPS.ub, isFinal = false, pcrs = pcrs)
                        else
                            updateAndNotifyForRegularP(newEPS.e, newEPS.lb, newEPS.ub, pcrs = pcrs)
                    } else {
                        if (tracer.isDefined) {
                            val partialResult = r.asInstanceOf[SomePartialResult]
                            tracer.get.uselessPartialResult(partialResult, eOptionP)
                        }
                        uselessPartialResultComputationCounter += 1
                    }

                case ExternalResult.id ⇒
                    val ExternalResult(e, p) = r
                    if (debug) {
                        val pkId = p.id
                        val oldP = properties(pkId).get(e)
                        if (oldP != null) {
                            throw new IllegalStateException(s"$e: already has a property $oldP")
                        }
                        if (dependees(pkId).containsKey(e)) {
                            throw new IllegalStateException(s"$e: is already computed/has dependees")
                        }
                    }
                    forceDependersNotifications -= EPK(e, p)
                    finalUpdate(e, p, pcrs)

                case IntermediateResult.id ⇒
                    val IntermediateResult(e, lb, ub, seenDependees, c, onUpdateContinuationHint) = r
                    val pk = ub.key
                    val pkId = pk.id
                    val epk = EPK(e, pk)

                    if (forceEvaluation) forcedComputations(pkId).put(e, e)

                    assertNoDependees(e, pkId)

                    // 1. let's check if a seen dependee is already updated; if so, we directly
                    //    schedule/execute the continuation function again to continue computing
                    //    the property
                    val seenDependeesIterator = seenDependees.toIterator
                    while (seenDependeesIterator.hasNext) {
                        val seenDependee = seenDependeesIterator.next()

                        if (debug && seenDependee.isFinal) {
                            throw new IllegalStateException(
                                s"$e (lb=$lb, ub=$ub): dependency to final property: $seenDependee"
                            )
                        }

                        val seenDependeeE = seenDependee.e
                        val seenDependeePKId = seenDependee.pk.id
                        val propertiesOfEntity = properties(seenDependeePKId)
                        // seenDependee is guaranteed to be not null
                        // currentDependee may be null => newDependee is an EPK => no update
                        val currentDependee = propertiesOfEntity.get(seenDependeeE)
                        if (currentDependee != null && seenDependee != currentDependee) {
                            // Make the current result available for other threads, but
                            // do not yet trigger dependers; however, we have to ensure
                            // that the dependers are eventually triggered if any update
                            // was relevant!
                            val updateAndNotifyState = updateAndNotifyForRegularP(
                                e, lb, ub,
                                notifyDependersAboutNonFinalUpdates = false,
                                pcrs
                            )
                            if (updateAndNotifyState.isNotificationRequired) {
                                forceDependersNotifications += epk
                            }

                            if (tracer.isDefined)
                                tracer.get.immediateDependeeUpdate(
                                    e, pk, seenDependee, currentDependee, updateAndNotifyState
                                )

                            if (onUpdateContinuationHint == CheapPropertyComputation) {
                                directDependeeUpdatesCounter += 1
                                // we want to avoid potential stack-overflow errors...
                                pcrs += c(currentDependee)
                            } else {
                                scheduledDependeeUpdatesCounter += 1
                                if (currentDependee.isFinal) {
                                    val t = ImmediateOnFinalUpdateComputationTask(
                                        store,
                                        currentDependee.asFinal,
                                        previousResult = r,
                                        forceDependersNotifications,
                                        c
                                    )
                                    appendTask(seenDependees.size, t)
                                } else {
                                    val t = ImmediateOnUpdateComputationTask(
                                        store,
                                        currentDependee.toEPK,
                                        previousResult = r,
                                        forceDependersNotifications,
                                        c
                                    )
                                    appendTask(seenDependees.size, t)
                                }
                                // We will postpone the notification to the point where
                                // the result(s) are handled...
                                forceDependersNotifications = Set.empty
                            }

                            return ;
                        }
                    }

                    // When we reach this point, all potential dependee updates are taken into account;
                    // otherwise we would have had an early return

                    // 2.1.  Update the value (trigger dependers/clear old dependees).
                    if (updateAndNotifyForRegularP(e, lb, ub, pcrs = pcrs).areDependersNotified) {
                        forceDependersNotifications -= epk
                    }

                    // 2.2.  The most current value of every dependee was taken into account
                    //       register with new (!) dependees.
                    this.dependees(pkId).put(e, seenDependees)
                    val updateFunction = (c, onUpdateContinuationHint)
                    seenDependees foreach { dependee ⇒
                        val dependeeE = dependee.e
                        val dependeePKId = dependee.pk.id
                        dependers(dependeePKId).
                            computeIfAbsent(dependeeE, _ ⇒ new JHashMap()).put(epk, updateFunction)
                    }

                case SimplePIntermediateResult.id ⇒
                    // TODO Unify handling with IntermediateResult (avoid code duplication)
                    val SimplePIntermediateResult(e, ub, seenDependees, c, onUpdateContinuationHint) = r
                    val pk = ub.key
                    val pkId = pk.id
                    val epk = EPK(e, pk)

                    if (forceEvaluation) forcedComputations(pkId).put(e, e)

                    assertNoDependees(e, pkId)

                    // 1. let's check if a seen dependee is already updated; if so, we directly
                    //    schedule/execute the continuation function again to continue computing
                    //    the property
                    val seenDependeesIterator = seenDependees.toIterator
                    while (seenDependeesIterator.hasNext) {
                        val seenDependee = seenDependeesIterator.next()

                        if (debug && seenDependee.isFinal) {
                            throw new IllegalStateException(
                                s"$e/$pk: dependency to final property: $seenDependee"
                            )
                        }

                        val seenDependeeE = seenDependee.e
                        val seenDependeePKId = seenDependee.pk.id
                        val propertiesOfEntity = properties(seenDependeePKId)
                        // seenDependee is guaranteed to be not null
                        // currentDependee may be null => newDependee is an EPK => no update
                        val currentDependee = propertiesOfEntity.get(seenDependeeE)
                        if (currentDependee != null && seenDependee != currentDependee) {
                            // Make the current result available for other threads, but
                            // do not yet trigger dependers; however, we have to ensure
                            // that the dependers are eventually triggered if any update
                            // was relevant!
                            val updateAndNotifyState = updateAndNotifyForSimpleP(
                                e, ub, false,
                                notifyDependersAboutNonFinalUpdates = false,
                                pcrs
                            )
                            if (updateAndNotifyState.isNotificationRequired) {
                                forceDependersNotifications += epk
                            }

                            if (tracer.isDefined)
                                tracer.get.immediateDependeeUpdate(
                                    e, pk, seenDependee, currentDependee, updateAndNotifyState
                                )

                            if (onUpdateContinuationHint == CheapPropertyComputation) {
                                directDependeeUpdatesCounter += 1
                                // we want to avoid potential stack-overflow errors...
                                pcrs += c(currentDependee)
                            } else {
                                scheduledDependeeUpdatesCounter += 1
                                if (currentDependee.isFinal) {
                                    val t = ImmediateOnFinalUpdateComputationTask(
                                        store,
                                        currentDependee.asFinal,
                                        previousResult = r,
                                        forceDependersNotifications,
                                        c
                                    )
                                    appendTask(seenDependees.size, t)
                                } else {
                                    val t = ImmediateOnUpdateComputationTask(
                                        store,
                                        currentDependee.toEPK,
                                        previousResult = r,
                                        forceDependersNotifications,
                                        c
                                    )
                                    appendTask(seenDependees.size, t)
                                }
                                // We will postpone the notification to the point where
                                // the result(s) are handled...
                                forceDependersNotifications = Set.empty
                            }

                            return ;
                        }
                    }

                    // When we reach this point, all potential dependee updates are taken into account;
                    // otherwise we would have had an early return

                    // 2.1.  Update the value (trigger dependers/clear old dependees).
                    if (updateAndNotifyForSimpleP(e, ub, isFinal = false, pcrs = pcrs).areDependersNotified) {
                        forceDependersNotifications -= epk
                    }

                    // 2.2.  The most current value of every dependee was taken into account;
                    //       register with new (!) dependees.
                    this.dependees(pkId).put(e, seenDependees)
                    val updateFunction = (c, onUpdateContinuationHint)
                    seenDependees foreach { dependee ⇒
                        val dependeeE = dependee.e
                        val dependeePKId = dependee.pk.id
                        dependers(dependeePKId).
                            computeIfAbsent(dependeeE, _ ⇒ new JHashMap()).
                            put(epk, updateFunction)
                    }
            }
        }

        do {
            while (pcrs.nonEmpty) {
                processResult(pcrs.pop())
            }
            if (forceDependersNotifications.nonEmpty) {
                val epk = forceDependersNotifications.head
                forceDependersNotifications = forceDependersNotifications.tail
                val eps = properties(epk.pk.id).get(epk.e)
                if (tracer.isDefined) tracer.get.delayedNotification(eps)

                notifyDependers(eps, pcrs)
            }
        } while (forceDependersNotifications.nonEmpty || pcrs.nonEmpty)
    }

    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind]
    ): Unit = handleExceptions {
        if (debug && openJobs.get > 0) {
            throw new IllegalStateException(
                "setup phase can only be called as long as no tasks are scheduled"
            )
        }
        val currentComputedPropertyKinds = this.computedPropertyKinds
        if (currentComputedPropertyKinds != null) {
            currentComputedPropertyKinds.iterator.zipWithIndex foreach { e ⇒
                val (isComputed, pkId) = e
                previouslyComputedPropertyKinds(pkId) |= isComputed
            }
        }

        val newComputedPropertyKinds = new Array[Boolean](SupportedPropertyKinds)
        computedPropertyKinds foreach { pk ⇒ newComputedPropertyKinds(pk.id) = true }
        this.computedPropertyKinds = newComputedPropertyKinds

        val newDelayedPropertyKinds = new Array[Boolean](SupportedPropertyKinds)
        delayedPropertyKinds foreach { pk ⇒ newDelayedPropertyKinds(pk.id) = true }
        this.delayedPropertyKinds = newDelayedPropertyKinds
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for handling fallbacks:
        do {
            continueComputation = false
            while (!isSuspended() && openJobs.get() > 0) {
                if (exception != null) throw exception;
                try {
                    latch.await()
                } catch {
                    case ie: InterruptedException ⇒
                        info("analysis progress", "processing aborted due to thread interruption")
                }
            }

            if (exception != null) throw exception;
            if (isSuspended())
                return ;

            assert(openJobs.get == 0, s"unexpected number of open jobs: ${openJobs.get}")

            // We have reached quiescence. Let's check if we have to fill in fallbacks.
            quiescenceCounter += 1
            if (tracer.isDefined) tracer.get.reachedQuiescence()

            val maxPKIndex = SupportedPropertyKinds // PropertyKey.maxId // properties.length

            // 1. Let's search all EPKs for which no values were computed (no analysis was
            //    scheduled) and fill it in.
            //    (Recall that we return fallback properties eagerly if no analysis is
            //     scheduled or will be scheduled; but it is still possible that we will
            //     not have a property for a specific entity, if the underlying analysis
            //     doesn't compute one; in that case we need to put in fallback values.)
            var pkId = 0
            while (pkId < maxPKIndex) {
                if (!delayedPropertyKinds(pkId)) {
                    val dependersOfEntity = dependers(pkId)
                    val propertiesOfEntity = properties(pkId)
                    if (isPropertyKeyForSimplePropertyBasedOnPKId(pkId)) {
                        // Iterate over all entities which have NO simple properties and use the
                        // "notComputedProperty".
                        var newProperties = List.empty[NewProperty]
                        dependersOfEntity.keySet() forEach { e ⇒
                            if (propertiesOfEntity.get(e) == null) {
                                // ... we have dependers on a property, which was not computed!
                                val p = notComputedPropertyBasedOnPKId(pkId)
                                newProperties ::= NewProperty(Result(e, p))

                            }
                        }
                        continueComputation ||= newProperties.nonEmpty
                        newProperties foreach { p ⇒ appendStoreUpdate(queueId = 0, p) }
                    } else {
                        var newProperties = List.empty[NewProperty]
                        dependersOfEntity.keySet() forEach { e ⇒
                            if (propertiesOfEntity.get(e) == null) {
                                val reason = {
                                    if (previouslyComputedPropertyKinds(pkId) ||
                                        computedPropertyKinds(pkId))
                                        PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                    else
                                        PropertyIsNotComputedByAnyAnalysis
                                }
                                val p = fallbackPropertyBasedOnPKId(this, reason, e, pkId)
                                if (traceFallbacks) {
                                    val message = s"used fallback $p (reason=$reason) for $e"
                                    trace("analysis progress", message)
                                }
                                fallbacksUsedCounter.incrementAndGet()
                                newProperties ::= NewProperty(Result(e, p))
                            }
                        }
                        continueComputation ||= newProperties.nonEmpty
                        newProperties foreach { p ⇒ appendStoreUpdate(queueId = 0, p) }
                    }
                } else {
                    if (!dependers(pkId).isEmpty) {
                        throw new IllegalStateException(
                            s"unexpected dependencies on ${PropertyKey.name(pkId)} found"
                        )
                    }
                }
                pkId += 1
            }

            // 2. The phase is really over, kill all state that is no longer required and
            //    commit all intermediate values
            if (!continueComputation) {
                // let's search for "unsatisfied computations" related to "forced properties"
                pkId = 0
                while (pkId < maxPKIndex) {
                    if (computedPropertyKinds(pkId)) {
                        // 2.1. drop the forced computations related to cycles:
                        forcedComputations(pkId).clear()

                        // 2.2. remaining depender/dependee relationships are no longer required:
                        dependees(pkId).clear()
                        dependers(pkId).clear()

                        // 2.3. commit all values:
                        if (isPropertyKeyForSimplePropertyBasedOnPKId(pkId)) {
                            properties(pkId).replaceAll { (e, eps) ⇒
                                if (eps.isFinal) { eps } else { eps.toUBEP }
                            }
                        } else {
                            properties(pkId).replaceAll { (e, eps) ⇒
                                if (eps.isFinal) {
                                    eps
                                } else {
                                    FinalEP(e, PropertyKey.resolveCycle(this, eps))
                                }
                            }
                        }
                    }
                    pkId += 1
                }
            }

        } while (continueComputation)

        if (exception != null) throw exception;
    }
}

/**
 * Factory for creating `PKEParallelTasksPropertyStore`s.
 *
 * @author Michael Eichberg
 */
object PKEParallelTasksPropertyStore extends PropertyStoreFactory {

    @volatile var NumberOfThreadsForProcessingPropertyComputations: Int = {
        // We need at least one thread for processing property computations.
        Math.max(NumberOfThreadsForCPUBoundTasks, 1)
    }

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKEParallelTasksPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap
        new PKEParallelTasksPropertyStore(
            contextMap,
            NumberOfThreadsForProcessingPropertyComputations,
            tracer = None
        )
    }

    def apply(
        tracer: PropertyStoreTracer
    )(
        implicit
        logContext: LogContext
    ): PKEParallelTasksPropertyStore = {

        new PKEParallelTasksPropertyStore(
            Map.empty,
            NumberOfThreadsForProcessingPropertyComputations,
            Some(tracer)
        )
    }

    def create(
        tracer:  PropertyStoreTracer,
        context: Map[Class[_], AnyRef] // ,PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKEParallelTasksPropertyStore = {

        new PKEParallelTasksPropertyStore(
            context,
            NumberOfThreadsForProcessingPropertyComputations,
            Some(tracer)
        )
    }

}

// USED INTERNALLY

private[par] sealed trait StoreUpdate

private[par] case class NewProperty(
        pcr:                         PropertyComputationResult,
        forceEvaluation:             Boolean                   = false,
        forceDependersNotifications: Set[SomeEPK]              = Set.empty
) extends StoreUpdate

private[par] case class TriggeredLazyComputation[E <: Entity](
        e:                E,
        pkId:             Int,
        triggeredByForce: Boolean,
        pc:               PropertyComputation[E]
) extends StoreUpdate

