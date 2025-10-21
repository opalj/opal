/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.fpcf.AnalysisScenario.AnalysisSchedulingStrategyKey
import org.opalj.fpcf.fixtures.PropertyStoreConfigurationRecorder
import org.opalj.fpcf.scheduling.IndependentPhaseMergeScheduling
import org.opalj.fpcf.scheduling.MaximumPhaseScheduling
import org.opalj.fpcf.scheduling.SchedulingStrategy
import org.opalj.fpcf.scheduling.SinglePhaseScheduling
import org.opalj.fpcf.scheduling.SmallestPhaseMergeScheduling
import org.opalj.log.GlobalLogContext

/**
 * Tests the property computations scheduler.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyComputationsSchedulerTest extends AnyFunSpec with Matchers with BeforeAndAfterEach {

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

        override def uses(ps: PropertyStore): Set[PropertyBounds] = uses
    }

    val pks: Array[PropertyKind] = new Array[PropertyKind](21)
    (0 to 20).foreach { i => pks(i) = PropertyKey.create[Null, Null]("p" + (i)) }

    val c1 = BasicComputationSpecification(
        "c1",
        EagerComputation,
        uses = Set.empty,
        derivesEagerly = Set(PropertyBounds.lub(pks(1)))
    )

    /**
     * Checks whether `analysis1` is scheduled before or in the same batch as `analysis2` within a given `schedule`.
     */
    def isNotScheduledAfter(
        schedule:  Schedule[Unit],
        analysis1: BasicComputationSpecification,
        analysis2: BasicComputationSpecification
    ): Unit = {
        var analysis1AlreadyComputed = false
        var analysis1phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis1)) if (!analysis1AlreadyComputed) analysis1AlreadyComputed = true
            else if (!analysis1AlreadyComputed)
                analysis1phase += 1
        }

        var analysis2AlreadyComputed = false
        var analysis2phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis2)) if (!analysis2AlreadyComputed) analysis2AlreadyComputed = true
            else if (!analysis2AlreadyComputed)
                analysis2phase += 1
        }

        if (analysis1phase > analysis2phase)
            fail(s"$analysis1 is not scheduled before or in the same batch as $analysis2!")
    }

    /**
     * Checks whether `analysis1` is scheduled before `analysis2` within a given `schedule`.
     */
    def isScheduledBefore(
        schedule:  Schedule[Unit],
        analysis1: BasicComputationSpecification,
        analysis2: BasicComputationSpecification
    ): Unit = {
        var analysis1AlreadyComputed = false
        var analysis1phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis1)) if (!analysis1AlreadyComputed) analysis1AlreadyComputed = true
            else if (!analysis1AlreadyComputed)
                analysis1phase += 1
        }

        var analysis2AlreadyComputed = false
        var analysis2phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis2)) if (!analysis2AlreadyComputed) analysis2AlreadyComputed = true
            else if (!analysis2AlreadyComputed)
                analysis2phase += 1
        }

        if (analysis1phase >= analysis2phase)
            fail(s"$analysis1 is not scheduled before $analysis2!")
    }

    /**
     * Checks whether `analysis1` and `analysis2` are scheduled in the same batch within a given `schedule`.
     */
    def isScheduledConcurrently(
        schedule:  Schedule[Unit],
        analysis1: BasicComputationSpecification,
        analysis2: BasicComputationSpecification
    ): Unit = {
        var analysis1AlreadyComputed = false
        var analysis1phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis1)) {
                if (!analysis1AlreadyComputed) {
                    analysis1AlreadyComputed = true
                }
            } else if (!analysis1AlreadyComputed) {
                analysis1phase += 1
            }
        }

        var analysis2AlreadyComputed = false
        var analysis2phase = 0
        schedule.batches.foreach { batch =>
            if (batch.scheduled.contains(analysis2)) {
                if (!analysis2AlreadyComputed) {
                    analysis2AlreadyComputed = true
                }
            } else if (!analysis2AlreadyComputed) {
                analysis2phase += 1
            }
        }

        if (analysis1phase != analysis2phase)
            fail(s"$analysis1 is not scheduled in the same batch as $analysis2!")

    }

    def setupPropertyStoreConfigurationRecorder(
        schedulingStrategy: SchedulingStrategy
    ): PropertyStoreConfigurationRecorder = {
        val config = BaseConfig.withValue(
            AnalysisSchedulingStrategyKey,
            ConfigValueFactory.fromAnyRef(schedulingStrategy.getClass.getName)
        )
        new PropertyStoreConfigurationRecorder() {
            override val ctx: Map[Class[?], AnyRef] = Map(classOf[Config] -> config)
        }
    }

    // **********************************************************************************************
    //
    // TESTS

    for {
        strategy <- Seq(
            SinglePhaseScheduling,
            MaximumPhaseScheduling,
            IndependentPhaseMergeScheduling,
            SmallestPhaseMergeScheduling
        )
    } {

        describe(s"an AnalysisScenario with strategy $strategy") {

            it("should be able to create an empty schedule") {
                val ps = setupPropertyStoreConfigurationRecorder(strategy)
                val batches = AnalysisScenario(Set(), ps).computeSchedule(ps).batches
                batches should be(Symbol("Empty"))
            }

            it("should produce an empty configuration in an empty scenary") {
                val ps = setupPropertyStoreConfigurationRecorder(strategy)
                val scenario = AnalysisScenario(Set.empty, ps)
                val schedule = scenario.computeSchedule(ps)
                /*smoke test: */
                schedule(ps, trace = false)
                schedule.batches should be(Symbol("Empty"))
                ps.phaseConfigurations.head should be((Set.empty, Set.empty, Map.empty))
            }

            it("should create a valid schedule when scheduling mixed property computations") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(10)))
                )
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1))),
                    uses = Set(PropertyBounds.lub(pks(10)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(13))),
                    uses = Set(PropertyBounds.lub(pks(1)))
                )
                val transformer1 =
                    BasicComputationSpecification(
                        "transformer1",
                        Transformer,
                        derivesLazily = Option(PropertyBounds.lub(pks(7))),
                        uses = Set(PropertyBounds.finalP(pks(1)))
                    )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(13))),
                    uses = Set(PropertyBounds.lub(pks(7)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(13)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(11))),
                    uses = Set(PropertyBounds.lub(pks(13)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(12))),
                    uses = Set(PropertyBounds.lub(pks(13)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(2))),
                    uses = Set(PropertyBounds.lub(pks(12)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3))),
                    uses = Set(PropertyBounds.lub(pks(13)))
                )
                val triggered1 =
                    BasicComputationSpecification(
                        "triggered1",
                        TriggeredComputation,
                        derivesEagerly = Set(PropertyBounds.lub(pks(4))),
                        uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(6)))
                    )
                val triggered2 =
                    BasicComputationSpecification(
                        "triggered2",
                        TriggeredComputation,
                        derivesEagerly = Set(PropertyBounds.lub(pks(5))),
                        uses = Set(PropertyBounds.lub(pks(4)))
                    )
                val triggered3 =
                    BasicComputationSpecification(
                        "triggered3",
                        TriggeredComputation,
                        derivesEagerly = Set(PropertyBounds.lub(pks(6))),
                        uses = Set(PropertyBounds.lub(pks(5)))
                    )

                val ps = setupPropertyStoreConfigurationRecorder(strategy)
                val scenario = AnalysisScenario(
                    Set(
                        eager1,
                        eager3,
                        eager4,
                        eager5,
                        eager6,
                        eager2,
                        lazy1,
                        lazy2,
                        lazy3,
                        transformer1,
                        triggered1,
                        triggered2,
                        triggered3
                    ),
                    ps
                )
                val schedule = scenario.computeSchedule(ps)

                isNotScheduledAfter(schedule, lazy1, eager1)
                isNotScheduledAfter(schedule, eager1, eager4)
                isNotScheduledAfter(schedule, eager1, eager5)
                isNotScheduledAfter(schedule, eager1, eager6)
                isNotScheduledAfter(schedule, eager1, transformer1)
                isNotScheduledAfter(schedule, eager4, lazy3)
                isNotScheduledAfter(schedule, eager4, eager2)
                isNotScheduledAfter(schedule, eager6, lazy2)
                isNotScheduledAfter(schedule, eager6, lazy2)
                isNotScheduledAfter(schedule, eager6, lazy3)
                isNotScheduledAfter(schedule, eager3, triggered1)
                isNotScheduledAfter(schedule, eager3, triggered2)
                isNotScheduledAfter(schedule, eager3, triggered3)
                isScheduledConcurrently(schedule, triggered1, triggered2)
                isScheduledConcurrently(schedule, triggered1, triggered3)
            }
        }
    }
}
