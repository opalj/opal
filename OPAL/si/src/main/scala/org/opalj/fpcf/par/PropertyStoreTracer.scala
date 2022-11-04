/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._
import org.opalj.io

/**
 * Enables the tracing of key events during the analysis progress.
 *
 * @author Michael Eichberg
 */
private[par] trait PropertyStoreTracer {

    /** Called when a value is explicitly set. */
    def set(epkState: EPKState): Unit

    /**
     * Called when the property of an entity that is computed using partial results is
     * pre-initialized.
     *
     * @param oldEPKState the old state (may be null).
     * @param newEPKState the new state.
     */
    def preInitialize(oldEPKState: EPKState, newEPKState: EPKState): Unit

    def triggeredComputation(e: Entity, pkId: Int, triggeredPC: SomePropertyComputation): Unit

    def scheduledResultProcessing(r: PropertyComputationResult): Unit

    def enqueueingEPKToForce(epk: SomeEPK): Unit

    def force(epk: SomeEPK): Unit

    def scheduledLazyComputation(requestedEPK: SomeEPK, lazyPC: SomePropertyComputation): Unit

    def computedFallback(ep: SomeFinalEP, why: String): Unit

    def evaluatedTransformer(source: SomeEOptionP, target: SomeFinalEP): Unit

    def registeredTransformer(source: EPKState, target: EPKState): Unit

    def scheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
    ): Unit

    def immediatelyRescheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
    ): Unit

    def scheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        finalEP:     SomeFinalEP,
        c:           OnUpdateContinuation
    ): Unit

    def immediateEvaluationOfLazyComputation(
        newEOptionP:            SomeEOptionP,
        evaluationDepthCounter: Int,
        lazyPC:                 SomePropertyComputation
    ): Unit

    def idempotentUpdate(epkState: EPKState): Unit

    def removedDepender(dependerEPK: SomeEPK, dependeeEPKState: EPKState): Unit

    def appliedUpdateComputation(
        newEPKState: EPKState,
        result:      Option[(SomeEOptionP, SomeInterimEP, Iterable[SomeEPK])]
    ): Unit

    def processingResult(r: PropertyComputationResult): Unit

    def startedMainLoop(): Unit
    def reachedQuiescence(): Unit

    def handlingInterimEPKsDueToSuppression(interimEPKS: String, cSCCs: String): Unit

    def makingIntermediateEPKStateFinal(interimEPKState: EPKState): Unit

    def subphaseFinalization(finalizedProperties: String): Unit

    def finalizedProperty(oldEOptionP: SomeEOptionP, finalEP: SomeFinalEP): Unit

    def firstException(t: Throwable): Unit

    def toTxt: String
}

sealed trait StoreEvent {
    def eventId: Int

    def toTxt: String
}

case class ProcessingResultEvent(eventId: Int, r: PropertyComputationResult) extends StoreEvent {
    override def toTxt: String = s"$eventId: ProcessingResult($r)"
}

case class DeferredProcessingResultEvent(eventId: Int, r: PropertyComputationResult) extends StoreEvent {
    override def toTxt: String = s"$eventId: DeferedProcessingResult($r)"
}

case class FirstExceptionEvent(
        eventId:    Int,
        t:          Throwable,
        thread:     String,
        stackTrace: String
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: FirstException(\n\t$t,\n\t$thread,\n\t$stackTrace\n)"
    }

}

case class ReachedQuiescenceEvent(eventId: Int) extends StoreEvent {
    override def toTxt: String = s"$eventId: ReachedQuiescence"
}

case class StartedMainLoopEvent(eventId: Int) extends StoreEvent {
    override def toTxt: String = s"$eventId: StartedMainLoop"
}

case class SubphaseFinalizationEvent(eventId: Int, properties: String) extends StoreEvent {
    override def toTxt: String = s"$eventId: SubphaseFinalization($properties)"
}

case class FinalizedPropertyEvent(
        eventId: Int,
        oldEP:   SomeEOptionP,
        finalEP: SomeFinalEP
) extends StoreEvent {
    override def toTxt: String = s"$eventId: FinalizedProperty($oldEP => $finalEP)"
}

case class AppliedUpdateComputationEvent(
        eventId:     Int,
        newEPKState: EPKState,
        result:      Option[(SomeEOptionP, SomeInterimEP, Iterable[SomeEPK])]
) extends StoreEvent {
    override def toTxt: String = s"$eventId: AppliedUpdateComputation($newEPKState; result: $result)"
}

case class IdempotentUpdateEvent(
        eventId:     Int,
        newEPKState: EPKState
) extends StoreEvent {
    override def toTxt: String = s"$eventId: IdempotentUpdate($newEPKState)"
}

case class RemovedDependerEvent(eventId: Int, epk: SomeEPK, epkState: EPKState) extends StoreEvent {
    override def toTxt: String = s"$eventId: RemovedDepender(removed=$epk;from=$epkState)"
}

