/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach

import org.opalj.log.GlobalLogContext

/**
 * Tests the propety computations scheduler
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyComputationsSchedulerTest extends FunSpec with Matchers with BeforeAndAfterEach {

    implicit val logContext = GlobalLogContext

    val pks: Array[PropertyKeyID] = new Array[PropertyKeyID](11)
    (0 to 10).foreach { i ⇒
        pks(i) = PropertyKey.create[Null](
            "p"+(i),
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        ).id
    }

    val c1 = new ComputationSpecification(
        "c1",
        Set.empty,
        Set(pks(1), pks(2))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c2 = new ComputationSpecification(
        "c2",
        Set(pks(2), pks(3)),
        Set(pks(4))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c3 = new ComputationSpecification(
        "c3",
        Set.empty,
        Set(pks(3))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c4 = new ComputationSpecification(
        "c4",
        Set(pks(1)),
        Set(pks(5))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c5 = new ComputationSpecification(
        "c5",
        Set(pks(3), pks(4)),
        Set(pks(0))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c6 = new ComputationSpecification(
        "c6",
        Set(pks(6), pks(8)),
        Set(pks(7))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c7 = new ComputationSpecification(
        "c7",
        Set(pks(5), pks(7), pks(8), pks(0)),
        Set(pks(6))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c8 = new ComputationSpecification(
        "c8",
        Set(pks(6)),
        Set(pks(8), pks(9))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    val c9 = new ComputationSpecification(
        "c9",
        Set(pks(9)),
        Set(pks(10))
    ) { def schedule(ps: PropertyStore): Unit = ??? }

    //**********************************************************************************************
    //
    // TESTS

    describe("an AnalysisScenario") {
        it("should be possible to see the dependencies between the computations") {
            (new AnalysisScenario(Set(c6, c7, c8))).propertyComputationsDependencies
        }
    }

    describe("the scheduling of property computations") {

        it("should be possible to create an empty schedule") {
            val batches = (new AnalysisScenario(Set())).computeSchedule.batches
            batches should be('empty)
        }

        it("should be possible to create a schedule with one computation") {
            val batches = (new AnalysisScenario(Set(c1))).computeSchedule.batches
            batches.size should be(1)
            batches.head.head should be(c1)
        }

        it("should be possible to create a schedule with two independent computations") {
            val batches = (new AnalysisScenario(Set(c1, c3))).computeSchedule.batches
            batches.size should be(2)
            batches.foreach(_.size should be(1))
            batches.flatMap(batch ⇒ batch).toSet should be(Set(c1, c3))
        }

        it("should be possible to create a schedule where not all properties are explicitly derived") {
            val batches = (new AnalysisScenario(Set(c1, c2))).computeSchedule.batches
            batches.size should be(2)
            batches.foreach(_.size should be(1))
            batches.head.head should be(c1)
            batches.tail.head.head should be(c2)
        }

        it("should be possible to create a schedule where all computations depend on each other") {
            val batches = (new AnalysisScenario(Set(c6, c7, c8))).computeSchedule.batches
            batches.size should be(1)
            batches.head.toSet should be(Set(c6, c7, c8))
        }

        it("should be possible to create a complex schedule") {
            val schedule = (new AnalysisScenario(Set(c1, c2, c3, c4, c5, c6, c7, c8, c9))).computeSchedule
            schedule.batches.take(5).flatMap(batch ⇒ batch).toSet should be(Set(c1, c2, c3, c4, c5))
            schedule.batches.drop(5).head.toSet should be(Set(c6, c7, c8))
        }
    }

}
