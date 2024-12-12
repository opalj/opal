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

    val pks: Array[PropertyKind] = new Array[PropertyKind](12)
    (0 to 11).foreach { i => pks(i) = PropertyKey.create[Null, Null]("p" + (i)) }

    val c1 = BasicComputationSpecification(
        "c1",
        EagerComputation,
        uses = Set.empty,
        derivesEagerly = Set(PropertyBounds.lub(pks(1)))
    )

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

                if (scheduleStrategy == 1) {
                    schedule.batches should have size 1
                } else if (scheduleStrategy == 2 && !scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
                } else if (scheduleStrategy == 2 && scheduleLazyTransformerInAllenBatches) {
                    schedule.batches should have size 3
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

                println(schedule.batches)
                schedule.batches should have size 3
                schedule.batches.head.scheduled should contain(eager1)

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

                schedule.batches should have size 3
                schedule.batches.last.scheduled should contain(eager1)
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

                schedule.batches should have size 3
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches(1).scheduled should contain(eager2)
                schedule.batches.last.scheduled should contain(eager3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
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

                schedule.batches should have size 3
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches(1).scheduled should contain(eager2)
                schedule.batches.last.scheduled should contain(eager3)
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

                schedule.batches should have size 3
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

                schedule.batches should have size 2

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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain allOf (eager1, eager2)
                schedule.batches.last.scheduled should contain(eager3)

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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager3)
                schedule.batches.last.scheduled should contain allOf (eager1, eager2)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)

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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, eager3)

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

                schedule.batches should have size 2
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 3
                println(schedule.batches)
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

                schedule.batches should have size 2

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

                schedule.batches should have size 2
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)

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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (lazy1, lazy2, lazy3)

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

                schedule.batches should have size 3
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

                schedule.batches should have size 2
                println(schedule.batches)
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

                print(schedule.batches)
                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager2)
                schedule.batches.last.scheduled should contain allOf (eager1, lazy3)
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

                schedule.batches should have size 3
                schedule.batches.last.scheduled should contain(lazy3)
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

                schedule.batches should have size 3
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain(lazy3)
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

                schedule.batches should have size 3
                schedule.batches.head.scheduled should contain(eager2)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain allOf (eager2, lazy3)
                schedule.batches.last.scheduled should contain(eager1)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (eager2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, eager2, lazy3)
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

                schedule.batches should have size 3
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

                schedule.batches should have size 2
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 2
                schedule.batches.head.scheduled should contain(eager1)
                schedule.batches.last.scheduled should contain allOf (lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 1
                schedule.batches.head.scheduled should contain allOf (eager1, lazy2, lazy3)
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

                schedule.batches should have size 2
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

                schedule.batches should have size 1
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

                schedule.batches should have size 1

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

                schedule.batches should have size 1
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

                schedule.batches should have size 1

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

                schedule.batches should have size 2

            }
        }
        describe("Combined Lazy, Transformer, Triggered and Eager Analysis Scheduling") {
            it("should correctly schedule this mix of analysis with cycles (ID.67)") {}
            it("should correctly schedule this mix of analysis with two cycles (ID.68)") {}
            it("should correctly schedule this mix of analysis with two cycles (ID.69)") {}
            it("should correctly schedule this mix of analysis with four derives collaboratory analysiscycles (ID.70)") {}
            it("should correctly schedule this mix of analysis with one cycle (ID.71)") {}
            it("should correctly schedule this mix of analysis with one cycle (ID.72)") {}
            it("should correctly schedule this mix of analysis with two cycles (ID.73)") {}
            it("should correctly schedule this mix of analysis with three derives collaboratory analysis (ID.74)") {}
            it("should correctly schedule this mix of analysis with two cycles (ID.75)") {}
            it("should correctly schedule this mix of analysis with two cycles (ID.76)") {}
            it("should correctly schedule this mix of analysis with four cycles (ID.77)") {}
            it("should correctly schedule this mix of analysis with two derives collaboratory analysis (ID.78)") {}
        }

        describe("non assigned tests") {
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

                schedule.batches should have size 4
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

                schedule.batches should have size 3
            }
            it("???") {
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
                val schedule = AnalysisScenario(Set(eager1, eager2, eager3, eager4), ps).computeSchedule(ps)

                schedule.batches should have size 3
            }
        }
    }
}
