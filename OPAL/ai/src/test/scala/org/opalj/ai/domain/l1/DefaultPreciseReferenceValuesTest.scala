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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * This test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of the methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultPreciseReferenceValuesTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    val domain = new DefaultConfigurableDomain("Reference Values Tests") {

    }
    import domain._

    val File = ObjectType("java/io/File")

    // Helper object to match against Sets which contain one element
    object Set1 {
        def unapply[T](s: Set[T]): Option[T] =
            if (s.size == 1) Some(s.head) else None
    }

    val ref1 = ObjectValue(444, No, true, ObjectType.Object)

    val ref1Alt = ObjectValue(444, No, true, ObjectType.Object)

    val ref2 = ObjectValue(668, No, true, File)

    val ref2Alt = ObjectValue(668, No, true, File)

    val ref3 = ObjectValue(732, No, true, File)

    val ref1MergeRef2 = ref1.join(-1, ref2).value

    val ref1AltMergeRef2Alt = ref1Alt.join(-1, ref2Alt).value

    val ref1MergeRef2MergeRef3 = ref1MergeRef2.join(-1, ref3).value

    val ref3MergeRef1MergeRef2 = ref3.join(-1, ref1MergeRef2).value

    //
    // TESTS
    //

    behavior of "the domain that models reference values at the type level"

    it should ("be able to handle upper bounds updates") in {

        val theObject = ObjectValue(-1, No, false, ObjectType.Object)
        val theFile = ObjectValue(-1, No, false, File)

        val update1 = theObject.refineUpperTypeBound(-1, File)
        update1.upperTypeBound.first should be(File)
        val update2 = theFile.refineUpperTypeBound(-1, File)
        update2.upperTypeBound.first should be(File)
        val update3 = theFile.refineUpperTypeBound(-1, ObjectType.Object)
        update3.upperTypeBound.first should be(File)
    }

    it should ("be able to create an ObjectValue with the expected values") in {
        val ref = ReferenceValue(444, No, true, ObjectType.Object)
        ref1 match {
            case `ref` ⇒ // OK
            case v     ⇒ fail("expected: "+ref1+";actual: "+v)
        }
    }

    it should ("keep the old value when we merge a value with an identical value") in {
        ref1.join(-1, ref1Alt) should be(NoUpdate)
    }

    it should ("represent both values after a merge of two independent value") in {
        val IsReferenceValue(values) = typeOfValue(ref1MergeRef2)
        values.exists(_ == ref1) should be(true)
        values.exists(_ == ref2) should be(true)
    }

    it should ("represent all three values when we merge \"some value\" with another \"value\"") in {
        val IsReferenceValue(values) = typeOfValue(ref1MergeRef2MergeRef3)
        values.exists(_ == ref1) should be(true)
        values.exists(_ == ref2) should be(true)
        values.exists(_ == ref3) should be(true)
    }

    it should ("be able to merge two value sets that contain equal values") in {
        val IsReferenceValue(values312) = typeOfValue(ref3MergeRef1MergeRef2)
        val IsReferenceValue(values123) = typeOfValue(ref1MergeRef2MergeRef3)
        values312.toSet should be(values123.toSet)
    }

    it should ("be able to merge two value sets where the original set is a superset of the second set") in {
        ref1MergeRef2MergeRef3.join(-1, ref1AltMergeRef2Alt) should be(NoUpdate)
    }

    it should ("be able to merge two value sets where the original set is a subset of the second set") in {
        ref1AltMergeRef2Alt.join(-1, ref1MergeRef2MergeRef3) should be(StructuralUpdate(ref1MergeRef2MergeRef3))
    }

    it should ("calculate a meaningful upper type bound if I have multiple different types of reference values") in {
        val nullRef = NullValue(444)
        summarize(-1, List(ref1, nullRef, ref2)) should be(ObjectValue(-1, Unknown, false, ObjectType.Object))
    }

    it should ("be able to handle the case where we throw a \"null\" value or some other value") in {
        import language.existentials
        
        val classFiles = reader.Java8Framework.ClassFiles(
            TestSupport.locateTestResources("classfiles/cornercases.jar", "ai"))
        val classFile = classFiles.find(_._1.thisType.fqn == "cornercases/ThrowsNullValue").get._1
        val method = classFile.methods.find(_.name == "main").get
       
        val exception = BaseAI(classFile, method, domain).operandsArray(20)
        domain.refIsNull(exception.head) should be (No)
    }

}
