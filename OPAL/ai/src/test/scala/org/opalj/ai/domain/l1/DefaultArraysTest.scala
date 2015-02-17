/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
import org.scalatest.ParallelTestExecution
import org.opalj.br.{ ObjectType, ArrayType }
import org.opalj.br.{ IntegerType, ByteType, ShortType }
import org.opalj.br.{ LongType, FloatType, DoubleType }
import org.opalj.br.{ BooleanType, CharType }
import org.opalj.bi.TestSupport.locateTestResources
import br.reader.Java8Framework.ClassFiles
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation
import org.opalj.br.instructions.ArrayLoadInstruction
import org.opalj.br.ComputationalType

/**
 * Tests the ArrayValues domain.
 *
 * @author Michael Eichberg
 * @author Christos Votskos
 */
@RunWith(classOf[JUnitRunner])
class DefaultArraysTest extends FunSpec with Matchers with ParallelTestExecution {

    import DefaultArraysTest._

    private def evaluateMethod(name: String, f: DefaultArraysTestDomain ⇒ Unit): Unit = {
        val domain = new DefaultArraysTestDomain()

        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(classFile, method, domain)

        dumpOnFailureDuringValidation(
            Some(classFile),
            Some(method),
            method.body.get,
            result) {
                f(domain)
            }
    }

    describe("array initializations") {

        it("should be able to analyze a simple int array initialization") {
            evaluateMethod("simpleIntArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val varray = allReturnedValues(21)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(IntegerType)) should be(Yes)

                arraylength(21, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(21, IntegerValue(4, 0), varray) should be(ComputedValue(IntegerValue(5, 1)))
                arrayload(21, IntegerValue(8, 1), varray) should be(ComputedValue(IntegerValue(9, 2)))
                arrayload(21, IntegerValue(12, 2), varray) should be(ComputedValue(IntegerValue(13, 3)))
                arrayload(21, IntegerValue(16, 3), varray) should be(ComputedValue(IntegerValue(17, 4)))

            })
        }

        it("should be able to analyze a simple byte array initialization") {
            evaluateMethod("simpleByteArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 21

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ByteType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(ByteValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(ByteValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(ByteValue(13, 3)))
                arrayload(returnIndex, IntegerValue(16, 3), varray) should be(ComputedValue(ByteValue(17, 4)))

            })
        }

        it("should be able to analyze a simple short array initialization") {
            evaluateMethod("simpleShortArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 21

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ShortType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(ShortValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(ShortValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(ShortValue(13, 3)))
                arrayload(returnIndex, IntegerValue(16, 3), varray) should be(ComputedValue(ShortValue(17, 4)))

            })
        }

        it("should be able to analyze a simple long array initialization") {
            evaluateMethod("simpleLongArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 27

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(LongType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(LongValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(LongValue(9, 2)))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(LongValue(15, 3)))
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(ComputedValue(LongValue(21, 4)))

            })
        }

        it("should be able to analyze a simple float array initialization") {
            evaluateMethod("simpleFloatArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 23

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(FloatType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(FloatValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(FloatValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(FloatValue(13, 3)))
                arrayload(returnIndex, IntegerValue(17, 3), varray) should be(ComputedValue(FloatValue(18, 4)))

            })
        }

        it("should be able to analyze a simple double array initialization") {
            evaluateMethod("simpleDoubleArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 27

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(DoubleType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(DoubleValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(DoubleValue(9, 2)))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(DoubleValue(15, 3)))
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(ComputedValue(DoubleValue(21, 4)))

            })
        }

