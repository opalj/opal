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
    (0 to 12).foreach { i => pks(i) = PropertyKey.create[Null, Null]("p" + (i)) }

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

        describe("Eager Analysis Scheduling") {

            // Scenario 1: 2→1, 2⟂3, 1⟂3
            it("should correctly schedule eager analyses where one analysis depends on one other analysis and one analysis have no dependencies to the other two (ID.1)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                analysis1IsScheduledBeforeAnalysis2(schedule, eager1, eager2)
                analysis1IsScheduledBeforeOrAtTheSameBatchAsAnalysis2(
                    schedule = schedule,
                    analysis1 = eager1,
                    analysis2 = eager2
                )

                if (scheduleStrategy == 1) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 2: 2→1, 3→1, 2⟂3
            it("should correctly schedule eager analyses where two analyses depend on one analysis (ID.2)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.head.scheduled should contain(eager1)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 3: 1→2, 1→3, 2⟂3
            it("should correctly schedule eager analyses where one analysis depends on two others (ID.3)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.last.scheduled should contain(eager1)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 4: 2→1, 3→2, 1⟂3
            it("should correctly schedule eager analyses in a chain dependency (ID.4)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches(1).scheduled should contain(eager2)
                    schedule.batches.last.scheduled should contain(eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 5: 2→1, 3→2, 1→3
            it("should correctly schedule eager analyses with circular dependencies (ID.5)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 6: 2→1, 3→2, 3→1
            it("should correctly schedule eager analyses with multiple paths to same node (ID.6)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches(1).scheduled should contain(eager2)
                    schedule.batches.last.scheduled should contain(eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 7: No connections
            it("should correctly schedule eager analyses with no dependencies (ID.7)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 8: 1↔2, 2⟂3, 1⟂3
            it("should correctly handle bidirectional dependencies between two nodes (ID.8)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                analysis1IsScheduledInTheSameBatchAsAnalysis2(schedule, eager1, eager2)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 9: 1↔2, 3→1, 2⟂3
            it("should handle bidirectional dependency with additional incoming edge (ID.9)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)
                println(schedule.batches)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2)
                    schedule.batches.last.scheduled should contain(eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 10: 1↔2, 1↔3, 2⟂3
            it("should handle multiple bidirectional dependencies (ID.10)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 11: 1↔2, 1→3, 2⟂3
            it("should handle bidirectional dependency with outgoing edge (ID.11)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager3)
                    schedule.batches.last.scheduled should contain allOf (eager1, eager2)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 12: 1↔2, 3→2, 1→3
            it("should handle bidirectional dependency with complex additional connections (ID.12)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 13: 1↔2, 3→2, 1↔3
            it("should handle multiple bidirectional dependencies with incoming edge (ID.13)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 14: 1↔2, 2↔3, 1↔3 (Fully connected bidirectional graph)
            it("should handle fully connected bidirectional graph (ID.14)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(eager1, eager2, eager3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
        }
        describe("Lazy Analysis Scheduling") {

            // Scenario 15: 2→1, 2⟂3, 1⟂3
            it("should correctly schedule lazy analyses where one analysis depends on one other analysis and one analysis have no dependencies to the other two (ID.15)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 16: 2→1, 3→1, 2⟂3
            it("should correctly schedule lazy analyses where two analyses depend on one analysis (ID.16)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 17: 1→2, 1→3, 2⟂3
            it("should correctly schedule lazy analyses where one analysis depends on two others (ID.17)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 18: 2→1, 3→2, 1⟂3
            it("should correctly schedule lazy analyses in a chain dependency (ID.18)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 19: 2→1, 3→2, 1→3
            it("should correctly schedule lazy analyses with circular dependencies (ID.19)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 20: 2→1, 3→2, 3→1
            it("should correctly schedule lazy analyses with multiple paths to same node (ID.20)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 21: No connections
            it("should correctly schedule lazy analyses with no dependencies (ID.21)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 22: 1↔2, 2⟂3, 1⟂3
            it("should correctly handle bidirectional dependencies between two nodes (ID.22)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 23: 1↔2, 3→1, 2⟂3
            it("should handle bidirectional dependency with additional incoming edge (ID.23)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 24: 1↔2, 1↔3, 2⟂3
            it("should handle multiple bidirectional dependencies (ID.24)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 25: 1↔2, 1→3, 2⟂3
            it("should handle bidirectional dependency with outgoing edge (ID.25)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 26: 1↔2, 3→2, 1→3
            it("should handle bidirectional dependency with complex additional connections (ID.26)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 27: 1↔2, 3→2, 1↔3
            it("should handle multiple bidirectional dependencies with incoming edge (ID.27)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 28: 1↔2, 2↔3, 1↔3 (Fully connected bidirectional graph)
            it("should handle fully connected bidirectional graph (ID.28)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()

                val schedule = AnalysisScenario(Set(lazy1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }
        }
        describe("Combined Lazy and Eager Analysis Scheduling") {
            // Scenario 29: 1⟂2, 2⟂3, 3→1
            it("should correctly schedule combined analyses where a lazy analysis depends on an eager analysis with two disconnected nodes (ID.29)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 30: 1⟂2, 2⟂3, 1→3
            it("should correctly schedule combined analyses where an eager analysis depends on a lazy analysis with two disconnected nodes (ID.30)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 31: 1⟂2, 3→2, 1→3
            it("should correctly schedule combined analyses with mixed dependencies where one eager and one lazy analysis have dependencies (ID.31)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager2)
                    schedule.batches.last.scheduled should contain allOf (eager1, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 32: 1⟂2, 3→2, 3→1
            it("should correctly schedule combined analyses where a lazy analysis has multiple dependencies on eager analyses (ID.32)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.last.scheduled should contain(lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 33: 2→1, 3→2, 3⟂1
            it("should correctly schedule combined analyses with a chain dependency where the last node is disconnected (ID.33)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain(lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 34: 1→2, 3→2, 3⟂1
            it("should correctly schedule combined analyses where two nodes depend on one node with one disconnected relationship (ID.34)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                    schedule.batches.head.scheduled should contain(eager2)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 35: 2→1, 2→3, 3⟂1
            it("should correctly schedule combined analyses where one eager analysis has multiple dependents with one disconnected relationship (ID.35)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)
                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 36: 1→2, 2→3, 3⟂1
            it("should correctly schedule combined analyses in a chain with one disconnected relationship (ID.36)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)
                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain allOf (eager2, lazy3)
                    schedule.batches.last.scheduled should contain(eager1)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 37: 1⟂2, 2→3, 1→3
            it("should correctly schedule combined analyses where two analyses depend on a lazy analysis with one disconnected relationship (ID.37)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 38: 1⟂2, 2→3, 3→1
            it("should correctly schedule combined analyses with cyclic dependency and one disconnected relationship (ID.38)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 39: 1⟂2, 2↔3, 3→1
            it("should correctly schedule combined analyses with bidirectional dependency and one dependent eager analysis (ID.39)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 40: 1⟂2, 2↔3, 3↔1
            it("should correctly schedule combined analyses with multiple bidirectional dependencies and one disconnected node (ID.40)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 41: 2→1, 2↔3, 3→1
            it("should correctly schedule combined analyses where one eager analysis has both unidirectional and bidirectional dependencies (ID.41)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 42: 1→2, 2↔3, 3→1
            it("should correctly schedule combined analyses with cyclic dependency including a bidirectional relationship (ID.42)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 43: 1→2, 2↔3, 3↔1
            it("should correctly schedule combined analyses with mixed unidirectional and bidirectional dependencies in a cycle (ID.43)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 44: 2→1, 2↔3, 3↔1
            it("should correctly schedule combined analyses with multiple bidirectional dependencies and one unidirectional dependency (ID.44)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, eager2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 45: 1⟂2, 2⟂3, 3→1
            it("should correctly schedule combined analyses where a lazy analysis depends on an eager analysis with two lazy disconnected nodes (ID.45)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 46: 1⟂2, 2⟂3, 1→3
            it("should correctly schedule combined analyses where an eager analysis depends on a lazy analysis with two other lazy disconnected nodes (ID.46)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            // Scenario 47: 1⟂2, 3→2, 1→3
            it("should correctly schedule combined analyses where an eager analysis depends on a lazy analysis and another lazy analysis depends on a lazy node (ID.47)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 48: 1⟂2, 3→2, 3→1
            it("should correctly schedule combined analyses where a lazy analysis has dependencies on both eager and lazy analyses (ID.48)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 49: 2→1, 3→2, 3⟂1
            it("should correctly schedule combined analyses with a chain dependency where a lazy analysis depends on another lazy analysis which depends on an eager analysis (ID.49)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 50: 1→2, 3→2, 3⟂1
            it("should correctly schedule combined analyses where an eager analysis depends on a lazy analysis and another lazy analysis has a disconnected relationship (ID.50)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 51: 2→1, 2→3, 3⟂1
            it("should correctly schedule combined analyses where a lazy analysis has dependencies on an eager analysis and another lazy analysis (ID.51)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 52: 1→2, 2→3, 3⟂1
            it("should correctly schedule combined analyses where an eager analysis depends on a lazy analysis which depends on another lazy analysis (ID.52)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 53: 1⟂2, 2→3, 1→3
            it("should correctly schedule combined analyses where an eager analysis and a lazy analysis depend on another lazy analysis (ID.53)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 54: 1⟂2, 2→3, 3→1
            it("should correctly schedule combined analyses where a lazy analysis depends on an eager analysis and another lazy analysis has a dependency (ID.54)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 55: 1⟂2, 2↔3, 3→1
            it("should correctly schedule combined analyses with bidirectional dependency between lazy analyses and one dependent eager analysis (ID.55)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 56: 1⟂2, 2↔3, 3↔1
            it("should correctly schedule combined analyses with multiple bidirectional dependencies between eager and lazy analyses (ID.56)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 57: 2→1, 2↔3, 3→1
            it("should correctly schedule combined analyses where a lazy analysis has both unidirectional and bidirectional dependencies with an eager analysis (ID.57)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                    schedule.batches.head.scheduled should contain(eager1)
                    schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 58: 1→2, 2↔3, 3→1
            it("should correctly schedule combined analyses with cyclic dependency including a bidirectional relationship between eager and lazy analyses (ID.58)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 59: 1→2, 2↔3, 3↔1
            it("should correctly schedule combined analyses with mixed unidirectional and bidirectional dependencies between eager and lazy analyses in a cycle (ID.59)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 60: 2→1, 2↔3, 3↔1
            it("should correctly schedule combined analyses with multiple bidirectional dependencies between lazy analyses and one unidirectional dependency to an eager analysis (ID.60)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                    schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }
        }
        describe("Multiple Analysis Cycles") {
            // Scenario 61: 1→2, 2→3, 3→4, 4→5, 5→1, 6→5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule eager analyses with multiple cyclic dependencies and branching paths (ID.61)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )
                val eager7 = BasicComputationSpecification(
                    "eager7",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(7)))
                )
                val eager8 = BasicComputationSpecification(
                    "eager8",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val eager10 = BasicComputationSpecification(
                    "eager10",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager1, eager2, eager3, eager4, eager5, eager6, eager7, eager8, eager9, eager10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 62: 1→2, 2→3, 3→4, 4→5, 5→1, 6→5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule lazy analyses with multiple cyclic dependencies and branching paths (ID.62)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )
                val lazy4 = BasicComputationSpecification(
                    "lazy4",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesLazily = Option(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val lazy6 = BasicComputationSpecification(
                    "lazy6",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val lazy7 = BasicComputationSpecification(
                    "lazy7",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val lazy9 = BasicComputationSpecification(
                    "lazy9",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesLazily = Option(PropertyBounds.lub(pks(9)))
                )
                val lazy10 = BasicComputationSpecification(
                    "lazy10",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesLazily = Option(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(lazy1, lazy2, lazy3, lazy4, lazy5, lazy6, lazy7, lazy8, lazy9, lazy10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 63: 1→2, 2→3, 3→4, 4→5, 5→1, 6↔5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule eager analyses with mixed unidirectional and bidirectional dependencies in complex cycles (ID.63)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )
                val eager7 = BasicComputationSpecification(
                    "eager7",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(7)))
                )
                val eager8 = BasicComputationSpecification(
                    "eager8",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val eager10 = BasicComputationSpecification(
                    "eager10",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager1, eager2, eager3, eager4, eager5, eager6, eager7, eager8, eager9, eager10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }

            // Scenario 64: 1→2, 2→3, 3→4, 4→5, 5→1, 6↔5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule lazy analyses with mixed unidirectional and bidirectional dependencies in complex cycles (ID.64)") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )
                val lazy4 = BasicComputationSpecification(
                    "lazy4",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesLazily = Option(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(6))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val lazy6 = BasicComputationSpecification(
                    "lazy6",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val lazy7 = BasicComputationSpecification(
                    "lazy7",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val lazy9 = BasicComputationSpecification(
                    "lazy9",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesLazily = Option(PropertyBounds.lub(pks(9)))
                )
                val lazy10 = BasicComputationSpecification(
                    "lazy10",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesLazily = Option(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(lazy1, lazy2, lazy3, lazy4, lazy5, lazy6, lazy7, lazy8, lazy9, lazy10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            // Scenario 65: 1→2, 2→3, 3→4, 4→5, 5→1, 6→5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule eager analyses with mixed unidirectional and bidirectional dependencies in complex cycles (ID.65)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(10))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )
                val eager7 = BasicComputationSpecification(
                    "eager7",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(7)))
                )
                val eager8 = BasicComputationSpecification(
                    "eager8",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val eager10 = BasicComputationSpecification(
                    "eager10",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager1, eager2, eager3, eager4, eager5, eager6, eager7, eager8, eager9, eager10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }
            // Scenario 66: 1→2, 2→3, 2→5,// 3→4, 4→5, 5→1, 6→5, 6→10, 10→9, 9→8, 8→7, 7→6
            it("should correctly schedule eager analyses with mixed unidirectional and bidirectional dependencies in complex cycles (ID.66)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(10))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )
                val eager7 = BasicComputationSpecification(
                    "eager7",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(7)))
                )
                val eager8 = BasicComputationSpecification(
                    "eager8",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val eager10 = BasicComputationSpecification(
                    "eager10",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(9))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager1, eager2, eager3, eager4, eager5, eager6, eager7, eager8, eager9, eager10),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }

            }
        }
        describe("Combined Lazy, Transformer, Triggered and Eager Analysis Scheduling") {
            it("should correctly schedule this mix of analysis with cycles (ID.67)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two cycles (ID.68)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 5
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two cycles (ID.69)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 5
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with four derives collaboratory analysiscycles (ID.70)") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(1)))
                )
                val triggered3 = BasicComputationSpecification(
                    "triggered3",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(1)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(6))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(1)))
                )
                val transformer5 = BasicComputationSpecification(
                    "transformer5",
                    Transformer,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager1, eager2, triggered3, triggered4, transformer5, eager6),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with one cycle (ID.71)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 7
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with one cycle (ID.72)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 7
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two cycles (ID.73)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(10)), PropertyBounds.lub(pks(6))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 5
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with three derives collaboratory analysis (ID.74)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(6))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val triggered3 = BasicComputationSpecification(
                    "triggered3",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(5))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val transformer5 = BasicComputationSpecification(
                    "transformer5",
                    Transformer,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(triggered1, lazy2, triggered3, eager4, transformer5, eager6),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 2
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two cycles (ID.75)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val transformer9 = BasicComputationSpecification(
                    "transformer9",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesLazily = Option(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val transformer11 = BasicComputationSpecification(
                    "transformer11",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        transformer9,
                        triggered10,
                        transformer11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 4
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two cycles (ID.76)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val triggered9 = BasicComputationSpecification(
                    "triggered9",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        triggered9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 4
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with four cycles (ID.77)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(9))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(5)), PropertyBounds.lub(pks(6))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val triggered4 = BasicComputationSpecification(
                    "triggered4",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )
                val lazy5 = BasicComputationSpecification(
                    "lazy5",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val transformer6 = BasicComputationSpecification(
                    "transformer6",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(6)))
                )
                val transformer7 = BasicComputationSpecification(
                    "transformer7",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(9)), PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(7)))
                )
                val lazy8 = BasicComputationSpecification(
                    "lazy8",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(10))),
                    derivesLazily = Option(PropertyBounds.lub(pks(8)))
                )
                val eager9 = BasicComputationSpecification(
                    "eager9",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(11))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(9)))
                )
                val triggered10 = BasicComputationSpecification(
                    "triggered10",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(12))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(10)))
                )
                val triggered11 = BasicComputationSpecification(
                    "triggered11",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(7))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(11)))
                )
                val eager12 = BasicComputationSpecification(
                    "eager12",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(8))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(12)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(
                        triggered1,
                        eager2,
                        eager3,
                        triggered4,
                        lazy5,
                        transformer6,
                        transformer7,
                        lazy8,
                        eager9,
                        triggered10,
                        triggered11,
                        eager12
                    ),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("should correctly schedule this mix of analysis with two derives collaboratory analysis (ID.78)") {
                val triggered1 = BasicComputationSpecification(
                    "triggered1",
                    TriggeredComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(4)))
                )
                val transformer5 = BasicComputationSpecification(
                    "transformer5",
                    Transformer,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(5)))
                )
                val lazy6 = BasicComputationSpecification(
                    "lazy6",
                    LazyComputation,
                    uses = Set(),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(4)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(triggered1, lazy2, eager3, eager4, transformer5, lazy6),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
        }

        // TODO: Analyses to be assigned
        describe("non assigned tests - need to be documented and assigned") {
            // Scenario ?
            it("?") {
                val lazy1 = BasicComputationSpecification(
                    "lazy1",
                    LazyComputation,
                    uses = Set(),
                    derivesLazily = Option(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2)), PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(lazy1, eager2, eager3, eager4), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 4
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            it("??") {
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val lazy2 = BasicComputationSpecification(
                    "lazy2",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(1)), PropertyBounds.lub(pks(3))),
                    derivesLazily = Option(PropertyBounds.lub(pks(2)))
                )
                val lazy3 = BasicComputationSpecification(
                    "lazy3",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesLazily = Option(PropertyBounds.lub(pks(3)))
                )
                val lazy4 = BasicComputationSpecification(
                    "lazy4",
                    LazyComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesLazily = Option(PropertyBounds.lub(pks(4)))
                )
                val eager5 = BasicComputationSpecification(
                    "eager5",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(5)))
                )
                val eager6 = BasicComputationSpecification(
                    "eager6",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(6)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager1, lazy2, lazy3, lazy4, eager5, eager6), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
            it("???") {
                val eager0 = BasicComputationSpecification(
                    "eager0",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(0)))
                )
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(0))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(1))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(Set(eager0, eager1, eager2, eager3, eager4), ps).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }

            it("????") {
                val eager0 = BasicComputationSpecification(
                    "eager0",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(0)))
                )
                val eager1 = BasicComputationSpecification(
                    "eager1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(2))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(1)))
                )
                val eager2 = BasicComputationSpecification(
                    "eager2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(3)), PropertyBounds.lub(pks(2))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val eager2_1 = BasicComputationSpecification(
                    "eager2_1",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val eager2_2 = BasicComputationSpecification(
                    "eager2_2",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(2))),
                    derivesCollaboratively = Set(PropertyBounds.lub(pks(2)))
                )
                val eager3 = BasicComputationSpecification(
                    "eager3",
                    EagerComputation,
                    uses = Set(PropertyBounds.lub(pks(4))),
                    derivesEagerly = Set(PropertyBounds.lub(pks(3)))
                )
                val eager4 = BasicComputationSpecification(
                    "eager4",
                    EagerComputation,
                    uses = Set(),
                    derivesEagerly = Set(PropertyBounds.lub(pks(4)))
                )

                val ps = new PropertyStoreConfigurationRecorder()
                val schedule = AnalysisScenario(
                    Set(eager0, eager1, eager2, eager2_1, eager2_2, eager3, eager4),
                    ps
                ).computeSchedule(ps)

                if (scheduleStrategy == 1) {
                    // TODO
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.1 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && !scheduleLazyTransformerInAllenBatches) {
                    // TODO
                } else if (scheduleStrategy == 3.2 && scheduleLazyTransformerInAllenBatches) {
                    // TODO
                }
            }
        }
    }
}
