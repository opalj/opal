/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

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

    val domain = new DefaultConfigurableDomain("DefaultPreciseIntegerValuesTest")
    import domain._

    //
    // TESTS
    //

    behavior of "IntegerRange values"

    it should ("be able to join two identical values") in {
        val v = IntegerRange(0, 0)
        v.join(-1, v) should be(NoUpdate)
    }

    it should ("be able to join two overlapping values") in {
        val v1 = IntegerRange(0, 1)
        val v2 = IntegerRange(0, 2)

        v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(0, 2)))
        v2.join(-1, v1) should be(NoUpdate)

        val v3 = IntegerRange(-10, 10)
        //v3.join(-1, v1) should be(NoUpdate)
        v1.join(-1, v3) should be(StructuralUpdate(v3))

        val v4 = IntegerRange(1, 0)
        val v5 = IntegerRange(-3, -10)
        v4.join(-1, v5) should be(StructuralUpdate(IntegerRange(1, -10)))

        val v6 = IntegerRange(-3, -102)
        v4.join(-1, v6) should be(StructuralUpdate(AnIntegerValue())) // > SPREAD!
    }

    it should ("be able to join with AnIntegerValue") in {
        val v1 = IntegerRange(0, 1)

        v1.join(-1, AnIntegerValue()) should be(StructuralUpdate(AnIntegerValue()))
    }

    behavior of "AnIntegerValue"

    it should ("be able to adapt to the same domain") in {
        val v1 = AnIntegerValue()

        v1.adapt(domain, -1) should be(AnIntegerValue())
    }

}
