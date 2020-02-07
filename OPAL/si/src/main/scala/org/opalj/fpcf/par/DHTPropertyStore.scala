/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.{Set ⇒ MutableSet}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.PriorityQueue
import scala.collection.mutable.Queue
import scala.util.control.ControlThrowable

import org.opalj.log.LogContext
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPKId

/**
 * Yet another parallel property store.
 * Based on the ideas of a distributed hash table, computations for an entity are bound to a
 * specific thread.
 *
 * @author Dominik Helm
 */
class DHTPropertyStore(
        final val ctx: Map[Class[_], AnyRef]
)(
        implicit
        val logContext: LogContext
) extends ParallelPropertyStore {
    propertyStore ⇒

    val THREAD_COUNT = 1

    private[this] val ps: Array[Array[AnyRefMap[Entity, DhtEpkState]]] =
        Array.fill(THREAD_COUNT) {
            Array.fill(PropertyKind.SupportedPropertyKinds) {
                AnyRefMap.empty
            }
        }

    private[this] val dependers: Array[AnyRefMap[SomeEPK, MutableSet[SomeEPK]]] =
        Array.fill(THREAD_COUNT) { AnyRefMap.empty }

    private[this] val tasks: Array[Array[ConcurrentLinkedQueue[DhtTask]]] =
        Array.fill(THREAD_COUNT) {
            Array.fill(THREAD_COUNT) {
                new ConcurrentLinkedQueue
            }
        }

    //TODO Need Priority Queue here
    private[this] val localTasks: Array[PriorityQueue[DhtContinuationTask]] =
        Array.fill(THREAD_COUNT) {
            new PriorityQueue()({
                (x: DhtContinuationTask, y: DhtContinuationTask) ⇒
                    x.priority - y.priority

            })
        }

    private[this] val fastQueues: Array[Queue[DhtTask]] =
        Array.fill(THREAD_COUNT) { new Queue() }

    private[this] val activeThreads: AtomicInteger = new AtomicInteger(THREAD_COUNT)

    private[this] val threads: Array[Thread] = new Array(THREAD_COUNT)

    private[this] val triggeredComputations: Array[Array[SomePropertyComputation]] =
        new Array(PropertyKind.SupportedPropertyKinds)

    private[this] var setAndPreinitializedValues: List[SomeEPK] = List.empty

    override def MaxEvaluationDepth: Int = 0 //TODO
    override def shutdown(): Unit = {}
    var idle: Boolean = true
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
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                ps.iterator.flatMap(_(pkId).valuesIterator.map {
                    _.eOptP.toString.replace("\n", "\n\t")
                }).toList.sorted.mkString(s"Entities for property key $pkId:\n\t", "\n\t", "\n")
                pkId += 1
            }
            ps.mkString("PropertyStore(\n\t", "\n\t", "\n)")
        } else {
            s"PropertyStore(properties=${ps.iterator.flatMap(_.map(_.size)).sum})"
        }

    override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = {
        ps.iterator.flatMap { propertiesPerThread ⇒
            propertiesPerThread.iterator.flatMap { propertiesPerKind ⇒
                propertiesPerKind.valuesIterator.filter { epkState ⇒
                    propertyFilter(epkState.eOptP.asEPS)
                }
            }
        }.map(_.eOptP.e)
    }

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = {
        ps.iterator.flatMap { propertiesPerThread ⇒
            propertiesPerThread(pk.id).valuesIterator.map(_.eOptP.asInstanceOf[EPS[Entity, P]])
        }
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
        ps(getResponsibleTId(e)).iterator.flatMap { propertiesPerKind ⇒
            val ePKState = propertiesPerKind.get(e)
            if (ePKState.exists(_.eOptP.isEPS))
                Iterator.single(ePKState.get.eOptP.asInstanceOf[EPS[E, Property]])
            else
                Iterator.empty
        }
    }

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = {
        val ePKState = ps(getResponsibleTId(e))(pk.id).get(e)
        ePKState.exists { state ⇒
            state.eOptP.hasUBP || state.eOptP.hasLBP
        }
    }

    override def isKnown(e: Entity): Boolean = {
        ps(getResponsibleTId(e)).exists { propertiesPerKind ⇒
            propertiesPerKind.contains(e)
        }
    }

    override def get[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Option[EOptionP[E, P]] = {
        ps(getResponsibleTId(e))(pk.id).get(e).map(_.eOptP.asInstanceOf[EOptionP[E, P]])
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
        schedulePropertyComputation(e, pc, 0)
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
        val epkState = DhtEpkState(FinalEP(e, p), null, null)

        val oldP = ps(getResponsibleTId(e))(p.id).put(e, epkState)
        if (oldP.isDefined) {
            throw new IllegalStateException(s"$e already had the property $oldP")
        }
        setAndPreinitializedValues ::= EPK(e, p.key)
    }

    override protected[this] def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(pc: EOptionP[E, P] ⇒ InterimEP[E, P]): Unit = {
        val pkId = pk.id
        val propertiesOfKind = ps(getResponsibleTId(e))(pkId)
        val oldEPKState = propertiesOfKind.get(e)
        val newInterimEP: SomeInterimEP =
            oldEPKState match {
                case None ⇒
                    val epk = EPK(e, pk)
                    setAndPreinitializedValues ::= epk
                    pc(epk)
                case Some(epkState) ⇒
                    pc(epkState.eOptP.asInstanceOf[EOptionP[E, P]])
            }
        assert(newInterimEP.isRefinable)
        val newEPKState = DhtEpkState(newInterimEP, null, null)
        propertiesOfKind.put(e, newEPKState)
    }

    // --------------------------------------------------------------------------------------------
    //
    // CORE IMPLEMENTATION - THREAD SAFE PART
    //
    // --------------------------------------------------------------------------------------------

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = {
        doApply(EPK(e, pk), e, pk.id)
    }

    override def execute(f: ⇒ Unit): Unit = {
        submitTask(new DhtExecuteTask(f), 0 /* TODO: Use some fair distribution */ , currentTId())
    }

    private[this] def schedulePropertyComputation[E <: Entity](
        e:   E,
        pc:  PropertyComputation[E],
        tId: Int
    ): Unit = {
        submitTask(new DhtPropertyComputationTask(e, pc), getResponsibleTId(e), tId)
    }

    private[this] def currentTId(): Int = {
        Thread.currentThread() match {
            case DhtThread(id) ⇒ id
            case _             ⇒ 0
        }
    }

    private[this] val fakeEntities: Array[Int] = Array.fill(THREAD_COUNT) { 0 }

    override def handleResult(r: PropertyComputationResult): Unit = {
        doHandleResult(r)
    }

    private[this] def doHandleResult(r: PropertyComputationResult, origin: DhtEpkState = null): Unit = {
        r.id match {

            case NoResult.id ⇒
            // A computation reported no result; i.e., it is not possible to
            // compute a/some property/properties for a given entity.

            //
            // Result containers
            //

            case Results.id ⇒
                r.asResults.foreach { doHandleResult(_, origin) }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs) = r
                doHandleResult(ir, origin)
                npcs /*: Iterator[(PropertyComputation[e],e)]*/ foreach { npc ⇒
                    val (pc, e) = npc
                    val tId = currentTId()
                    schedulePropertyComputation(e, pc, tId)
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

                val ownTId = currentTId()

                /*if((origin ne null) && origin.eOptP.e.isInstanceOf[DhtFakeEntity]){
                    val epk = origin.eOptP.asEPK
                    val e = epk.e

                    val epkState = DhtEpkState(
                        epk,
                        { dependee: SomeEPS ⇒
                            val result = c(dependee)

                            ps(ownTId)(AnalysisKeyId).remove(e)

                            val ds = dependers(ownTId)
                            dependees.foreach { dependee ⇒
                                ds(dependee.toEPK) -= epk
                            }

                            result
                        },
                        dependees
                    )

                    ps(ownTId)(AnalysisKeyId).put(epk, epkState)

                    updateDependees(epk, dependees, Some(origin), ownTId)
                } else {*/
                val e = new DhtFakeEntity(ownTId + THREAD_COUNT * fakeEntities(ownTId))
                fakeEntities(ownTId) += 1
                val epk = EPK(e, AnalysisKey)

                val epkState = DhtEpkState(
                    epk,
                    { dependee: SomeEPS ⇒
                        val result = c(dependee)

                        ps(ownTId)(AnalysisKeyId).remove(e)

                        val ds = dependers(ownTId)
                        dependees.foreach { dependee ⇒
                            ds(dependee.toEPK) -= epk
                        }

                        result
                    },
                    dependees
                )

                ps(ownTId)(AnalysisKeyId).put(e, epkState)

                updateDependees(epk, dependees, None, ownTId)
            //}
        }
    }

    private[this] def notifyDependers(
        dependee:      SomeEPS,
        ownTId:        Int,
        unnotifiedPKs: Set[PropertyKind] = Set.empty
    ): Unit = {
        var tId = 0
        while (tId < THREAD_COUNT) {
            submitTask(new DhtNotifyDependersTask(dependee, unnotifiedPKs), tId, ownTId)
            tId += 1
        }
    }

    private[this] def triggerComputations(e: Entity, pkId: Int, tId: Int): Unit = {
        val computations = triggeredComputations(pkId)
        if (computations ne null) {
            computations foreach { pc ⇒
                schedulePropertyComputation(e, pc.asInstanceOf[PropertyComputation[Entity]], tId)
            }
        }
    }

    private[this] def handleFinalResult(
        finalEP:       FinalEP[Entity, Property],
        unnotifiedPKs: Set[PropertyKind]         = Set.empty
    ): Unit = {
        val epkState = DhtEpkState(finalEP, null, null)

        val FinalEP(e, p) = finalEP
        val ownTId = currentTId()
        val targetTId = getResponsibleTId(e)

        if (targetTId != ownTId) {
            submitTask(new DhtSetTask(e, finalEP, p.id), targetTId, ownTId)
            return ;
        }

        val oldPO = ps(ownTId)(p.id).put(e, epkState)

        if (oldPO.isDefined) {
            val oldP = oldPO.get

            if (debug) {
                if (oldP.eOptP.isFinal) {
                    throw new IllegalStateException(s"$e already had the property $oldP")
                } else {
                    oldP.eOptP.checkIsValidPropertiesUpdate(finalEP, Nil)
                }
            }

            val dependees = oldP.dependees
            if (dependees ne null) {
                val ds = dependers(ownTId)
                dependees.foreach { dependee ⇒
                    val theDependers = ds.get(dependee.toEPK)
                    if (theDependers.isDefined) theDependers.get -= finalEP.toEPK
                }
            }

            if (oldP.eOptP.isEPK)
                triggerComputations(e, p.id, ownTId)
        } else {
            triggerComputations(e, p.id, ownTId)
        }

        notifyDependers(finalEP, ownTId, unnotifiedPKs)
    }

    private[this] def handleInterimResult(
        interimEP: InterimEP[Entity, _ >: Null <: Property],
        c:         ProperOnUpdateContinuation,
        dependees: Traversable[SomeEOptionP]
    ): Unit = {
        val epkState = DhtEpkState(interimEP, c, dependees)

        val SomeEPS(e, p) = interimEP
        val ownTId = currentTId()
        val targetTId = getResponsibleTId(e)

        if (targetTId != ownTId) {
            submitTask(
                new DhtPropertyComputationTask(
                    e,
                    { _: Entity ⇒ InterimResult(interimEP, dependees, c) }
                ),
                targetTId,
                ownTId
            )
            return ;
        }

        val oldPO = ps(ownTId)(p.id).put(e, epkState)

        if (oldPO.isDefined) {
            val oldP = oldPO.get

            if (debug) {
                oldPO.get.eOptP.checkIsValidPropertiesUpdate(interimEP, dependees)
            }

            if (oldP.eOptP.isEPK)
                triggerComputations(e, p.id, ownTId)
        } else {
            triggerComputations(e, p.id, ownTId)
        }

        updateDependees(interimEP.toEPK, dependees, oldPO, ownTId)

        if (oldPO.isEmpty || interimEP.isUpdatedComparedTo(oldPO.get.eOptP))
            notifyDependers(interimEP, ownTId)
    }

    private[this] def updateDependees(depender: SomeEPK, dependees: Traversable[SomeEOptionP], oldPO: Option[DhtEpkState], ownTId: Int): Unit = {
        val oldDependees: Set[SomeEOptionP] = if (oldPO.isDefined) oldPO.get.dependees.toSet else Set.empty
        var newDependees: Set[SomeEOptionP] = Set.empty

        val ds = dependers(ownTId)
        dependees.foreach { dependee ⇒
            if (oldDependees.contains(dependee)) {
                //oldDependees -= dependee
            } else {
                ds.getOrElseUpdate(dependee.toEPK, MutableSet.empty) += depender
                newDependees += dependee
            }
        }

        //println(s"${dependees.size} vs ${newDependees.size} vs ${oldDependees.size}")

        //TODO Drop old status as depender for removed dependees?

        val suppressedPKs = suppressInterimUpdates(depender.pk.id)
        var updateTasks: List[DhtTask] = List.empty
        val hasKnownUpdate = newDependees.exists { dependee ⇒
            val dependeeState = ps(ownTId)(dependee.pk.id).get(dependee.e)
            dependeeState match {
                case Some(DhtEpkState(eOptP, _, _)) if (eOptP ne dependee) && eOptP.isEPS && (eOptP.isFinal || !suppressedPKs(eOptP.pk.id)) ⇒
                    updateTasks = List(new DhtContinuationTask(depender.e, depender.pk.id, dependees.size, eOptP.asEPS))
                    true
                case _ ⇒
                    updateTasks ::= new DhtNotifyIfUpdatedTask(depender, dependees.size, dependee)
                    false
            }
        }

        if (hasKnownUpdate) {
            submitTask(updateTasks.head, ownTId, ownTId)
        } else {
            updateTasks.foreach { task ⇒
                val targetTId = getResponsibleTId(task.asInstanceOf[DhtNotifyIfUpdatedTask].dependee.e)
                if (targetTId != ownTId)
                    submitTask(task, targetTId, ownTId)
            }
        }
    }

    private[this] def handlePartialResult(
        pr:        PropertyComputationResult,
        getResult: (SomeEOptionP ⇒ Option[SomeInterimEP]),
        e:         Entity,
        pk:        SomePropertyKey
    ): Unit = {
        val ownTId = currentTId()
        val targetTId = getResponsibleTId(e)

        if (targetTId != ownTId) {
            submitTask(
                new DhtPropertyComputationTask(
                    e,
                    { _: Entity ⇒ pr }
                ),
                targetTId,
                ownTId
            )
            return ;
        }

        val oldPO = ps(ownTId)(pk.id).get(e)

        val oldEOptP = if (oldPO.isDefined) {
            oldPO.get.eOptP
        } else {
            EPK(e, pk)
        }

        if (oldEOptP.isFinal) {
            throw new IllegalStateException(s"$e already had the property ${oldEOptP.asFinal.p}")
        }

        val result = getResult(oldEOptP)

        if (result.isDefined) {
            ps(ownTId)(pk.id).put(e, DhtEpkState(result.get, null, null))
            if (oldEOptP.isEPK)
                triggerComputations(e, pk.id, ownTId)

            notifyDependers(result.get, ownTId)
        }
    }

    override protected[this] def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        //TODO always return current state?
        val targetTId = getResponsibleTId(e)
        val ownTId = currentTId()
        if (targetTId == ownTId || isIdle) {
            val ePKState = ps(targetTId)(pkId).get(e)
            if (ePKState.isDefined)
                ePKState.get.eOptP.asInstanceOf[EOptionP[E, P]]
            else
                handleUnknownEPK(epk, e, pkId, targetTId, ownTId)
        } else {
            handleUnknownEPK(epk, e, pkId, targetTId, ownTId)
        }
    }

    private[this] def handleUnknownEPK[E <: Entity, P <: Property](
        epk:       EPK[E, P],
        e:         E,
        pkId:      Int,
        targetTId: Int,
        sourceTId: Int
    ): EOptionP[E, P] = {
        val lazyComputation = lazyComputations(pkId)
        if (lazyComputation ne null) {
            submitTask(
                new DhtLazyComputationTask(
                    e,
                    pkId,
                    lazyComputation.asInstanceOf[E ⇒ PropertyComputationResult]
                ),
                targetTId,
                sourceTId
            )
            epk
        } else if (propertyKindsComputedInThisPhase(pkId)) {
            val transformer = transformersByTargetPK(pkId)
            if (transformer ne null) {
                val dependee = this(e, transformer._1)
                if (dependee.isFinal) {
                    val result = transformer._2(e, dependee.asFinal.p)
                    ps(targetTId)(pkId).put(e, DhtEpkState(result, null, null))
                    triggerComputations(e, pkId, sourceTId)
                    result.asInstanceOf[FinalEP[E, P]]
                } else {
                    ps(targetTId)(pkId).put(e, DhtEpkState(epk, d ⇒ new Result(transformer._2(e, d.asFinal.p)), Some(dependee)))
                    updateDependees(epk, Some(dependee), None, targetTId)
                    epk
                }
            } else {
                submitTask(new DhtSetInitialTask(e, epk, pkId), targetTId, sourceTId)
                epk
            }
        } else {
            val finalEP = computeFallback[E, P](e, pkId)
            submitTask(new DhtSetInitialTask(e, finalEP, pkId), targetTId, sourceTId)
            finalEP
        }
    }

    private[this] def startThreads(thread: Int ⇒ DhtThread): Unit = {
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

    private[this] def hasActiveThreads: Boolean = {
        var tId = 0
        while (tId < THREAD_COUNT) {
            if (isActive(tId).get()) {
                return true;
            }
            tId += 1
        }
        false
    }

    override def waitOnPhaseCompletion(): Unit = {
        idle = false

        // If some values were explicitly set, we have to trigger corresponding triggered
        // computations.
        setAndPreinitializedValues.foreach { epk ⇒ triggerComputations(epk.e, epk.pk.id, 0) }
        setAndPreinitializedValues = List.empty

        while (subPhaseId < subPhaseFinalizationOrder.length) {
            var continueCycles = false
            do {
                var continueFallbacks = false
                do {
                    activeThreads.set(THREAD_COUNT)
                    startThreads(new DhtWorkerThread(_))

                    quiescenceCounter += 1

                    startThreads(new DhtFallbackThread(_))

                    continueFallbacks = hasActiveThreads
                } while (continueFallbacks)

                startThreads(new DhtCycleResolutionThread(_))

                resolveCycles()

                continueCycles = hasActiveThreads
            } while (continueCycles)

            startThreads(new DhtPartialPropertiesFinalizerThread(_))

            subPhaseId += 1
        }

        idle = true
    }

    private[this] def resolveCycles(): Unit = {
        val theInterimStates = new ArrayBuffer[DhtEpkState](interimStates.iterator.map(_.size).sum)
        var tId = 0
        while (tId < THREAD_COUNT) {
            theInterimStates ++= interimStates(tId)
            tId += 1
        }

        val theSuccessors = (interimEPKState: DhtEpkState) ⇒ {
            successors(getResponsibleTId(interimEPKState.eOptP.e))(interimEPKState)
        }

        val cSCCs = graphs.closedSCCs(theInterimStates, theSuccessors)

        for (cSCC ← cSCCs) {
            for (interimEPKState ← cSCC) {
                val dependees = interimEPKState.dependees
                val epk = interimEPKState.eOptP.toEPK
                val e = epk.e
                val targetTId = getResponsibleTId(e)
                dependees.foreach { dependee ⇒
                    dependers(targetTId)(dependee.toEPK) -= epk
                }
                submitTask(
                    new DhtSetTask(
                        e,
                        interimEPKState.eOptP.toFinalEP,
                        epk.pk.id
                    ),
                    targetTId,
                    0
                )
            }
        }
    }

    private[this] val isActive: Array[AtomicBoolean] = Array.fill(THREAD_COUNT)(new AtomicBoolean(true))

    private[this] def getResponsibleTId(e: Entity): Int = {
        Math.abs(e.hashCode() >> 5) % THREAD_COUNT
    }

    private[this] val interimStates: Array[ArrayBuffer[DhtEpkState]] =
        Array.fill(THREAD_COUNT)(null)
    private[this] val successors: Array[DhtEpkState ⇒ Traversable[DhtEpkState]] =
        Array.fill(THREAD_COUNT)(null)

    private[this] def submitTask(task: DhtTask, targetTId: Int, sourceTId: Int): Unit = {
        if (targetTId == sourceTId) {
            if (task.isInstanceOf[DhtContinuationTask]) {
                val ct = task.asInstanceOf[DhtContinuationTask]
                localTasks(sourceTId).enqueue(ct)
                val lastUpdate = nextUpdate(sourceTId).put((ct.dependee.toEPK, ct.dependerE, ct.dependerPKId), ct)
                lastUpdate.foreach(_.perform = false)
            } else {
                fastQueues(sourceTId).enqueue(task)
            }
            isActive(targetTId).set(true)
        } else {
            tasks(targetTId)(sourceTId).offer(task)
            val targetThread = threads(targetTId)
            val wasActive = isActive(targetTId).getAndSet(true)
            if (!wasActive) {
                targetThread synchronized {
                    targetThread.notifyAll()
                }
            }
        }
    }

    //TODO this is a slow hack to prevent reordered continuation task messing up the state
    private[this] val nextUpdate: Array[AnyRefMap[(SomeEPK, Entity, Int), DhtContinuationTask]] = Array.fill(THREAD_COUNT)(AnyRefMap.empty)

    class DhtThread(val ownTId: Int, name: String) extends Thread(name)

    object DhtThread {
        def unapply(t: DhtThread): Some[Int] = {
            Some(t.ownTId)
        }
    }

    class DhtWorkerThread(ownTId: Int) extends DhtThread(ownTId, s"PropertyStoreThread-#$ownTId") {

        private[this] val localQueue: PriorityQueue[DhtContinuationTask] = localTasks(ownTId)
        private[this] val localQueues: Array[ConcurrentLinkedQueue[DhtTask]] = tasks(ownTId)
        private[this] val fastQueue = fastQueues(ownTId)

        override def run(): Unit = {
            try {
                while (!doTerminate) {
                    consolidateQueues()
                    if (localQueue.isEmpty && fastQueue.isEmpty) {
                        val active = activeThreads.decrementAndGet()
                        if (active == 0) {
                            var continue = false
                            var tId = 0
                            while (tId < THREAD_COUNT) {
                                if (tId != ownTId && isActive(tId).get()) {
                                    continue = true
                                }
                                tId += 1
                            }
                            consolidateQueues()
                            if (!continue && localQueue.isEmpty && fastQueue.isEmpty) {
                                threads.foreach { t ⇒
                                    if (t ne this)
                                        t.interrupt()
                                }
                                isActive(ownTId).set(false)
                                return ;
                            }
                            activeThreads.incrementAndGet()
                        } else {
                            val t = Thread.currentThread()
                            t synchronized {
                                isActive(ownTId).set(false) //TODO does this have to be later?
                                consolidateQueues()
                                if (localQueue.isEmpty && fastQueue.isEmpty) {
                                    try {
                                        t.wait()
                                    } catch {
                                        case _: InterruptedException ⇒ return ;
                                    }
                                } else {
                                    isActive(ownTId).set(true)
                                }
                            }
                            activeThreads.incrementAndGet()
                        }
                    } else {
                        while (fastQueue.nonEmpty && !doTerminate)
                            fastQueue.dequeue().apply()
                        if (localQueue.nonEmpty)
                            localQueue.dequeue().apply()
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

        private[this] def consolidateQueues(): Unit = {
            var tId = 0
            val ownNextUpdates = nextUpdate(ownTId)
            while (tId < THREAD_COUNT) {
                val queue = localQueues(tId)
                var t: DhtTask = null
                while ({ t = queue.poll(); t ne null }) {
                    if (t.isInstanceOf[DhtContinuationTask]) {
                        val ct = t.asInstanceOf[DhtContinuationTask]
                        localQueue += ct
                        val lastUpdate = ownNextUpdates.put((ct.dependee.toEPK, ct.dependerE, ct.dependerPKId), ct)
                        lastUpdate.foreach(_.perform = false)
                    } else {
                        fastQueue.enqueue(t)
                    }
                }

                tId += 1
            }
        }
    }

    class DhtFallbackThread(ownTId: Int) extends DhtThread(ownTId, s"PropertyStoreFallbackThread-#$ownTId") {

        override def run(): Unit = {
            val localStore = ps(ownTId)

            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId)) {
                    localStore(pkId).valuesIterator.foreach { oldState ⇒
                        if (oldState.eOptP.isEPK && ((oldState.dependees eq null) || oldState.dependees.isEmpty)) {
                            val e = oldState.eOptP.e
                            val reason = PropertyIsNotDerivedByPreviouslyExecutedAnalysis
                            val p = fallbackPropertyBasedOnPKId(propertyStore, reason, e, pkId)
                            val finalEP = FinalEP(e, p)
                            incrementFallbacksUsedForComputedPropertiesCounter()
                            handleFinalResult(finalEP)
                        }
                    }
                }
                pkId += 1
            }
        }
    }

    class DhtCycleResolutionThread(ownTId: Int) extends DhtThread(ownTId, s"PropertyStoreCycleResolutionThread-#$ownTId") {

        override def run(): Unit = {
            val localInterimStates = ArrayBuffer.empty[DhtEpkState]
            var pkId = 0
            while (pkId <= PropertyKey.maxId) {
                if (propertyKindsComputedInThisPhase(pkId)) {
                    ps(ownTId)(pkId).valuesIterator.foreach { epkState ⇒
                        if (epkState.eOptP.isRefinable) localInterimStates += epkState
                    }
                }
                pkId += 1
            }
            interimStates(ownTId) = localInterimStates

            successors(ownTId) = (interimEPKState: DhtEpkState) ⇒ {
                val dependees = interimEPKState.dependees
                if (dependees != null) {
                    interimEPKState.dependees.map { eOptionP ⇒
                        val tId = getResponsibleTId(eOptionP.e)
                        ps(tId)(eOptionP.pk.id)(eOptionP.e)
                    }
                } else {
                    Traversable.empty
                }
            }
        }
    }

    class DhtPartialPropertiesFinalizerThread(ownTId: Int) extends DhtThread(ownTId, s"PropertyStorePartialPropertiesFinalizerThread-#$ownTId") {

        override def run(): Unit = {
            val pksToFinalize = subPhaseFinalizationOrder(subPhaseId).toSet

            pksToFinalize foreach { pk ⇒
                val pkId = pk.id
                ps(ownTId)(pkId).valuesIterator.foreach { epkState ⇒
                    val eOptP = epkState.eOptP
                    if (eOptP.isRefinable && !eOptP.isEPK) //TODO Won't be required once subPhaseFinalizationOrder is reliably only the partial properties
                        handleFinalResult(eOptP.toFinalEP, pksToFinalize)
                }
            }
        }
    }

    trait DhtTask extends (() ⇒ Unit) {
        val priority = 1000000
    }

    class DhtExecuteTask(f: ⇒ Unit) extends DhtTask {
        override def apply(): Unit = {
            f
        }
    }

    class DhtSetTask[E <: Entity, P <: Property](e: E, finalEP: FinalEP[E, P], pkId: Int) extends DhtTask {
        override def apply(): Unit = {
            val ownTId = currentTId()
            val oldStateO = ps(ownTId)(pkId).put(e, DhtEpkState(finalEP, null, null))
            notifyDependers(finalEP, ownTId)
            if (oldStateO.isEmpty) triggerComputations(e, pkId, ownTId)
        }
    }

    class DhtSetInitialTask[E <: Entity, P <: Property](e: E, eOptP: EOptionP[E, P], pkId: Int) extends DhtTask {
        override def apply(): Unit = {
            val ownTId = currentTId()
            val newState = DhtEpkState(eOptP, null, null)
            val epkState = ps(ownTId)(pkId).getOrElseUpdate(e, newState)
            if (!eOptP.isEPK && (epkState eq newState)) {
                notifyDependers(eOptP.asEPS, ownTId)
                triggerComputations(e, pkId, ownTId)
            }
        }
    }

    class DhtPropertyComputationTask[E <: Entity](e: E, pc: PropertyComputation[E]) extends DhtTask {
        scheduledTasks(currentTId()) += 1

        override def apply(): Unit = {
            handleResult(pc(e))
        }
    }

    class DhtLazyComputationTask[E <: Entity](e: E, pkId: Int, pc: PropertyComputation[E]) extends DhtTask {
        scheduledTasks(currentTId()) += 1

        override def apply(): Unit = {
            val tId = currentTId()
            val epkState = ps(tId)(pkId).get(e)
            if (epkState.isEmpty)
                handleResult(pc(e))
        }
    }

    class DhtContinuationTask(val dependerE: Entity, val dependerPKId: Int, val dependeeCount: Int, val dependee: SomeEPS) extends DhtTask {
        override val priority = dependeeCount

        var perform = true

        scheduledOnUpdateComputations(currentTId()) += 1

        override def apply(): Unit = {
            if (perform) {
                val ownTId = currentTId()
                /*if (ownTId != getResponsibleTId(dependee.e))
                ps(ownTId)(dependee.pk.id)(dependee.e) = DhtEpkState(dependee, null, null)*/
                val eSO = ps(ownTId)(dependerPKId).get(dependerE)
                if (eSO.isDefined) {
                    val epkState = eSO.get
                    val dependeeEPK = dependee.toEPK
                    if (epkState.dependees != null && epkState.dependees.exists(_.toEPK == dependeeEPK)) {
                        //val ds = dependers(ownTId)
                        //ds(dependeeEPK) -= epkState.eOptP.toEPK
                        // TODO maybe get current state of dependee here?
                        doHandleResult(epkState.c(dependee), epkState)
                    }
                }
            }
        }
    }

    class DhtNotifyIfUpdatedTask(depender: SomeEPK, val dependeeCount: Int, val dependee: SomeEOptionP) extends DhtTask {
        override def apply(): Unit = {
            val dependeeTId = currentTId()
            val dependerTId = getResponsibleTId(depender.e)
            val e = dependee.e
            val pk = dependee.pk
            val pkId = pk.id
            val ePKState = ps(dependeeTId)(pkId).get(e)
            if (ePKState.isEmpty) {
                handleUnknownEPK(EPK(e, pk), e, pkId, dependeeTId, dependerTId)
            } else {
                val eOptP = ePKState.get.eOptP
                if ((eOptP ne dependee) && eOptP.isEPS) {
                    val continuationTask = new DhtContinuationTask(depender.e, depender.pk.id, dependeeCount, eOptP.asEPS)
                    assert(dependeeTId != dependerTId)
                    submitTask(
                        continuationTask,
                        dependerTId,
                        dependeeTId
                    )
                }
            }
        }
    }

    class DhtNotifyDependersTask(val dependee: SomeEPS, val unnotifiedPKs: Set[PropertyKind]) extends DhtTask {
        override def apply(): Unit = {
            val ownTId = currentTId()
            val ds = dependers(ownTId)
            val theDependers = ds.get(dependee.toEPK)
            if (theDependers.isDefined) {
                theDependers.get.foreach { depender ⇒
                    val EOptionP(e, pk) = depender
                    if (!unnotifiedPKs.contains(pk) && (dependee.isFinal || !suppressInterimUpdates(depender.pk.id)(dependee.pk.id))) {
                        submitTask(new DhtContinuationTask(e, pk.id, 0, dependee), ownTId, ownTId)
                    }
                }
            }
        }
    }
}

object DHTPropertyStore extends PropertyStoreFactory[DHTPropertyStore] {

    final val MaxEvaluationDepthKey = "org.opalj.fpcf.par.PKECPropertyStore.MaxEvaluationDepth"

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): DHTPropertyStore = {
        val contextMap: Map[Class[_], AnyRef] = context.map(_.asTuple).toMap

        val ps = new DHTPropertyStore(contextMap)
        ps
    }
}

case class DhtEpkState(eOptP: SomeEOptionP, c: OnUpdateContinuation, dependees: Traversable[SomeEOptionP])

class DhtFakeEntity(override val hashCode: Int)