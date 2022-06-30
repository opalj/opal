/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import scala.collection.immutable.SortedSet
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.br.ObjectType

/**
 * Tests the LongSets Domain.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
@RunWith(classOf[JUnitRunner])
class DefaultLongSetsTest extends AnyFunSpec with Matchers {

    final val IrrelevantPC = Int.MinValue
    final val SomePC = 100000

    class LongSetsTestDomain(
            override val maxCardinalityOfLongSets: Int = Int.MaxValue
    ) extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelFloatValues
        with l1.DefaultIntegerRangeValues // <---- Required to test the shift operators
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultLongSetValues // <----- Test target
        with l1.LongSetValuesShiftOperators // <----- Test target
        with l0.TypeLevelPrimitiveValuesConversions
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with PredefinedClassHierarchy
        with RecordLastReturnedValues

    describe("central properties of domains that use LongSet values") {

        it("two instances of ALongValue that may represent different values must not be identical") {
            val theDomain = new LongSetsTestDomain
            import theDomain._
            val v1 = LongValue(origin = 1)
            val v2 = LongValue(origin = 1)
            v1 should not be theSameInstanceAs(v2)
        }
    }

    describe("operations involving LongSet values") {

        describe("the behavior of join if we exceed the max cardinality") {

            val theDomain = new LongSetsTestDomain(8)
            import theDomain._

            it("(join of two sets with positive values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongValue)\"") {
                val v1 = LongSet(SortedSet(0L, 2L, 4L, 9L))
                val v2 = LongSet(SortedSet(1L, 3L, 5L, 6L, 7L))
                v1.join(-1, v2) should be(StructuralUpdate(LongValue(-1)))
                v2.join(-1, v1) should be(StructuralUpdate(LongValue(-1)))
            }

            it("(join of two sets with positive values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongSet(0, 1, 2, 3, 4, 5, 6, 9))\"") {
                val v1 = LongSet(SortedSet(0L, 2L, 4L, 6L, 9L))
                val v2 = LongSet(SortedSet(1L, 3L, 5L, 6L))
                val expected = LongSet(SortedSet(0L, 1L, 2L, 3L, 4L, 5L, 6L, 9L))
                v1.join(-1, v2) should be(StructuralUpdate(expected))
                v2.join(-1, v1) should be(StructuralUpdate(expected))
            }

            it("(join of two sets with positive and negative values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongValue)\"") {
                val v1 = LongSet(SortedSet(0L, 2L, 4L, 9L))
                val v2 = LongSet(SortedSet(1L, 3L, 5L, 6L, 7L))
                v1.join(-1, v2) should be(StructuralUpdate(LongValue(-1)))
                v2.join(-1, v1) should be(StructuralUpdate(LongValue(-1)))
            }

            it("(join of two sets with positive and negative values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongSet(-10, -7, -3, -1, 0, 5, 6, 9))\"") {
                val v1 = LongSet(SortedSet(-7L, -3L, 0L, 6L, 9L))
                val v2 = LongSet(SortedSet(-10L, -1L, 5L, 6L))
                val expected = LongSet(SortedSet(-10L, -7L, -3L, -1L, 0L, 5L, 6L, 9L))
                v1.join(-1, v2) should be(StructuralUpdate(expected))
                v2.join(-1, v1) should be(StructuralUpdate(expected))
            }

        }

        val theDomain = new LongSetsTestDomain
        import theDomain._

        describe("the behavior of the join operation if we do not exceed the max. spread") {

            it("(join of disjoint sets) {Long.MinValue,-1} join {1,Long.MaxValue} => {Long.MinValue,-1,1,Long.MaxValue}") {
                val v1 = LongSet(SortedSet(Long.MinValue, -1L))
                val v2 = LongSet(SortedSet(1, Long.MaxValue))

                val expected = LongSet(SortedSet(Long.MinValue, -1L, 1L, Long.MaxValue))
                v1.join(-1, v2) should be(StructuralUpdate(expected))
                v2.join(-1, v1) should be(StructuralUpdate(expected))
            }

            it("(join of intersecting LongSets) {-1,1} join {0,1} => {-1,0,1}") {
                val v1 = LongSet(SortedSet(-1L, 1L))
                val v2 = LongSet(SortedSet(0L, 1L))

                val expected = LongSet(SortedSet(-1L, 0L, 1L))
                v1.join(-1, v2) should be(StructuralUpdate(expected))
                v2.join(-1, v1) should be(StructuralUpdate(expected))
            }

            it("(join of two LongSets with the same values) {-1,1} join {-1,1} => \"MetaInformationUpdate\"") {
                val v1 = LongSet(SortedSet(-1L, 1L))
                val v2 = LongSet(SortedSet(-1L, 1L))

                v1.join(-1, v2) should be(Symbol("isMetaInformationUpdate"))
            }

        }

        describe("the behavior of the \"summarize\" function") {

            it("it should be able to handle intersecting LongSets") {
                val v1 = LongSet(SortedSet(-3L, -2L))
                val v2 = LongSet(SortedSet(-2L, -1L))

                val expected = LongSet(SortedSet(-3L, -2L, -1L))
                summarize(-1, Iterable(v1, v2)) should be(expected)
                summarize(-1, Iterable(v2, v1)) should be(expected)
            }

            it("it should be able to handle disjunct LongSets") {
                val v1 = LongSet(SortedSet(-3L, Long.MaxValue))
                val v2 = LongSet(SortedSet(-2L, -1L))

                val expected = SortedSet(-3L, -2L, -1L, Long.MaxValue)
                summarize(-1, Iterable(v1, v2)) should be(LongSet(expected))
                summarize(-1, Iterable(v2, v1)) should be(LongSet(expected))
            }

            it("a summary involving some LongValueValue should result in LongValue") {
                val v1 = LongSet(SortedSet(-3L, Long.MaxValue))
                val v2 = LongValue(-1 /*PC*/ )

                summarize(-1, Iterable(v1, v2)) should be(LongValue(-1))
                summarize(-1, Iterable(v2, v1)) should be(LongValue(-1))
            }

            it("should calculate the correct summary if Long.MaxValue is involved") {
                val v1 = LongSet(SortedSet(-3L, Long.MaxValue))
                val v2 = LongSet(SortedSet(-2L, Long.MaxValue))
                val expected = LongSet(SortedSet(-3L, -2L, Long.MaxValue))
                summarize(-1, Iterable(v1, v2)) should be(expected)
                summarize(-1, Iterable(v2, v1)) should be(expected)
            }

            it("should calculate the correct summary if Long.MinValue is involved") {
                val v1 = LongSet(SortedSet(Long.MinValue, 0L))
                val v2 = LongSet(SortedSet(Long.MinValue, 0L))
                val expected = LongSet(SortedSet(Long.MinValue, 0L))
                summarize(-1, Iterable(v1, v2)) should be(expected)
                summarize(-1, Iterable(v2, v1)) should be(expected)
            }
        }

        describe("the behavior of lmul") {

            it("{0,3} * {0,2} => {0,6}") {
                val v1 = LongSet(SortedSet(0L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(0L, 6L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(0L, 6L)))
            }

            it("{-3,-1} * {-10,-2} => {2,6,10,30}") {
                val v1 = LongSet(SortedSet(-3L, -1L))
                val v2 = LongSet(SortedSet(-10L, -2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(2L, 6L, 10L, 30L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(2L, 6L, 10L, 30L)))
            }

            it("{-1,3} * {0,2} => {-2,0,6}") {
                val v1 = LongSet(SortedSet(-1L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(-2L, 0L, 6L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(-2L, 0L, 6L)))
            }

            it("{-3,3} * {-2,2} => {-6,6}") {
                val v1 = LongSet(SortedSet(-3L, 3L))
                val v2 = LongSet(SortedSet(-2L, 2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(-6L, 6L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(-6L, 6L)))
            }

            it("{Long.MinValue} * {0} => {0}") {
                val v1 = LongSet(SortedSet(Long.MinValue))
                val v2 = LongSet(SortedSet(0L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("{Long.MaxValue} * {0} => {0}") {
                val v1 = LongSet(SortedSet(Long.MaxValue))
                val v2 = LongSet(SortedSet(0L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("{Long.MinValue} * {2} => {Long.MinValue*2}") {
                val v1 = LongSet(SortedSet(Long.MinValue))
                val v2 = LongSet(SortedSet(2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(Long.MinValue * 2L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(Long.MinValue * 2L)))
            }

            it("{Long.MaxValue} * {2} => {Long.MaxValue*2}") {
                val v1 = LongSet(SortedSet(Long.MaxValue))
                val v2 = LongSet(SortedSet(2L))

                lmul(-1, v1, v2) should be(LongSet(SortedSet(Long.MaxValue * 2L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(Long.MaxValue * 2L)))
            }

            it("{0,Long.MaxValue} * {Long.MinValue,0} => {Long.MaxValue*Long.MinValue,0}") {
                val v1 = LongSet(SortedSet(0L, Long.MaxValue))
                val v2 = LongSet(SortedSet(Long.MinValue, 0L))
                val expected = SortedSet(Long.MaxValue * Long.MinValue, 0L)
                lmul(-1, v1, v2) should be(LongSet(expected))
                lmul(-1, v2, v1) should be(LongSet(expected))
            }

            it("The result of the mul of a set s and {1} should be s itself; {2,4} * {1} => {2,4}") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(1L))

                lmul(-1, v1, v2) should be(v1)
                lmul(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value * {0} should be {0}") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongValue(-1)

                lmul(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }
        }

        describe("the behavior of lor") {

            it("LongValue | {8,19} => LongValue") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(8L, 19L))

                lor(-1, v1, v2) should be(LongValue(-1))
                lor(-1, v2, v1) should be(LongValue(-1))
            }

            it("{Long.MinValue,Long.MaxValue} | {8,19} => {Long.MinValue+8, Long.MinValue+19, Long.MaxValue}") {
                val v1 = LongSet(SortedSet(Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet(8L, 19L))

                val expected = SortedSet(Long.MinValue + 8L, Long.MinValue + 19L, Long.MaxValue)
                lor(-1, v1, v2) should be(LongSet(expected))
                lor(-1, v2, v1) should be(LongSet(expected))
            }

            it("{Long.MaxValue-2,Long.MaxValue-1} | {Long.MaxValue-1,Long.MaxValue} => {Long.MaxValue-1, Long.MaxValue}") {
                val v1 = LongSet(SortedSet(Long.MaxValue - 2L, Long.MaxValue - 1L))
                val v2 = LongSet(SortedSet(Long.MaxValue - 1L, Long.MaxValue))

                val expected = SortedSet(Long.MaxValue - 1L, Long.MaxValue)
                lor(-1, v1, v2) should be(LongSet(expected))
                lor(-1, v2, v1) should be(LongSet(expected))
            }

            it("{3} | {8,19} => {11,19}") {
                val v1 = LongSet(SortedSet(3L))
                val v2 = LongSet(SortedSet(8L, 19L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(11L, 19L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(11L, 19L)))
            }

            it("{0} | {0} => {0}") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongSet(SortedSet(0L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("{0} | {1} => {1}") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongSet(SortedSet(1L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(1L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(1L)))
            }

            it("{1} | {1} => {1}") {
                val v1 = LongSet(SortedSet(1L))
                val v2 = LongSet(SortedSet(1L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(1L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(1L)))
            }

            it("{1, 3} | {7, 15} => {7, 15}") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(7L, 15L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(7L, 15L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(7L, 15L)))
            }

            it("{8} | {2, 7} => {10, 15}") {
                val v1 = LongSet(SortedSet(8L))
                val v2 = LongSet(SortedSet(2L, 7L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(10L, 15L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(10L, 15L)))
            }

            it("{Long.MaxValue} | {0} => {Long.MaxValue}") {
                val v1 = LongSet(SortedSet(Long.MaxValue))
                val v2 = LongSet(SortedSet(0L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(Long.MaxValue)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(Long.MaxValue)))
            }

            it("The result of the or of a set s and {0} should be s itself; {2,4} | {0} => {2,4}") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(0L))

                lor(-1, v1, v2) should be(v1)
                lor(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value | {-1} should be {-1}") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(-1L))

                lor(-1, v1, v2) should be(LongSet(SortedSet(-1L)))
                lor(-1, v2, v1) should be(LongSet(SortedSet(-1L)))
            }
        }

        describe("the behavior of lxor") {

            it("LongValue ^ {8,19} => LongValue") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(8L, 19L))

                lxor(-1, v1, v2) should be(LongValue(-1))
                lxor(-1, v2, v1) should be(LongValue(-1))
            }

            it("{Long.MinValue,Long.MaxValue} ^ {8,19} => {Long.MinValue+8,Long.MinValue+19,Long.MaxValue-19,Long.MaxValue-8}") {
                val v1 = LongSet(SortedSet(Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet(8L, 19L))
                val expected =
                    SortedSet(
                        Long.MinValue + 8L, Long.MinValue + 19L,
                        Long.MaxValue - 19L, Long.MaxValue - 8L
                    )
                lxor(-1, v1, v2) should be(LongSet(expected))
                lxor(-1, v2, v1) should be(LongSet(expected))
            }

            it("{Long.MaxValue-2,Long.MaxValue-1} ^ {Long.MaxValue-1,Long.MaxValue} => {0,1,2,3}") {
                val v1 = LongSet(SortedSet(Long.MaxValue - 2L, Long.MaxValue - 1L))
                val v2 = LongSet(SortedSet(Long.MaxValue - 1L, Long.MaxValue))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(0L, 1L, 2L, 3L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(0L, 1L, 2L, 3L)))
            }

            it("{3} ^ {8,19} => {11,16}") {
                val v1 = LongSet(SortedSet(3L))
                val v2 = LongSet(SortedSet(8L, 19L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(11L, 16L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(11L, 16L)))
            }

            it("{0} ^ {0} => {0}") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongSet(SortedSet(0L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("{0} ^ {1} => {1}") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongSet(SortedSet(1L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(1L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(1L)))
            }

            it("{1} ^ {1} => {0}") {
                val v1 = LongSet(SortedSet(1L))
                val v2 = LongSet(SortedSet(1L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("{1, 3} ^ {7, 15} => {4,6,12,14}") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(7L, 15L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(4L, 6L, 12L, 14L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(4L, 6L, 12L, 14L)))
            }

            it("{8} ^ {2, 7} => {15}") {
                val v1 = LongSet(SortedSet(8L))
                val v2 = LongSet(SortedSet(2L, 7L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(10L, 15L)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(10L, 15L)))
            }

            it("{Long.MaxValue} ^ {0} => {Long.MaxValue}") {
                val v1 = LongSet(SortedSet(Long.MaxValue))
                val v2 = LongSet(SortedSet(0L))

                lxor(-1, v1, v2) should be(LongSet(SortedSet(Long.MaxValue)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet(Long.MaxValue)))
            }
        }

        describe("the behavior of ladd") {

            it("{0,3} + {0,2} => {0,2,3,5}") {
                val v1 = LongSet(SortedSet(0L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                ladd(SomePC, v1, v2) should be(LongSet(SortedSet(0L, 2L, 3L, 5L)))
                ladd(SomePC, v2, v1) should be(LongSet(SortedSet(0L, 2L, 3L, 5L)))
            }

            it("{-3,-1} + {-10,-2} => {-13,-11,-5,-3}") {
                val v1 = LongSet(SortedSet(-3L, -1L))
                val v2 = LongSet(SortedSet(-10L, -2L))

                ladd(SomePC, v1, v2) should be(LongSet(SortedSet(-13L, -11L, -5L, -3L)))
                ladd(SomePC, v2, v1) should be(LongSet(SortedSet(-13L, -11L, -5L, -3L)))
            }

            it("{-1,3} + {0,2} => {-1,1,3,5}") {
                val v1 = LongSet(SortedSet(-1L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                ladd(SomePC, v1, v2) should be(LongSet(SortedSet(-1L, 1L, 3L, 5L)))
                ladd(SomePC, v2, v1) should be(LongSet(SortedSet(-1L, 1L, 3L, 5L)))
            }

            it("{0} + LongValue => LongValue") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongValue(-1)

                ladd(SomePC, v1, v2) should be(LongValue(-1))
                ladd(SomePC, v2, v1) should be(LongValue(-1))
            }

            it("{Long.MinValue,3} + {3,2} => {Long.MinValue+2,Long.MinValue+3,5,6}") {
                val v1 = LongSet(SortedSet(Long.MinValue, 3L))
                val v2 = LongSet(SortedSet(3L, 2L))

                val expected = SortedSet(Long.MinValue + 2L, Long.MinValue + 3L, 5L, 6L)
                ladd(SomePC, v1, v2) should be(LongSet(expected))
                ladd(SomePC, v2, v1) should be(LongSet(expected))
            }

            it("{-3,-1} + {-3,Long.MaxValue} => {-6,-4,Long.MaxValue-3,Long.MaxValue-1}") {
                val v1 = LongSet(SortedSet(-3L, -1L))
                val v2 = LongSet(SortedSet(-3L, Long.MaxValue))

                val expected = SortedSet(-6L, -4L, Long.MaxValue - 3L, Long.MaxValue - 1L)
                ladd(SomePC, v1, v2) should be(LongSet(expected))
                ladd(SomePC, v2, v1) should be(LongSet(expected))
            }

        }

        describe("the behavior of lsub") {

            it("{0,3} - {0,2} => {-2,0,1,3}") {
                val v1 = LongSet(SortedSet(0L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                lsub(-1, v1, v2) should be(LongSet(SortedSet(-2L, 0L, 1L, 3L)))
            }

            it("{-3,-1} - {-10,-2} => {-1,1,7,9}") {
                val v1 = LongSet(SortedSet(-3L, -1L))
                val v2 = LongSet(SortedSet(-10L, -2L))

                lsub(-1, v1, v2) should be(LongSet(SortedSet(-1L, 1L, 7L, 9L)))
            }

            it("{-1,3} - {0,2} => {-3,-1,1,3}") {
                val v1 = LongSet(SortedSet(-1L, 3L))
                val v2 = LongSet(SortedSet(0L, 2L))

                lsub(-1, v1, v2) should be(LongSet(SortedSet(-3L, -1L, 1L, 3L)))
            }

            it("{0} - LongValue => LongValue") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongValue(-1)

                lsub(-1, v1, v2) should be(LongValue(-1))
            }

            it("LongValue - {0} => LongValue") {
                val v1 = LongSet(SortedSet(0L))
                val v2 = LongValue(-1)

                lsub(-1, v2, v1) should be(LongValue(-1))
            }

            it("{Long.MinValue,3} - {3,2} => {0,1,Long.MinValue-2,Long.MinValue-3}") {
                val v1 = LongSet(SortedSet(Long.MinValue, 3L))
                val v2 = LongSet(SortedSet(3L, 2L))

                lsub(-1, v1, v2) should be(
                    LongSet(SortedSet(0L, 1L, Long.MinValue - 2L, Long.MinValue - 3L))
                )
            }

            it("{Long.MaxValue,3} - {-3,2} => {Long.MaxValue+3,1,6,Long.MaxValue-2}") {
                val v1 = LongSet(SortedSet(Long.MaxValue, 3L))
                val v2 = LongSet(SortedSet(-3L, 2L))

                lsub(-1, v1, v2) should be(
                    LongSet(SortedSet(Long.MaxValue + 3L, 1L, 6L, Long.MaxValue - 2L))
                )
            }

        }

        describe("the behavior of ldiv") {

            it("{1,3} / {2} => {0,1}") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(2L))

                ldiv(SomePC, v1, v2) should be(ComputedValue(LongSet(SortedSet(0L, 1L))))
            }

            it("{1,3} / {1} => {1,3}") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(1L))

                ldiv(SomePC, v1, v2) should be(ComputedValue(LongSet(SortedSet(1L, 3L))))
            }

            it("{1,3} / {0} => ThrowsException") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(0L))

                val result = ldiv(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{1,3} / {-1} => {-3,-1}") {
                val v1 = LongSet(SortedSet(1L, 3L))
                val v2 = LongSet(SortedSet(-1L))

                ldiv(SomePC, v1, v2) should be(ComputedValue(LongSet(SortedSet(-3L, -1L))))
            }

            it("LongValue / {0} => ThrowsException") {
                val v1 = LongValue(SomePC)
                val v2 = LongSet(SortedSet(0L))

                val result = ldiv(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("LongValue / LongValue => Value and ThrowsException") {
                val v1 = LongValue(SomePC)
                val v2 = LongValue(SomePC)

                val result = ldiv(SomePC, v1, v2)
                result.result should be { LongValue(-1) }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{-1,200} / LongValue => Value and ThrowsException") {
                val v1 = LongSet(SortedSet(-1L, 200L))
                val v2 = LongValue(SomePC)

                val result = ldiv(SomePC, v1, v2)
                result.result should be { LongValue(-1) }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{Long.MinValue,-1} / Long.MaxValue => {-1,0}") {
                val v1 = LongSet(SortedSet(Long.MinValue, -1))
                val v2 = LongSet(SortedSet(Long.MaxValue, Long.MaxValue))

                ldiv(SomePC, v1, v2) should be(ComputedValue(LongSet(SortedSet(-1L, 0L))))
            }

            it("{Long.MinValue,Long.MaxValue} / Long.MaxValue => {-1,1}") {
                val v1 = LongSet(SortedSet(Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet(Long.MaxValue, Long.MaxValue))

                ldiv(SomePC, v1, v2) should be(ComputedValue(LongSet(SortedSet(-1L, 1L))))
            }
        }

        describe("the behavior of lrem") {

            it("LongValue % LongValue => LongValue + Exception") {
                val v1 = LongValue(SomePC)
                val v2 = LongValue(SomePC)

                val result = lrem(SomePC, v1, v2)
                result.result should be { LongValue(-1) }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is known, but the divisor is 0) {0,3} % {0} => Exception") {
                val v1 = LongSet(SortedSet(0L, 3L))
                val v2 = LongSet(SortedSet(0L))

                val result = lrem(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is 0) LongValue % {0} => Exception") {
                val v1 = LongValue(SomePC)
                val v2 = LongSet(SortedSet(0L))

                val result = lrem(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is known) LongValue % {2} => LongValue") {
                val v1 = LongValue(SomePC)
                val v2 = LongSet(SortedSet(2L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongValue(-1))
                result.throwsException should be(false)
            }

            it("(dividend and divisor are positive) {0,3} % {1,2} => {0,1}") {
                val v1 = LongSet(SortedSet(0L, 3L))
                val v2 = LongSet(SortedSet(1L, 2L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(0L, 1L)))
            }

            it("(dividend and divisor are negative) {-10,-3} % {-2,-1} => {-1,0}") {
                val v1 = LongSet(SortedSet(-10L, -3L))
                val v2 = LongSet(SortedSet(-2L, -1L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(-1L, 0L)))
            }

            it("(the dividend may be positive OR negative) {-10,3} % {1,2} => {0,1}") {
                val v1 = LongSet(SortedSet(-10L, 3L))
                val v2 = LongSet(SortedSet(1L, 2L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(0L, 1L)))
            }

            it("(the dividend and the divisor may be positive OR negative) {-10,3} % {-3,4} => {-2,-1,0,3}") {
                val v1 = LongSet(SortedSet(-10L, 3L))
                val v2 = LongSet(SortedSet(-3L, 4L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(-2L, -1L, 0L, 3L)))
            }

            it("(the dividend and the divisor are positive) {0,Long.MaxValue} % {16} => {0,15}") {
                val v1 = LongSet(SortedSet(0L, Long.MaxValue))
                val v2 = LongSet(SortedSet(16L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(0L, 15L)))
            }

            it("(the dividend and the divisor are single values) {2} % {16} => {2}") {
                val v1 = LongSet(SortedSet(2L))
                val v2 = LongSet(SortedSet(16L))

                val result = lrem(SomePC, v1, v2)
                result.result should be(LongSet(SortedSet(2L)))
            }
        }

        describe("the behavior of land") {

            it("{3} & {255} => {0}") {
                val v1 = LongSet(SortedSet(3L))
                val v2 = LongSet(SortedSet(255L))

                land(-1, v1, v2) should be(LongSet(SortedSet(3L)))
                land(-1, v2, v1) should be(LongSet(SortedSet(3L)))
            }

            it("{4} & {2} => {0}") {
                val v1 = LongSet(SortedSet(4L))
                val v2 = LongSet(SortedSet(2L))

                land(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                land(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }

            it("LongValue & {2} => LongValue") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(2L))

                land(-1, v1, v2) should be(LongValue(-1))
                land(-1, v2, v1) should be(LongValue(-1))
            }

            it("{-2} & LongValue  => LongValue") {
                val v1 = LongSet(SortedSet(-2L))
                val v2 = LongValue(-1)

                land(-1, v1, v2) should be(LongValue(-1))
                land(-1, v2, v1) should be(LongValue(-1))
            }

            it("The result of the and of a set s and {-1} should be s itself; {2,4} & {-1} => {2,4}") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(-1L))

                land(-1, v1, v2) should be(v1)
                land(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value & {0} should be {0}") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(0L))

                land(-1, v1, v2) should be(LongSet(SortedSet(0L)))
                land(-1, v2, v1) should be(LongSet(SortedSet(0L)))
            }
        }

        describe("the behavior of lshl") {

            it("LongValue l<< {2} => LongValue") {
                val v = LongValue(-1)
                val s = IntegerValue(-2, 2)

                lshl(-1, v, s) should be(LongValue(-1))
            }

            it("{2} l<< IntegerValue => LongValue") {
                val v = LongSet(SortedSet(2L))
                val s = IntegerValue(origin = -2)

                lshl(-1, v, s) should be(LongValue(-1))
            }

            it("{-1,1} l<< {2} => {-4,4}") {
                val v = LongSet(SortedSet(-1L, 1L))
                val s = IntegerValue(-2, 2)

                lshl(-1, v, s) should be(LongSet(SortedSet(-4L, 4L)))
            }

            it("{64} l<< {64} => {64}") {
                val v = LongSet(SortedSet(64L))
                val s = IntegerValue(-2, 64)

                lshl(-1, v, s) should be(LongSet(SortedSet(64L)))
            }

            it("{1} l<< {64} => {1}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 64)

                lshl(-1, v, s) should be(LongSet(SortedSet(1L)))
            }

            it("{0} l<< {64} => {0}") {
                val v = LongSet(SortedSet(0L))
                val s = IntegerValue(-2, 64)

                lshl(-1, v, s) should be(LongSet(SortedSet(0L)))
            }

            it("{1} l<< {30} => {1073741824}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 30)

                lshl(-1, v, s) should be(LongSet(SortedSet(1073741824L)))
            }

            it("{1} l<< {2} => {4}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 2)

                lshl(-1, v, s) should be(LongSet(SortedSet(4L)))
            }

            it("{0,2} l<< {2} => {0,8}") {
                val v = LongSet(SortedSet(0L, 2L))
                val s = IntegerValue(-2, 2)

                lshl(-1, v, s) should be(LongSet(SortedSet(0L, 8L)))
            }

            it("{1,2} l<< {2} => {4,8}") {
                val v = LongSet(SortedSet(1L, 2L))
                val s = IntegerValue(-2, 2)

                lshl(-1, v, s) should be(LongSet(SortedSet(4L, 8L)))
            }

        }

        describe("the behavior of lshr") {

            it("LongValue l>> {2} => LongValue") {
                val v = LongValue(-1)
                val s = IntegerValue(-2, 2)

                lshr(-1, v, s) should be(LongValue(-1))
            }

            it("{2} l>> AnIntegerValue => LongValue") {
                val v = LongSet(SortedSet(2L))
                val s = IntegerValue(-1)

                lshr(-1, v, s) should be(LongValue(-1))
            }

            it("{-1,1} l>> {2} => {-1,0}") {
                val v = LongSet(SortedSet(-1L, 1L))
                val s = IntegerValue(-2, 2)

                lshr(-1, v, s) should be(LongSet(SortedSet(-1L, 0L)))
            }

            it("{256} l>> {64} => {256}") {
                val v = LongSet(SortedSet(256L))
                val s = IntegerValue(-2, 64)

                lshr(-1, v, s) should be(LongSet(SortedSet(256L)))
            }

            it("{256} l>> {8} => {1}") {
                val v = LongSet(SortedSet(256L))
                val s = IntegerValue(-2, 8)

                lshr(-1, v, s) should be(LongSet(SortedSet(1L)))
            }

            it("{256} l>> {9} => {0}") {
                val v = LongSet(SortedSet(256L))
                val s = IntegerValue(-2, 9)

                lshr(-1, v, s) should be(LongSet(SortedSet(0L)))
            }

            it("{0} l>> {64} => {0}") {
                val v = LongSet(SortedSet(0L))
                val s = IntegerValue(-2, 64)

                lshr(-1, v, s) should be(LongSet(SortedSet(0L)))
            }

            it("{1} l>> {30} => {0}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 30)

                lshr(-1, v, s) should be(LongSet(SortedSet(0L)))
            }

            it("{1} l>> {2} => {0}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 2)

                lshr(-1, v, s) should be(LongSet(SortedSet(0L)))
            }

            it("{1} l>> {0} => {1}") {
                val v = LongSet(SortedSet(1L))
                val s = IntegerValue(-2, 0)

                lshr(-1, v, s) should be(LongSet(SortedSet(1L)))
            }

            it("{32,64} l>> {2} => {8,16}") {
                val v = LongSet(SortedSet(32L, 64L))
                val s = IntegerValue(-2, 2)

                lshr(-1, v, s) should be(LongSet(SortedSet(8L, 16L)))
            }

        }

        describe("the behaviour of lcmp") {

            it("compare two single-element sets where v1 < v2; lcmp({2}, {4}) => [-1,-1]") {
                val v1 = LongSet(SortedSet(2L))
                val v2 = LongSet(SortedSet(4L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1))
            }

            it("compare two single-element sets where v1 = v2; lcmp({2}, {2}) => [0,0]") {
                val v1 = LongSet(SortedSet(2L))
                val v2 = LongSet(SortedSet(2L))

                lcmp(-1, v1, v2) should be(IntegerRange(0))
            }

            it("compare two single-element sets where v1 > v2; lcmp({4}, {2}) => [1,1]") {
                val v1 = LongSet(SortedSet(4L))
                val v2 = LongSet(SortedSet(2L))

                lcmp(-1, v1, v2) should be(IntegerRange(1))
            }

            it("compare a specific (but unknown) LongValue with {Long.MinValue} where v1 can't be < v2; lcmp(LongValue, {Long.MinValue}) => [0,1]") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(Long.MinValue))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare a specific (but unknown) LongValue with {Long.MaxValue} where v1 can't be > v2; lcmp({LongValue, {Long.MaxValue}) => [-1,0]") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(Long.MaxValue))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the single-element set v1 containing Long.MinValue with a specific (but unknown) LongValue where v1 can't be > v2; lcmp({Long.MinValue}, LongValue) => [-1,0]") {
                val v1 = LongSet(SortedSet(Long.MinValue))
                val v2 = LongValue(-1)

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the single-element set v1 containing Long.MaxValue with a specific (but unknown) LongValue where v1 can't be < v2; lcmp({Long.MaxValue}, LongValue) => [0,1]") {
                val v1 = LongSet(SortedSet(Long.MaxValue))
                val v2 = LongValue(-1)

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare the multi-element set v1 with a specific (but unknown) LongValue where no information can be deduced; lcmp({-2,0,2}, LongValue) => [-1,1]") {
                val v1 = LongSet(SortedSet(-2L, 0L, 2L))
                val v2 = LongValue(-1)

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }

            it("compare a specific (but unknown) LongValue with the multi-element set v2 where no information can be deduced; lcmp(LongValue, {-2,0,2}) => [-1,1]") {
                val v1 = LongValue(-1)
                val v2 = LongSet(SortedSet(-2L, 0L, 2L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }

            it("compare two multi-element sets where the smallest element of v1 is greater than the largest element of v2; lcmp({2,4,6}, {-4,-2,0}) => [1,1]") {
                val v1 = LongSet(SortedSet(2L, 4L, 6L))
                val v2 = LongSet(SortedSet(-4L, -2L, 0L))

                lcmp(-1, v1, v2) should be(IntegerRange(1, 1))
            }

            it("compare two multi-element sets where the greatest element of v1 is less than the smallest element of v2; lcmp({2,4}, {6,8}) => [-1,-1]") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(6L, 8L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, -1))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.last overlaps with v2.head; lcmp({2,4}, {4}) => [-1,0]") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(4L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare two multi-element sets where v1.last overlaps with v2.head; lcmp({2,4}, {4,5}) => [-1,0]") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(4L, 5L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.head overlaps with v2.last; lcmp({4}, {-4,4}) => [0,1]") {
                val v1 = LongSet(SortedSet(4L))
                val v2 = LongSet(SortedSet(-4L, 4L))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare two multi-element sets where v1.head overlaps with v2.last; lcmp({2,4}, {-4,2}) => [0,1]") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(-4L, 2L))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare the single-element set v1 with the multi-element set v2 where v1.head overlaps with v2.head; lcmp({-7},{-7,-5}) => [-1,0]") {
                val v1 = LongSet(SortedSet(-7L))
                val v2 = LongSet(SortedSet(-7L, -5L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.head overlaps with v2.head; lcmp({2,4}, {2}) => [0,1]") {
                val v1 = LongSet(SortedSet(2L, 4L))
                val v2 = LongSet(SortedSet(2L))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare two multi-element sets where v1.last overlaps with v2.last; lcmp({-2,0},{-1,0}) => [-1,1]") {
                val v1 = LongSet(SortedSet(-2L, 0L))
                val v2 = LongSet(SortedSet(-1L, 0L))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }
        }
    }
}
