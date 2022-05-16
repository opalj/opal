/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.BeforeAndAfterEach

import org.opalj.log.GlobalLogContext
import org.opalj.fpcf.fixtures.PropertyStoreConfigurationRecorder

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
    (0 to 11).foreach { i => pks(i) = PropertyKey.create[Null, Null]("p"+(i)) }

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
            val ps = new PropertyStoreConfigurationRecorder()
            val batches = AnalysisScenario(Set(), ps).computeSchedule(ps).batches
            batches should be(Symbol("Empty"))
        }

        describe("the scheduling of mixed eager and lazy property computations") {

            it("an empty analysis scenario should lead to an empty phase configuration in the ps") {
                val ps = new PropertyStoreConfigurationRecorder()
                val scenario = AnalysisScenario(Set.empty, ps)
                val schedule = scenario.computeSchedule(ps)
                /*smoke test: */ schedule(ps, trace = false)
                schedule.batches should be(Symbol("Empty"))
                ps.phaseConfigurations.head should be((Set.empty, Set.empty, Map.empty))
            }

        }
    }
}

