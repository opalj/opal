/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import scala.annotation.switch

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.LinkedBlockingQueue

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.util.control.ControlThrowable

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId

/**
 * Yet another parallel property store.
 *
 * @param taskManager The strategy for prioritizing tasks
 * @param THREAD_COUNT Number of threads to use for simultaneous processing
 * @param MaxEvaluationDepth Maximum recursion level for lazy property computations before a task
 *                           is spawned to be handled by another thread
 *
 * @author Dominik Helm
 */
class PKECPropertyStore(
        final val ctx:                   Map[Class[_], AnyRef],
        val taskManager:                 PKECTaskManager,
        val THREAD_COUNT:                Int,
        override val MaxEvaluationDepth: Int
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore {

    implicit val propertyStore: PKECPropertyStore = this

    var evaluationDepth: Int = 0

    val ps: Array[ConcurrentHashMap[Entity, EPKState]] =
        Array.fill(PropertyKind.SupportedPropertyKinds) { new ConcurrentHashMap() }

    private[this] val triggeredComputations: Array[Array[SomePropertyComputation]] =
        new Array(PropertyKind.SupportedPropertyKinds)

    private[this] val queues: Array[LinkedBlockingQueue[QualifiedTask]] =
        Array.fill(THREAD_COUNT) { new LinkedBlockingQueue[QualifiedTask]() }

    private[this] val initialQueues: Array[java.util.ArrayDeque[QualifiedTask]] =
        Array.fill(THREAD_COUNT) { new java.util.ArrayDeque[QualifiedTask](50000 / THREAD_COUNT) }

    private[this] var setAndPreinitializedValues: List[SomeEPK] = List.empty

    override def shutdown(): Unit = {}

    var idle = true
    override def isIdle: Boolean = idle

    // --------------------------------------------------------------------------------------------
    //
    // STATISTICS
    //
    // --------------------------------------------------------------------------------------------

    private[this] var quiescenceCounter = 0
    override def quiescenceCount: Int = quiescenceCounter

    private[this] val scheduledTasks = new AtomicInteger(0)
    override def scheduledTasksCount: Int = scheduledTasks.get()

    private[this] val scheduledOnUpdateComputations = new AtomicInteger(0)
    override def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputations.get

    private[this] val fallbacksForComputedProperties = new AtomicInteger(0)
    override def fallbacksUsedForComputedPropertiesCount: Int = fallbacksForComputedProperties.get
    override private[fpcf] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = {
        fallbacksForComputedProperties.getAndIncrement()
    }

    // --------------------------------------------------------------------------------------------
    //
    // BASIC QUERY METHODS (ONLY TO BE CALLED WHEN THE STORE IS QUIESCENT)
    //
    // --------------------------------------------------------------------------------------------

    override def toString(printProperties: Boolean): String = {
        if (printProperties) {
            val properties = for (pkId <- 0 to PropertyKey.maxId) yield {
                var entities: List[String] = List.empty
                ps(pkId).forEachValue(Long.MaxValue, { state: EPKState =>
                    entities ::= state.eOptP.toString.replace("\n", "\n\t")
                })
                entities.sorted.mkString(s"Entities for property key $pkId:\n\t", "\n\t", "\n")
            }
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n)")
        } else {
            s"PropertyStore(properties=${ps.iterator.map(_.size).sum})"
        }
    }

    override def entities(propertyFilter: SomeEPS => Boolean): Iterator[Entity] = {
        ps.iterator.flatMap { propertiesPerKind =>
            val result: ListBuffer[Entity] = ListBuffer.empty
            propertiesPerKind.forEachValue(Long.MaxValue, {
                state: EPKState => if (propertyFilter(state.eOptP.asEPS)) result.append(state.eOptP.e)
            })
            result
        }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        val result: ListBuffer[EPS[Entity, P]] = ListBuffer.empty
        ps(pk.id).forEachValue(Long.MaxValue, {
            state: EPKState => result.append(state.eOptP.asInstanceOf[EPS[Entity, P]])
        })
        result.iterator
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        entities { eps => eps.lb == lb && eps.ub == ub }
    }

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = {
        entities { eps => eps.lb == lb }
    }

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = {
        entities { eps => eps.ub == ub }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        ps.iterator.flatMap { propertiesPerKind =>
            val ePKState = propertiesPerKind.get(e)
            if ((ePKState ne null) && ePKState.eOptP.isEPS)
                Iterator.single(ePKState.eOptP.asInstanceOf[EPS[E, Property]])
            else
                Iterator.empty
        }
    }

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        val ePKState = ps(pk.id).get(e)
        (ePKState ne null) && (ePKState.eOptP.hasUBP || ePKState.eOptP.hasLBP)
    }

    override def isKnown(e: Entity): Boolean = {
        ps.exists { propertiesPerKind =>
            propertiesPerKind.containsKey(e)
        }
    }

    override def get[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Option[EOptionP[E, P]] = {
        val ePKState = ps(pk.id).get(e)
        if (ePKState eq null)
            None
        else
            Some(ePKState.eOptP.asInstanceOf[EOptionP[E, P]])
    }

    override def get[E <: Entity, P <: Property](epk: EPK[E, P]): Option[EOptionP[E, P]] = {
        get(epk.e, epk.pk)
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - NOT THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    override protected[this] def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(pc: PropertyComputation[E]): Unit = {
        schedulePropertyComputation(e, pc)
    }

    override protected[this] def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {

        // Recall that the scheduler has to take care of registering a triggered computation
        // before the first analysis derives a respective value!
        // Hence, there is no need to immediately check that we have to trigger a computation.

        val pkId = pk.id
        val oldComputations: Array[SomePropertyComputation] = triggeredComputations(pkId)
        var newComputations: Array[SomePropertyComputation] = null

        if (oldComputations == null) {
            newComputations = Array[SomePropertyComputation](pc)
        } else {
            newComputations = java.util.Arrays.copyOf(oldComputations, oldComputations.length + 1)
            newComputations(oldComputations.length) = pc
        }
        triggeredComputations(pkId) = newComputations
    }

    override protected[this] def doSet(e: Entity, p: Property): Unit = {
        val epkState = EPKState(FinalEP(e, p), null, null)

        val oldP = ps(p.id).put(e, epkState)
        if (oldP ne null) {
            throw new IllegalStateException(s"$e already had the property $oldP")
        }
        setAndPreinitializedValues ::= EPK(e, p.key)
    }

    override protected[this] def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(pc: EOptionP[E, P] => InterimEP[E, P]): Unit = {
        val pkId = pk.id
        val propertiesOfKind = ps(pkId)
        val oldEPKState = propertiesOfKind.get(e)
        val newInterimEP: SomeInterimEP =
            oldEPKState match {
                case null =>
                    val epk = EPK(e, pk)
                    setAndPreinitializedValues ::= epk
                    pc(epk)
                case epkState =>
                    pc(epkState.eOptP.asInstanceOf[EOptionP[E, P]])
            }
        assert(newInterimEP.isRefinable)
        val newEPKState = EPKState(newInterimEP, null, null)
        propertiesOfKind.put(e, newEPKState)
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    private[par] def scheduleTask(task: QualifiedTask): Unit = handleExceptions {
        val numTasks = scheduledTasks.incrementAndGet()
        if (idle) {
            initialQueues(numTasks % THREAD_COUNT).offer(task)
        } else {
            activeTasks.incrementAndGet()
            queues(numTasks % THREAD_COUNT).offer(task)
        }
    }

    private[this] def schedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    ): Unit = {
        scheduleTask(new PropertyComputationTask(e, pc))
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        doApply(EPK(e, pk), e, pk.id)
    }

    override def execute(f: => Unit): Unit = {
        scheduleTask(new ExecuteTask(f))
    }

    override def handleResult(r: PropertyComputationResult): Unit = handleExceptions {
        (r.id: @switch) match {

            case NoResult.id =>
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            //
            // Result containers
            //

            case Results.id =>
                r.asResults.foreach { handleResult }

            case IncrementalResult.id =>
                val IncrementalResult(ir, npcs) = r
                handleResult(ir)
                npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc =>
                    val (pc, e) = npc
                    schedulePropertyComputation(e, pc)
                }

            //
            // Methods which actually store results...
            //

            case Result.id =>
                handleFinalResult(r.asResult.finalEP)

            case MultiResult.id =>
                val MultiResult(results) = r
                results.iterator.foreach { finalEP => handleFinalResult(finalEP) }

            case InterimResult.id =>
                val interimR = r.asInterimResult
                handleInterimResult(
                    interimR.eps,
                    interimR.c,
                    interimR.dependees
                )

            case PartialResult.id =>
                val PartialResult(e, pk, u) = r
                handlePartialResult(u, e, pk)

            case InterimPartialResult.id =>
                val InterimPartialResult(prs, dependees, c) = r

                prs foreach { pr =>
                    handlePartialResult(
                        pr.u.asInstanceOf[SomeEOptionP => Option[SomeInterimEP]],
                        pr.e,
                        pr.pk
                    )
                }

                val e = new FakeEntity()
                val epk = EPK(e, AnalysisKey)

                val epkState = EPKState(epk, null, dependees)
                epkState.c = { dependee: SomeEPS =>
                    val result = c(dependee)

                    val state = ps(AnalysisKeyId).remove(e)
                    state.dependees = null

                    result
                }

                ps(AnalysisKeyId).put(e, epkState)

                updateDependees(epkState, dependees)
        }
    }

    private[this] def handleFinalResult(
        finalEP:       FinalEP[Entity, Property],
        unnotifiedPKs: Set[PropertyKind]         = Set.empty
    ): Unit = {
        val SomeEPS(e, pk) = finalEP
        var isFresh = false
        val ePKState = ps(pk.id).computeIfAbsent(e, { _ => isFresh = true; EPKState(finalEP, null, null) })
        if (isFresh) triggerComputations(e, pk.id)
        else ePKState.setFinal(finalEP, unnotifiedPKs)
    }

    private[par] def triggerComputations(e: Entity, pkId: Int): Unit = {
        val computations = triggeredComputations(pkId)
        if (computations ne null) {
            computations foreach { pc =>
                schedulePropertyComputation(e, pc.asInstanceOf[PropertyComputation[Entity]])
            }
        }
    }

    private[this] def handleInterimResult(
        interimEP: InterimEP[Entity, _ >: Null <: Property],
        c:         ProperOnUpdateContinuation,
        dependees: Set[SomeEOptionP]
    ): Unit = {
        val SomeEPS(e, pk) = interimEP
        var isFresh = false
        val ePKState =
            ps(pk.id).computeIfAbsent(e, { _ => isFresh = true; EPKState(interimEP, c, dependees) })
        if (isFresh) {
            triggerComputations(e, pk.id)
            updateDependees(ePKState, dependees)
        } else ePKState.interimUpdate(interimEP, c, dependees)
    }

    private[this] def handlePartialResult(
        update: UpdateComputation[Entity, Property],
        e:      Entity,
        pk:     PropertyKey[Property]
    ): Unit = {
        val ePKState = ps(pk.id).computeIfAbsent(e, _ => EPKState(EPK(e, pk), null, null))
        ePKState.partialUpdate(update)
    }

    def updateDependees(depender: EPKState, newDependees: Set[SomeEOptionP]): Unit = {
        val suppressedPKs = suppressInterimUpdates(depender.eOptP.pk.id)
        newDependees.forall { dependee =>
            val dependeePK = dependee.pk.id
            val dependeeState = ps(dependeePK).get(dependee.e)
            dependeeState.addDependerOrScheduleContinuation(depender, dependee, dependeePK, suppressedPKs)
        }
    }

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        val current = ps(pkId).get(e)
        if (current eq null) {
            val lazyComputation = lazyComputations(pkId).asInstanceOf[E => PropertyComputationResult]
            if (lazyComputation ne null) {
                val previous = ps(pkId).putIfAbsent(e, EPKState(epk, null, null))
                if (previous eq null) {
                    /* We try to evaluate lazy computations in the current thread to avoid
                       synchronization overhead, but we restrict ourselves to at most
                       MaxEvaluationDepth levels of recursion before scheduling a task for a
                       different thread instead. */
                    if (evaluationDepth < MaxEvaluationDepth) {
                        evaluationDepth += 1
                        handleResult(lazyComputation(e))
                        evaluationDepth -= 1
                        ps(pkId).get(e).eOptP.asInstanceOf[EOptionP[E, P]]
                    } else {
                        scheduleTask(
                            new LazyComputationTask(
                                e,
                                lazyComputation,
                                pkId
                            )
                        )
                        epk
                    }
                } else {
                    previous.eOptP.asInstanceOf[EOptionP[E, P]]
                }
            } else if (propertyKindsComputedInThisPhase(pkId)) {
                val transformer = transformersByTargetPK(pkId)
                if (transformer ne null) {
                    val dependee = this(e, transformer._1)
                    if (dependee.isFinal) {
                        val result = transformer._2(e, dependee.asFinal.p)
                        val previous = ps(pkId).putIfAbsent(e, EPKState(result, null, null))
                        if (previous eq null) {
                            triggerComputations(e, pkId)
                            result.asInstanceOf[FinalEP[E, P]]
                        } else {
                            previous.eOptP.asInstanceOf[EOptionP[E, P]]
                        }
                    } else {
                        val newState = EPKState(epk, d => new Result(transformer._2(e, d.asFinal.p)), Set(dependee))
                        val previous = ps(pkId).putIfAbsent(e, newState)
                        if (previous eq null) {
                            updateDependees(newState, Set(dependee))
                            epk
                        } else {
                            previous.eOptP.asInstanceOf[EOptionP[E, P]]
                        }
                    }
                } else {
                    val previous = ps(pkId).putIfAbsent(e, EPKState(epk, null, null))
                    if (previous eq null) {
                        epk
                    } else {
                        previous.eOptP.asInstanceOf[EOptionP[E, P]]
                    }
                }
            } else {
                val finalEP = computeFallback[E, P](e, pkId)
                val previous = ps(pkId).putIfAbsent(e, EPKState(finalEP, null, null))
                if (previous eq null) {
                    triggerComputations(e, pkId)
                    finalEP
                } else {
                    previous.eOptP.asInstanceOf[EOptionP[E, P]]
                }
            }
        } else {
            current.eOptP.asInstanceOf[EOptionP[E, P]]
        }
    }

    private[this] val activeTasks = new AtomicInteger(0)
    private[this] val threads: Array[PKECThread] = Array.fill(THREAD_COUNT) { null }

    private[this] def startThreads(thread: Int => PKECThread): Unit = {
        var tId = 0
        while (tId < THREAD_COUNT) {
            val t = thread(tId)
            threads(tId) = t
            tId += 1
        }
        threads.foreach { _.start }
        threads.foreach { _.join }
        if (doTerminate) {
            if (exception ne null) throw exception;
            else throw new InterruptedException
        }
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        idle = false

        // If some values were explicitly set, we have to trigger corresponding triggered
        // computations.
        setAndPreinitializedValues.foreach { epk => triggerComputations(epk.e, epk.pk.id) }
        setAndPreinitializedValues = List.empty

        activeTasks.addAndGet(initialQueues.iterator.map(_.size()).sum)

        while (subPhaseId < subPhaseFinalizationOrder.length) {
            var continueCycles = false
            do {
                var continueFallbacks = false
                do {
                    startThreads(new WorkerThread(_))

                    quiescenceCounter += 1

                    startThreads(new FallbackThread(_))

                    continueFallbacks = activeTasks.get() > 0
                } while (continueFallbacks)

                startThreads(new CycleResolutionThread(_))

                resolveCycles()

                continueCycles = activeTasks.get() > 0
            } while (continueCycles)

            startThreads(new PartialPropertiesFinalizerThread(_))

            subPhaseId += 1

            ps(AnalysisKeyId).clear()
        }

        idle = true
    }

    private[this] val interimStates: Array[ArrayBuffer[EPKState]] =
        Array.fill(THREAD_COUNT)(null)
    private[this] val successors: Array[EPKState => Iterable[EPKState]] =
        Array.fill(THREAD_COUNT)(null)

    // executed on the main thread only
    private[this] def resolveCycles(): Unit = {
        val theInterimStates = new ArrayBuffer[EPKState](interimStates.iterator.map(_.size).sum)
        var tId = 0
        while (tId < THREAD_COUNT) {
            theInterimStates ++= interimStates(tId)
            tId += 1
        }

        val theSuccessors = (interimEPKState: EPKState) => {
            successors(getResponsibleTId(interimEPKState.eOptP.e))(interimEPKState)
        }

        val cSCCs = graphs.closedSCCs(theInterimStates, theSuccessors)

        for (cSCC <- cSCCs) {
            for (interimEPKState <- cSCC) {
                interimEPKState.dependees = null
                scheduleTask(new SetTask(interimEPKState.eOptP.toFinalEP))
            }
        }
    }

    class PKECThread(name: String) extends Thread(name)

    class WorkerThread(ownTId: Int) extends PKECThread(s"PropertyStoreThread-#$ownTId") {

        override def run(): Unit = {
            try {
                val initialTasks = initialQueues(ownTId)
                val initialTaskSize = initialTasks.size()
                var curInitialTask: QualifiedTask = null
                while ({ curInitialTask = initialTasks.poll(); curInitialTask != null }) {
                    curInitialTask.apply()
                }
                // Subtract the processed tasks just once to avoid synchronization overhad for
                // decrementing every time we process a task
                activeTasks.addAndGet(-initialTaskSize)

                val tasksQueue = queues(ownTId)
                val tasks = new java.util.ArrayDeque[QualifiedTask](50000 / THREAD_COUNT)
                while (!doTerminate) {
                    tasksQueue.drainTo(tasks)
                    if (tasks.isEmpty) {
                        val active = activeTasks.get()
                        if (active == 0) {
                            return ;
                        } else {
                            // try workstealing:
                            val largestQueue = queues.maxBy(_.size())
                            val largestQueueSize = largestQueue.size()
                            if (largestQueueSize > 100) {
                                largestQueue.drainTo(tasks, largestQueueSize / (THREAD_COUNT + 1))
                            } else {
                                val nextTask = tasksQueue.take()
                                if (!doTerminate) {
                                    nextTask.apply()
                                    activeTasks.decrementAndGet()
                                }
                            }
                        }
                    } else {
                        var curTask: QualifiedTask = null
                        while ({ curTask = tasks.poll(); curTask != null } && !doTerminate) {
                            curTask.apply()
                            activeTasks.decrementAndGet()
                        }
                    }
                }
            } catch {
                case ct: ControlThrowable    => throw ct
                case _: InterruptedException =>
                case ex: Throwable =>
                    collectException(ex)
                    doTerminate = true
            } finally {
                threads.foreach { t =>
                    if (t ne this)
                        t.interrupt()
                }
            }
        }
    }

    class FallbackThread(ownTId: Int) extends PKECThread(s"PropertyStoreFallbackThread-#$ownTId") {

        override def run(): Unit = handleExceptions {
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId) && (lazyComputations(pkId) eq null)) {
                    ps(pkId).forEachValue(Long.MaxValue, { epkState: EPKState =>
                        if (epkState.eOptP.isEPK && ((epkState.dependees eq null) || epkState.dependees.isEmpty)) {
                            val e = epkState.eOptP.e
                            if (getResponsibleTId(e) == ownTId) {
                                val reason = PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                                val p = fallbackPropertyBasedOnPKId(propertyStore, reason, e, pkId)
                                val finalEP = FinalEP(e, p)
                                incrementFallbacksUsedForComputedPropertiesCounter()
                                handleFinalResult(finalEP)
                            }
                        }
                    })
                }
                pkId += 1
            }
        }
    }

    class CycleResolutionThread(ownTId: Int) extends PKECThread(s"PropertyStoreCycleResolutionThread-#$ownTId") {

        override def run(): Unit = handleExceptions {
            val localInterimStates = ArrayBuffer.empty[EPKState]
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId)) {
                    ps(pkId).forEachValue(Long.MaxValue, { epkState: EPKState =>
                        val eOptP = epkState.eOptP
                        if (eOptP.isRefinable && getResponsibleTId(eOptP.e) == ownTId) {
                            localInterimStates.append(epkState)
                        }
                    })
                }
                pkId += 1
            }
            interimStates(ownTId) = localInterimStates

            successors(ownTId) = (interimEPKState: EPKState) => {
                val dependees = interimEPKState.dependees
                if (dependees != null) {
                    dependees.map { eOptionP =>
                        ps(eOptionP.pk.id).get(eOptionP.e)
                    }
                } else {
                    Iterable.empty
                }
            }
        }
    }

    class PartialPropertiesFinalizerThread(ownTId: Int) extends PKECThread(s"PropertyStorePartialPropertiesFinalizerThread-#$ownTId") {

        override def run(): Unit = handleExceptions {
            val pksToFinalize = subPhaseFinalizationOrder(subPhaseId).toSet

            pksToFinalize foreach { pk =>
                val pkId = pk.id
                ps(pkId).forEachValue(Long.MaxValue, { epkState: EPKState =>
                    val eOptP = epkState.eOptP
                    if (getResponsibleTId(eOptP.e) == ownTId && eOptP.isRefinable && !eOptP.isEPK) //TODO Won't be required once subPhaseFinalizationOrder is reliably only the partial properties
                        handleFinalResult(eOptP.toFinalEP, pksToFinalize)
                })
            }
        }
    }

    sealed trait QualifiedTask extends (() => Unit) with Comparable[QualifiedTask] {
        def priority: Int

        override def compareTo(other: QualifiedTask): Int = priority - other.priority
    }

    class ExecuteTask(f: => Unit) extends QualifiedTask {
        val priority = 0

        override def apply(): Unit = handleExceptions {
            f
        }
    }

    class SetTask[E <: Entity, P <: Property](
            finalEP: FinalEP[E, P]
    ) extends QualifiedTask {
        val priority = 0

        override def apply(): Unit = handleExceptions {
            handleFinalResult(finalEP)
        }
    }

    class PropertyComputationTask[E <: Entity](
            e:  E,
            pc: PropertyComputation[E]
    ) extends QualifiedTask {
        val priority = 0

        override def apply(): Unit = {
            handleResult(pc(e))
        }
    }

    class LazyComputationTask[E <: Entity](
            e:    E,
            pc:   PropertyComputation[E],
            pkId: Int
    ) extends QualifiedTask {
        val priority = 0

        override def apply(): Unit = {
            val state = ps(pkId).get(e)
            state.synchronized {
                if (state.eOptP.isEPK)
                    handleResult(pc(e))
            }
        }
    }

    class ContinuationTask(
            depender: EPKState, oldDependee: SomeEOptionP, dependee: EPKState
    ) extends QualifiedTask {
        scheduledOnUpdateComputations.incrementAndGet()

        val priority: Int = taskManager.weight(depender, dependee)

        override def apply(): Unit = {
            depender.applyContinuation(oldDependee)
        }
    }

    private[this] def getResponsibleTId(e: Entity): Int = {
        Math.abs(e.hashCode() >> 5) % THREAD_COUNT
    }
}

