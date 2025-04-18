/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.fpcf.AnalysisScenario.AnalysisScheduleLazyTransformerInMultipleBatches
import org.opalj.fpcf.AnalysisScenario.AnalysisScheduleStrategy
import org.opalj.fpcf.fixtures.PropertyStoreConfigurationRecorder
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

    val pks: Array[PropertyKind] = new Array[PropertyKind](13)
    (0 to 20).foreach { i => pks(i) = PropertyKey.create[Null, Null]("p" + (i)) }

    val c1 = BasicComputationSpecification(
        "c1",
        EagerComputation,
        uses = Set.empty,
        derivesEagerly = Set(PropertyBounds.lub(pks(1)))
    )

    def analysis1IsScheduledBeforeOrAtTheSameBatchAsAnalysis2(
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

        if (analysis1phase <= analysis2phase) {
            println("Success")
        } else {
            fail(s"$analysis1 is not scheduled before or in the same batch as $analysis2!")
        }
    }

    def analysis1IsScheduledBeforeAnalysis2(
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

        if (analysis1phase < analysis2phase) {
            println("Success")
        } else {
            fail(s"$analysis1 is not scheduled before $analysis2!")
        }
    }

    def analysis1IsScheduledInTheSameBatchAsAnalysis2(
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

        if (analysis1phase == analysis2phase) {
            println("Success")
        } else {
            fail(s"$analysis1 is not scheduled in the same batch as $analysis2!")
        }
    }

    // **********************************************************************************************
    //
    // TESTS

    val scheduleStrategy: Int = BaseConfig.getInt(AnalysisScheduleStrategy)
    val scheduleLazyTransformerInAllenBatches: Boolean =
        BaseConfig.getBoolean(AnalysisScheduleLazyTransformerInMultipleBatches)

    describe("an AnalysisScenario") {

        it("should be possible to create an empty schedule") {
            val ps = new PropertyStoreConfigurationRecorder()
            val batches = AnalysisScenario(Set(), ps).computeSchedule(ps).batches
            batches should be(Symbol("Empty"))
        }

        describe("the scheduling of mixed eager and lazy property computations") {

            it("an empty analysis scenario should lead to an empty phase configuration in the ps") {
                val ps = new PropertyStoreConfigurationRecorder()
                val scenario = AnalysisScenario(Set.empty, ps)
                val schedule = scenario.computeSchedule(ps)
                /*smoke test: */
                schedule(ps, trace = false)
                schedule.batches should be(Symbol("Empty"))
                ps.phaseConfigurations.head should be((Set.empty, Set.empty, Map.empty))
            }
        }
    }
}
