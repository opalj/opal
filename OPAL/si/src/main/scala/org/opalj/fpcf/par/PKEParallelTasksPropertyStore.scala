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
package par

import java.lang.System.identityHashCode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicReferenceArray

import scala.reflect.runtime.universe.Type
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable

import org.opalj.graphs
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.log.OPALLogger.{debug ⇒ trace}
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyKey.fallbackPropertyBasedOnPkId
import org.opalj.fpcf.PropertyKind.SupportedPropertyKinds
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

/**
 * A concurrent implementation of the property store which parallels the execution of the scheduled
 * computations.
 *
 * Entities are stored after computation.
 *
 * ==Implementation==
 * The idea is to use one specific thread (the `store updates thread) for processing updates
 * to the store. This enables us to avoid any synchronization w.r.t. updating the
 * depender/dependee relations.
 *
 * For processing the scheduled computations we use
 * `NumberOfThreadsForProcessingPropertyComputations` threads.
 *
 * @author Michael Eichberg
 */
final class PKEParallelTasksPropertyStore private (
        val ctx:                                              Map[Type, AnyRef],
        val NumberOfThreadsForProcessingPropertyComputations: Int,
        val tracer:                                           Option[PropertyStoreTracer]
)(
        implicit
        val logContext: LogContext
) extends PropertyStore {
    store ⇒

    // Tasks are allowed to schedule further tasks... therefore, the scheduled tasks counter
    // has to be thread-safe.
    private[this] val scheduledTasksCounter: AtomicInteger = new AtomicInteger(0)
    def scheduledTasksCount: Int = scheduledTasksCounter.get

    @volatile private[this] var fallbacksUsedCounter: AtomicInteger = new AtomicInteger(0)
    def fallbacksUsedCount: Int = fallbacksUsedCounter.get

    @volatile private[this] var scheduledOnUpdateComputationsCounter = 0
    def scheduledOnUpdateComputationsCount: Int = scheduledOnUpdateComputationsCounter

    @volatile private[this] var immediateOnUpdateComputationsCounter = 0
    def immediateOnUpdateComputationsCount: Int = immediateOnUpdateComputationsCounter

    @volatile private[this] var resolvedCSCCsCounter = 0
    def resolvedCSCCsCount: Int = resolvedCSCCsCounter

    @volatile private[this] var quiescenceCounter = 0
    def quiescenceCount: Int = quiescenceCounter

    def fastTrackPropertiesCount: Int = 0 // TODO Not yet supported

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

    // Those computations that will only be scheduled if the property is required.
    private[this] var lazyComputations: AtomicReferenceArray[SomePropertyComputation] = {
        new AtomicReferenceArray(SupportedPropertyKinds)
    }

    // Read by many threads, updated only by the store updates thread.
    // ONLY contains `true` intermediate and final properties; i.e., the value is never null.
    private[this] val properties: Array[ConcurrentHashMap[Entity, SomeEPS]] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }
    // Contains all those EPKs that should be computed until we have a final result.
    // When we have computed a final result, the epk will be deleted.
    private[this] val forcedComputations: Array[ConcurrentHashMap[Entity, Entity] /*...a set*/ ] = {
        Array.fill(SupportedPropertyKinds) { new ConcurrentHashMap() }
    }

    @volatile private[this] var computedPropertyKinds: Array[Boolean] = _ /*null*/

    @volatile private[this] var delayedPropertyKinds: Array[Boolean] = _ /*null*/

    // ---------------------------------------------------------------------------------------------
    // The following three data-structures are never read or updated concurrently;
    // they are only read/updated by the store updates thread.
    //
    private[this] val dependers: Array[AnyRefMap[Entity, AnyRefMap[SomeEPK, OnUpdateContinuation]]] = {
        Array.fill(SupportedPropertyKinds) { AnyRefMap.empty }
    }
    private[this] val dependees: Array[AnyRefMap[Entity, Traversable[SomeEOptionP]]] = {
        Array.fill(SupportedPropertyKinds) { AnyRefMap.empty }
    }
    private[this] val triggeredLazyComputations: Array[mutable.Set[Entity]] = {
        Array.fill(SupportedPropertyKinds) { mutable.HashSet.empty }
    }

    // ---------------------------------------------------------------------------------------------
    // Helper data-structure to control the overall progress.
    //

    // The latch is initialized whenever the number of jobs goes from "0" to "1".
    // Note that the setup thread will always be responsible to -- at least -- schedule an initial
    // task/force an initial evaluation.
    @volatile private[this] var latch: CountDownLatch = _

    /**
     * The number of property computations and results which have not been completely processed.
     */
    private[this] val openJobs = new AtomicInteger(0)
    /**
     * MUST BE CALLED AFTER the respective task was processed.
     */
    private[this] def decOpenJobs(): Unit = {
        val v = openJobs.decrementAndGet()
        if (v == 0) { latch.countDown() }
    }
    /**
     * MUST BE CALLED BEFORE the job is actually processed.
     */
    private[this] def incOpenJobs(): Unit = {
        val v = openJobs.getAndIncrement()
        if (v == 0) { latch = new CountDownLatch(0) }
    }

    // --------------------------------------------------------------------------------------------
    //
    // Handling property computations.
    //
    // --------------------------------------------------------------------------------------------

    private[this] val propertyStoreThreads: ThreadGroup = {
        new ThreadGroup(s"OPAL - Property Store ${store.hashCode().toHexString} Threads")
    }

    /**
     * The list of scheduled (on update) property computations -
     * they will be processed in parallel.
     */
    private[this] val tasks = new LinkedBlockingDeque[QualifiedTask[_ <: Entity]]()

    private[this] def prependTask(task: QualifiedTask[_ <: Entity]): Unit = {
        incOpenJobs()
        if (!tasks.offerFirst(task)) {
            decOpenJobs()
            throw new UnknownError(s"prepending task $task to unbounded queue failed")
        }
    }

    private[this] def appendTask(task: QualifiedTask[_ <: Entity]): Unit = {
        incOpenJobs()
        if (!tasks.offerLast(task)) {
            decOpenJobs()
            throw new UnknownError(s"appending task $task to unbounded queue failed")
        }
    }

    private[this] var tasksProcessors: ThreadGroup = {

        @inline def processTask(): Unit = {
            try {
                handleExceptions {
                    val t = tasks.poll(60, TimeUnit.SECONDS)
                    if (t != null) {
                        // As a sideeffect of processing a task, we may have (implicit) calls
                        // to schedule and also implicit handleResult calls; both will
                        // increase openJobs.
                        if (t.isInitialTask) {
                            t.apply()
                        } else {
                            // TODO check if required; i.e., if we are forced or have dependees.
                            t.apply()
                        }
                        decOpenJobs()
                    }
                }
            } catch {
                case ie: InterruptedException ⇒
                    // No element was taken from the queue; hence, we must not decrement open jobs.
                    throw ie;
                case t: Throwable ⇒
                    decOpenJobs()
                    throw t;
            }
        }

        val tg = new ThreadGroup(propertyStoreThreads, "OPAL - Property Computations Processors")
        for { i ← 1 to NumberOfThreadsForProcessingPropertyComputations } {
            val t = new Thread(tg, s"OPAL - Property Computations Processor $i") {
                thread ⇒
                override def run(): Unit = {
                    do {
                        while (!store.isSuspended()) { processTask() }
                        if (store.isSuspended()) Thread.sleep(1000)
                    } while (!thread.isInterrupted())
                }
            }
            t.setDaemon(true)
            t.setUncaughtExceptionHandler(store.handleUncaughtException)
            t.start()
        }
        tg
    }

    // --------------------------------------------------------------------------------------------
    //
    // Handling PropertyStore updates.
    //
    // --------------------------------------------------------------------------------------------

    private[this] sealed trait StoreUpdate

    private[this] case class PropertyUpdate(
            pcr:                       PropertyComputationResult,
            forceEvaluation:           Boolean,
            forceDependerNotification: Boolean
    ) extends StoreUpdate

    private[this] case class TriggeredLazyComputation[E <: Entity](
            e:    E,
            pkId: Int,
            pc:   PropertyComputation[E]
    ) extends StoreUpdate

    /**
     * The jobs which update the store.
     */
    private[this] val storeUpdates = new LinkedBlockingDeque[StoreUpdate]()

    private[this] def prependStoreUpdate(update: StoreUpdate): Unit = {
        incOpenJobs()
        if (!storeUpdates.offerFirst(update)) {
            decOpenJobs()
            throw new UnknownError(s"prepending store update $update to unbounded queue failed")
        }
    }

    private[this] def appendStoreUpdate(update: StoreUpdate): Unit = {
        incOpenJobs()
        if (!storeUpdates.offerLast(update)) {
            decOpenJobs()
            throw new UnknownError(s"appending store update $update to unbounded queue failed")
        }
    }

    private[this] var storeUpdatesProcessor = {

        @inline def processUpdate(): Unit = {
            try {
                handleExceptions {
                    storeUpdates.poll(60, TimeUnit.SECONDS) match {

                        case null ⇒ // nothing to do at the moment...

                        case PropertyUpdate(r, forceEvaluation, forceDependerNotification) ⇒
                            doHandleResult(r, forceEvaluation, forceDependerNotification)
                            decOpenJobs()

                        case TriggeredLazyComputation(e, pkId, lc) ⇒
                            // Recall, that -- once we have a final result -- all meta data
                            // is deleted; in particular information about triggeredLazyComputations.
                            if (properties(pkId).get(e) == null &&
                                triggeredLazyComputations(pkId).add(e)) {
                                if (tracer.isDefined)
                                    tracer.get.schedulingLazyComputation(e, pkId)

                                scheduledTasksCounter.incrementAndGet()
                                appendTask(new PropertyComputationTask[Entity](store, e, pkId, lc))
                            }
                            decOpenJobs()
                    }
                }
            } catch {
                case ie: InterruptedException ⇒
                    // No element was taken from the queue; hence, we must not decrement open jobs.
                    throw ie;
                case t: Throwable ⇒
                    decOpenJobs()
                    throw t;
            }
        }

        val t = new Thread(propertyStoreThreads, "OPAL - Property Updates Processor") { thread ⇒
            override def run(): Unit = {
                do {
                    while (!store.isSuspended()) { processUpdate() }
                    if (store.isSuspended()) Thread.sleep(1000)
                } while (!thread.isInterrupted())
            }

        }
        t.setDaemon(true)
        t.setPriority(8)
        t.setUncaughtExceptionHandler(store.handleUncaughtException)
        t.start()
        t
    }

    // --------------------------------------------------------------------------------------------
    //
    // Failure handling
    //
    // --------------------------------------------------------------------------------------------

    private[this] def shutdownPropertyStore(): Unit = {
        isSuspended = () ⇒ true
        // We use the "Thread"s' interrupt method to finally abort the threads...
        if (storeUpdatesProcessor ne null) storeUpdatesProcessor.interrupt()
        storeUpdatesProcessor = null
        if (tasksProcessors ne null) tasksProcessors.interrupt()
        tasksProcessors = null

        if (latch != null) latch.countDown()
    }

    override def finalize(): Unit = {
        // DEPRECATED: super.finalize()
        shutdownPropertyStore()
    }

    private[this] def handleUncaughtException(t: Thread, e: Throwable): Unit = {
        e match {
            case underlyingException: AbortedDueToException ⇒ /*ignore*/
            case t: Throwable                               ⇒ collectException(e)
        }
        shutdownPropertyStore()
    }

    protected[this] override def onFirstException(t: Throwable): Unit = {
        if (tracer.isDefined) tracer.get.firstException(t)
        super.onFirstException(t)
        shutdownPropertyStore()
    }

    // --------------------------------------------------------------------------------------------
    //
    // Core functionality
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

    override def registerLazyPropertyComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {
        if (debug && openJobs.get() > 0) {
            throw new IllegalStateException(
                "lazy computations can only be registered while no analysis are scheduled"
            )
        }
        lazyComputations.set(pk.id, pc.asInstanceOf[SomePropertyComputation])
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
     * Returns all entities which have the given property bounds based on an "==" (equals)
     * comparison.
     */
    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = {
        entities((otherEPS: SomeEPS) ⇒ lb == otherEPS.lb && ub == otherEPS.ub)
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
        scheduledTasksCounter.incrementAndGet()
        appendTask(new InitialPropertyComputationTask[E](this, e, pc, forceEvaluation))
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
    private[this] def scheduleLazyComputationForEntity[E <: Entity](
        e:    E,
        pkId: Int,
        pc:   PropertyComputation[E]
    ): Unit = {
        appendStoreUpdate(TriggeredLazyComputation(e, pkId, pc))
    }

    // Thread Safe!
    override def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = {
        val pkId = pk.id
        properties(pkId).get(e) match {
            case null ⇒
                // the entity is unknown ...
                lazyComputations.get(pkId) match {
                    case null ⇒
                        if (debug && computedPropertyKinds == null) {
                            /*&& delayedPropertyKinds ne null (not necessary)*/
                            throw new IllegalStateException("setup phase was not called")
                        }
                        if (!computedPropertyKinds(pkId) && !delayedPropertyKinds(pkId)) {
                            // We schedule the computation of the fallback to avoid that the
                            // fallback is computed multiple times (which – in some cases –
                            // is not always just a bottom value!)
                            scheduleLazyComputationForEntity(
                                e, pkId,
                                (_: E) ⇒ {
                                    fallbacksUsedCounter.incrementAndGet()
                                    Result(e, PropertyKey.fallbackProperty(store, e, pk))
                                }
                            ) // <= handles redundant starts
                        }

                    case lc: PropertyComputation[E] @unchecked ⇒
                        scheduleLazyComputationForEntity(e, pkId, lc) // <= handles redundant starts
                }
                EPK(e, pk)

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
            if (tracer.isDefined) tracer.get.force(e, pkId)

            val lc = lazyComputations.get(pkId)
            if (lc != null && properties(pkId).get(e) == null) {
                scheduleLazyComputationForEntity(e, pkId, lc) // <= handles redundant starts
            }
        }
    }

    /**
     * Removes the e/pk from `dependees` and also removes it from the dependers of the
     * e/pk's dependees.
     */
    private[this] def clearDependees(epk: SomeEPK): Unit = {
        assert(
            Thread.currentThread() == storeUpdatesProcessor,
            "only to be called by the store updates processing thread"
        )

        val pkId = epk.pk.id
        val dependeesOfEntity = this.dependees(pkId)
        for {
            oldDependees ← dependeesOfEntity.remove(epk.e) // <= the old ones
            EOptionP(oldDependeeE, oldDependeePK) ← oldDependees
            dependersOfOldDependee ← dependers(oldDependeePK.id).get(oldDependeeE)
        } {
            dependersOfOldDependee -= epk
        }
    }

    /**
     * Updates the entity and optionally notifies all dependers.
     */
    private[this] def updateAndNotify(
        e:  Entity,
        lb: Property, ub: Property,
        notifyDependersAboutNonFinalUpdates: Boolean = true,
        forceDependerNotification:           Boolean = false
    ): Boolean = {
        assert(
            Thread.currentThread() == storeUpdatesProcessor,
            "only to be called by the store updates processing thread"
        )
        if (debug) {
            if (e == null) {
                throw new IllegalArgumentException("the entity must not be null")
            }
            if (ub.key != lb.key) {
                throw new IllegalArgumentException(
                    s"property keys for lower $lb and upper $ub bound don't match"
                )
            }
            if (lb.isOrderedProperty) {
                val ubAsOP = ub.asOrderedProperty
                ubAsOP.checkIsEqualOrBetterThan(e, lb.asInstanceOf[ubAsOP.Self])
            }
        }

        val pk = ub.key
        val pkId = pk.id
        val newEPS = EPS[Entity, Property](e, lb, ub)
        val propertiesOfEntity = properties(pkId)

        // 1. update property
        val oldEPS = propertiesOfEntity.put(e, newEPS)
        if (tracer.isDefined) tracer.get.update(oldEPS, newEPS)

        // 2. check if update was ok
        if (debug && oldEPS != null) {
            // The entity is known and we have a property value for the respective
            // kind; i.e., we may have (old) dependees and/or also dependers.
            val oldLB = oldEPS.lb
            val oldUB = oldEPS.ub
            if (oldEPS.isFinal) {
                throw new IllegalStateException(
                    s"$e@${identityHashCode(e).toHexString}: already final: $oldEPS (given:lb=$lb,ub=$ub)"
                )
            }
            if (lb.isOrderedProperty) {
                try {
                    val lbAsOP = lb.asOrderedProperty
                    val oldLBWithUBType = oldLB.asInstanceOf[lbAsOP.Self]
                    lbAsOP.checkIsEqualOrBetterThan(e, oldLBWithUBType)
                    val pValueUBAsOP = oldUB.asOrderedProperty
                    val ubWithOldUBType = ub.asInstanceOf[pValueUBAsOP.Self]
                    pValueUBAsOP.checkIsEqualOrBetterThan(e, ubWithOldUBType)
                } catch {
                    case t: Throwable ⇒
                        throw new IllegalArgumentException(
                            s"$e: illegal update: (old)lb=$oldLB -> $lb; (old)ub=$oldUB -> $ub; "+
                                "; cause="+t.getMessage,
                            t
                        )
                }
            }
        }

        // 3. handle relevant updates
        val isFinal = newEPS.isFinal
        val relevantUpdate = newEPS != oldEPS
        if (isFinal ||
            forceDependerNotification ||
            (notifyDependersAboutNonFinalUpdates && relevantUpdate)) {
            // 3.1. notify dependers
            val oldDependersOption = this.dependers(pkId).remove(e)
            if (oldDependersOption.isDefined) {
                oldDependersOption.get foreach { oldDepender ⇒
                    val (oldDependerEPK, c) = oldDepender

                    if (tracer.isDefined) tracer.get.notification(newEPS, oldDependerEPK)

                    // Clear depender => dependee lists.
                    // Given that we will trigger the depender, we now have to remove the
                    // respective onUpdateContinuation from all dependees of the respective
                    // depender to avoid that the onUpdateContinuation is triggered multiple times!
                    clearDependees(oldDependerEPK)
                    scheduledOnUpdateComputationsCounter += 1
                    if (isFinal) {
                        prependTask(new OnFinalUpdateComputationTask(this, FinalEP(e, ub), c))
                    } else {
                        appendTask(new OnUpdateComputationTask(this, EPK(e, ub), c))
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
        relevantUpdate
    }

    // Thread safe!
    override def set(e: Entity, p: Property): Unit = handleExceptions {
        if (debug && lazyComputations.get(p.key.id) != null) {
            throw new IllegalStateException(
                s"$e: setting $p is not supported; lazy computation is registered"
            )
        }
        val r = ExternalResult(e, p)
        prependStoreUpdate(PropertyUpdate(r, /*doesn't matte:*/ false, /*doesn't matter:*/ false))
    }

    override def handleResult(
        r:               PropertyComputationResult,
        forceEvaluation: Boolean
    ): Unit = {
        appendStoreUpdate(PropertyUpdate(r, forceEvaluation, false))
    }

    // Thread safe!
    private[par] def handleResult(
        r:                         PropertyComputationResult,
        forceEvaluation:           Boolean,
        forceDependerNotification: Boolean
    ): Unit = {
        appendStoreUpdate(PropertyUpdate(r, forceEvaluation, forceDependerNotification))
    }

    private[this] def doHandleResult(
        r:                         PropertyComputationResult,
        forceEvaluation:           Boolean,
        forceDependerNotification: Boolean
    ): Unit = {
        assert(
            Thread.currentThread() == storeUpdatesProcessor,
            "only to be called by the store updates processing thread"
        )

        if (tracer.isDefined) tracer.get.handlingResult(r, forceEvaluation)

        r.id match {

            case NoResult.id ⇒ {
                // A computation reported no result; i.e., it is not possible to
                // compute a/some property/properties for a given entity.
            }

            //
            // Result containers
            //

            case Results.id ⇒
                val Results(results) = r
                results foreach { r ⇒ doHandleResult(r, forceEvaluation, false) }

            case IncrementalResult.id ⇒
                val IncrementalResult(ir, npcs /*: Traversable[(PropertyComputation[e],e)]*/ ) = r
                doHandleResult(ir, forceEvaluation, false)
                npcs foreach { npc ⇒
                    val (pc, e) = npc
                    scheduleComputationForEntity(e, pc, forceEvaluation)
                }

            //
            // Methods which actually store results...
            //

            case Result.id ⇒
                val Result(e, p) = r
                clearDependees(EPK(e, p.key))
                updateAndNotify(e, p, p)

            case MultiResult.id ⇒
                val MultiResult(results) = r
                results foreach { ep ⇒
                    clearDependees(ep.toEPK)
                    updateAndNotify(ep.e, ep.p, ep.p)
                }

            case PartialResult.id ⇒
                val PartialResult(e, pk, u) = r
                type E = e.type
                type P = Property
                val eOptionP = apply[E, P](e: E, pk: PropertyKey[P])
                val newEPSOption = u.asInstanceOf[EOptionP[E, P] ⇒ Option[EPS[E, P]]](eOptionP)
                newEPSOption foreach { newEPS ⇒
                    clearDependees(newEPS.toEPK)
                    updateAndNotify(newEPS.e, newEPS.lb, newEPS.ub)
                }

            case ExternalResult.id ⇒
                val ExternalResult(e, p) = r
                if (debug) {
                    val pkId = p.id
                    val oldP = properties(pkId).get(e)
                    if (oldP != null) {
                        throw new IllegalStateException(s"$e: already has a property $oldP")
                    }
                    if (dependees(pkId).contains(e)) {
                        throw new IllegalStateException(s"$e: is already computed/has dependees")
                    }
                }
                updateAndNotify(e, p, p)

            case CSCCsResult.id ⇒
                val CSCCsResult(cSCCs) = r
                for (cSCC ← cSCCs) {
                    if (traceCycleResolutions) {
                        val cSCCAsText =
                            if (cSCC.size > 10)
                                cSCC.take(10).mkString("", ",", "...")
                            else
                                cSCC.mkString(",")
                        info(
                            "analysis progress",
                            s"resolving cSCC(iteration:$quiescenceCounter): $cSCCAsText"
                        )
                    }
                    // 1. Kill for every member its dependee => depender relation to prevent inner
                    //    cSCC notifications (i.e.,  don't kill the dependers)
                    // 2. update all cycle members and inform the dependers (which, due to step 1,
                    //    do not contain members of the cSCC.)
                    // 3. clean-up all temporary information
                    cSCC.foreach(clearDependees)
                    for (epk ← cSCC) {
                        val e = epk.e
                        val pkId = epk.pk.id
                        val eps = properties(pkId).get(e)
                        val newP = PropertyKey.resolveCycle(this, eps)
                        updateAndNotify(e, newP, newP)
                    }
                    resolvedCSCCsCounter += 1
                }

            case IntermediateResult.id ⇒
                val IntermediateResult(e, lb, ub, seenDependees, c) = r
                val pk = ub.key
                val pkId = pk.id
                val epk = EPK(e, pk)

                if (forceEvaluation) forcedComputations(pkId).put(e, e)

                // Given that "on notification" dependees are eagerly killed, clearing
                // dependees is not necessary!
                if (debug && dependees(pkId).contains(e)) {
                    throw new IllegalStateException(
                        s"$e: ${properties(pkId).get(e)} has (unexpected) dependees: \n\t"+
                            s"${dependees(pkId).get(e).get.mkString(", ")}\n"+
                            "this happens, e.g., if computations are started eagerly while "+
                            "also a respective lazy property computation is scheduled; "+
                            "in this case use force instead!"
                    )
                }

                // 1. let's check if a seen dependee is already updated; if so, we directly
                //    schedule a task again to compute the property.
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
                        val relevantUpdate =
                            updateAndNotify(
                                e, lb, ub,
                                notifyDependersAboutNonFinalUpdates = false,
                                forceDependerNotification = false
                            )
                        if (tracer.isDefined)
                            tracer.get.immediateDependeeUpdate(e, lb, ub, seenDependee, currentDependee)
                        immediateOnUpdateComputationsCounter += 1
                        if (currentDependee.isFinal) {
                            prependTask(
                                ImmediateOnFinalUpdateComputationTask(
                                    store,
                                    currentDependee.asFinal,
                                    previousResult = r,
                                    forceDependerNotification || relevantUpdate,
                                    c
                                )
                            )
                        } else {
                            appendTask(
                                ImmediateOnUpdateComputationTask(
                                    store,
                                    currentDependee.toEPK,
                                    previousResult = r,
                                    forceDependerNotification || relevantUpdate,
                                    c
                                )
                            )
                        }

                        return ;
                    }
                }

                // When we reach this point, all potential dependee updates are taken into account;
                // otherwise we would have had an early return

                // 2.1.  Update the value (trigger dependers/clear old dependees).
                updateAndNotify(e, lb, ub, forceDependerNotification = forceDependerNotification)

                // 2.2.  The most current value of every dependee was taken into account
                //       register with new (!) dependees.
                this.dependees(pkId).put(e, seenDependees)
                val dependency = (epk, c)
                seenDependees foreach { dependee ⇒
                    val dependeeE = dependee.e
                    val dependeePKId = dependee.pk.id
                    dependers(dependeePKId).getOrElseUpdate(dependeeE, AnyRefMap.empty) +=
                        dependency
                }
        }
    }

    override def setupPhase(
        computedPropertyKinds: Set[PropertyKind],
        delayedPropertyKinds:  Set[PropertyKind]
    ): Unit = {
        if (debug && openJobs.get > 0) {
            throw new IllegalStateException(
                "setup phase can only be called as long as no tasks are scheduled"
            )
        }

        val newComputedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        computedPropertyKinds foreach { pk ⇒ newComputedPropertyKinds(pk.id) = true }
        this.computedPropertyKinds = newComputedPropertyKinds

        val newDelayedPropertyKinds = new Array[Boolean](PropertyKind.SupportedPropertyKinds)
        delayedPropertyKinds foreach { pk ⇒ newDelayedPropertyKinds(pk.id) = true }
        this.delayedPropertyKinds = newDelayedPropertyKinds
    }

    override def waitOnPhaseCompletion(): Unit = handleExceptions {
        var continueComputation: Boolean = false
        // We need a consistent interrupt state for fallback and cycle resolution:
        var isSuspended: Boolean = this.isSuspended()
        do {
            continueComputation = false

            while (!isSuspended && openJobs.get > 0) {
                latch.await()
                validateState()
                isSuspended = this.isSuspended()

            }

            assert(openJobs.get >= 0, s"unexpected number of openJobs: $openJobs")

            if (!isSuspended) {
                assert(openJobs.get == 0, s"unexpected number of open jobs: ${openJobs.get}")

                // We have reached quiescence. let's check if we have to
                // fill in fallbacks or if we have to resolve cyclic computations.
                quiescenceCounter += 1
                if (tracer.isDefined) tracer.get.reachedQuiescence()

                // 1. Let's search all EPKs for which we have no analyses scheduled in the
                //    future and use the fall back for them.
                //    (Recall that we return fallback properties eagerly if no analysis is
                //     scheduled or will be scheduled; but it is still possible that we will
                //     not have a property for a specific entity, if the underlying analysis
                //     doesn't compute one; in that case we need to put in fallback values.)
                val maxPKIndex = SupportedPropertyKinds // PropertyKey.maxId // properties.length
                var pkId = 0
                while (pkId < maxPKIndex) {
                    val dependersOfEntity = dependers(pkId)
                    val propertiesOfEntity = properties(pkId)
                    if (!delayedPropertyKinds(pkId)) {
                        dependersOfEntity.keys foreach { e ⇒
                            if (propertiesOfEntity.get(e) == null) {
                                val fallbackProperty = fallbackPropertyBasedOnPkId(this, e, pkId)
                                if (traceFallbacks) {
                                    var message = s"used fallback $fallbackProperty for $e"
                                    if (computedPropertyKinds(pkId)) {
                                        message += " (though an analysis was supposedly scheduled)"
                                    }
                                    trace("analysis progress", message)
                                }
                                fallbacksUsedCounter.incrementAndGet()
                                handleResult(Result(e, fallbackProperty))

                                continueComputation = true
                            }
                        }
                    }
                    pkId += 1
                }

                // 2. let's search for cSCCs that only consist of properties which will not be
                //    updated later on
                if (!continueComputation) {
                    val epks = ArrayBuffer.empty[SomeEPK]
                    val maxPKIndex = SupportedPropertyKinds // PropertyKey.maxId // properties.length
                    var pkId = 0
                    while (pkId < maxPKIndex) {
                        if (!delayedPropertyKinds(pkId)) {
                            val dependeesOfEntity = this.dependees(pkId)
                            val dependersOfEntity = this.dependers(pkId)
                            this.properties(pkId) forEach { (e, eps) ⇒
                                if (dependeesOfEntity.contains(e) &&
                                    dependersOfEntity.contains(e)) {
                                    epks += eps.toEPK
                                }
                            }
                        }
                        pkId += 1
                    }

                    val cSCCs = graphs.closedSCCs(
                        epks,
                        (epk: SomeEPK) ⇒ dependees(epk.pk.id)(epk.e).map(_.toEPK)
                    )
                    if (cSCCs.nonEmpty) {
                        handleResult(CSCCsResult(cSCCs))
                        continueComputation = true
                    }
                }

                if (!continueComputation) {
                    // We used no fallbacks and found no cycles, but we may still have
                    // (collaboratively computed) properties (e.g. CallGraph) which are
                    // not yet final; let's finalize them!
                    val maxPKIndex = SupportedPropertyKinds // PropertyKey.maxId // properties.length
                    var pkId = 0
                    var toBeFinalized: List[SomeEPS] = Nil
                    while (pkId < maxPKIndex) {
                        val dependeesOfEntity = dependees(pkId)
                        // Check that the property will not be computed later on.
                        if (!delayedPropertyKinds(pkId)) {
                            properties(pkId) forEach { (e, eps) ⇒
                                // Check that we have no running computations.
                                if (eps.isRefinable && dependeesOfEntity.get(e).isEmpty) {
                                    toBeFinalized ::= eps
                                }
                            }
                        }
                        pkId += 1
                    }
                    if (toBeFinalized.nonEmpty) {
                        toBeFinalized foreach { eps ⇒ handleResult(Result(eps.e, eps.ub)) }
                        continueComputation = true
                    }
                }
            }
        } while (continueComputation)

        if (debug && !isSuspended) {
            // let's search for "unsatisfied computations" related to "forced properties"
            val maxPKIndex = SupportedPropertyKinds // PropertyKey.maxId // properties.length
            var pkId = 0
            while (pkId < maxPKIndex) {
                properties(pkId) forEach { (e, eps) ⇒
                    if (eps.isFinal && forcedComputations(pkId).containsKey(e)) {
                        error(
                            "analysis progress",
                            s"intermediate property state for forced property: $eps"
                        )
                    }
                }
                pkId += 1
            }
        }
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
        Math.max(NumberOfThreadsForCPUBoundTasks - 1, 1)
    }

    def apply(
        context: PropertyStoreContext[_ <: AnyRef]*
    )(
        implicit
        logContext: LogContext
    ): PKEParallelTasksPropertyStore = {
        val contextMap: Map[Type, AnyRef] = context.map(_.asTuple).toMap
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
}