case class SetPropertyEvent(eventId: Int, epkState: EPKState) extends StoreEvent {
    override def toTxt: String = s"$eventId: SetProperty($epkState)"
}

case class PreInitializationEvent(eventId: Int, oldEPKState: EPKState, newEPKState: EPKState) extends StoreEvent {
    override def toTxt: String = s"$eventId: PreInitialization($oldEPKState => $newEPKState)"
}

case class EnqueuedEPKToForceEvent(eventId: Int, epk: SomeEPK) extends StoreEvent {
    override def toTxt: String = s"$eventId: EnqueuedEPKToForce($epk)"
}

case class ForcingEPKEvaluationEvent(eventId: Int, epk: SomeEPK) extends StoreEvent {
    override def toTxt: String = s"$eventId: ForcingEPKEvaluation($epk)"
}

case class TriggeredComputationEvent(
        eventId: Int,
        e:       Entity,
        pkId:    UShort,
        c:       SomePropertyComputation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: TriggeredComputation(${anyRefToShortString(e)},${PropertyKey.name(pkId)},${anyRefToShortString(c)})"
    }
}

case class ComputedFallbackEvent(
        eventId: Int, ep: SomeFinalEP, why: String
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ComputedFallback($ep,$why)"
    }
}

case class ScheduledLazyComputationEvent(
        eventId: Int,
        epk:     SomeEPK,
        c:       SomePropertyComputation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ScheduledLazyComputation($epk,c=${anyRefToShortString(c)})"
    }
}

case class ImmediatelyExecutedLazyComputationEvent(
        eventId:         Int,
        newEOptionP:     SomeEOptionP,
        evaluationDepth: Int,
        c:               SomePropertyComputation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ImmediatelyExecutedLazyComputation"+
            s"(for=$newEOptionP,evalDepth=$evaluationDepth,c=${anyRefToShortString(c)})"
    }
}

case class EvaluatedTransformerEvent(
        eventId: Int,
        source:  SomeEOptionP,
        target:  SomeFinalEP
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: EvaluatedTransformer($source => $target)"
    }
}

case class RegisteredTransformerEvent(
        eventId: Int,
        source:  EPKState,
        target:  EPKState
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: RegisteredTransformer($source => $target)"
    }
}

case class HandlingInterimEPKsDueToSuppressionEvent(
        eventId:     Int,
        interimEPKs: String,
        cSCCs:       String
) extends StoreEvent {
    override def toTxt: String = {
        val mEPKs = interimEPKs.replace("\t", "\t\t")
        val mCSCCs = cSCCs.replace("\t", "\t\t")
        s"$eventId: HandlingInterimEPKsDueToSuppression(\n\t\tinterimEPKs=$mEPKs,\n\t\tcSCCs=$mCSCCs)"
    }
}

case class MakingIntermediateEPKStateFinalEvent(
        eventId:         Int,
        interimEPKState: EPKState
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: MakingIntermediateEPKStateFinal($interimEPKState)"
    }
}

case class ScheduledOnUpdateComputationEvent(
        eventId:     Int,
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ScheduledOnUpdateComputation($dependerEPK, $oldEOptionP => $newEOptionP,${anyRefToShortString(c)})"
    }
}

case class ImmediatelyRescheduledOnUpdateComputationEvent(
        eventId:     Int,
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ImmediatelyRescheduledOnUpdateComputation($dependerEPK, $oldEOptionP => $newEOptionP,${anyRefToShortString(c)})"
    }
}

case class ScheduledOnUpdateComputationForFinalEPEvent(
        eventId:     Int,
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        finalEP:     SomeFinalEP,
        c:           OnUpdateContinuation
) extends StoreEvent {
    override def toTxt: String = {
        s"$eventId: ScheduledOnUpdateComputationForFinalEP($dependerEPK, $oldEOptionP => $finalEP,${anyRefToShortString(c)})"
    }
}