case class EPKState(
        var eOptP:     SomeEOptionP,
        var c:         OnUpdateContinuation,
        var dependees: Set[SomeEOptionP],
        // Use Java's HashSet here, this is internal implementiton only and they are *way* faster
        dependers:           java.util.HashSet[EPKState] = new java.util.HashSet(),
        suppressedDependers: java.util.HashSet[EPKState] = new java.util.HashSet()
) {

    override lazy val hashCode: Int = eOptP.hashCode()

    override def equals(obj: Any): Boolean = obj match {
        case other: EPKState => eOptP == other.eOptP
        case _               => false
    }

    def setFinal(finalEP: FinalEP[Entity, Property], unnotifiedPKs: Set[PropertyKind])(implicit ps: PKECPropertyStore): Unit = {
        var theEOptP: SomeEOptionP = null
        this.synchronized {
            theEOptP = eOptP
            if (theEOptP.isFinal) {
                throw new IllegalStateException(s"${theEOptP.e} already had the property $theEOptP")
            } else {
                if (ps.debug) eOptP.checkIsValidPropertiesUpdate(finalEP, Nil)
                dependers.synchronized {
                    eOptP = finalEP
                    notifyAndClearDependers(theEOptP, dependers, unnotifiedPKs)
                    notifyAndClearDependers(finalEP, suppressedDependers, unnotifiedPKs)
                }
            }
            dependees = null
        }

        if (theEOptP.isEPK) ps.triggerComputations(theEOptP.e, theEOptP.pk.id)
    }

    def interimUpdate(
        interimEP:    InterimEP[Entity, Property],
        newC:         OnUpdateContinuation,
        newDependees: Set[SomeEOptionP]
    )(implicit ps: PKECPropertyStore): Unit = {

        var theEOptP: SomeEOptionP = null
        this.synchronized {
            theEOptP = eOptP
            if (theEOptP.isFinal) {
                throw new IllegalStateException(s"${theEOptP.e} already had the property $theEOptP")
            } else {
                if (ps.debug) theEOptP.checkIsValidPropertiesUpdate(interimEP, newDependees)
                if (interimEP.isUpdatedComparedTo(theEOptP)) {
                    dependers.synchronized {
                        eOptP = interimEP
                        notifyAndClearDependers(theEOptP, dependers)
                    }
                }
                c = newC
                dependees = newDependees
            }
        }

        ps.updateDependees(this, newDependees)

        if (theEOptP.isEPK) ps.triggerComputations(theEOptP.e, theEOptP.pk.id)
    }

    def partialUpdate(updateComputation: UpdateComputation[Entity, Property])(implicit ps: PKECPropertyStore): Unit = {
        var theEOptP: SomeEOptionP = null

        this.synchronized {
            theEOptP = eOptP
            if (theEOptP.isFinal) {
                throw new IllegalStateException(s"${theEOptP.e} already had the property $theEOptP")
            } else {
                updateComputation(theEOptP) match {
                    case Some(interimEP) =>
                        if (ps.debug) assert(eOptP != interimEP)
                        dependers.synchronized {
                            eOptP = interimEP
                            notifyAndClearDependers(theEOptP, dependers)
                        }
                    case _ =>
                }
            }
        }

        if (theEOptP.isEPK) ps.triggerComputations(theEOptP.e, theEOptP.pk.id)
    }

    def addDependerOrScheduleContinuation(
        depender:      EPKState,
        dependee:      SomeEOptionP,
        dependeePK:    Int,
        suppressedPKs: Array[Boolean]
    )(implicit ps: PKECPropertyStore): Boolean = {
        dependers.synchronized {
            val theEOptP = eOptP
            // If the epk state is already updated (compared to the given dependee)
            // AND that update must not be suppressed (either final or not a suppressed PK).
            val isSuppressed = suppressedPKs(dependeePK)
            if ((theEOptP ne dependee) && (!isSuppressed || theEOptP.isFinal)) {
                if (isSuppressed)
                    ps.scheduleTask(new ps.ContinuationTask(depender, theEOptP, this))
                else
                    ps.scheduleTask(new ps.ContinuationTask(depender, dependee, this))
                false
            } else {
                if (isSuppressed) {
                    suppressedDependers.add(depender)
                } else {
                    dependers.add(depender)
                }
                true
            }
        }
    }

    def removeDepender(dependerState: EPKState): Unit = {
        dependers.synchronized {
            dependers.remove(dependerState)
            suppressedDependers.remove(dependerState)
        }
    }

    def notifyAndClearDependers(
        oldEOptP:      SomeEOptionP,
        theDependers:  java.util.HashSet[EPKState],
        unnotifiedPKs: Set[PropertyKind]           = Set.empty
    )(implicit ps: PKECPropertyStore): Unit = {
        theDependers.forEach { dependerState =>
            if (!unnotifiedPKs.contains(dependerState.eOptP.pk) && dependerState.dependees != null) {
                ps.scheduleTask(new ps.ContinuationTask(dependerState, oldEOptP, this))
            }
        }

        // Clear all dependers that will be notified, they will re-register if required
        theDependers.clear()
    }

    def applyContinuation(oldDependee: SomeEOptionP)(implicit ps: PKECPropertyStore): Unit = {
        this.synchronized {
            val theDependees = dependees
            // Are we still interested in that dependee?
            if (theDependees != null &&
                (oldDependee.isFinal || theDependees.contains(oldDependee))) {
                // We always retrieve the most up-to-date state of the dependee.
                val currentDependee = ps.ps(oldDependee.pk.id).get(oldDependee.e).eOptP.asEPS
                // IMPROVE: If we would know about ordering, we could only perform the operation
                // if the given value of the dependee is actually the "newest".
                ps.handleResult(c(currentDependee))
            }
        }
    }
}

