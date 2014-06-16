/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai
package domain
package li

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.ObjectType

/**
 * This test(suite) tests various aspects related to the handling of integer values.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultPreciseIntegerValuesTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import DefaultPreciseIntegerValuesTest._
    val TestDomain = new DefaultPreciseIntegerValuesTest.TestDomain

    //
    // TESTS
    //

    final val SomePC = Int.MinValue

    behavior of "the Precise Integer Values domain"

    it should ("be able to join a value with itself") in {
        import TestDomain._
        val v = IntegerValue(SomePC, 0)
        v.join(-1, v) should be(NoUpdate)
    }

    it should ("be able to join two new values") in {
        import TestDomain._
        val v1 = IntegerValue(SomePC, 1)
        val v2 = IntegerValue(SomePC, 2)

        v1.join(SomePC, v2) should be(StructuralUpdate(TheIntegerValue(2, 1)))
        v2.join(SomePC, v1) should be(StructuralUpdate(TheIntegerValue(1, 1)))
    }

    it should ("be able to join two identical values without updating the update count") in {
        import TestDomain._
        val v1 = TheIntegerValue(5, 1 /*updates*/ )
        val v2 = TheIntegerValue(5, 1 /*updates*/ )

        v1.join(SomePC, v2) should be(NoUpdate)
        v2.join(SomePC, v1) should be(NoUpdate)
    }

    it should ("be able to join two identical values even if the update count is exceed") in {
        import TestDomain._
        val v1 = TheIntegerValue(5, 5 /*updates*/ )
        val v2 = TheIntegerValue(5, 5 /*updates*/ )

        v1.join(SomePC, v2) should be(NoUpdate)
        v2.join(SomePC, v1) should be(NoUpdate)
    }

    it should ("be able to join two values where one value was already updated the maximum number of times") in {
        import TestDomain._
        val v1 = TheIntegerValue(5, 5 /*updates*/ )
        val v2 = IntegerValue(SomePC, 10)

        v1.join(SomePC, v2) should be(StructuralUpdate(AnIntegerValue()))
        v2.join(SomePC, v1) should be(StructuralUpdate(AnIntegerValue()))
    }

    it should ("be able to compare two values for equality") in {
        import TestDomain._
        val vconst0 = IntegerConstant0
        val v0 = IntegerValue(SomePC, 0)
        val v1 = IntegerValue(SomePC, 1)

        intAreEqual(vconst0, v0) should be(Yes)
        intAreEqual(v0, vconst0) should be(Yes)

        intAreEqual(vconst0, v1) should be(No)
        intAreEqual(v1, vconst0) should be(No)

    }

    it should ("be able to join with AnIntegerValue") in {
        import TestDomain._
        val v1 = IntegerValue(0, 1)

        v1.join(-1, AnIntegerValue()) should be(StructuralUpdate(AnIntegerValue()))
        AnIntegerValue().join(-1, v1) should be(NoUpdate)
    }

    it should ("be able to analyze a method when we pass in concrete values") in {
        val domain = new DefaultPreciseIntegerValuesTest.TestDomain
        val method = ConditionalMath.findMethod("m1").get
        val result = BaseAI.perform(ConditionalMath, method, domain)(Some(IndexedSeq(domain.IntegerValue(-1, 100))))
        domain.allReturnedValues should not be (empty)
        domain.allReturnedValues.head should be((29, domain.IntegerValue(SomePC, 10)))
        // domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(175))
    }
}

object DefaultPreciseIntegerValuesTest {

    class TestDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultTypeLevelLongValues
            with l0.TypeLevelFieldAccessInstructions
            with l0.TypeLevelInvokeInstructions
            with l0.DefaultReferenceValuesBinding
            with DefaultPreciseIntegerValues
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with RecordLastReturnedValues
            with IgnoreSynchronization {

        type Id = String

        override def id = "DefaultPreciseIntegerValuesTest-Domain"

        override def maxUpdatesForIntegerValues: Long = 5

    }

    val testClassFileName = "classfiles/ai.jar"
    val testClassFile = org.opalj.br.TestSupport.locateTestResources(testClassFileName, "ai")
    val project = org.opalj.br.analyses.Project(testClassFile)
    val ConditionalMath = project.classFile(ObjectType("ai/domain/ConditionalMath")).get

}