        it("should be able to analyze a simple boolean array initialization") {
            evaluateMethod("simpleBooleanArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 13

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(BooleanType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                // Every boolean array is automatically filled with the value false
                // after declaration.
                // In the byte code instructions only the commands for storing the true
                // values at the appropriate index position are listed.
                // This is the reason why the false values have the same origin.
                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(BooleanValue(5, true)))
                arrayload(returnIndex, IntegerValue(3, 1), varray) should be(ComputedValue(BooleanValue(3, false)))
                arrayload(returnIndex, IntegerValue(8, 2), varray) should be(ComputedValue(BooleanValue(9, true)))
                arrayload(returnIndex, IntegerValue(3, 3), varray) should be(ComputedValue(BooleanValue(3, false)))

            })
        }

        it("should be able to analyze a simple char array initialization") {
            evaluateMethod("simpleCharArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 25

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(CharType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(CharValue(5, 'A')))
                arrayload(returnIndex, IntegerValue(9, 1), varray) should be(ComputedValue(CharValue(10, 'B')))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(CharValue(15, 'C')))
                arrayload(returnIndex, IntegerValue(19, 3), varray) should be(ComputedValue(CharValue(20, 'D')))

            })
        }

        it("should be able to analyze a simple string array initialization") {
            evaluateMethod("simpleStringArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 26

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ObjectType.String)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(5, 0), varray) should be(ComputedValue(StringValue(6, "A1")))
                arrayload(returnIndex, IntegerValue(10, 1), varray) should be(ComputedValue(StringValue(11, "B2")))
                arrayload(returnIndex, IntegerValue(15, 2), varray) should be(ComputedValue(StringValue(16, "C3")))
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(ComputedValue(StringValue(21, "D4")))

            })
        }

        it("should be able to analyze a simple object array initialization") {
            evaluateMethod("simpleObjectArrayInitializationWithLength4", domain ⇒ {
                import domain._

                val returnIndex = 46

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(
                    returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

            })
        }

        it("should be able to analyze a an object array initialization with different concrete types") {
            evaluateMethod("differentTypesInOneArrayInitialization", domain ⇒ {
                import domain._

                val returnIndex = 44

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(5)))

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(10, 0),
                        varray).result, ObjectType.Integer) should be(Yes)

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(17, 1),
                        varray).result, ObjectType.Float) should be(Yes)

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(26, 2),
                        varray).result, ObjectType.Double) should be(Yes)

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(33, 3),
                        varray).result, ObjectType.Boolean) should be(Yes)

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(41, 4),
                        varray).result, ObjectType.Character) should be(Yes)

                isValueSubtypeOf(
                    arrayload(returnIndex,
                        IntegerValue(41, 4),
                        varray).result, ObjectType.Character) should be(Yes)

                arrayload(
                    returnIndex,
                    IntegerValue(20, 2),
                    varray).result.computationalType.computationalTypeCategory.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(13, 1),
                    varray).result.computationalType.computationalTypeCategory.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(6, 0),
                    varray).result.computationalType.computationalTypeCategory.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(29, 3),
                    varray).result.computationalType.computationalTypeCategory.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(36, 4),
                    varray).result.computationalType.computationalTypeCategory.id should be(1)

            })
        }

    }

    describe("complex array operations") {
        it("should be able to analyze that every returned array is null") {
            evaluateMethod("setArrayNull", domain ⇒ {
                import domain._

                val returnIndex = 7

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(returnIndex, varray) should be(throws(InitializedObjectValue(-100007, ObjectType.NullPointerException)))

                arraystore(returnIndex, IntegerValue(20), IntegerValue(12, 0), varray) should be(ThrowsException(List(InitializedObjectValue(-100007, ObjectType.NullPointerException))))

                arrayload(returnIndex, IntegerValue(1), varray) should be(ThrowsException(List(InitializedObjectValue(-100007, ObjectType.NullPointerException))))

            })
        }

        it("should be able to analyze array initializations of different number types with different length") {
            evaluateMethod("branchInits", domain ⇒ {
                import domain._

                val returnIndex = 98

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueSubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

            })
        }

    }

}

class DefaultArraysTestDomain(
    override val maxCardinalityOfIntegerRanges: Long = -(Int.MinValue.toLong) + Int.MaxValue)
        extends CorrelationalDomain
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l1.DefaultLongValues
        with l1.DefaultStringValuesBinding
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l1.DefaultIntegerRangeValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with PredefinedClassHierarchy
        with RecordLastReturnedValues
        with DefaultArrayValuesBinding

private object DefaultArraysTest {
    val classFiles = ClassFiles(locateTestResources("classfiles/ai.jar", "ai"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithArrays").get
}
