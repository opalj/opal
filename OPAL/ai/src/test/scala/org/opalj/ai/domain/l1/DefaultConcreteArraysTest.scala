/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.opalj.io.OpeningFileFailedException
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.ArrayType
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ObjectType
import org.opalj.br.ShortType
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation

/**
 * Tests the ArrayValues domain.
 *
 * @author Michael Eichberg
 * @author Christos Votskos
 */
@RunWith(classOf[JUnitRunner])
class DefaultConcreteArraysTest extends AnyFunSpec with Matchers {

    import org.opalj.ai.domain.l1.DefaultArraysTest._

    private def evaluateMethod(name: String)(f: DefaultConcreteArraysTestDomain => Unit): Unit = {
        val domain = new DefaultConcreteArraysTestDomain()

        val method = classFile.methods.find(_.name == name).get
        val code = method.body.get
        val result = BaseAI(method, domain)

        try {
            dumpOnFailureDuringValidation(Some(classFile), Some(method), code, result) {
                f(domain)
            }
        } catch {
            case t: OpeningFileFailedException => info(s"ignored ${t.toString}")
        }
    }

    describe("array initializations") {

        it("should be able to analyze a simple int array initialization") {
            evaluateMethod("simpleIntArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 21
                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(IntegerType)) should be(Yes)

                arraylength(21, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(21, IntegerValue(4, 0), varray) should be(ComputedValue(IntegerValue(5, 1)))
                arrayload(21, IntegerValue(8, 1), varray) should be(ComputedValue(IntegerValue(9, 2)))
                arrayload(21, IntegerValue(12, 2), varray) should be(ComputedValue(IntegerValue(13, 3)))
                arrayload(21, IntegerValue(16, 3), varray) should be(ComputedValue(IntegerValue(17, 4)))

            }
        }

        it("should be able to analyze a simple byte array initialization") {
            evaluateMethod("simpleByteArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 21

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ByteType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(ByteValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(ByteValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(ByteValue(13, 3)))
                arrayload(returnIndex, IntegerValue(16, 3), varray) should be(ComputedValue(ByteValue(17, 4)))

            }
        }

        it("should be able to analyze a simple short array initialization") {
            evaluateMethod("simpleShortArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 21

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ShortType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(ShortValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(ShortValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(ShortValue(13, 3)))
                arrayload(returnIndex, IntegerValue(16, 3), varray) should be(ComputedValue(ShortValue(17, 4)))

            }
        }

        it("should be able to analyze a simple long array initialization") {
            evaluateMethod("simpleLongArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 27

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(LongType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(LongValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(LongValue(9, 2)))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(LongValue(15, 3)))
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(ComputedValue(LongValue(21, 4)))

            }
        }

        it("should be able to analyze a simple float array initialization") {
            evaluateMethod("simpleFloatArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 23

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(FloatType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(FloatValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(FloatValue(9, 2)))
                arrayload(returnIndex, IntegerValue(12, 2), varray) should be(ComputedValue(FloatValue(13, 3)))
                arrayload(returnIndex, IntegerValue(17, 3), varray) should be(ComputedValue(FloatValue(18, 4)))

            }
        }

        it("should be able to analyze a simple double array initialization") {
            evaluateMethod("simpleDoubleArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 27

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(DoubleType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(DoubleValue(5, 1)))
                arrayload(returnIndex, IntegerValue(8, 1), varray) should be(ComputedValue(DoubleValue(9, 2)))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(DoubleValue(15, 3)))
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(ComputedValue(DoubleValue(21, 4)))

            }
        }

        it("should be able to analyze a simple boolean array initialization") {
            evaluateMethod("simpleBooleanArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 13

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(BooleanType)) should be(Yes)

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

            }
        }

        it("should be able to analyze a simple char array initialization") {
            evaluateMethod("simpleCharArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 25

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(CharType)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(4, 0), varray) should be(ComputedValue(CharValue(5, 'A')))
                arrayload(returnIndex, IntegerValue(9, 1), varray) should be(ComputedValue(CharValue(10, 'B')))
                arrayload(returnIndex, IntegerValue(14, 2), varray) should be(ComputedValue(CharValue(15, 'C')))
                arrayload(returnIndex, IntegerValue(19, 3), varray) should be(ComputedValue(CharValue(20, 'D')))

            }
        }

        it("should be able to analyze a simple string array initialization") {
            evaluateMethod("simpleStringArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 26

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.String)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(4)))