private[par] class RecordAllPropertyStoreEvents extends PropertyStoreTracer {

    val eventCounter = new AtomicInteger(0)

    private[this] def nextEventId(): Int = eventCounter.incrementAndGet()

    private[this] val events = new ConcurrentLinkedQueue[StoreEvent]

    override def set(epkState: EPKState): Unit = {
        events offer SetPropertyEvent(nextEventId(), epkState)
    }

    override def preInitialize(oldEPKState: EPKState, newEPKState: EPKState): Unit = {
        events offer PreInitializationEvent(nextEventId(), oldEPKState, newEPKState)
    }

    override def triggeredComputation(
        e:           Entity,
        pkId:        UShort,
        triggeredPC: SomePropertyComputation
    ): Unit = {
        events offer TriggeredComputationEvent(nextEventId(), e, pkId, triggeredPC)
    }

    override def scheduledResultProcessing(r: PropertyComputationResult): Unit = {
        events offer DeferredProcessingResultEvent(nextEventId(), r)
    }

    override def enqueueingEPKToForce(epk: SomeEPK): Unit = {
        events offer EnqueuedEPKToForceEvent(nextEventId(), epk)
    }

    override def force(epk: SomeEPK): Unit = {
        events offer ForcingEPKEvaluationEvent(nextEventId(), epk)
    }

    override def scheduledLazyComputation(
        requestedEPK: SomeEPK,
        lazyPC:       SomePropertyComputation
    ): Unit = {
        events offer ScheduledLazyComputationEvent(nextEventId(), requestedEPK, lazyPC)
    }

    override def immediateEvaluationOfLazyComputation(
        newEOptionP:            SomeEOptionP,
        evaluationDepthCounter: Int,
        lazyPC:                 SomePropertyComputation
    ): Unit = {
        events offer ImmediatelyExecutedLazyComputationEvent(
            nextEventId(), newEOptionP, evaluationDepthCounter, lazyPC
        )
    }

    override def computedFallback(ep: SomeFinalEP, why: String): Unit = {
        events offer ComputedFallbackEvent(nextEventId(), ep, why)
    }

    override def evaluatedTransformer(source: SomeEOptionP, target: SomeFinalEP): Unit = {
        events offer EvaluatedTransformerEvent(nextEventId(), source, target)
    }

    override def registeredTransformer(source: EPKState, target: EPKState): Unit = {
        events offer RegisteredTransformerEvent(nextEventId(), source, target)
    }

    override def scheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
    ): Unit = {
        events offer ScheduledOnUpdateComputationEvent(
            nextEventId(), dependerEPK, oldEOptionP, newEOptionP, c
        )

    }

    override def immediatelyRescheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        newEOptionP: SomeEOptionP,
        c:           OnUpdateContinuation
    ): Unit = {
        events offer ImmediatelyRescheduledOnUpdateComputationEvent(
            nextEventId(), dependerEPK, oldEOptionP, newEOptionP, c
        )

    }

    override def scheduledOnUpdateComputation(
        dependerEPK: SomeEPK,
        oldEOptionP: SomeEOptionP,
        finalEP:     SomeFinalEP,
        c:           OnUpdateContinuation
    ): Unit = {
        events offer ScheduledOnUpdateComputationForFinalEPEvent(
            nextEventId(), dependerEPK, oldEOptionP, finalEP, c
        )
    }

    override def idempotentUpdate(epkState: EPKState): Unit = {
        events offer IdempotentUpdateEvent(nextEventId(), epkState)
    }

    override def removedDepender(dependerEPK: SomeEPK, dependeeEPKState: EPKState): Unit = {
        events offer RemovedDependerEvent(nextEventId(), dependerEPK, dependeeEPKState)
    }

    override def appliedUpdateComputation(
        newEPKState: EPKState,
        result:      Option[(SomeEOptionP, SomeInterimEP, Iterable[SomeEPK])]
    ): Unit = {
        events offer AppliedUpdateComputationEvent(nextEventId(), newEPKState, result)
    }

    override def processingResult(r: PropertyComputationResult): Unit = {
        events offer ProcessingResultEvent(nextEventId(), r)
    }

    override def startedMainLoop(): Unit = {
        events offer StartedMainLoopEvent(nextEventId())
    }

    override def handlingInterimEPKsDueToSuppression(interimEPKS: String, cSCCs: String): Unit = {
        events offer HandlingInterimEPKsDueToSuppressionEvent(nextEventId(), interimEPKS, cSCCs)
    }

    override def makingIntermediateEPKStateFinal(interimEPKState: EPKState): Unit = {
        events offer MakingIntermediateEPKStateFinalEvent(nextEventId(), interimEPKState)
    }

    override def subphaseFinalization(finalizedProperties: String): Unit = {
        events offer SubphaseFinalizationEvent(nextEventId(), finalizedProperties)
    }

    override def finalizedProperty(oldEOptionP: SomeEOptionP, finalEP: SomeFinalEP): Unit = {
        events offer FinalizedPropertyEvent(nextEventId(), oldEOptionP, finalEP)
    }

    override def reachedQuiescence(): Unit = {
        events offer ReachedQuiescenceEvent(eventCounter.incrementAndGet())
    }

    override def firstException(t: Throwable): Unit = {
        events offer FirstExceptionEvent(
            eventCounter.incrementAndGet(),
            t,
            Thread.currentThread().getName,
            Thread.currentThread().getStackTrace.mkString("\n\t", "\n\t", "\n") // dropWhile(_.getMethodName != "handleException")
        )
    }

    //
    // QUERYING THE EVENTS
    //

    def allEvents: List[StoreEvent] = events.iterator.asScala.toList.sortWith(_.eventId < _.eventId)

    def toTxt: String = {
        allEvents.map {
            case e: ProcessingResultEvent => "->\t"+e.toTxt
            case e                        => "\t"+e.toTxt
        }.mkString("Events [\n", "\n", "\n]")
    }

    def writeAsTxt: File = {
        io.write(toTxt, "Property Store Events", ".txt").toFile
    }

    def writeAsTxtAndOpen: File = {
        io.writeAndOpen(toTxt, "Property Store Events", ".txt")
    }

}
