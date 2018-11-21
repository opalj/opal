/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.opalj.fpcf.seq.PKESequentialPropertyStore
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

    case class BasicComputationSpecification(
            override val name: String,
            uses:              Set[PropertyKind],
            derives:           Set[PropertyKind],
            isLazy:            Boolean           = false
    ) extends ComputationSpecification {

        override type InitializationData = Null
        override def init(ps: PropertyStore): Null = null

        override def beforeSchedule(ps: PropertyStore): Unit = {}

        override def afterPhaseCompletion(ps: PropertyStore): Unit = {}

        override def schedule(ps: PropertyStore, unused: Null): Unit = {}

    }

    implicit val logContext = GlobalLogContext

    val pks: Array[PropertyKind] = new Array[PropertyKind](12)
    (0 to 11).foreach { i ⇒
        pks(i) = PropertyKey.create[Null, Null](
            "p"+(i),
            (_: PropertyStore, _: FallbackReason, _: Entity) ⇒ ???,
            (_: PropertyStore, _: SomeEPS) ⇒ ???,
            (_: PropertyStore, _: Entity) ⇒ None
        )
    }

    val c1 = BasicComputationSpecification(
        "c1",
        Set.empty,
        Set(pks(1), pks(2))
    )

    val c2 = BasicComputationSpecification(
        "c2",
        Set(pks(2), pks(3)),
        Set(pks(4))
    )

    val c3 = BasicComputationSpecification(
        "c3",
        uses = Set.empty,
        derives = Set(pks(3))
    )

    val c4 = BasicComputationSpecification(
        "c4",
        Set(pks(1)),
        Set(pks(5))
    )

    val c5 = BasicComputationSpecification(
        "c5",
        Set(pks(3), pks(4)),
        Set(pks(0))
    )

    val c6 = BasicComputationSpecification(
        "c6",
        Set(pks(6), pks(8)),
        Set(pks(7))
    )

    val c7 = BasicComputationSpecification(
        "c7",
        Set(pks(5), pks(7), pks(8), pks(0)),
        Set(pks(6))
    )
    val c7Lazy = BasicComputationSpecification(
        "c7lazy",
        Set(pks(5), pks(7), pks(8), pks(0)),
        Set(pks(6)),
        isLazy = true
    )

    val c8 = BasicComputationSpecification(
        "c8",
        Set(pks(6)),
        Set(pks(8), pks(9))
    )

    val c8Lazy = BasicComputationSpecification(
        "c8lazy",
        Set(pks(6)),
        Set(pks(8), pks(9)),
        isLazy = true
    )

    val c9 = BasicComputationSpecification(
        "c9",
        Set(pks(9)),
        Set(pks(10))
    )

    val c10 = BasicComputationSpecification(
        "c10",
        Set.empty,
        Set(pks(10)) // this one also derives property 10; e.g., at a more basic level
    )

    val c10Lazy = BasicComputationSpecification(
        "c10lazy",
        Set.empty,
        Set(pks(10)), // this one also derives property 10; e.g., at a more basic level
        isLazy = true
    )

    val c11 = BasicComputationSpecification(
        "c11",
        Set.empty,
        Set(pks(11), pks(10)) // this one also derives property 10; e.g., at a more basic level
    )

    //**********************************************************************************************
    //
    // TESTS

    describe("an AnalysisScenario") {

        it("should be possible to see the dependencies between the computations") {
            AnalysisScenario(Set(c6, c7, c8)).propertyComputationsDependencies
        }

        it("should be possible to create an empty schedule") {
            val batches = AnalysisScenario(Set()).computeSchedule.batches
            batches should be('empty)
        }

        describe("the scheduling of eager property computations") {

            it("should be possible to create a schedule where a property is computed by two computations") {
                val batches = AnalysisScenario(Set(c9, c10)).computeSchedule.batches
                batches.size should be(1)
            }

            it("should be possible to create a schedule where a property is computed by three computations") {
                val batches = AnalysisScenario(Set(c9, c10, c11)).computeSchedule.batches
                batches.size should be(1)
            }

            it("should be possible to create a schedule with one computation") {
                val batches = AnalysisScenario(Set(c1)).computeSchedule.batches
                batches.size should be(1)
                batches.head.head should be(c1)
            }

            it("should be possible to create a schedule with two independent computations") {
                val batches = AnalysisScenario(Set(c1, c3)).computeSchedule.batches
                batches.size should be(2)
                batches.foreach(_.size should be(1))
                batches.flatMap(batch ⇒ batch).toSet should be(Set(c1, c3))
            }

            it("should be possible to create a schedule where not all properties are explicitly derived") {
                val batches = AnalysisScenario(Set(c1, c2)).computeSchedule.batches
                batches.size should be(2)
                batches.foreach(_.size should be(1))
                batches.head.head should be(c1)
                batches.tail.head.head should be(c2)
            }

            it("should be possible to create a schedule where all computations depend on each other") {
                val batches = AnalysisScenario(Set(c6, c7, c8)).computeSchedule.batches
                batches.size should be(1)
                batches.head.toSet should be(Set(c6, c7, c8))
            }

            it("should be possible to create a complex schedule") {
                val schedule = AnalysisScenario(Set(c1, c2, c3, c4, c5, c6, c7, c8, c9)).computeSchedule
                schedule.batches.take(5).flatMap(batch ⇒ batch).toSet should be(Set(c1, c2, c3, c4, c5))
                schedule.batches.drop(5).head.toSet should be(Set(c6, c7, c8))
            }
        }

        describe("the scheduling of mixed eager and lazy property computations") {

            class PropertyStoreConfigurationRecorder extends PropertyStore {
                implicit val logContext: LogContext = GlobalLogContext
                val ctx: Map[Class[_], AnyRef] = Map.empty
                def shutdown(): Unit = ???
                def supportsFastTrackPropertyComputations: Boolean = ???
                def toString(printProperties: Boolean): String = ???
                def scheduledTasksCount: Int = ???
                def scheduledOnUpdateComputationsCount: Int = ???
                def immediateOnUpdateComputationsCount: Int = ???
                def resolvedCSCCsCount: Int = ???
                def quiescenceCount: Int = ???
                def fastTrackPropertiesCount: Int = ???
                def statistics: scala.collection.Map[String, Int] = ???
                def isKnown(e: Entity): Boolean = ???
                def hasProperty(e: Entity, pk: PropertyKind): Boolean = ???
                def properties[E <: Entity](e: E): Iterator[EPS[E, Property]] = ???
                def entities[P <: Property](pk: PropertyKey[P]): Iterator[EPS[Entity, P]] = ???
                def entities[P <: Property](lb: P, ub: P): Iterator[Entity] = ???
                def entities(propertyFilter: SomeEPS ⇒ Boolean): Iterator[Entity] = ???
                def set(e: Entity, p: Property): Unit = ???
                def apply[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): EOptionP[E, P] = ???
                def apply[E <: Entity, P <: Property](epk: EPK[E, P]): EOptionP[E, P] = ???
                def force[E <: Entity, P <: Property](e: E, pk: PropertyKey[P]): Unit = ???

                def registerLazyPropertyComputation[E <: Entity, P <: Property](
                    pk:       PropertyKey[P],
                    pc:       PropertyComputation[E],
                    finalEPs: TraversableOnce[FinalEP[E, P]]
                ): Unit = {}
                def registerTriggeredComputation[E <: Entity, P <: Property](
                    pk: PropertyKey[P],
                    pc: PropertyComputation[E]
                ): Unit = {}
                def scheduleEagerComputationForEntity[E <: Entity](e: E)(pc: PropertyComputation[E]): Unit = {}
                def handleResult(r: PropertyComputationResult, forceEvaluation: Boolean = false): Unit = {}
                def waitOnPhaseCompletion(): Unit = {}

                var phaseConfigurations: List[(Set[PropertyKind], Set[PropertyKind])] = List.empty
                def setupPhase(
                    computedPropertyKinds: Set[PropertyKind],
                    delayedPropertyKinds:  Set[PropertyKind] = Set.empty
                ): Unit = {
                    phaseConfigurations ::= ((computedPropertyKinds, delayedPropertyKinds))
                }

            }

            it("should be possible to create a schedule where a property is computed by multiple computations") {
                val schedule = AnalysisScenario(Set(c9, c10Lazy)).computeSchedule

                /*smoke test: */ schedule(PKESequentialPropertyStore(), trace = false)

                val batches = schedule.batches
                batches.size should be(1)
            }

            it("should be possible to create a mixed schedule where a property is computed by three computations") {
                val batches = AnalysisScenario(Set(c9, c10Lazy, c11)).computeSchedule.batches
                batches.size should be(1)
            }

            it("should be possible to create a complex schedule where some lazily computed properties are computed across multiple batches") {
                val scenario = AnalysisScenario(Set(c1, c2, c3, c4, c5, c6, c7Lazy, c8Lazy, c9))
                val schedule = scenario.computeSchedule
                val ps = new PropertyStoreConfigurationRecorder()
                /*smoke test: */ schedule(ps, trace = false)

                schedule.batches(5).toSet should contain(c7Lazy)
                schedule.batches(5).toSet should contain(c8Lazy)
                ps.phaseConfigurations.tail.head._1 should be(Set(pks(10), pks(9), pks(8), pks(6)))
                // ensure that at the end we always set the phase to "empty, empty"
                ps.phaseConfigurations.head should be((Set.empty, Set.empty))
            }
        }
    }
}

