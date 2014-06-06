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
package l1

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

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

    object TestDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultReferenceValuesBinding
            with l0.TypeLevelFieldAccessInstructions
            with l0.TypeLevelInvokeInstructions
            with DefaultPreciseIntegerValues
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with PredefinedClassHierarchy {

        type Id = String
        
        def id = "TestDomain"
        
        override def maxUpdateCountForIntegerValues: Int = 2
    }
    import TestDomain._

    //
    // TESTS
    //

    final val SomePC = Int.MinValue

    behavior of "the Precise Integer Values domain"

    it should ("be able to join a value with itself") in {
        val v = IntegerValue(SomePC, 0)
        v.join(-1, v) should be(NoUpdate)
    }

    it should ("be able to join two new values") in {
        val v1 = IntegerValue(SomePC, 1)
        val v2 = IntegerValue(SomePC, 2)

        v1.join(SomePC, v2) should be(StructuralUpdate(TheIntegerValue(2, 1)))
        v2.join(SomePC, v1) should be(StructuralUpdate(TheIntegerValue(1, 1)))
    }

    it should ("be able to join two identical values without updating the update count") in {
        val v1 = TheIntegerValue(5, 1 /*updates*/ )
        val v2 = TheIntegerValue(5, 1 /*updates*/ )

        v1.join(SomePC, v2) should be(NoUpdate)
        v2.join(SomePC, v1) should be(NoUpdate)
    }

    it should ("be able to join two identical values even if the update count is exceed") in {
        val v1 = TheIntegerValue(5, 5 /*updates*/ )
        val v2 = TheIntegerValue(5, 5 /*updates*/ )

        v1.join(SomePC, v2) should be(NoUpdate)
        v2.join(SomePC, v1) should be(NoUpdate)
    }

    it should ("be able to join two values where one value was already updated the maximum number of times") in {
        val v1 = TheIntegerValue(5, 2 /*updates*/ )
        val v2 = IntegerValue(SomePC, 10)

        v1.join(SomePC, v2) should be(StructuralUpdate(AnIntegerValue()))
        v2.join(SomePC, v1) should be(StructuralUpdate(AnIntegerValue()))
    }

    it should ("be able to join with AnIntegerValue") in {
        val v1 = IntegerValue(0, 1)

        v1.join(-1, AnIntegerValue()) should be(StructuralUpdate(AnIntegerValue()))
        AnIntegerValue().join(-1, v1) should be(NoUpdate)
    }
}
