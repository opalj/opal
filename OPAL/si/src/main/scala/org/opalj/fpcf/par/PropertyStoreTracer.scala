/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._
import org.opalj.io

/**
 * Enables the tracing of key events during the analysis progress.
 *
 * @author Michael Eichberg
 */
trait PropertyStoreTracer {

    //
    // POTENTIALLY CALLED CONCURRENTLY
    //

    def force(e: Entity, pkId: Int): Unit

    def forceForComputedEPK(e: Entity, pkId: Int): Unit

    //
    // CALLED SEQUENTIALLY
    //

    /** Called if a lazy or fallback computation is eventually scheduled. */
    def schedulingLazyComputation(e: Entity, pkId: Int): Unit

    /**
     * Called when a property is updated.
     *
     * @param oldEPS The old value of the property; may be null.
     * @param newEPS The new value of the property.
     */
    def update(oldEPS: SomeEPS, newEPS: SomeEPS): Unit

    def notification(newEPS: SomeEPS, depender: SomeEPK): Unit

    def delayedNotification(newEPS: SomeEPS): Unit

    def immediateDependeeUpdate(
        e: Entity, pk: SomePropertyKey,
        processedDependee:    SomeEOptionP,
        currentDependee:      SomeEPS,
        updateAndNotifyState: UpdateAndNotifyState
    ): Unit

    def handlingResult(
        r:                               PropertyComputationResult,
        epksWithNotYetNotifiedDependers: Set[SomeEPK]
    ): Unit

    def uselessPartialResult(
        r:           SomePartialResult,
        oldEOptionP: SomeEOptionP
    ): Unit

    def metaInformationDeleted(finalP: SomeFinalEP): Unit

    def reachedQuiescence(): Unit

    def firstException(t: Throwable): Unit
}

sealed trait StoreEvent {
    def eventId: Int

    def toTxt: String
}

