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
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution

import org.opalj.bi.TestSupport.locateTestResources

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.collection.mutable.Locals

/**
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultReferenceValuesTest extends FunSpec with Matchers with ParallelTestExecution {

    object TheDomain extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with PredefinedClassHierarchy
            with PerInstructionPostProcessing
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultTypeLevelLongValues
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l1.DefaultReferenceValuesBinding // <- PRIMARY GOAL!
            // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultStringValuesBinding
            // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultClassValuesBinding
            // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
            with l1.DefaultIntegerRangeValues
            with l0.DefaultPrimitiveValuesConversions {

        override protected def maxCardinalityOfIntegerRanges: Long = 25l
    }

    import TheDomain._

    //
    // TESTS
    //

    describe("the DefaultReferenceValues domain") {

        //
        // FACTORY METHODS
        //

        describe("using the factory methods") {
            it("it should be possible to create a representation for a non-null object with a specific type") {
                val ref = ReferenceValue(444, No, true, ObjectType.Object)
                if (!ref.isNull.isNo || ref.origin != 444 || !ref.isPrecise)
                    fail("expected a precise, non-null reference value with pc 444;"+
                        " actual: "+ref)

            }
        }

        //
        // SIMPLE REFINEMENT
        //

        describe("refining a DomainValue that represents a reference value") {

            it("it should be able to update the upper bound") {

                val File = ObjectType("java/io/File")

                val theObject = ObjectValue(-1, No, false, ObjectType.Object)
                val theFile = ObjectValue(-1, No, false, File)

                val (update1, _) = theObject.refineUpperTypeBound(-1, File, List(theObject), Locals.empty)
                update1.head.asInstanceOf[ReferenceValue].upperTypeBound.first should be(File)
                val (update2, _) = theFile.refineUpperTypeBound(-1, File, List(theObject), Locals.empty)
                update2.head.asInstanceOf[ReferenceValue].upperTypeBound.first should be(File)
                val (update3, _) = theFile.refineUpperTypeBound(-1, ObjectType.Object, List(theFile), Locals.empty)
                update3.head.asInstanceOf[ReferenceValue].upperTypeBound.first should be(File)
            }

        }

        //
        // SUMMARIES
        //

        describe("the summarize function") {

            it("it should calculate a meaningful upper type bound given multiple different types of reference values") {
                summarize(
                    -1,
                    List(
                        ObjectValue(444, No, true, ObjectType.Object),
                        NullValue(444),
                        ObjectValue(668, No, true, ObjectType("java/io/File"))
                    )
                ) should be(ObjectValue(-1, Unknown, false, ObjectType.Object))
            }
        }

        //
        // JOIN
        //

        describe("joining two DomainValues that represent reference values") {

            val ref1 = ObjectValue(444, No, true, ObjectType.Object)

            val ref1Alt = ObjectValue(444, No, true, ObjectType.Object)

            val ref2 = ObjectValue(668, No, true, ObjectType.String)

            val ref2Alt = ObjectValue(668, No, true, ObjectType.String)

            val ref3 = ObjectValue(732, No, true, ObjectType.String)

            val ref1MergeRef2 = ref1.join(-1, ref2).value

            val ref1AltMergeRef2Alt = ref1Alt.join(-1, ref2Alt).value

            val ref1MergeRef2MergeRef3 = ref1MergeRef2.join(-1, ref3).value

            val ref3MergeRef1MergeRef2 = ref3.join(-1, ref1MergeRef2).value

            it("it should keep the old value when we merge a value with an identical value") {
                ref1.join(-1, ref1Alt) should be(NoUpdate)
            }

            it("it should represent both values after a merge of two independent values") {
                val IsReferenceValue(values) = typeOfValue(ref1MergeRef2)
                values.exists(_ == ref1) should be(true)
                values.exists(_ == ref2) should be(true)
            }

            it("it should represent all three values when we merge a MultipleReferenceValue with an ObjectValue if all three values are independent") {
                val IsReferenceValue(values) = typeOfValue(ref1MergeRef2MergeRef3)
                values.exists(_ == ref1) should be(true)
                values.exists(_ == ref2) should be(true)
                values.exists(_ == ref3) should be(true)
            }

            it("it should be able to merge two value sets that contain (reference) identical values") {
                val IsReferenceValue(values312) = typeOfValue(ref3MergeRef1MergeRef2)
                val IsReferenceValue(values123) = typeOfValue(ref1MergeRef2MergeRef3)
                values312.toSet should be(values123.toSet)
            }

            it("it should be able to merge two value sets where the original set is a superset of the second set") {
                // the values represent different values in time...
                val update = ref1MergeRef2MergeRef3.join(-1, ref1AltMergeRef2Alt)
                if (!update.isMetaInformationUpdate)
                    fail("expected: MetaInformationUpdate; actual: "+update)
            }

            it("it should be able to merge two value sets where the original set is a subset of the second set") {
                ref1AltMergeRef2Alt.join(-1, ref1MergeRef2MergeRef3) should be(StructuralUpdate(ref1MergeRef2MergeRef3))
            }
        }

        //
        // USAGE
        //

        describe("using the DefaultReferenceValues domain") {

            it("it should be able to handle the case where we throw a \"null\" value or some other value") {

                val classFiles = ClassFiles(locateTestResources("classfiles/cornercases.jar", "ai"))
                val classFile = classFiles.find(_._1.thisType.fqn == "cornercases/ThrowsNullValue").get._1
                val method = classFile.methods.find(_.name == "main").get

                val result = BaseAI(classFile, method, TheDomain)
                val exception = result.operandsArray(20)
                TheDomain.refIsNull(-1, exception.head) should be(No)
            }

            val theProject = Project(locateTestResources("classfiles/ai.jar", "ai"))
            val targetType = ObjectType("ai/domain/ReferenceValuesFrenzy")
            val ReferenceValues = theProject.classFile(targetType).get

            //            it("it should be possible to get precise information about a method's return values (maybeNull)") {
            //                val result = BaseAI(ReferenceValues, method, TheDomain)
            //                val exception = result.operandsArray(20)
            //                TheDomain.refIsNull(exception.head) should be(No)
            //            }
            //
            //            it("it should be able to correctly distinguish different values that were created at different points in time.") {
            //
            //            }
        }
    }
}