                arrayload(returnIndex, IntegerValue(5, 0), varray) should be(
                    ComputedValue(StringValue(6, "A1"))
                )
                arrayload(returnIndex, IntegerValue(10, 1), varray) should be(
                    ComputedValue(StringValue(11, "B2"))
                )
                arrayload(returnIndex, IntegerValue(15, 2), varray) should be(
                    ComputedValue(StringValue(16, "C3"))
                )
                arrayload(returnIndex, IntegerValue(20, 3), varray) should be(
                    ComputedValue(StringValue(21, "D4"))
                )
            }
        }

        it("should be able to analyze a simple object array initialization") {
            evaluateMethod("simpleObjectArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 46

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(
                    returnIndex, varray
                ) should be(ComputedValue(IntegerRange(4)))

            }
        }

        it("should be able to analyze a an object array initialization with different concrete types") {
            evaluateMethod("differentTypesInOneArrayInitialization") { domain =>
                import domain._

                val returnIndex = 44

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(returnIndex, varray) should be(ComputedValue(IntegerRange(5)))

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(10, 0),
                        varray
                    ).result, ObjectType.Integer
                ) should be(Yes)

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(17, 1),
                        varray
                    ).result, ObjectType.Float
                ) should be(Yes)

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(26, 2),
                        varray
                    ).result, ObjectType.Double
                ) should be(Yes)

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(33, 3),
                        varray
                    ).result, ObjectType.Boolean
                ) should be(Yes)

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(41, 4),
                        varray
                    ).result, ObjectType.Character
                ) should be(Yes)

                isValueASubtypeOf(
                    arrayload(
                        returnIndex,
                        IntegerValue(41, 4),
                        varray
                    ).result, ObjectType.Character
                ) should be(Yes)

                arrayload(
                    returnIndex,
                    IntegerValue(20, 2),
                    varray
                ).result.computationalType.category.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(13, 1),
                    varray
                ).result.computationalType.category.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(6, 0),
                    varray
                ).result.computationalType.category.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(29, 3),
                    varray
                ).result.computationalType.category.id should be(1)

                arrayload(
                    returnIndex,
                    IntegerValue(36, 4),
                    varray
                ).result.computationalType.category.id should be(1)

            }
        }

        it("should be able to analyze a simple 4-dimensional array initialization") {
            evaluateMethod("a4DimensionalArray") { domain =>
                import domain._
                val twoDimIntArray = ArrayType(ArrayType(IntegerType))
                val fourDimIntArray = ArrayType(ArrayType(twoDimIntArray))
                val operandsArray = domain.operandsArray

                // we are just testing the dimensions of the array property
                operandsArray(8).head should be(InitializedArrayValue(4, fourDimIntArray, 2))
            }
        }

        it("should be able to analyze a simple 2-dimensional array initialization") {
            evaluateMethod("a2DimensionalArray") { domain =>
                import domain._
                val twoDimIntArray = ArrayType(ArrayType(IntegerType))
                val operandsArray = domain.operandsArray

                operandsArray(18).head should be(InitializedArrayValue(2, twoDimIntArray, 2))
                operandsArray(31).head should be(InitializedArrayValue(2, twoDimIntArray, 2))
                operandsArray(35).head should be(InitializedArrayValue(2, twoDimIntArray, 2))
            }
        }

        it("should be able to analyze a simple 3-dimensional array initialization") {
            evaluateMethod("a3DimensionalArray") { domain =>
                import domain._
                val threeDimIntArray = ArrayType(ArrayType(ArrayType(IntegerType)))
                val operandsArray = domain.operandsArray

                operandsArray(20).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(36).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(40).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

            }
        }

        ignore("should be able to analyze a 3-dimensional array initialization with potential exceptions") {
            evaluateMethod("a3DimensionalArrayWithPotentialExceptions") { domain =>
                import domain._
                val threeDimIntArray = ArrayType(ArrayType(ArrayType(IntegerType)))
                val operandsArray = domain.operandsArray

                operandsArray(20).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(28).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(32).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(48).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(64).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )

                operandsArray(68).head should be(
                    InitializedArrayValue(3, threeDimIntArray, 2)
                )
            }
        }
    }

    describe("array accesses that lead to exceptions") {

        it("if an index is out of bounds a corresponding exception should be thrown even if the store is potentially impossible") {
            evaluateMethod("simpleByteArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 21

                val varray = allReturnedValues(returnIndex)
                val expectedException =
                    ThrowsException(List(
                        InitializedObjectValue(-100021, ObjectType.ArrayIndexOutOfBoundsException)
                    ))

                allReturnedValues.size should be(1)
                arrayload(returnIndex, IntegerValue(returnIndex, 4), varray) should be(expectedException)
                arrayload(returnIndex, IntegerValue(returnIndex, -1), varray) should be(expectedException)
                arraystore(returnIndex, ByteValue(4), IntegerValue(returnIndex, 4), varray) should be(expectedException)
                arraystore(returnIndex, ByteValue(4), IntegerValue(returnIndex, -1), varray) should be(expectedException)
                arraystore(returnIndex, LongValue(4), IntegerValue(returnIndex, 4), varray) should be(expectedException)
                arraystore(returnIndex, LongValue(4), IntegerValue(returnIndex, -1), varray) should be(expectedException)
            }
        }

        it("should lead to an array store exception if the value cannot be stored in the array") {
            evaluateMethod("simpleStringArrayInitializationWithLength4") { domain =>
                import domain._

                val returnIndex = 26
                val varray = allReturnedValues(returnIndex)
                val expectedException =
                    ThrowsException(List(InitializedObjectValue(-100026, ObjectType.ArrayStoreException)))

                // make sure the class hierarchy is as expected
                assert(domain.classHierarchy.isKnown(ObjectType.Class))
                assert(domain.classHierarchy.isKnown(ObjectType.String))
                assert(!isSubtypeOf(ObjectType.Class, ObjectType.String))
                assert(!isSubtypeOf(ObjectType.String, ObjectType.Class))

                val array = InitializedObjectValue(returnIndex, ObjectType.Class)
                val index = IntegerValue(returnIndex, 3)
                arraystore(returnIndex, array, index, varray) should be(expectedException)
            }
        }
    }

    describe("array stores") {

        ignore("should be able to analyze a method that updates a value stored in an array in a branch") {
            evaluateMethod("setValInBranch") { domain =>
                import domain._

                val returnIndex = 20
                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(IntegerType)) should be(Yes)
                arraylength(returnIndex, varray) should be(ComputedValue(IntegerValue(2)))

                // after array initialization all values should be 0
                val arrayRef3 = asArrayAbstraction(operandsArray(3).head)
                arrayRef3.load(3, IntegerValue(origin = 3, 0)) should be(ComputedValue(IntegerValue(origin = 3, 0)))
                arrayRef3.load(3, IntegerValue(origin = 3, 1)) should be(ComputedValue(IntegerValue(origin = 3, 0)))

                val arrayRef12 = asArrayAbstraction(localsArray(12)(1))
                arrayRef12.load(12, IntegerValue(origin = 9, 1)) should be(ComputedValue(IntegerValue(origin = 10, 1)))
                arrayRef12.load(12, IntegerValue(origin = 9, 0)) should be(ComputedValue(IntegerValue(origin = 10, 0)))

                val arrayRef19 = asArrayAbstraction(localsArray(19)(1))
                arrayRef19.load(19, IntegerValue(origin = 16, 1)) should be(ComputedValue(IntegerRange(origin = 17, 1, 2)))
                arrayRef19.load(19, IntegerValue(origin = 16, 0)) should be(ComputedValue(IntegerValue(origin = 17, 0)))

            }
        }

        ignore("should be able to detect a possible array store exception and the default array value") {
            evaluateMethod("arrayStoreException") { domain =>
                import domain._

                val returnIndex = 20

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.Cloneable)) should be(Yes)

                val returnedValue = arrayload(returnIndex, IntegerValue(returnIndex, 0), varray)
                val exceptions = List(ObjectType.ArrayStoreException)
                returnedValue should be(ComputedValueOrException(null, exceptions))
            }
        }
    }

    describe("complex array operations") {
        it("should be able to analyze that every returned array is null") {
            evaluateMethod("setArrayNull") { domain =>
                import domain._

                val returnIndex = 7

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

                arraylength(returnIndex, varray) should be(throws(InitializedObjectValue(-100007, ObjectType.NullPointerException)))

                arraystore(returnIndex, IntegerValue(20), IntegerValue(12, 0), varray) should be(ThrowsException(List(InitializedObjectValue(-100007, ObjectType.NullPointerException))))

                arrayload(returnIndex, IntegerValue(1), varray) should be(ThrowsException(List(InitializedObjectValue(-100007, ObjectType.NullPointerException))))

            }
        }

        it("should be able to analyze array initializations of different number types with different length") {
            evaluateMethod("branchInits") { domain =>
                import domain._

                val returnIndex = 98

                val varray = allReturnedValues(returnIndex)

                allReturnedValues.size should be(1)

                isValueASubtypeOf(varray, ArrayType(ObjectType.Object)) should be(Yes)

            }
        }

    }

}

class DefaultConcreteArraysTestDomain(
        override val maxCardinalityOfIntegerRanges: Long = -(Int.MinValue.toLong) + Int.MaxValue
) extends CorrelationalDomain
    with GlobalLogContextProvider
    with DefaultSpecialDomainValuesBinding
    with ThrowAllPotentialExceptionsConfiguration
    with l0.SimpleTypeLevelInvokeInstructions
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelDynamicLoads
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
    with DefaultConcreteArrayValuesBinding
    with TheMemoryLayout {

    // we don't collect "precise" information w.r.t. the heap about the content of an
    // array, hence we can track the contents
    override protected def reifyArray(pc: Int, count: Int, arrayType: ArrayType): Boolean = {
        super.reifyArray(pc, count, arrayType) ||
            arrayType.componentType.isObjectType && count < maxTrackedArraySize
    }
}

private object DefaultArraysTest {
    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithArrays").get
}
