/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.Iterable
import scala.collection.immutable.SortedSet

import org.opalj.br.ArrayType
import org.opalj.br.IntegerType
import org.opalj.br.ObjectType
import org.opalj.ai.NoUpdate
import org.opalj.ai.domain.DefaultSpecialDomainValuesBinding
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.PredefinedClassHierarchy
import org.opalj.ai.domain.RecordLastReturnedValues
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration

/**
 * Tests the IntegerSets Domain.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
@RunWith(classOf[JUnitRunner])
class DefaultIntegerSetsTest extends AnyFunSpec with Matchers {

    final val IrrelevantPC = Int.MinValue
    final val SomePC = 100000

    class IntegerSetsTestDomain(
            override val maxCardinalityOfIntegerSets: Int = 126 // <= MAX SUPPORTED VALUE
    ) extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultIntegerSetValues // <----- The one we are going to test
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with PredefinedClassHierarchy
        with RecordLastReturnedValues

    describe("central properties of domains that use IntegerSet values") {

        val theDomain = new IntegerSetsTestDomain
        import theDomain._

        it("the representation of the constant integer value 0 should be an IntegerSet value") {
            theDomain.IntegerConstant0 should be(IntegerSet(0))
        }
    }

    describe("operations involving IntegerSet values") {

        describe("the behavior of join if we exceed the max cardinality") {

            val theDomain = new IntegerSetsTestDomain(8)
            import theDomain._

            it("(join of two sets with positive values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerSet(SortedSet[Int](0, 2, 4, 9))
                val v2 = IntegerSet(SortedSet[Int](1, 3, 5, 6, 7))
                v1.join(-1, v2) should be(StructuralUpdate(U7BitSet()))
                v2.join(-1, v1) should be(StructuralUpdate(U7BitSet()))
            }

            it("(join of two sets with positive values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(IntegerSet(0, 1, 2, 3, 4, 5, 6, 9))\"") {
                val v1 = IntegerSet(SortedSet[Int](0, 2, 4, 6, 9))
                val v2 = IntegerSet(SortedSet[Int](1, 3, 5, 6))

                v1.join(-1, v2) should be(StructuralUpdate(
                    IntegerSet(SortedSet[Int](0, 1, 2, 3, 4, 5, 6, 9))
                ))

                v2.join(-1, v1) should be(StructuralUpdate(
                    IntegerSet(SortedSet[Int](0, 1, 2, 3, 4, 5, 6, 9))
                ))
            }

            it("(join of two sets with positive and negative values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerSet(SortedSet[Int](0, 2, 4, 9))
                val v2 = IntegerSet(SortedSet[Int](1, 3, 5, 6, 7))
                v1.join(-1, v2) should be(StructuralUpdate(U7BitSet()))
                v2.join(-1, v1) should be(StructuralUpdate(U7BitSet()))
            }

            it("(join of two sets with positive and negative values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(IntegerSet(-10, -7, -3, -1, 0, 5, 6, 9))\"") {
                val v1 = IntegerSet(SortedSet[Int](-7, -3, 0, 6, 9))
                val v2 = IntegerSet(SortedSet[Int](-10, -1, 5, 6))

                v1.join(-1, v2) should be(StructuralUpdate(
                    IntegerSet(SortedSet[Int](-10, -7, -3, -1, 0, 5, 6, 9))
                ))

                v2.join(-1, v1) should be(StructuralUpdate(
                    IntegerSet(SortedSet[Int](-10, -7, -3, -1, 0, 5, 6, 9))
                ))
            }
        }

        val theDomain = new IntegerSetsTestDomain
        import theDomain._

        describe("the behavior of the join operation if we do not exceed the max. spread") {

            it("(join with itself) IntegerSet(0) join IntegerSet(0) => \"NoUpdate\"") {
                val v1 = IntegerSet(0)
                val v2 = IntegerSet(0)
                v1.join(-1, v2) should be(NoUpdate)
            }

            it("(join of disjoint sets) {Int.MinValue,-1} join {1,Int.MaxValue} => {Int.MinValue,-1,1,Int.MaxValue}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, -1))
                val v2 = IntegerSet(SortedSet[Int](1, Int.MaxValue))

                v1.join(-1, v2) should be(StructuralUpdate(IntegerSet(SortedSet[Int](Int.MinValue, -1, 1, Int.MaxValue))))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerSet(SortedSet[Int](Int.MinValue, -1, 1, Int.MaxValue))))
            }

            it("(join of intersecting IntegerSets) {-1,1} join {0,1} => {-1,0,1}") {
                val v1 = IntegerSet(SortedSet[Int](-1, 1))
                val v2 = IntegerSet(SortedSet[Int](0, 1))

                v1.join(-1, v2) should be(StructuralUpdate(IntegerSet(SortedSet[Int](-1, 0, 1))))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerSet(SortedSet[Int](-1, 0, 1))))
            }

            it("(join of two IntegerSets with the same values) {-1,1} join {-1,1} => \"MetaInformationUpdate\"") {
                val v1 = IntegerSet(SortedSet[Int](-1, 1))
                val v2 = IntegerSet(SortedSet[Int](-1, 1))

                v1.join(-1, v2) should be(Symbol("isMetaInformationUpdate"))
            }
        }

        describe("the behavior of the \"summarize\" function") {

            it("it should be able to handle intersecting IntegerSets") {
                val v1 = IntegerSet(SortedSet[Int](-3, -2))
                val v2 = IntegerSet(SortedSet[Int](-2, -1))

                summarize(-1, Iterable(v1, v2)) should be(IntegerSet(SortedSet[Int](-3, -2, -1)))
                summarize(-1, Iterable(v2, v1)) should be(IntegerSet(SortedSet[Int](-3, -2, -1)))
            }

            it("it should be able to handle disjunct IntegerSets") {
                val v1 = IntegerSet(SortedSet[Int](-3, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](-2, -1))

                summarize(-1, Iterable(v1, v2)) should be(IntegerSet(SortedSet[Int](-3, -2, -1, Int.MaxValue)))
                summarize(-1, Iterable(v2, v1)) should be(IntegerSet(SortedSet[Int](-3, -2, -1, Int.MaxValue)))
            }

            it("a summary involving some IntegerValue should result in AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](-3, Int.MaxValue))
                val v2 = IntegerValue(-1 /*PC*/ )

                summarize(-1, Iterable(v1, v2)) should be(AnIntegerValue())
                summarize(-1, Iterable(v2, v1)) should be(AnIntegerValue())
            }

            it("should calculate the correct summary if Int.MaxValue is involved") {
                val v1 = IntegerSet(SortedSet[Int](-3, 2147483647))
                val v2 = IntegerSet(SortedSet[Int](-2, Int.MaxValue))
                summarize(-1, Iterable(v1, v2)) should be(IntegerSet(SortedSet[Int](-3, -2, 2147483647)))
                summarize(-1, Iterable(v2, v1)) should be(IntegerSet(SortedSet[Int](-3, -2, 2147483647)))
            }

            it("should calculate the correct summary if Int.MinValue is involved") {
                val v1 = IntegerSet(SortedSet[Int](-2147483648, 0))
                val v2 = IntegerSet(SortedSet[Int](Int.MinValue, 0))
                summarize(-1, Iterable(v1, v2)) should be(IntegerSet(SortedSet[Int](-2147483648, 0)))
                summarize(-1, Iterable(v2, v1)) should be(IntegerSet(SortedSet[Int](-2147483648, 0)))
            }
        }

        describe("the behavior of imul") {

            it("{0,3} * {0,2} => {0,6}") {
                val v1 = IntegerSet(SortedSet[Int](0, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0, 6)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0, 6)))
            }

            it("{-3,-1} * {-10,-2} => {2,6,10,30}") {
                val v1 = IntegerSet(SortedSet[Int](-3, -1))
                val v2 = IntegerSet(SortedSet[Int](-10, -2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](2, 6, 10, 30)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](2, 6, 10, 30)))
            }

            it("{-1,3} * {0,2} => {-2,0,6}") {
                val v1 = IntegerSet(SortedSet[Int](-1, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-2, 0, 6)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-2, 0, 6)))
            }

            it("{-3,3} * {-2,2} => {-6,6}") {
                val v1 = IntegerSet(SortedSet[Int](-3, 3))
                val v2 = IntegerSet(SortedSet[Int](-2, 2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-6, 6)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-6, 6)))
            }

            it("{3} * AnIntegerValue => AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](3))
                val v2 = AnIntegerValue()

                imul(-1, v1, v2) should be(AnIntegerValue())
                imul(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{Int.MinValue} * {0} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue))
                val v2 = IntegerSet(SortedSet[Int](0))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{Int.MaxValue} * {0} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](0))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{Int.MinValue} * {2} => {Int.MinValue*2}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue))
                val v2 = IntegerSet(SortedSet[Int](2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MinValue * 2)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MinValue * 2)))
            }

            it("{Int.MaxValue} * {2} => {Int.MaxValue*2}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](2))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MaxValue * 2)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MaxValue * 2)))
            }

            it("{0,Int.MaxValue} * {Int.MinValue,0} => {Int.MaxValue*Int.MinValue,0}") {
                val v1 = IntegerSet(SortedSet[Int](0, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](Int.MinValue, 0))

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MaxValue * Int.MinValue, 0)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MaxValue * Int.MinValue, 0)))
            }

            it("The result of the mul of a set s and {1} should be s itself; {2,4} * {1} => {2,4}") {
                val v1 = IntegerSet(SortedSet[Int](2, 4))
                val v2 = IntegerSet(SortedSet[Int](1))

                imul(-1, v1, v2) should be(v1)
                imul(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value * {0} should be {0}") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = AnIntegerValue()

                imul(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                imul(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }
        }

        describe("the behavior of ior") {

            it("AnIntegerValue | {8,19} => AnIntegerValue") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ior(-1, v1, v2) should be(AnIntegerValue())
                ior(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{Int.MinValue,Int.MaxValue} | {8,19} => {Int.MinValue+8, Int.MinValue+19, Int.MaxValue}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MinValue + 8, Int.MinValue + 19, Int.MaxValue)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MinValue + 8, Int.MinValue + 19, Int.MaxValue)))
            }

            it("{Int.MaxValue-2,Int.MaxValue-1} | {Int.MaxValue-1,Int.MaxValue} => {Int.MaxValue-1, Int.MaxValue}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue - 2, Int.MaxValue - 1))
                val v2 = IntegerSet(SortedSet[Int](Int.MaxValue - 1, Int.MaxValue))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MaxValue - 1, Int.MaxValue)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MaxValue - 1, Int.MaxValue)))
            }

            it("{3} | {8,19} => {11,19}") {
                val v1 = IntegerSet(SortedSet[Int](3))
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](11, 19)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](11, 19)))
            }

            it("{0} | {0} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = IntegerSet(SortedSet[Int](0))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{0} | {1} => {1}") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = IntegerSet(SortedSet[Int](1))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](1)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{1} | {1} => {1}") {
                val v1 = IntegerSet(SortedSet[Int](1))
                val v2 = IntegerSet(SortedSet[Int](1))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](1)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{1, 3} | {7, 15} => {7, 15}") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](7, 15))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](7, 15)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](7, 15)))
            }

            it("{8} | {2, 7} => {10, 15}") {
                val v1 = IntegerSet(SortedSet[Int](8))
                val v2 = IntegerSet(SortedSet[Int](2, 7))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](10, 15)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](10, 15)))
            }

            it("The result of the or of a set s and {0} should be s itself; {2,4} | {0} => {2,4}") {
                val v1 = IntegerSet(SortedSet[Int](2, 4))
                val v2 = IntegerSet(SortedSet[Int](0))

                ior(-1, v1, v2) should be(v1)
                ior(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value | {-1} should be {-1}") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](-1))

                ior(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-1)))
                ior(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-1)))
            }
        }

        describe("the behavior of ixor") {

            it("AnIntegerValue ^ {8,19} => AnIntegerValue") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ixor(-1, v1, v2) should be(AnIntegerValue())
                ixor(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{Int.MinValue,Int.MaxValue} ^ {8,19} => {Int.MinValue+8,Int.MinValue+19,Int.MaxValue-19,Int.MaxValue-8}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MinValue + 8, Int.MinValue + 19, Int.MaxValue - 19, Int.MaxValue - 8)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MinValue + 8, Int.MinValue + 19, Int.MaxValue - 19, Int.MaxValue - 8)))
            }

            it("{Int.MaxValue-2,Int.MaxValue-1} ^ {Int.MaxValue-1,Int.MaxValue} => {0,1,2,3}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue - 2, Int.MaxValue - 1))
                val v2 = IntegerSet(SortedSet[Int](Int.MaxValue - 1, Int.MaxValue))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0, 1, 2, 3)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0, 1, 2, 3)))
            }

            it("{3} ^ {8,19} => {11,16}") {
                val v1 = IntegerSet(SortedSet[Int](3))
                val v2 = IntegerSet(SortedSet[Int](8, 19))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](11, 16)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](11, 16)))
            }

            it("{0} ^ {0} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = IntegerSet(SortedSet[Int](0))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{0} ^ {1} => {1}") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = IntegerSet(SortedSet[Int](1))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](1)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{1} ^ {1} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](1))
                val v2 = IntegerSet(SortedSet[Int](1))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{1, 3} ^ {7, 15} => {4,6,12,14}") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](7, 15))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](4, 6, 12, 14)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](4, 6, 12, 14)))
            }

            it("{8} ^ {2, 7} => {15}") {
                val v1 = IntegerSet(SortedSet[Int](8))
                val v2 = IntegerSet(SortedSet[Int](2, 7))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](10, 15)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](10, 15)))
            }

            it("{Int.MaxValue} ^ {0} => {Int.MaxValue}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](0))

                ixor(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MaxValue)))
                ixor(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MaxValue)))
            }
        }

        describe("the behavior of iadd") {

            it("{0,3} + {0,2} => {0,2,3,5}") {
                val v1 = IntegerSet(SortedSet[Int](0, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                iadd(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0, 2, 3, 5)))
                iadd(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0, 2, 3, 5)))
            }

            it("{-3,-1} + {-10,-2} => {-13,-11,-5,-3}") {
                val v1 = IntegerSet(SortedSet[Int](-3, -1))
                val v2 = IntegerSet(SortedSet[Int](-10, -2))

                iadd(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-13, -11, -5, -3)))
                iadd(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-13, -11, -5, -3)))
            }

            it("{-1,3} + {0,2} => {-1,1,3,5}") {
                val v1 = IntegerSet(SortedSet[Int](-1, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                iadd(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-1, 1, 3, 5)))
                iadd(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-1, 1, 3, 5)))
            }

            it("{0} + AnIntegerValue => AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = AnIntegerValue()

                iadd(-1, v1, v2) should be(AnIntegerValue())
                iadd(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{Int.MinValue,3} + {2,3} => {Int.MinValue+2,Int.MinValue+3,5,6}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, 3))
                val v2 = IntegerSet(SortedSet[Int](2, 3))

                iadd(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MinValue + 2, Int.MinValue + 3, 5, 6)))
                iadd(-1, v2, v1) should be(IntegerSet(SortedSet[Int](Int.MinValue + 2, Int.MinValue + 3, 5, 6)))
            }

            it("{-3,-1} + {-3,Int.MaxValue} => {-6,-4,Int.MaxValue-3,Int.MaxValue-1}") {
                val v1 = IntegerSet(SortedSet[Int](-3, -1))
                val v2 = IntegerSet(SortedSet[Int](-3, Int.MaxValue))

                iadd(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-6, -4, Int.MaxValue - 3, Int.MaxValue - 1)))
                iadd(-1, v2, v1) should be(IntegerSet(SortedSet[Int](-6, -4, Int.MaxValue - 3, Int.MaxValue - 1)))
            }
        }

        describe("the behavior of isub") {

            it("{0,3} - {0,2} => {-2,0,1,3}") {
                val v1 = IntegerSet(SortedSet[Int](0, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                isub(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-2, 0, 1, 3)))
            }

            it("{-3,-1} - {-10,-2} => {-1,1,7,9}") {
                val v1 = IntegerSet(SortedSet[Int](-3, -1))
                val v2 = IntegerSet(SortedSet[Int](-10, -2))

                isub(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-1, 1, 7, 9)))
            }

            it("{-1,3} - {0,2} => {-3,-1,1,3}") {
                val v1 = IntegerSet(SortedSet[Int](-1, 3))
                val v2 = IntegerSet(SortedSet[Int](0, 2))

                isub(-1, v1, v2) should be(IntegerSet(SortedSet[Int](-3, -1, 1, 3)))
            }

            it("{0} - AnIntegerValue => AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = AnIntegerValue()

                isub(-1, v1, v2) should be(AnIntegerValue())
            }

            it("AnIntegerValue - {0} => AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](0))
                val v2 = AnIntegerValue()

                isub(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{Int.MinValue,3} - {2,3} => {0,1,Int.MinValue-2,Int.MinValue-3}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, 3))
                val v2 = IntegerSet(SortedSet[Int](2, 3))

                isub(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0, 1, Int.MinValue - 2, Int.MinValue - 3)))
            }

            it("{Int.MaxValue,3} - {-3,2} => {Int.MaxValue+3,1,6,Int.MaxValue-2}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MaxValue, 3))
                val v2 = IntegerSet(SortedSet[Int](-3, 2))

                isub(-1, v1, v2) should be(IntegerSet(SortedSet[Int](Int.MaxValue + 3, 1, 6, Int.MaxValue - 2)))
            }
        }

        describe("the behavior of idiv") {

            it("{1,3} / {2} => {0,1}") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](2))

                idiv(SomePC, v1, v2) should be(ComputedValue(IntegerSet(SortedSet[Int](0, 1))))
            }

            it("{1,3} / {1} => {1,3}") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](1))

                idiv(SomePC, v1, v2) should be(ComputedValue(IntegerSet(SortedSet[Int](1, 3))))
            }

            it("{1,3} / {0} => ThrowsException") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](0))

                val result = idiv(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{1,3} / {-1} => {-3,-1}") {
                val v1 = IntegerSet(SortedSet[Int](1, 3))
                val v2 = IntegerSet(SortedSet[Int](-1))

                idiv(SomePC, v1, v2) should be(ComputedValue(IntegerSet(SortedSet[Int](-3, -1))))
            }

            it("AnIntegerValue / {0} => ThrowsException") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](0))

                val result = idiv(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("AnIntegerValue / AnIntegerValue => Value and ThrowsException") {
                val v1 = AnIntegerValue()
                val v2 = AnIntegerValue()

                val result = idiv(SomePC, v1, v2)
                result.result should be { AnIntegerValue() }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{-1,200} / AnIntegerValue => Value and ThrowsException") {
                val v1 = IntegerSet(SortedSet[Int](-1, 200))
                val v2 = AnIntegerValue()

                val result = idiv(SomePC, v1, v2)
                result.result should be { AnIntegerValue() }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{Int.MinValue,-1} / Int.MaxValue => {-1,0}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, -1))
                val v2 = IntegerSet(SortedSet[Int](Int.MaxValue, Int.MaxValue))

                idiv(SomePC, v1, v2) should be(ComputedValue(IntegerSet(SortedSet[Int](-1, 0))))
            }

            it("{Int.MinValue,Int.MaxValue} / Int.MaxValue => {-1,1}") {
                val v1 = IntegerSet(SortedSet[Int](Int.MinValue, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](Int.MaxValue, Int.MaxValue))

                idiv(SomePC, v1, v2) should be(ComputedValue(IntegerSet(SortedSet[Int](-1, 1))))
            }
        }

        describe("the behavior of irem") {

            it("AnIntegerValue % AnIntegerValue => AnIntegerValue + Exception") {
                val v1 = AnIntegerValue()
                val v2 = AnIntegerValue()

                val result = irem(SomePC, v1, v2)
                result.result should be { AnIntegerValue() }
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is known, but the divisor is 0) {0,3} % {0} => Exception") {
                val v1 = IntegerSet(SortedSet[Int](0, 3))
                val v2 = IntegerSet(SortedSet[Int](0))

                val result = irem(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is 0) AnIntegerValue % {0} => Exception") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](0))

                val result = irem(SomePC, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValueLike(ObjectType.ArithmeticException) => /*OK*/
                    case v                                                => fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is known) AnIntegerValue % {2} => AnIntegerValue") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](2))

                val result = irem(SomePC, v1, v2)
                result.result should be(AnIntegerValue())
                result.throwsException should be(false)
            }

            it("(dividend and divisor are positive) {0,3} % {1,2} => {0,1}") {
                val v1 = IntegerSet(SortedSet[Int](0, 3))
                val v2 = IntegerSet(SortedSet[Int](1, 2))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](0, 1)))
            }

            it("(dividend and divisor are negative) {-10,-3} % {-2,-1} => {-1,0}") {
                val v1 = IntegerSet(SortedSet[Int](-10, -3))
                val v2 = IntegerSet(SortedSet[Int](-2, -1))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](-1, 0)))
            }

            it("(the dividend may be positive OR negative) {-10,3} % {1,2} => {0,1}") {
                val v1 = IntegerSet(SortedSet[Int](-10, 3))
                val v2 = IntegerSet(SortedSet[Int](1, 2))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](0, 1)))
            }

            it("(the dividend and the divisor may be positive OR negative) {-10,3} % {-3,4} => {-2,-1,0,3}") {
                val v1 = IntegerSet(SortedSet[Int](-10, 3))
                val v2 = IntegerSet(SortedSet[Int](-3, 4))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](-2, -1, 0, 3)))
            }

            it("(the dividend and the divisor are positive) {0,Int.MaxValue} % {16} => {0,15}") {
                val v1 = IntegerSet(SortedSet[Int](0, Int.MaxValue))
                val v2 = IntegerSet(SortedSet[Int](16))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](0, 15)))
            }

            it("(the dividend and the divisor are single values) {2} % {16} => {2}") {
                val v1 = IntegerSet(SortedSet[Int](2))
                val v2 = IntegerSet(SortedSet[Int](16))

                val result = irem(SomePC, v1, v2)
                result.result should be(IntegerSet(SortedSet[Int](2)))
            }
        }

        describe("the behavior of iand") {

            it("{3} & {255} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](3))
                val v2 = IntegerSet(SortedSet[Int](255))

                iand(-1, v1, v2) should be(IntegerSet(SortedSet[Int](3)))
                iand(-1, v2, v1) should be(IntegerSet(SortedSet[Int](3)))
            }

            it("{4} & {2} => {0}") {
                val v1 = IntegerSet(SortedSet[Int](4))
                val v2 = IntegerSet(SortedSet[Int](2))

                iand(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                iand(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("AnIntegerValue & {2} => AnIntegerValue") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](2))

                iand(-1, v1, v2) should be(AnIntegerValue())
                iand(-1, v2, v1) should be(AnIntegerValue())
            }

            it("{-2} & AnIntegerValue  => AnIntegerValue") {
                val v1 = IntegerSet(SortedSet[Int](-2))
                val v2 = AnIntegerValue()

                iand(-1, v1, v2) should be(AnIntegerValue())
                iand(-1, v2, v1) should be(AnIntegerValue())
            }

            it("The result of the and of a set s and {-1} should be s itself; {2,4} & {-1} => {2,4}") {
                val v1 = IntegerSet(SortedSet[Int](2, 4))
                val v2 = IntegerSet(SortedSet[Int](-1))

                iand(-1, v1, v2) should be(v1)
                iand(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value & {0} should be {0}") {
                val v1 = AnIntegerValue()
                val v2 = IntegerSet(SortedSet[Int](0))

                iand(-1, v1, v2) should be(IntegerSet(SortedSet[Int](0)))
                iand(-1, v2, v1) should be(IntegerSet(SortedSet[Int](0)))
            }
        }

        describe("the behavior of ishl") {

            it("AnIntegerValue << {2} => AnIntegerValue") {
                val v = AnIntegerValue()
                val s = IntegerSet(SortedSet[Int](2))

                ishl(-1, v, s) should be(AnIntegerValue())
            }

            it("{2} << AnIntegerValue => AnIntegerValue") {
                val v = IntegerSet(SortedSet[Int](2))
                val s = AnIntegerValue()

                ishl(-1, v, s) should be(AnIntegerValue())
            }

            it("{-1,1} << {2} => {-4,4}") {
                val v = IntegerSet(SortedSet[Int](-1, 1))
                val s = IntegerSet(SortedSet[Int](2))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](-4, 4)))
            }

            it("{64} << {64} => {64}") {
                val v = IntegerSet(SortedSet[Int](64))
                val s = IntegerSet(SortedSet[Int](64))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](64)))
            }

            it("{1} << {64} => {1}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](64))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{0} << {64} => {0}") {
                val v = IntegerSet(SortedSet[Int](0))
                val s = IntegerSet(SortedSet[Int](64))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{1} << {30} => {1073741824}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](30))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](1073741824)))
            }

            it("{1} << {2} => {4}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](2))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](4)))
            }

            it("{0,2} << {2} => {0,8}") {
                val v = IntegerSet(SortedSet[Int](0, 2))
                val s = IntegerSet(SortedSet[Int](2))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](0, 8)))
            }

            it("{1,2} << {2} => {4,8}") {
                val v = IntegerSet(SortedSet[Int](1, 2))
                val s = IntegerSet(SortedSet[Int](2))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](4, 8)))
            }

            it("{1,2} << {2,3} => {4,8,16}") {
                val v = IntegerSet(SortedSet[Int](1, 2))
                val s = IntegerSet(SortedSet[Int](2, 3))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](4, 8, 16)))
            }

            it("{Int.MinValue,-64,Int.MaxValue} << {2,3} => {-512,-256,-8,-4,0}") {
                val v = IntegerSet(SortedSet[Int](Int.MinValue, -64, Int.MaxValue))
                val s = IntegerSet(SortedSet[Int](2, 3))

                ishl(-1, v, s) should be(IntegerSet(SortedSet[Int](-512, -256, -8, -4, 0)))
            }

        }

        describe("the behavior of ishr") {

            it("AnIntegerValue >> {2} => AnIntegerValue") {
                val v = AnIntegerValue()
                val s = IntegerSet(SortedSet[Int](2))

                ishr(-1, v, s) should be(AnIntegerValue())
            }

            it("{2} >> AnIntegerValue => AnIntegerValue") {
                val v = IntegerSet(SortedSet[Int](2))
                val s = AnIntegerValue()

                ishr(-1, v, s) should be(AnIntegerValue())
            }

            it("{-1,1} >> {2} => {-1,0}") {
                val v = IntegerSet(SortedSet[Int](-1, 1))
                val s = IntegerSet(SortedSet[Int](2))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](-1, 0)))
            }

            it("{256} >> {64} => {256}") {
                val v = IntegerSet(SortedSet[Int](256))
                val s = IntegerSet(SortedSet[Int](64))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](256)))
            }

            it("{256} >> {8} => {1}") {
                val v = IntegerSet(SortedSet[Int](256))
                val s = IntegerSet(SortedSet[Int](8))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{256} >> {9} => {0}") {
                val v = IntegerSet(SortedSet[Int](256))
                val s = IntegerSet(SortedSet[Int](9))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{0} >> {64} => {0}") {
                val v = IntegerSet(SortedSet[Int](0))
                val s = IntegerSet(SortedSet[Int](64))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{1} >> {30} => {0}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](30))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{1} >> {2} => {0}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](2))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](0)))
            }

            it("{1} >> {0} => {1}") {
                val v = IntegerSet(SortedSet[Int](1))
                val s = IntegerSet(SortedSet[Int](0))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](1)))
            }

            it("{32,64} >> {2} => {8,16}") {
                val v = IntegerSet(SortedSet[Int](32, 64))
                val s = IntegerSet(SortedSet[Int](2))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](8, 16)))
            }

            it("{Int.MinValue,Int.MaxValue} >> {16,31} => {-32768,-1,0,32767}") {
                val v = IntegerSet(SortedSet[Int](Int.MinValue, Int.MaxValue))
                val s = IntegerSet(SortedSet[Int](16, 31))

                ishr(-1, v, s) should be(IntegerSet(SortedSet[Int](-32768, -1, 0, 32767)))
            }

        }

        describe("the behavior of the i2b cast operator") {

            it("(byte){-10,...,19} => {-10,...,+19}") {
                val v1 = IntegerSet(SortedSet[Int](-10 to 19: _*))
                i2b(-1, v1) should be(IntegerSet(SortedSet[Int](-10 to +19: _*)))
            }

            it("(byte){0,...,129} => {-128,-127,0,...,+127}") {
                val v1 = IntegerSet(SortedSet[Int](0 to 129: _*))
                i2b(-1, v1) should be(ByteValue(IrrelevantPC))
            }
        }

        describe("the behavior of the i2s cast operator") {

            it("(short)AnIntegerValue => AnIntegerValue") {
                val v1 = AnIntegerValue()
                i2s(-1, v1) should be(ShortValue(IrrelevantPC))
            }

            it("(short){-10,...,129} => {-10,...,60}") {
                val v1 = IntegerSet(SortedSet[Int](-10 to 60: _*))
                i2s(-1, v1) should be(IntegerSet(SortedSet[Int](-10 to 60: _*)))
            }

            it("(short){-128,...,+129000} => {-Short.MinValue,...,Short.MaxValue}") {
                val v1 = IntegerSet(SortedSet[Int](-128 to 129000: _*))
                i2s(-1, v1) should be(ShortValue(IrrelevantPC))
            }

        }

        describe("the behavior of the relational operators") {

            describe("the behavior of the greater or equal than (>=) operator") {
                it("{3} >= {2} => Yes") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](2))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{3} >= {3} => Yes") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](3))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{Int.MaxValue} >= AnIntegerValue should be Yes") {
                    val p1 = IntegerSet(SortedSet[Int](Int.MaxValue))
                    val p2 = AnIntegerValue()
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{0} >= {3} => No") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](0))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p2, p1) should be(No)
                }

                it("{2,3} >= {1,4} => Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](2, 3))
                    val p2 = IntegerSet(SortedSet[Int](1, 4))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("{1,4} >= {2,3} => Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](1, 4))
                    val p2 = IntegerSet(SortedSet[Int](2, 3))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("{Int.MinValue} >= AnIntegerValue should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](Int.MinValue))
                    val p2 = AnIntegerValue()
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("{3} >= {4} => No") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](4))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(No)
                }

                it("{-3,3} >= {-5,-4} => Yes") {
                    val p1 = IntegerSet(SortedSet[Int](-3, 3))
                    val p2 = IntegerSet(SortedSet[Int](-5, -4))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{-3,-2} >= {-4,-3} => Yes") {
                    val p1 = IntegerSet(SortedSet[Int](-3, -2))
                    val p2 = IntegerSet(SortedSet[Int](-4, -3))
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("a specific (but unknown) value compared (>=) with itself should be Yes") {
                    val p = AnIntegerValue()
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the greater or equal than (<=) operator") {
                it("a specific (but unknown) value compared (<=) with itself should be Yes") {
                    val p = AnIntegerValue()
                    intIsLessThanOrEqualTo(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the greater than (>) operator") {

                it("{3} > {2} should be Yes") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](2))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{3,300} > {0,2} should be Yes") {
                    val p1 = IntegerSet(SortedSet[Int](3, 300))
                    val p2 = IntegerSet(SortedSet[Int](0, 2))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("{3} > {0,3} should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](0, 3))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("{0} > {3} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](0))
                    intIsGreaterThan(IrrelevantPC, p2, p1) should be(No)
                }

                it("{2,3} > {1,4} should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](2, 3))
                    val p2 = IntegerSet(SortedSet[Int](1, 4))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("{-3,3} > {3,30} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](-3, 3))
                    val p2 = IntegerSet(SortedSet[Int](3, 30))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                }

                it("{3} > {3} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](3))
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                    intIsGreaterThan(IrrelevantPC, p1, p1) should be(No)
                }

                it("{Int.MinValue} > AnIntegerValue should be No") {
                    val p1 = IntegerSet(SortedSet[Int](Int.MinValue))
                    val p2 = AnIntegerValue()
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                }

                it("a specific (but unknown) value compared (>) with itself should be No") {
                    val p = AnIntegerValue()
                    intIsGreaterThan(IrrelevantPC, p, p) should be(No)
                }
            }

            describe("the behavior of the small than (<) operator") {

                it("a specific (but unknown) value compared (<) with itself should be No") {
                    val p = AnIntegerValue()
                    intIsLessThan(IrrelevantPC, p, p) should be(No)
                }

                it("{0,3} < {4,10} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](0, 3))
                    val p2 = IntegerSet(SortedSet[Int](4, 10))
                    intIsLessThan(IrrelevantPC, p1, p2) should be(Yes)
                    intIsLessThan(IrrelevantPC, p2, p1) should be(No)
                }

                it("{0,3} < {0,3} should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](0, 3))
                    val p2 = IntegerSet(SortedSet[Int](0, 3))
                    intIsLessThan(IrrelevantPC, p1, p2) should be(Unknown)
                    intIsLessThan(IrrelevantPC, p2, p1) should be(Unknown) // reflexive
                }

            }

            describe("the behavior of the equals (==) operator") {

                it("{3} == {3} should be Yes") {
                    val p1 = IntegerSet(SortedSet[Int](3))
                    val p2 = IntegerSet(SortedSet[Int](3))
                    intAreEqual(IrrelevantPC, p1, p2) should be(Yes)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Yes)
                    intAreEqual(IrrelevantPC, p1, p1) should be(Yes) // reflexive
                }

                it("{2} == {3} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](2))
                    val p2 = IntegerSet(SortedSet[Int](3))
                    intAreEqual(IrrelevantPC, p1, p2) should be(No)
                    intAreEqual(IrrelevantPC, p2, p1) should be(No) // reflexive
                }

                it("{0,3} == {4,10} should be No") {
                    val p1 = IntegerSet(SortedSet[Int](0, 3))
                    val p2 = IntegerSet(SortedSet[Int](4, 10))
                    intAreEqual(IrrelevantPC, p1, p2) should be(No)
                    intAreEqual(IrrelevantPC, p2, p1) should be(No) // reflexive
                }

                it("{0,3} == {3} should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](0, 3))
                    val p2 = IntegerSet(SortedSet[Int](3))
                    intAreEqual(IrrelevantPC, p1, p2) should be(Unknown)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Unknown) // reflexive
                }

                it("{0,3} == {0,3} should be Unknown") {
                    val p1 = IntegerSet(SortedSet[Int](0, 3))
                    val p2 = IntegerSet(SortedSet[Int](0, 3))
                    intAreEqual(IrrelevantPC, p1, p2) should be(Unknown)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Unknown) // reflexive
                }

                it("a specific (but unknown) value compared (==) with itself should be Yes") {
                    val p = AnIntegerValue()
                    intAreEqual(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the not equals (!=) operator") {

                it("a specific (but unknown) value compared (!=) with itself should be Yes") {
                    val p = AnIntegerValue()
                    intAreNotEqual(IrrelevantPC, p, p) should be(No)
                }

            }

            describe("the behavior of intIsSomeValueInRange") {

                it("if the precise value is unknown") {
                    val p = AnIntegerValue()
                    intIsSomeValueInRange(IrrelevantPC, p, -10, 20) should be(Unknown)
                }

                it("if no value is in the range (values are smaller)") {
                    val p = IntegerSet(SortedSet[Int](-10, 10))
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -20) should be(No)
                }

                it("if no value is in the range (values are larger)") {
                    val p = IntegerSet(SortedSet[Int](100, 10000))
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -20) should be(No)
                }

                it("if some value is in the range (completely enclosed)") {
                    val p = IntegerSet(SortedSet[Int](-19, -10))
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }

                it("if some value is in the range (lower-end is overlapping)") {
                    val p = IntegerSet(SortedSet[Int](-1000, -80))
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }

                it("if some value is in the range (higher-end is overlapping)") {
                    val p = IntegerSet(SortedSet[Int](-10, 10))
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }

                it("if the small values are too small and the large values are too large") {
                    val p = IntegerSet(SortedSet[Int](-10, 10))
                    intIsSomeValueInRange(IrrelevantPC, p, 0, 1) should be(No)
                }

            }
        }

        describe("using IntegerSetValues") {

            val aiProject = org.opalj.br.TestSupport.biProject("ai.jar")
            val IntegerValues = aiProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

            describe("without constraint tracking between values") {

                it("it should be able to collect a switch statement's cases and use that information to calculate a result") {
                    val domain = new IntegerSetsTestDomain
                    val method = IntegerValues.findMethod("someSwitch").head
                    /*val result =*/ BaseAI(method, domain)
                    if (domain.allReturnedValues.size != 1)
                        fail("expected one result; found: "+domain.allReturnedValues)

                    domain.allReturnedValues.head._2 should be(domain.IntegerSet(SortedSet[Int](0, 2, 4, 8)))
                }

                it("it should be able to track integer values such that it is possible to potentially identify an array index out of bounds exception") {
                    val domain = new IntegerSetsTestDomain(20) // the array has a maximum size of 10
                    val method = IntegerValues.findMethod("array10").head
                    val result = BaseAI(method, domain)
                    if (domain.allReturnedValues.size != 1)
                        fail("expected one result; found: "+domain.allReturnedValues)

                    // we don't know the size of the array
                    domain.allReturnedValues.head._2 abstractsOver (
                        domain.ReferenceValue(2, ArrayType(IntegerType))
                    ) should be(true)

                    // get the loop counter at the "icmple instruction" which controls the
                    // loops that initializes the array
                    result.operandsArray(20).tail.head should be(domain.IntegerSet(SortedSet[Int](0 to 11: _*)))
                }

                it("it should be possible to identify dead code - even for complex conditions") {
                    val domain = new IntegerSetsTestDomain
                    val method = IntegerValues.findMethod("deadCode").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(47) should be(null)
                    result.operandsArray(48) should be(null)
                    result.operandsArray(50) should be(null)

                    result.operandsArray(62) should be(null)
                    result.operandsArray(62) should be(null)
                    result.operandsArray(65) should be(null)
                    result.operandsArray(68) should be(null)
                    //...
                    result.operandsArray(89) should be(null)

                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v1)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues1_v1").head
                    val result = BaseAI(method, domain)

                    if (result.operandsArray(37) != null) {
                        result.operandsArray(37).head should be(domain.AnIntegerValue())
                        result.operandsArray(41).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    } else {
                        // OK - the code is dead, however, we cannot identify this, but the
                        // above is a safe approximation!
                    }
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v2)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues1_v2").head
                    val result = BaseAI(method, domain)

                    if (result.operandsArray(37) != null) {
                        result.operandsArray(37).head should be(domain.AnIntegerValue())
                        result.operandsArray(41).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    } else {
                        // OK - the code is dead, however, we cannot identify this, but the
                        // above is a safe approximation!
                    }
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v3)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues1_v3").head
                    val result = BaseAI(method, domain)

                    if (result.operandsArray(38) != null) {
                        result.operandsArray(38).head should be(domain.AnIntegerValue())
                        result.operandsArray(42).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    } else {
                        // OK - the code is dead, however, we cannot identify this, but the
                        // above is a safe approximation!
                    }
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues2)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues2").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(38).head should be(domain.AnIntegerValue())
                    result.operandsArray(42).head should be(domain.IntegerSet(SortedSet[Int](0)))
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues3)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues3").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(45).head should be(domain.AnIntegerValue())
                    result.operandsArray(49).head should be(domain.IntegerSet(SortedSet[Int](0)))
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues4)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues4").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(46).head should be(domain.IntegerSet(SortedSet[Int](2)))
                    result.operandsArray(50).head should be(domain.AnIntegerValue())
                    result.operandsArray(54).head should be(domain.AnIntegerValue())
                    if (result.operandsArray(50).head eq result.operandsArray(54).head)
                        fail("unequal values are made equal")
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues5)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues5").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(47).head should be(domain.IntegerSet(SortedSet[Int](2)))
                    result.operandsArray(51).head should be(domain.IntegerSet(SortedSet[Int](0, 1)))
                    result.operandsArray(55).head should be(domain.AnIntegerValue())
                }

                it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues6)") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("cfDependentValues6").head
                    val result = BaseAI(method, domain)

                    result.operandsArray(77).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    result.operandsArray(81).head should be(domain.AnIntegerValue())
                    result.operandsArray(85).head should be(domain.AnIntegerValue())
                    result.operandsArray(89).head should be(domain.IntegerSet(SortedSet[Int](0)))

                    result.operandsArray(97).head should be(domain.AnIntegerValue())
                    result.operandsArray(101).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    result.operandsArray(105).head should be(domain.AnIntegerValue())
                    result.operandsArray(109).head should be(domain.IntegerSet(SortedSet[Int](0)))

                    result.operandsArray(117).head should be(domain.AnIntegerValue())
                    result.operandsArray(121).head should be(domain.AnIntegerValue())
                    result.operandsArray(125).head should be(domain.IntegerSet(SortedSet[Int](0)))
                    result.operandsArray(129).head should be(domain.IntegerSet(SortedSet[Int](0)))
                }

                it("it should not perform useless evaluations") {
                    val domain = new IntegerSetsTestDomain(8)
                    val method = IntegerValues.findMethod("complexLoop").head
                    val result = BaseAI(method, domain)
                    result.operandsArray(35).head should be(domain.IntegerSet(SortedSet[Int](0, 1, 2)))
                    // when we perform a depth-first evaluation we do not want to
                    // evaluate the same instruction with the same abstract state
                    // multiple times
                    result.evaluatedPCs.size should be(43)
                }

                it("it should handle cases where we have more complex aliasing") {
                    val domain = new IntegerSetsTestDomain(4)
                    val method = IntegerValues.findMethod("moreComplexAliasing").head
                    val result = BaseAI(method, domain)

                    result.operandsArray(20).head should be(domain.AnIntegerValue())
                }
            }
        }
    }
}
