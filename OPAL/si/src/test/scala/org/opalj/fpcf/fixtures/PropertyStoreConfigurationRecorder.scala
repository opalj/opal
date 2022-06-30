/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package fixtures

import scala.collection.immutable.IntMap
import scala.collection.mutable

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext

class BasePropertyStoreMockup extends PropertyStore {

    implicit val logContext: LogContext = GlobalLogContext

    val ctx: Map[Class[_], AnyRef] = Map.empty

    //
    // Methods which are not required by the following tests...
    //

    override def MaxEvaluationDepth: Int = ???

    override def shutdown(): Unit = ???

    override def toString(printProperties: Boolean): String = ???

    override def scheduledTasksCount: Int = ???

    override def scheduledOnUpdateComputationsCount: Int = ???

    override def quiescenceCount: Int = ???

    override def fallbacksUsedForComputedPropertiesCount: Int = ???

    override def incrementFallbacksUsedForComputedPropertiesCounter(): Unit = ???

    override def isKnown(e: Entity): Boolean = ???

    override def hasProperty(e: Entity, pk: PropertyKind): Boolean = ???

    override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = ???

    override def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = ???

    override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = ???

    override def entities(propertyFilter: SomeEPS => Boolean): Iterator[Entity] = ???

    override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = ???

    override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = ???

    override def finalEntities[P <: Property](p: P): Iterator[Entity] = ???

    override def get[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Option[EOptionP[E, P]] = ???

    override def get[E <: Entity, P <: Property](epk: EPK[E, P]): Option[EOptionP[E, P]] = ???

    override def doSet(e: Entity, p: Property): Unit = ???

    override def doPreInitialize[E <: Entity, P <: Property](
        e:  E,
        pk: PropertyKey[P]
    )(
        pc: EOptionP[E, P] => InterimEP[E, P]
    ): Unit = ???

    override def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = ???

    override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = ???

    override def isIdle: Boolean = true

    override def execute(f: => Unit): Unit = ???

    override def doRegisterTriggeredComputation[E <: Entity, P <: Property](
        pk: PropertyKey[P],
        pc: PropertyComputation[E]
    ): Unit = {}

    override def doScheduleEagerComputationForEntity[E <: Entity](
        e: E
    )(
        pc: PropertyComputation[E]
    ): Unit = {}

    override def handleResult(r: PropertyComputationResult): Unit = {}

    override def waitOnPhaseCompletion(): Unit = {}

    override def newPhaseInitialized(
        propertyKindsComputedInThisPhase:  Set[PropertyKind],
        propertyKindsComputedInLaterPhase: Set[PropertyKind],
        suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]],
        finalizationOrder:                 List[List[PropertyKind]]
    ): Unit = {
    }
}

/** A simple property store which will return the values when queried consecutively */
class InitializedPropertyStore(
        val data: IntMap[Map[Entity, mutable.Queue[EOptionP[Entity, Property]]]]
) extends BasePropertyStoreMockup {

    override def doApply[E <: Entity, P <: Property](
        epk:  EPK[E, P],
        e:    E,
        pkId: Int
    ): EOptionP[E, P] = {
        data(pkId)(e).dequeue().asInstanceOf[EOptionP[E, P]]
    }
}

class PropertyStoreConfigurationRecorder extends BasePropertyStoreMockup {

    //
    // Methods containing test fixture related logic.
    //

    var phaseConfigurations: List[(Set[PropertyKind], Set[PropertyKind], Map[PropertyKind, Set[PropertyKind]])] = List.empty

    override def newPhaseInitialized(
        propertyKindsComputedInThisPhase:  Set[PropertyKind],
        propertyKindsComputedInLaterPhase: Set[PropertyKind],
        suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]],
        finalizationOrder:                 List[List[PropertyKind]]
    ): Unit = {
        phaseConfigurations ::=
            ((
                propertyKindsComputedInThisPhase,
                propertyKindsComputedInLaterPhase,
                suppressInterimUpdates
            ))
    }
}