trait PKECTaskManager {
    def weight(
        depender: EPKState, // The state to be updated
        dependee: EPKState // The dependee that triggered this update
    ): Int
}

object PKECTaskManager {
    def dependeesCount(depender: EPKState): Int = {
        val dependerDependees = if (depender == null) null else depender.dependees
        if (dependerDependees == null) 0 else dependerDependees.size
    }

    def dependersCount(dependee: EPKState): Int = {
        dependee.dependers.size() + dependee.suppressedDependers.size()
    }
}

case object PKECNoPriorityTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int = 0
}

case object PKECFIFOTaskManager extends PKECTaskManager {
    val counter = new AtomicInteger(0)

    override def weight(depender: EPKState, dependee: EPKState): Int = counter.getAndIncrement()
}

case object PKECLIFOTaskManager extends PKECTaskManager {
    val counter = new AtomicInteger(Int.MaxValue)

    override def weight(depender: EPKState, dependee: EPKState): Int = counter.getAndDecrement()
}

case object PKECManyDependeesFirstTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        -PKECTaskManager.dependeesCount(depender)
}

case object PKECManyDependeesLastTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        PKECTaskManager.dependeesCount(depender)
}

case object PKECManyDependersFirstTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        -PKECTaskManager.dependersCount(dependee)
}

