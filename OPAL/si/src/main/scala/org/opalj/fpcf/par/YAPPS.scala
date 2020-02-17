/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable.ArrayBuffer
import scala.util.control.ControlThrowable

import org.opalj.log.LogContext
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId

/**
 * Yet another parallel property store.
 *
 * @author Dominik Helm
 */
class YAPPS(
        final val ctx: Map[Class[_], AnyRef]
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore {
    propertyStore ⇒

    val THREAD_COUNT = 4

    override def MaxEvaluationDepth: Int = 0

    val ps: Array[ConcurrentHashMap[Entity, YappsEPKState]] =
        Array.fill(PropertyKind.SupportedPropertyKinds) { new ConcurrentHashMap() }

    val tasks: PriorityBlockingQueue[YappsTask] = new PriorityBlockingQueue()

    private[this] val triggeredComputations: Array[Array[SomePropertyComputation]] =
        new Array(PropertyKind.SupportedPropertyKinds)

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

    private[this] val scheduledTasks: Array[Int] = Array.fill(THREAD_COUNT) { 0 }
    override def scheduledTasksCount: Int = scheduledTasks.sum

    private[this] val scheduledOnUpdateComputations: Array[Int] = Array.fill(THREAD_COUNT) { 0 }
    override def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputations.sum

    override def fallbacksUsedForComputedPropertiesCount: Int = 0 //TODO

    override private[fpcf] def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = {
        // ???
    }

    // --------------------------------------------------------------------------------------------
    //
    // BASIC QUERY METHODS (ONLY TO BE CALLED WHEN THE STORE IS QUIESCENT)
    //
    // --------------------------------------------------------------------------------------------

    override def toString(printProperties: Boolean): String =
        if (printProperties) {
            val properties = for (pkId ← 0 to PropertyKey.maxId) yield {
                var entities: List[String] = List.empty
                ps(pkId).forEachValue(Long.MaxValue, { state: YappsEPKState ⇒
                    entities ::= state.eOptP.toString.replace("\n", "\n\t")
                })
                entities.sorted.mkString(s"Entities for property key $pkId:\n\t", "\n\t", "\n")
            }
            properties.mkString("PropertyStore(\n\t", "\n\t", "\n)")
        } else {
            s"PropertyStore(properties=${ps.iterator.map(_.size).sum})"
        }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        ps.iterator.flatMap { propertiesPerKind ⇒
            var result: List[Entity] = List.empty
            propertiesPerKind.forEachValue(Long.MaxValue, { state: YappsEPKState ⇒ if (propertyFilter(state.eOptP.asEPS)) result ::= state.eOptP.e })
            result
        }
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        var result: List[EPS[Entity, P]] = List.empty
        ps(pk.id).forEachValue(Long.MaxValue, { state: YappsEPKState ⇒ result ::= state.eOptP.asInstanceOf[EPS[Entity, P]] })
        result.iterator
    }

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        entities { eps ⇒ eps.lb == lb && eps.ub == ub }
    }

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = {
        entities { eps ⇒ eps.lb == lb }
    }

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = {
        entities { eps ⇒ eps.ub == ub }
    }

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = {
        ps.iterator.flatMap { propertiesPerKind ⇒
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
        ps.exists { propertiesPerKind ⇒
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
        val epkState = YappsEPKState(FinalEP(e, p), null, null)

        val oldP = ps(p.id).put(e, epkState)
        if (oldP ne null) {
            throw new IllegalStateException(s"$e already had the property $oldP")
        }
        setAndPreinitializedValues ::= EPK(e, p.key)
    }

    override protected[this] def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(pc: EOptionP[E, P] ⇒ InterimEP[E, P]): Unit = {
        val pkId = pk.id
        val propertiesOfKind = ps(pkId)
        val oldEPKState = propertiesOfKind.get(e)
        val newInterimEP: SomeInterimEP =
            oldEPKState match {
                case null ⇒
                    val epk = EPK(e, pk)
                    setAndPreinitializedValues ::= epk
                    pc(epk)
                case epkState ⇒
                    pc(epkState.eOptP.asInstanceOf[EOptionP[E, P]])
            }
        assert(newInterimEP.isRefinable)
        val newEPKState = YappsEPKState(newInterimEP, null, null)
        propertiesOfKind.put(e, newEPKState)
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    private[this] def scheduleTask(task: YappsTask): Unit = {
        activeTasks.incrementAndGet()
        tasks.offer(task)
    }

    private[this] def schedulePropertyComputation[E <: Entity](
        e:  E,
        pc: PropertyComputation[E]
    ): Unit = {
        scheduleTask(new YappsPropertyComputationTask(e, pc))
    }

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        doApply(EPK(e, pk), e, pk.id)
    }

    override def execute(f: ⇒ Unit): Unit = {
        scheduleTask(new YappsExecuteTask(f))
    }

    override def handleResult(r: PropertyComputationResult): Unit = {
        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            //
            // Result containers
            //

            case Results.id ⇒
                r.asResults.foreach { handleResult }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs) = r
                handleResult(ir)
                npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                    val (pc, e) = npc
                    schedulePropertyComputation(e, pc)
                }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                handleFinalResult(r.asResult.finalEP)

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { finalEP ⇒ handleFinalResult(finalEP) }

            case InterimResult.id ⇒
                val interimR = r.asInterimResult
                handleInterimResult(
                    interimR.eps,
                    interimR.c,
                    interimR.dependees
                )

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                handlePartialResult(r, u, e, pk)

            case InterimPartialResult.id ⇒
                val InterimPartialResult(prs, dependees, c) = r

                prs foreach { pr ⇒
                    handlePartialResult(
                        pr,
                        pr.u.asInstanceOf[SomeEOptionP ⇒ Option[SomeInterimEP]],
                        pr.e,
                        pr.pk
                    )
                }

                val e = new FakeEntity()
                val epk = EPK(e, AnalysisKey)

                val epkState = YappsEPKState(
                    epk,
                    { dependee: SomeEPS ⇒
                        val result = c(dependee)

                        ps(AnalysisKeyId).remove(e)

                        //println(s"interim partial result remove dependees (${dependees} from depender: $epk")
                        dependees.foreach { dependee ⇒
                            ps(dependee.pk.id).get(dependee.e).removeDepender(epk)
                        }

                        result
                    },
                    dependees
                )

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
        val ePKState = ps(pk.id).computeIfAbsent(e, { _ ⇒ isFresh = true; YappsEPKState(finalEP, null, null) })
        if (isFresh) triggerComputations(e, pk.id)
        else ePKState.setFinal(finalEP, unnotifiedPKs)

        //TODO remove depender status
    }

    private[this] def triggerComputations(e: Entity, pkId: Int): Unit = {
        val computations = triggeredComputations(pkId)
        if (computations ne null) {
            computations foreach { pc ⇒
                schedulePropertyComputation(e, pc.asInstanceOf[PropertyComputation[Entity]])
            }
        }
    }

    private[this] def handleInterimResult(
        interimEP: InterimEP[Entity, _ >: Null <: Property],
        c:         ProperOnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        val SomeEPS(e, pk) = interimEP
        var isFresh = false
        val ePKState =
            ps(pk.id).computeIfAbsent(e, { _ ⇒ isFresh = true; YappsEPKState(interimEP, c, dependees) })
        if (isFresh) {
            triggerComputations(e, pk.id)
            updateDependees(ePKState, dependees)
        } else ePKState.interimUpdate(interimEP, c, dependees)

        //TODO update depender status
    }

    private[this] def handlePartialResult(
        pr:     PropertyComputationResult,
        update: UpdateComputation[Entity, Property],
        e:      Entity,
        pk:     PropertyKey[Property]
    ): Unit = {
        val ePKState = ps(pk.id).computeIfAbsent(e, _ ⇒ YappsEPKState(EPK(e, pk), null, null))
        ePKState.partialUpdate(update)
    }

    def updateDependees(depender: YappsEPKState, newDependees: Traversable[SomeEOptionP]): Unit = {
        val dependerEpk = depender.eOptP.toEPK
        val suppressedPKs = suppressInterimUpdates(dependerEpk.pk.id)
        newDependees.forall { dependee ⇒
            val dependeeState = ps(dependee.pk.id).get(dependee.e)
            dependeeState.addDependerOrScheduleContinuation(dependerEpk, dependee, suppressedPKs)
        }
    }

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        val current = ps(pkId).get(e)
        if (current eq null) {
            val lazyComputation = lazyComputations(pkId)
            if (lazyComputation ne null) {
                val previous = ps(pkId).putIfAbsent(e, YappsEPKState(epk, null, null))
                if (previous eq null) {
                    scheduleTask(
                        new YappsLazyComputationTask(
                            e,
                            lazyComputation.asInstanceOf[E ⇒ PropertyComputationResult],
                            pkId
                        )
                    )
                    epk
                } else {
                    previous.eOptP.asInstanceOf[EOptionP[E, P]]
                }
            } else if (propertyKindsComputedInThisPhase(pkId)) {
                val transformer = transformersByTargetPK(pkId)
                if (transformer ne null) {
                    val dependee = this(e, transformer._1)
                    if (dependee.isFinal) {
                        val result = transformer._2(e, dependee.asFinal.p)
                        val previous = ps(pkId).putIfAbsent(e, YappsEPKState(result, null, null))
                        if (previous eq null) {
                            triggerComputations(e, pkId)
                            result.asInstanceOf[FinalEP[E, P]]
                        } else {
                            previous.eOptP.asInstanceOf[EOptionP[E, P]]
                        }
                    } else {
                        val newState = YappsEPKState(epk, d ⇒ new Result(transformer._2(e, d.asFinal.p)), Some(dependee))
                        val previous = ps(pkId).putIfAbsent(e, newState)
                        if (previous eq null) {
                            updateDependees(newState, Some(dependee))
                            epk
                        } else {
                            previous.eOptP.asInstanceOf[EOptionP[E, P]]
                        }
                    }
                } else {
                    val previous = ps(pkId).putIfAbsent(e, YappsEPKState(epk, null, null))
                    if (previous eq null) {
                        epk
                    } else {
                        previous.eOptP.asInstanceOf[EOptionP[E, P]]
                    }
                }
            } else {
                val finalEP = computeFallback[E, P](e, pkId)
                val previous = ps(pkId).putIfAbsent(e, YappsEPKState(finalEP, null, null))
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
    private[this] val threads: Array[YappsThread] = Array.fill(THREAD_COUNT) { null }

    private[this] def startThreads(thread: (Int) ⇒ YappsThread): Unit = {
        var tId = 0
        while (tId < THREAD_COUNT) {
            val t = thread(tId)
            threads(tId) = t
            tId += 1
        }
        threads.foreach { _.start() }
        threads.foreach { _.join }
        if (doTerminate) {
            if (exception ne null) throw exception
            else throw new InterruptedException
        }
    }

    override def waitOnPhaseCompletion(): Unit = {
        idle = false

        // If some values were explicitly set, we have to trigger corresponding triggered
        // computations.
        setAndPreinitializedValues.foreach { epk ⇒ triggerComputations(epk.e, epk.pk.id) }
        setAndPreinitializedValues = List.empty

        while (subPhaseId < subPhaseFinalizationOrder.length) {
            var continueCycles = false
            do {
                var continueFallbacks = false
                do {
                    startThreads(new YappsWorkerThread(_))

                    quiescenceCounter += 1

                    startThreads(new YappsFallbackThread(_))

                    continueFallbacks = !tasks.isEmpty
                } while (continueFallbacks)

                startThreads(new YappsCycleResolutionThread(_))

                resolveCycles()

                continueCycles = !tasks.isEmpty
            } while (continueCycles)

            startThreads(new YappsPartialPropertiesFinalizerThread(_))

            subPhaseId += 1
        }

        idle = true
    }

    private[this] val interimStates: Array[ArrayBuffer[YappsEPKState]] =
        Array.fill(THREAD_COUNT)(null)
    private[this] val successors: Array[YappsEPKState ⇒ Traversable[YappsEPKState]] =
        Array.fill(THREAD_COUNT)(null)

    // executed on the main thread only
    private[this] def resolveCycles(): Unit = {
        val theInterimStates = new ArrayBuffer[YappsEPKState](interimStates.iterator.map(_.size).sum)
        var tId = 0
        while (tId < THREAD_COUNT) {
            theInterimStates ++= interimStates(tId)
            tId += 1
        }

        val theSuccessors = (interimEPKState: YappsEPKState) ⇒ {
            successors(getResponsibleTId(interimEPKState.eOptP.e))(interimEPKState)
        }

        val cSCCs = graphs.closedSCCs(theInterimStates, theSuccessors)

        for (cSCC ← cSCCs) {
            for (interimEPKState ← cSCC) {
                val dependees = interimEPKState.dependees
                val epk = interimEPKState.eOptP.toEPK
                dependees.foreach { dependee ⇒
                    // during execution, no other thread accesses the dependers of the EPKState
                    ps(dependee.pk.id).get(dependee.e).dependers -= epk
                }
                scheduleTask(new YappsSetTask(interimEPKState.eOptP.toFinalEP))
            }
        }
    }

    class YappsThread(name: String) extends Thread(name)

    class YappsWorkerThread(ownTId: Int) extends YappsThread(s"PropertyStoreThread-#$ownTId") {

        override def run(): Unit = {
            try {
                while (!doTerminate) {
                    val curTask = tasks.poll()
                    if (curTask eq null) {
                        val active = activeTasks.get()
                        if (active == 0) {
                            threads.foreach { t ⇒
                                if (t ne this)
                                    t.interrupt()
                            }
                            return ;
                        } else {
                            val nextTask = tasks.take()
                            if (!doTerminate) {
                                nextTask.apply()
                                activeTasks.decrementAndGet()
                            }
                        }
                    } else {
                        if (!doTerminate) {
                            curTask.apply()
                            activeTasks.decrementAndGet()
                        }
                    }
                }
            } catch {
                case ct: ControlThrowable    ⇒ throw ct
                case _: InterruptedException ⇒
                case ex: Throwable ⇒
                    exception = ex
                    doTerminate = true
            } finally {
                threads.foreach { t ⇒
                    if (t ne this)
                        t.interrupt()
                }
            }
        }
    }

    class YappsFallbackThread(ownTId: Int) extends YappsThread(s"PropertyStoreFallbackThread-#$ownTId") {

        override def run(): Unit = {
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId) && (lazyComputations(pkId) eq null)) {
                    ps(pkId).forEachValue(Long.MaxValue, { epkState: YappsEPKState ⇒
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

    class YappsCycleResolutionThread(ownTId: Int) extends YappsThread(s"PropertyStoreCycleResolutionThread-#$ownTId") {

        override def run(): Unit = {
            val localInterimStates = ArrayBuffer.empty[YappsEPKState]
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId)) {
                    ps(pkId).forEachValue(Long.MaxValue, { epkState: YappsEPKState ⇒
                        val eOptP = epkState.eOptP
                        if (getResponsibleTId(eOptP.e) == ownTId &&
                            epkState.eOptP.isRefinable) {
                            localInterimStates.append(epkState)
                        }
                    })
                }
                pkId += 1
            }
            interimStates(ownTId) = localInterimStates

            successors(ownTId) = (interimEPKState: YappsEPKState) ⇒ {
                val dependees = interimEPKState.dependees
                if (dependees != null) {
                    interimEPKState.dependees.map { eOptionP ⇒
                        ps(eOptionP.pk.id).get(eOptionP.e)
                    }
                } else {
                    Traversable.empty
                }
            }
        }
    }

    class YappsPartialPropertiesFinalizerThread(ownTId: Int) extends YappsThread(s"PropertyStorePartialPropertiesFinalizerThread-#$ownTId") {

        override def run(): Unit = {
            val pksToFinalize = subPhaseFinalizationOrder(subPhaseId).toSet

            pksToFinalize foreach { pk ⇒
                val pkId = pk.id
                ps(pkId).forEachValue(Long.MaxValue, { epkState: YappsEPKState ⇒
                    val eOptP = epkState.eOptP
                    if (getResponsibleTId(eOptP.e) == ownTId && eOptP.isRefinable && !eOptP.isEPK) //TODO Won't be required once subPhaseFinalizationOrder is reliably only the partial properties
                        handleFinalResult(eOptP.toFinalEP, pksToFinalize)
                })
            }
        }
    }

    trait YappsTask extends (() ⇒ Unit) with Comparable[YappsTask] {
        val priority: Int

        override def compareTo(other: YappsTask): Int = this.priority - other.priority
    }

    class YappsExecuteTask(f: ⇒ Unit) extends YappsTask {
        val priority = 0

        override def apply(): Unit = {
            f
        }
    }

    class YappsSetTask[E <: Entity, P <: Property](
            finalEP: FinalEP[E, P]
    ) extends YappsTask {
        val priority = 0

        override def apply(): Unit = {
            handleFinalResult(finalEP)
        }
    }

    class YappsPropertyComputationTask[E <: Entity](
            e:  E,
            pc: PropertyComputation[E]
    ) extends YappsTask {
        val priority = 0

        override def apply(): Unit = {
            handleResult(pc(e))
        }
    }

    class YappsLazyComputationTask[E <: Entity](
            e:    E,
            pc:   PropertyComputation[E],
            pkId: Int
    ) extends YappsTask {
        val priority = 0

        override def apply(): Unit = {
            val state = ps(pkId).get(e)
            state.lock.lock()
            if (state.eOptP.isEPK)
                handleResult(pc(e))
            state.lock.unlock()
        }
    }

    class YappsContinuationTask(depender: SomeEPK, oldDependee: SomeEOptionP) extends YappsTask {
        val priority = 0

        override def apply(): Unit = {
            val epkState = ps(depender.pk.id).get(depender.e)
            if (epkState ne null)
                epkState.applyContinuation(oldDependee)
        }
    }

    case class YappsEPKState(
            @volatile var eOptP:     SomeEOptionP,
            @volatile var c:         OnUpdateContinuation,
            @volatile var dependees: Traversable[SomeEOptionP],
            @volatile var dependers: Set[SomeEPK]              = Set.empty
    ) {
        val lock = new ReentrantLock()
        val dependersLock = new ReentrantLock()

        def setFinal(finalEP: FinalEP[Entity, Property], unnotifiedPKs: Set[PropertyKind]): Unit = {
            lock.lockInterruptibly()
            val theEOptP = eOptP
            if (theEOptP.isFinal) {
                throw new IllegalStateException(s"${theEOptP.e} already had the property $theEOptP")
            } else {
                if (debug) eOptP.checkIsValidPropertiesUpdate(finalEP, Nil)
                dependersLock.lock()
                eOptP = finalEP
            }
            dependees = null
            lock.unlock()

            val theDependers = dependers
            dependers = null
            dependersLock.unlock()

            notifyDependers(finalEP, theEOptP, theDependers, unnotifiedPKs)

            if (theEOptP.isEPK) triggerComputations(theEOptP.e, theEOptP.pk.id)
        }

        def interimUpdate(
            interimEP:    InterimEP[Entity, Property],
            newC:         OnUpdateContinuation,
            newDependees: Traversable[SomeEOptionP]
        ): Unit = {
            var requiresNotification = false

            lock.lockInterruptibly()
            val theEOptP = eOptP
            if (theEOptP.isFinal) {
                throw new IllegalStateException(s"${theEOptP.e} already had the property $theEOptP")
            } else {
                if (debug) theEOptP.checkIsValidPropertiesUpdate(interimEP, newDependees)
                if (interimEP.isUpdatedComparedTo(theEOptP)) {
                    requiresNotification = true
                    dependersLock.lockInterruptibly()
                    eOptP = interimEP
                }
                c = newC
                dependees = newDependees
            }
            lock.unlock()

            if (requiresNotification) {
                val theDependers = dependers
                // Clear all dependers that will be notified, they will re-register if required
                dependers = dependers.filter(d ⇒ suppressInterimUpdates(d.pk.id)(theEOptP.pk.id))
                dependersLock.unlock()
                notifyDependers(interimEP, theEOptP, theDependers)
            }

            updateDependees(this, newDependees)

            if (theEOptP.isEPK) triggerComputations(theEOptP.e, theEOptP.pk.id)
        }

        def partialUpdate(updateComputation: UpdateComputation[Entity, Property]): Unit = {
            lock.lockInterruptibly()
            val theEOptP = eOptP
            val u = updateComputation(theEOptP)
            val newEOptP = u match {
                case Some(interimEP) ⇒
                    if (debug) assert(eOptP != interimEP)
                    dependersLock.lockInterruptibly()
                    eOptP = interimEP
                    interimEP
                case _ ⇒
                    null
            }
            lock.unlock()

            if (newEOptP ne null) {
                val theDependers = dependers
                dependers = dependers.filter(d ⇒ suppressInterimUpdates(d.pk.id)(theEOptP.pk.id))
                dependersLock.unlock()
                notifyDependers(newEOptP, theEOptP, theDependers)
            }

            if (theEOptP.isEPK) triggerComputations(theEOptP.e, theEOptP.pk.id)
        }

        def addDependerOrScheduleContinuation(
            depender:      SomeEPK,
            dependee:      SomeEOptionP,
            suppressedPKs: Array[Boolean]
        ): Boolean = {
            dependersLock.lockInterruptibly()
            try {
                val theEOptP = eOptP
                // If the epk state is already updated (compared to the given dependee)
                // AND that update must not be suppressed (either final or not a suppressed PK).
                if ((theEOptP ne dependee) &&
                    (theEOptP.isFinal || !suppressedPKs(dependee.pk.id))) {
                    scheduleTask(new YappsContinuationTask(depender, dependee))
                    false
                } else {
                    dependers += depender
                    true
                }
            } finally {
                dependersLock.unlock()
            }
        }

        def removeDepender(epk: SomeEPK): Unit = {
            dependersLock.lockInterruptibly()
            if (dependers != null) dependers -= epk
            dependersLock.unlock()
        }

        def notifyDependers(
            theEOptP:      SomeEPS,
            oldEOptP:      SomeEOptionP,
            theDependers:  Set[SomeEPK],
            unnotifiedPKs: Set[PropertyKind] = Set.empty
        ): Unit = {
            if (theDependers ne null) {
                theDependers.foreach { depender ⇒
                    if (!unnotifiedPKs.contains(depender.pk) &&
                        (theEOptP.isFinal || !suppressInterimUpdates(depender.pk.id)(theEOptP.pk.id))) {
                        scheduleTask(new YappsContinuationTask(depender, oldEOptP))
                    }
                }
            }
        }

        def applyContinuation(oldDependee: SomeEOptionP): Unit = {
            // IMPROVE: Use tryLock() instead
            val isSuppressed = suppressInterimUpdates(eOptP.pk.id)(oldDependee.pk.id)
            val epk = oldDependee.toEPK
            lock.lockInterruptibly()
            val theDependees = dependees
            // We are still interessted in that dependee?
            if (theDependees != null && theDependees.exists { d ⇒
                (d eq oldDependee) || (isSuppressed && epk == d.toEPK)
            }) {
                // We always retrieve the most up-to-date state of the dependee.
                val currentDependee = ps(oldDependee.pk.id).get(oldDependee.e).eOptP.asEPS
                // IMPROVE: If we would know about ordering, we could only perform the operation
                // if the given value of the dependee is actually the "newest".
                handleResult(c(currentDependee))
            }
            lock.unlock()
        }

    }

    private[this] def getResponsibleTId(e: Entity): Int = {
        Math.abs(e.hashCode() >> 5) % THREAD_COUNT
    }
}

object YAPPS extends PropertyStoreFactory[YAPPS] {

    final val MaxEvaluationDepthKey = "org.opalj.fpcf.par.PKECPropertyStore.MaxEvaluationDepth"

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): YAPPS = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap

        val ps = new YAPPS(contextMap)
        ps
    }
}