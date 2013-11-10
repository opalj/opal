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
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

/**
 * This test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of the methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultPreciseReferenceValuesTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    val domain = new PreciseConfigurableDomain("Reference Values Tests")
    import domain._

    // Helper object to match against Sets which contain one element
    object Set1 {
        def unapply[T](s: Set[T]): Option[T] =
            if (s.size == 1) Some(s.head) else None
    }

    val ref1 = AReferenceValue(444, ObjectType.Object, No, true)

    val ref1Alt = AReferenceValue(444, ObjectType.Object, No, true)

    val ref2 = AReferenceValue(668, ObjectType("java/io/File"), No, true)

    val ref2Alt = AReferenceValue(668, ObjectType("java/io/File"), No, true)

    val ref3 = AReferenceValue(732, ObjectType("java/io/File"), No, true)

    val ref1MergeRef2 =
        ref1.join(-1, ref2).value.asInstanceOf[MultipleReferenceValues]

    val ref1AltMergeRef2Alt =
        ref1Alt.join(-1, ref2Alt).value.asInstanceOf[MultipleReferenceValues]

    val ref1MergeRef2MergeRef3 =
        ref1MergeRef2.join(-1, ref3).value.asInstanceOf[MultipleReferenceValues]

    val ref3MergeRef1MergeRef2 =
        ref3.join(-1, ref1MergeRef2).value.asInstanceOf[MultipleReferenceValues]

    //
    // TESTS
    //

    behavior of "the domain that models reference values at the type level"

    it should ("be able to handle upper bounds updates") in {
        val File = ObjectType("java/io/File")
        val theObject = AReferenceValue(-1, ObjectType.Object, No, false)
        val theFile = AReferenceValue(-1, File, No, false)

        val update1 = theObject.addUpperBound(-1, File)
        update1.upperBound.head should be(File)
        val update2 = theFile.addUpperBound(-1, File)
        update2.upperBound.head should be(File)
        val update3 = theFile.addUpperBound(-1, ObjectType.Object)
        update3.upperBound.head should be(File)
    }

    it should ("be able to create an AReferenceValue with the expected values") in {
        ref1 match {
            case AReferenceValue(444, Set1(ObjectType.Object), No, true) ⇒ // OK
            case v ⇒ fail("expected: "+ref1+";actual: "+v)
        }
    }

    it should ("keep the old value when we merge a value with an identical value") in {
        ref1.join(-1, ref1Alt) should be(NoUpdate)
    }

    it should ("represent both values after a merge of two independent value") in {
        ref1MergeRef2.values should contain(ref1)
        ref1MergeRef2.values should contain(ref2)
    }

    it should ("represent all three values when we merge \"some value\" with another \"value\"") in {
        ref1MergeRef2MergeRef3.values should contain(ref1)
        ref1MergeRef2MergeRef3.values should contain(ref2)
        ref1MergeRef2MergeRef3.values should contain(ref3)
    }

    it should ("be able to merge two value sets that contain equal values") in {
        ref3MergeRef1MergeRef2.values should be(ref1MergeRef2MergeRef3.values)
    }

    it should ("be able to merge two value sets where the original set is a superset of the second set") in {
        ref1MergeRef2MergeRef3.join(-1, ref1AltMergeRef2Alt) should be(NoUpdate)
    }

    it should ("be able to merge two value sets where the original set is a subset of the second set") in {
        ref1AltMergeRef2Alt.join(-1, ref1MergeRef2MergeRef3) should be(StructuralUpdate(ref1MergeRef2MergeRef3))
    }

}