case object PKECManyDependersLastTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        PKECTaskManager.dependersCount(dependee)
}

case object PKECManyDependenciesFirstTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        -(Math.max(1, PKECTaskManager.dependersCount(dependee)) *
            Math.max(PKECTaskManager.dependeesCount(depender), 1))
}

case object PKECManyDependenciesLastTaskManager extends PKECTaskManager {
    override def weight(depender: EPKState, dependee: EPKState): Int =
        Math.max(1, PKECTaskManager.dependersCount(dependee)) *
            Math.max(PKECTaskManager.dependeesCount(depender), 1)
}

private class FakeEntity {
    override def toString: String = "FakeEntity"
}

object PKECPropertyStore extends PropertyStoreFactory[PKECPropertyStore] {

    final val TaskManagerKey = "org.opalj.fpcf.par.PKECPropertyStore.TasksManager"
    final val MaxEvaluationDepthKey = "org.opalj.fpcf.par.PKECPropertyStore.MaxEvaluationDepth"

    @volatile var MaxThreads: Int = org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKECPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap

        val config =
            contextMap.get(classOf[Config]) match {
                case Some(config: Config) => config
                case _                    => org.opalj.BaseConfig
            }

        val taskManager = config.getString(TaskManagerKey) match {
            case "NoPriority"            => PKECNoPriorityTaskManager
            case "FIFO"                  => PKECFIFOTaskManager
            case "LIFO"                  => PKECLIFOTaskManager
            case "ManyDependeesFirst"    => PKECManyDependeesFirstTaskManager
            case "ManyDependeesLast"     => PKECManyDependeesLastTaskManager
            case "ManyDependersFirst"    => PKECManyDependersFirstTaskManager
            case "ManyDependersLast"     => PKECManyDependersLastTaskManager
            case "ManyDependenciesFirst" => PKECManyDependenciesFirstTaskManager
            case "ManyDependenciesLast"  => PKECManyDependenciesLastTaskManager
        }

        val maxEvaluationDepth = config.getInt(MaxEvaluationDepthKey)

        val ps = new PKECPropertyStore(contextMap, taskManager, MaxThreads, maxEvaluationDepth)
        ps
    }
}