case class Force(
        eventId: Int,
        e:       Entity,
        pkId:    Int
) extends StoreEvent {

    override def toString: String = {
        s"Force($eventId,$e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }

    override def toTxt: String = {
        s"$eventId: Force($e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }
}

case class ForceForComputedEPK(
        eventId: Int,
        e:       Entity,
        pkId:    Int
) extends StoreEvent {

    override def toString: String = {
        s"ForceForComputedEPK($eventId,"+
            s"$e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }

    override def toTxt: String = {
        s"$eventId: ForceForComputedEPK("+
            s"$e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }
}

case class LazyComputationScheduled(
        eventId: Int,
        e:       Entity,
        pkId:    Int
) extends StoreEvent {

    override def toString: String = {
        s"LazyComputationScheduled($eventId,"+
            s"$e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }

    override def toTxt: String = {
        s"$eventId: LazyComputationScheduled("+
            s"$e@${System.identityHashCode(e).toHexString},${PropertyKey.name(pkId)})"
    }
}

case class PropertyUpdate(
        eventId: Int,
        oldEPS:  SomeEPS,
        newEPS:  SomeEPS
) extends StoreEvent {

    override def toString: String = s"PropertyUpdate($eventId,oldEPS=$oldEPS,newEPS=$newEPS)"
    override def toTxt: String = s"$eventId: PropertyUpdate(oldEPS=$oldEPS,newEPS=$newEPS)"
}

case class DependerNotification(
        eventId:     Int,
        newEPS:      SomeEPS,
        dependerEPK: SomeEPK
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: DependerNotification(newEPS=$newEPS, dependerEPK=$dependerEPK)"
    }

}

case class DelayedDependersNotification(
        eventId: Int,
        newEPS:  SomeEPS
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: DelayedDependersNotification(newEPS=$newEPS)"
    }

}

case class ImmediateDependeeUpdate(
        eventId:              Int,
        e:                    Entity,
        pk:                   SomePropertyKey,
        processedDependee:    SomeEOptionP,
        currentDependee:      SomeEPS,
        updateAndNotifyState: UpdateAndNotifyState
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: ImmediateDependeeUpdate($e@${System.identityHashCode(e).toHexString},"+
            s"pk=$pk,processedDependee=$processedDependee,currentDependee=$currentDependee,"+
            s"updateAndNotifyState=$updateAndNotifyState)"
    }

}

case class HandlingResult(
        eventId:                         Int,
        r:                               PropertyComputationResult,
        epksWithNotYetNotifiedDependers: Set[SomeEPK]
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: HandlingResult($r,"+
            s"epksWithNotYetNotifiedDependers=$epksWithNotYetNotifiedDependers)"
    }

}

case class UselessPartialResult(
        eventId:     Int,
        r:           SomePartialResult,
        oldEOptionP: SomeEOptionP
) extends StoreEvent {

    override def toTxt: String = {
        val e = oldEOptionP.e
        s"$eventId: UselessPartialResult($e@${System.identityHashCode(e).toHexString},"+
            s"$r,old=$oldEOptionP)"
    }

}

case class MetaInformationDeleted(
        eventId: Int,
        finalP:  SomeFinalEP
) extends StoreEvent {

    override def toTxt: String = s"$eventId: MetaInformationDeleted(${finalP.toEPK})"

}

case class FirstException(
        eventId:    Int,
        t:          Throwable,
        thread:     String,
        stackTrace: String
) extends StoreEvent {

    override def toTxt: String = {
        s"$eventId: FirstException(\n\t$t,\n\t$thread,\n\t$stackTrace\n)"
    }

}

case class ReachedQuiescence(eventId: Int) extends StoreEvent {

    override def toTxt: String = s"$eventId: ReachedQuiescence"

}

class RecordAllPropertyStoreTracer extends PropertyStoreTracer {

    val eventCounter = new AtomicInteger(0)

    private[this] val events = new ConcurrentLinkedQueue[StoreEvent]

    //
    // POTENTIALLY CALLED CONCURRENTLY
    //

    override def force(e: Entity, pkId: Int): Unit = {
        events.offer(Force(eventCounter.incrementAndGet(), e, pkId))
    }

    override def forceForComputedEPK(e: Entity, pkId: Int): Unit = {
        events.offer(ForceForComputedEPK(eventCounter.incrementAndGet(), e, pkId))
    }

    //
    // CALLED BY THE STORE UPDATES PROCESSOR THREAD
    //

    override def schedulingLazyComputation(e: Entity, pkId: Int): Unit = {
        events offer LazyComputationScheduled(eventCounter.incrementAndGet(), e, pkId)
    }

    override def update(oldEPS: SomeEPS, newEPS: SomeEPS): Unit = {
        events offer PropertyUpdate(eventCounter.incrementAndGet(), oldEPS, newEPS)
    }

    override def notification(newEPS: SomeEPS, dependerEPK: SomeEPK): Unit = {
        events offer DependerNotification(eventCounter.incrementAndGet(), newEPS, dependerEPK)
    }

    override def delayedNotification(newEPS: SomeEPS): Unit = {
        events offer DelayedDependersNotification(eventCounter.incrementAndGet(), newEPS)
    }

    override def immediateDependeeUpdate(
        e: Entity, pk: SomePropertyKey,
        processedDependee: SomeEOptionP, currentDependee: SomeEPS,
        updateAndNotifyState: UpdateAndNotifyState
    ): Unit = {
        events offer ImmediateDependeeUpdate(
            eventCounter.incrementAndGet(),
            e, pk,
            processedDependee,
            currentDependee,
            updateAndNotifyState
        )
    }

    override def handlingResult(
        r:                               PropertyComputationResult,
        epksWithNotYetNotifiedDependers: Set[SomeEPK]
    ): Unit = {
        val eventId = eventCounter.incrementAndGet()
        events offer HandlingResult(eventId, r, epksWithNotYetNotifiedDependers)
    }

    override def uselessPartialResult(
        r:           SomePartialResult,
        oldEOptionP: SomeEOptionP
    ): Unit = {
        val eventId = eventCounter.incrementAndGet()
        events offer UselessPartialResult(eventId, r, oldEOptionP)
    }

    override def metaInformationDeleted(finalEP: SomeFinalEP): Unit = {
        events offer MetaInformationDeleted(eventCounter.incrementAndGet(), finalEP)
    }

    override def reachedQuiescence(): Unit = {
        events offer ReachedQuiescence(eventCounter.incrementAndGet())
    }

    override def firstException(t: Throwable): Unit = {
        events offer FirstException(
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
            case e: HandlingResult ⇒ "\n"+e.toTxt
            case e                 ⇒ "\t"+e.toTxt
        }.mkString("Events [\n", "\n", "\n]")
    }

    def writeAsTxt: File = {
        io.write(toTxt, "Property Store Events", ".txt").toFile
    }

    def writeAsTxtAndOpen: File = {
        io.writeAndOpen(toTxt, "Property Store Events", ".txt")
    }
}
