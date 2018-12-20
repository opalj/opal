/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext

/**
 * Tests the property computations scheduler.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyComputationsSchedulerTest extends FunSpec with Matchers with BeforeAndAfterEach {

    implicit val logContext: GlobalLogContext.type = GlobalLogContext

    case class BasicComputationSpecification(
            override val name:      String,
            computationType:        ComputationType,
            uses:                   Set[PropertyBounds]    = Set.empty,
            derivesLazily:          Option[PropertyBounds] = None,
            derivesEagerly:         Set[PropertyBounds]    = Set.empty,
            derivesCollaboratively: Set[PropertyBounds]    = Set.empty
    ) extends SimpleComputationSpecification[Unit] {
        override def schedule(ps: PropertyStore, unused: Null): Unit = {}

    }

    val pks: Array[PropertyKind] = new Array[PropertyKind](12)
    (0 to 11).foreach { i ⇒ pks(i) = PropertyKey.create[Null, Null]("p"+(i)) }

    val c1 = BasicComputationSpecification(
        "c1",
        EagerComputation,
        uses = Set.empty,
        derivesEagerly = Set(PropertyBounds.lub(pks(1)))
    )

    //**********************************************************************************************
    //
    // TESTS

    describe("an AnalysisScenario") {

        it("should be possible to create an empty schedule") {
            val batches = AnalysisScenario(Set()).computeSchedule.batches
            batches should be('empty)
        }

        describe("the scheduling of mixed eager and lazy property computations") {

            class PropertyStoreConfigurationRecorder extends PropertyStore {
                implicit val logContext: LogContext = GlobalLogContext
                val ctx: Map[Class[_], AnyRef] = Map.empty

                //
                // Methods which are not required by the following tests...
                //

                override def shutdown(): Unit = ???

                override def supportsFastTrackPropertyComputations: Boolean = ???

                override def toString(printProperties: Boolean): String = ???

                override def scheduledTasksCount: Int = ???

                override def scheduledOnUpdateComputationsCount: Int = ???

                override def immediateOnUpdateComputationsCount: Int = ???

                override def quiescenceCount: Int = ???

                override def fastTrackPropertiesCount: Int = ???

                override def statistics: scala.collection.Map[String, Int] = ???

                override def isKnown(e: Entity): Boolean = ???

                override def hasProperty(e: Entity, pk: PropertyKind): Boolean = ???

                override def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = ???

                override def entities[P <: Property](
                    pk: PropertyKey[P]
                ): Iterator[EPS[Entity, P]] = ???

                override def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = ???

                override def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = ???

                override def entitiesWithLB[P <: Property](lb: P): Iterator[Entity] = ???

                override def entitiesWithUB[P <: Property](ub: P): Iterator[Entity] = ???

                override def finalEntities[P <: Property](p: P): Iterator[Entity] = ???

                override def doSet(e: Entity, p: Property): Unit = ???

                override def doPreInitialize[E <: Entity, P <: Property](
                    e:  E,
                    pk: PropertyKey[P]
                )(
                    pc: EOptionP[E, P] ⇒ InterimEP[E, P]
                ): Unit = ???

                override def apply[E <: Entity, P <: Property](
                    e:  E,
                    pk: PropertyKey[P]
                ): EOptionP[E, P] = ???

                override def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = ???

                override def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = ???

                override protected[this] def isIdle: Boolean = true

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

                //
                // Methods containing test logic.
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

            it("an empty analysis scenario should lead to an empty phase configuration in the ps") {
                val scenario = AnalysisScenario(Set.empty)
                val schedule = scenario.computeSchedule
                val ps = new PropertyStoreConfigurationRecorder()
                /*smoke test: */ schedule(ps, trace = false)
                schedule.batches.size should be(1)
                ps.phaseConfigurations.head should be((Set.empty, Set.empty, Map.empty))
            }

        }
    }
}

