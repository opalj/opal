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
import org.scalatest.ParallelTestExecution

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.{ ObjectType, ArrayType, IntegerType }

/**
 * Tests the IntegerRanges Domain.
 *
 * @author Michael Eichberg
 * @author Christos Votskos
 */
@RunWith(classOf[JUnitRunner])
class DefaultIntegerRangesTest extends FunSpec with Matchers with ParallelTestExecution {

    final val IrrelevantPC = Int.MinValue

    class IntegerRangesTestDomain(
        override val maxCardinalityOfIntegerRanges: Long = -(Int.MinValue.toLong) + Int.MaxValue)
            extends CoRelationalDomain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultReferenceValuesBinding
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l1.DefaultIntegerRangeValues // <----- The one we are going to test
            with l0.DefaultPrimitiveValuesConversions
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with PredefinedClassHierarchy
            with RecordLastReturnedValues

    describe("central properties of domains that use IntegerRange values") {

        val theDomain = new IntegerRangesTestDomain
        import theDomain._

        it("the representation of the constant integer value 0 should be an IntegerRange value") {
            theDomain.IntegerConstant0 should be(IntegerRange(0, 0))
        }
    }

    describe("operations involving IntegerRange values") {

        describe("the behavior of join if we exceed the max spread") {

            val theDomain = new IntegerRangesTestDomain(2l)
            import theDomain._

            it("(join of two ranges with positive values that exceed the spread); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerRange(lb = 2, ub = 3)
                val v2 = IntegerRange(lb = 5, ub = 6)
                v1.join(-1, v2) should be(StructuralUpdate(AnIntegerValue))
                v2.join(-1, v1) should be(StructuralUpdate(AnIntegerValue))
            }

            it("(join of two ranges with positive values that do not exceed the spread); i1 join i2 => \"StructuralUpdate(IntegerRange(2,4))\"") {
                val v1 = IntegerRange(lb = 2, ub = 3)
                val v2 = IntegerRange(lb = 3, ub = 4)
                v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(2, 4)))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerRange(2, 4)))
            }

            it("(join of two ranges with negative values that exceed the spread); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerRange(lb = -2, ub = -1)
                val v2 = IntegerRange(lb = -5, ub = -4)
                v1.join(-1, v2) should be(StructuralUpdate(AnIntegerValue))
                v2.join(-1, v1) should be(StructuralUpdate(AnIntegerValue))
            }

            it("(join of two ranges with negative values that do not exceed the spread); i1 join i2 => \"StructuralUpdate(IntegerRange(-3,-1))\"") {
                val v1 = IntegerRange(lb = -2, ub = -1)
                val v2 = IntegerRange(lb = -3, ub = -2)
                v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(-3, -1)))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerRange(-3, -1)))
            }

            it("(join of two ranges with Int.MaxValue); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerRange(lb = 1, ub = Int.MaxValue)
                val v2 = IntegerRange(lb = -10, ub = -1)
                v1.join(-1, v2) should be(StructuralUpdate(AnIntegerValue))
                v2.join(-1, v1) should be(StructuralUpdate(AnIntegerValue))
            }

            it("(join of two ranges one with [Int.MinValue+1 and Int.MaxValue]); i1 join i2 => \"StructuralUpdate(AnIntegerValue)\"") {
                val v1 = IntegerRange(lb = Int.MinValue + 1, ub = Int.MaxValue)
                val v2 = IntegerRange(lb = -10, ub = -1)
                v1.join(-1, v2) should be(StructuralUpdate(AnIntegerValue))
                v2.join(-1, v1) should be(StructuralUpdate(AnIntegerValue))
            }

        }

        val theDomain = new IntegerRangesTestDomain
        import theDomain._

        describe("the behavior of the join operation if we do not exceed the max. spread") {

            it("(join with itself) val ir = IntegerRange(...); ir join ir => \"NoUpdate\"") {
                val v = IntegerRange(0, 0)
                v.join(-1, v) should be(NoUpdate)
            }

            it("(join of disjoint ranges) [Int.MinValue,-1] join [1,Int.MaxValue] => [Int.MinValue,Int.MaxValue]") {
                val v1 = IntegerRange(Int.MinValue, -1)
                val v2 = IntegerRange(1, Int.MaxValue)

                v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(Int.MinValue, Int.MaxValue)))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerRange(Int.MinValue, Int.MaxValue)))
            }

            it("(join of overlapping IntegerRange values) [-1,1] join [0,2] => [-1,2]") {
                val v1 = IntegerRange(-1, 1)
                val v2 = IntegerRange(0, 2)

                v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(-1, 2)))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerRange(-1, 2)))
            }

            it("(join of an IntegerRange value and an IntegerRange value that describes a sub-range) [-1,3] join [0,2] => \"MetaInformationUpdate\"") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                v1.join(-1, v2) should be('isMetaInformationUpdate)
            }

            it("(join of an IntegerRange value and an IntegerRange value that describes a sub-range) [0,2] join [-1,3] => \"StructuralUpdate\";  [0,2] join l @ [-1,3] => l'") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                val result = v2.join(-1, v1)
                result should be(StructuralUpdate(IntegerRange(-1, 3)))
                assert(result.value ne v1)
            }

            it("(join of a \"point\" range with a non-overlapping range) [0,0] join [1,Int.MaxValue]") {
                val v1 = IntegerRange(lb = 0, ub = 0)
                val v2 = IntegerRange(lb = 1, ub = 2147483647)
                v1.join(-1, v2) should be(StructuralUpdate(IntegerRange(0, 2147483647)))
                v2.join(-1, v1) should be(StructuralUpdate(IntegerRange(0, 2147483647)))
            }

        }

        describe("the behavior of the \"summarize\" function") {

            it("it should be able to handle overlapping values") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                summarize(-1, Iterable(v1, v2)) should be(IntegerRange(-1, 3))
                summarize(-1, Iterable(v2, v1)) should be(IntegerRange(-1, 3))
            }

            it("it should calculate the maximum range for non-overlapping values") {
                val v1 = IntegerRange(2, Int.MaxValue)
                val v2 = IntegerRange(-1, 2)

                summarize(-1, Iterable(v1, v2)) should be(IntegerRange(-1, Int.MaxValue))
                summarize(-1, Iterable(v2, v1)) should be(IntegerRange(-1, Int.MaxValue))
            }

            it("a summary involving some IntegerValue should result in AnIntegerValue") {
                val v1 = IntegerRange(2, Int.MaxValue)
                val v2 = IntegerValue(-1 /*PC*/ )

                summarize(-1, Iterable(v1, v2)) should be(AnIntegerValue())
                summarize(-1, Iterable(v2, v1)) should be(AnIntegerValue())
            }

            it("should calculate the correct summary if Int.MaxValue is involved") {
                val v1 = IntegerRange(lb = 0, ub = 0)
                val v2 = IntegerRange(lb = 1, ub = 2147483647)
                summarize(-1, Iterable(v1, v2)) should be(IntegerRange(0, 2147483647))
                summarize(-1, Iterable(v2, v1)) should be(IntegerRange(0, 2147483647))
            }

            it("should calculate the correct summary if Int.MinValue is involved") {
                val v1 = IntegerRange(lb = Int.MinValue, ub = 0)
                val v2 = IntegerRange(lb = 1, ub = 2)
                summarize(-1, Iterable(v1, v2)) should be(IntegerRange(Int.MinValue, 2))
                summarize(-1, Iterable(v2, v1)) should be(IntegerRange(Int.MinValue, 2))
            }
        }

        describe("the behavior of imul") {

            it("[0,3] * [0,2] => [lb*lb=0,ub*ub=6]") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(0, 2)

                imul(-1, v1, v2) should be(IntegerRange(0, 6))
                imul(-1, v2, v1) should be(IntegerRange(0, 6))
            }

            it("[-3,-1] * [-10,-2] => [ub*ub=2,lb*lb=30]") {
                val v1 = IntegerRange(-3, -1)
                val v2 = IntegerRange(-10, -2)

                imul(-1, v1, v2) should be(IntegerRange(2, 30))
                imul(-1, v2, v1) should be(IntegerRange(2, 30))
            }

            it("[-1,3] * [0,2] => [lb*ub=-2,ub*ub=6]") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                imul(-1, v1, v2) should be(IntegerRange(-2, 6))
                imul(-1, v2, v1) should be(IntegerRange(-2, 6))
            }

            it("[-3,3] * [-3,2] => [lb*ub=-6,lb*lb=9]") {
                val v1 = IntegerRange(-3, 3)
                val v2 = IntegerRange(-3, 2)

                imul(-1, v1, v2) should be(IntegerRange(-9, 9))
                imul(-1, v2, v1) should be(IntegerRange(-9, 9))
            }

            it("[0,0] * AnIntegerValue => [0,0]") {
                val v1 = IntegerRange(0, 0)
                val v2 = AnIntegerValue()

                imul(-1, v1, v2) should be(IntegerRange(0, 0))
                imul(-1, v2, v1) should be(IntegerRange(0, 0))
            }

            it("[Int.MinValue,3] * [-3,2] => AnIntegerValue") {
                val v1 = IntegerRange(Int.MinValue, 3)
                val v2 = IntegerRange(-3, 2)

                imul(-1, v1, v2) should be(AnIntegerValue())
                imul(-1, v2, v1) should be(AnIntegerValue())
            }

            it("[0,3] * [-3,Int.MaxValue] => AnIntegerValue") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(-3, Int.MaxValue)

                imul(-1, v1, v2) should be(AnIntegerValue())
                imul(-1, v2, v1) should be(AnIntegerValue())
            }

        }

        describe("the behavior of ior") {

            it("AnIntegerValue | [8,19] => AnIntegerRange") {
                val v = AnIntegerValue
                val s = IntegerRange(8, 19)

                ior(-1, v, s) should be(AnIntegerValue)
            }

            it("[Int.MinValue,Int.MaxValue] | [8,19] => [Int.MinValue, Int.MaxValue]") {
                val v = IntegerRange(Int.MinValue, Int.MaxValue)
                val s = IntegerRange(8, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(Int.MinValue)
                        ub should ===(Int.MaxValue)
                    case v ⇒
                        fail(s"expected [Int.MinValue, Int.MaxValue]; found $v")
                }
            }

            it("(two point ranges) [Int.MaxValue-2,Int.MaxValue-2] | [Int.MaxValue-1,Int.MaxValue-1] => [Int.MaxValue, Int.MaxValue]") {
                val v = IntegerRange(Int.MaxValue - 2, Int.MaxValue - 2)
                val s = IntegerRange(Int.MaxValue - 1, Int.MaxValue - 1)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(Int.MaxValue)
                        ub should ===(Int.MaxValue)
                    case v ⇒
                        fail(s"expected [Int.MinValue, Int.MaxValue]; found $v")
                }
            }

            it("[Int.MaxValue-16,Int.MaxValue-8] | [Int.MaxValue-32,Int.MaxValue-16] => [Int.MaxValue, Int.MaxValue]") {
                val v = IntegerRange(Int.MaxValue - 16, Int.MaxValue - 8)
                val s = IntegerRange(Int.MaxValue - 32, Int.MaxValue - 16)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(Int.MaxValue - 32)
                        ub should ===(Int.MaxValue)
                    case v ⇒
                        fail(s"expected [Int.MinValue, Int.MaxValue]; found $v")
                }
            }

            it("[Int.MinValue, Int.MaxValue] | [8,19] => [Int.MinValue, Int.MaxValue]") {
                val v1 = IntegerRange(Int.MinValue, Int.MaxValue)
                val v2 = IntegerRange(8, 19)

                ior(-1, v1, v2) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(Int.MinValue)
                        ub should ===(Int.MaxValue)
                    case v ⇒
                        fail(s"expected [Int.MinValue, Int.MaxValue]; found $v")
                }
            }

            it("[8,19] | [Int.MinValue, Int.MaxValue] => [Int.MinValue, Int.MaxValue]") {
                val v1 = IntegerRange(8, 19)
                val v2 = IntegerRange(Int.MinValue, Int.MaxValue)

                ior(-1, v1, v2) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(Int.MinValue)
                        ub should ===(Int.MaxValue)
                    case v ⇒
                        fail(s"expected [Int.MinValue, Int.MaxValue]; found $v")
                }
            }

            it("[Int.MinValue,2] | [8,19] => [Int.MinValue, 19]") {
                val v1 = IntegerRange(Int.MinValue, 2)
                val v2 = IntegerRange(8, 19)

                ior(-1, v1, v2) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===((Int.MinValue))
                        ub should be >= 19
                    case v ⇒
                        fail(s"expected [Int.MinValue, 19]; found $v")
                }
            }

            it("[3,3] | [8,19] => [11, 19]") {
                val v = IntegerRange(3, 3)
                val s = IntegerRange(8, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 11
                        ub should be >= 19
                    case v ⇒
                        fail(s"expected [11,19]; found $v")
                }
            }

            it("[3,3] | [19,19] => [19,19]") {
                val v = IntegerRange(3, 3)
                val s = IntegerRange(19, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 19
                        ub should be >= 19
                    case v ⇒ fail(s"expected [19,19]; found $v")
                }
            }

            it("[3,3] | [1,19] => [3,19]") {
                val v = IntegerRange(3, 3)
                val s = IntegerRange(1, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 3
                        ub should be >= 19
                    case v ⇒
                        fail(s"expected [3,19]; found $v")
                }
            }

            it("[1,3] | [1,4] => [1,7]") {
                val v = IntegerRange(1, 3)
                val s = IntegerRange(1, 4)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 1
                        ub should be >= 7
                    case v ⇒
                        fail(s"expected [1,7]; found $v")
                }
            }

            it("[0,20] | [8,10] => [8,30]") {
                val v = IntegerRange(0, 20)
                val s = IntegerRange(8, 10)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 8
                        ub should be >= 30
                    case v ⇒
                        fail(s"expected [8,30]; found $v")
                }
            }

            it("[0,0] | [0,0] => [0,0]") {
                val v = IntegerRange(0, 0)
                val s = IntegerRange(0, 0)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 0
                    case v ⇒
                        fail(s"expected [0,0]; found $v")
                }
            }

            it("[-5,3] | [8,19] => [-5, 19]") {
                val v = IntegerRange(-5, 3)
                val s = IntegerRange(8, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= 19
                    case v ⇒
                        fail(s"expected [-5,19]; found $v")
                }
            }

            it("[-5,-3] | [8,19] => [-5, -1]") {
                val v = IntegerRange(-5, -3)
                val s = IntegerRange(8, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-5,-1]; found $v")
                }
            }

            it("[-5,-3] | [-8,19] => [-5, -1]") {
                val v = IntegerRange(-5, -3)
                val s = IntegerRange(-8, 19)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-5,-1]; found $v")
                }
            }

            it("[-5,-3] | [-19,-8] => [-5, -1]") {
                val v = IntegerRange(-5, -3)
                val s = IntegerRange(-19, -8)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-5,-1]; found $v")
                }
            }

            it("[-5,0] | [-19,-8] => [-19, -1]") {
                val v = IntegerRange(-5, 0)
                val s = IntegerRange(-19, -8)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -19
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-19,-]; found $v")
                }
            }

            it("[-5,-3] | [-19,0] => [-5, -1]") {
                val v = IntegerRange(-5, -3)
                val s = IntegerRange(-19, 0)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-5,-1]; found $v")
                }
            }

            it("[3,5] | [-19,-1] => [-19, -1]") {
                val v = IntegerRange(3, 5)
                val s = IntegerRange(-19, -1)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -19
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-19,-1]; found $v")
                }
            }

            it("[3,5] | [-19,1] => [-19, 5]") {
                val v = IntegerRange(3, 5)
                val s = IntegerRange(-19, 1)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -19
                        ub should be >= 5
                    case v ⇒
                        fail(s"expected [-19,5]; found $v")
                }
            }

            it("[-3,5] | [-19,1] => [-19, 5]") {
                val v = IntegerRange(-3, 5)
                val s = IntegerRange(-19, 1)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -19
                        ub should be >= 5
                    case v ⇒
                        fail(s"expected [-19,5]; found $v")
                }
            }

            it("[-1,1] | [0,1] => [-1, 1]") {
                val v = IntegerRange(-1, 1)
                val s = IntegerRange(0, 1)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -1
                        ub should be >= 1
                    case v ⇒
                        fail(s"expected [-1,1]; found $v")
                }
            }

            it("[-10,-10] | [-9,-9] => [-9, -9]") {
                val v = IntegerRange(-10, -10)
                val s = IntegerRange(-9, -9)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -9
                        ub should be >= -9
                    case v ⇒
                        fail(s"expected [-9,-9]; found $v")
                }
            }

            it("[-10,-10] | [-9,0] => [-10, -1]") {
                val v = IntegerRange(-10, -10)
                val s = IntegerRange(-9, 0)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -10
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-10,-1]; found $v")
                }
            }

            it("[10,10] | [-9,-9] => [-1, -1]") {
                val v = IntegerRange(10, 10)
                val s = IntegerRange(-9, -9)

                ior(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -1
                        ub should be >= -1
                    case v ⇒
                        fail(s"expected [-1,-1]; found $v")
                }
            }
        }

        describe("the behavior of ishr") {

            it("[-100,-100] >> [4,4] => [-7, -7]") {
                val v = IntegerRange(-100, -100)
                val s = IntegerRange(4, 4)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(-7)
                        ub should ===(-7)
                    case v ⇒
                        fail(s"expected [-7,-7]; found $v")
                }
            }

            it("[20,20] >> [4,4] => [1, 1]") {
                val v = IntegerRange(20, 20)
                val s = IntegerRange(4, 4)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(1)
                        ub should ===(1)
                    case v ⇒
                        fail(s"expected [1,1]; found $v")
                }
            }

            it("[-10,5] >> [3,3] => [-2, 0]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(3, 3)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -2
                        ub should be >= 0
                    case v ⇒
                        fail(s"expected lb <= -2 and ub >= 0; found $v")
                }
            }

            it("[-10,5] >> [3,31] => [-2, 0]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(3, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -2
                        ub should be >= 0
                    case v ⇒
                        fail(s"expected lb <= -2 and ub >= 0; found $v")
                }
            }

            it("[-10,5] >> [1,3] => [-5, 2]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(1, 3)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= 2
                    case v ⇒
                        fail(s"expected lb <= -5 and ub >= 2; found $v")
                }
            }

            it("[-1,1] >> [0,31] => [-1, 1]") {
                val v = IntegerRange(-1, 1)
                val s = IntegerRange(0, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-1)
                        ub should be >= (1)
                    case v ⇒
                        fail(s"expected lb <= -1 and ub >= 1; found $v")
                }
            }

            it("[-10,-5] >> [1,3] => [-5, -1]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(1, 3)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-5)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -5 and ub >= -1; found $v")
                }
            }

            it("[-10,-5] >> [1,31] => [-5, -1]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(1, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-5)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -5 and ub >= -1; found $v")
                }
            }

            it("[10,50] >> [1,31] => [0, 25]") {
                val v = IntegerRange(10, 50)
                val s = IntegerRange(1, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (25)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 25; found $v")
                }
            }

            it("[1,5] >> [0,2] => [0, 5]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(0, 2)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (5)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 5; found $v")
                }
            }

            it("[Int.MinValue, Int.MinValue+100] >> [0,31] => [-2147483648,-1]") {
                val v = IntegerRange(Int.MinValue, Int.MinValue + 100)
                val s = IntegerRange(0, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= -1; found $v")
                }
            }

            it("[Int.MaxValue-1000, Int.MaxValue] >> [10,12] => [524287,2097151]") {
                val v = IntegerRange(Int.MaxValue - 1000, Int.MaxValue)
                val s = IntegerRange(10, 12)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (524287)
                        ub should be >= (2097151)
                    case v ⇒
                        fail(s"expected lb <= 524287 and ub >= 2097151; found $v")
                }
            }

            it("[-1000, -1] >> [-5,12] => [-1000,-1]") {
                val v = IntegerRange(-1000, -1)
                val s = IntegerRange(-5, 12)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-1000)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -1000 and ub >= -1; found $v")
                }
            }

            it("[-10, 1] >> [-8,12] => [-10,1]") {
                val v = IntegerRange(-10, 1)
                val s = IntegerRange(-8, 12)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-10)
                        ub should be >= (1)
                    case v ⇒
                        fail(s"expected lb <= -10 and ub >= 1; found $v")
                }
            }

            it("[10, 12] >> [-12,12] => [0,12]") {
                val v = IntegerRange(10, 12)
                val s = IntegerRange(-12, 12)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (12)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 12; found $v")
                }
            }

            it("[-1000, -1] >> [13,35] => [-1,-1]") {
                val v = IntegerRange(-1000, -1)
                val s = IntegerRange(13, 35)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(-1)
                        ub should ===(-1)
                    case v ⇒
                        fail(s"expected [-1,-1]; found $v")
                }
            }

            it("[-10, 1] >> [30,45] => [-1,0]") {
                val v = IntegerRange(-10, 1)
                val s = IntegerRange(30, 45)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(-1)
                        ub should ===(0)
                    case v ⇒
                        fail(s"expected [-1,0]; found $v")
                }
            }

            it("[1, 12] >> [20,120] => [0,0]") {
                val v = IntegerRange(1, 12)
                val s = IntegerRange(20, 120)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(0)
                        ub should ===(0)
                    case v ⇒
                        fail(s"expected [0,0]; found $v")
                }
            }

            it("[10, 10] >> [-9,-9] => [0,0]") {
                val v = IntegerRange(10, 10)
                val s = IntegerRange(-9, -9)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(0)
                        ub should ===(0)
                    case v ⇒
                        fail(s"expected [0,0]; found $v")
                }
            }

            it("[10, 10] >> [128,135] => [0,10]") {
                val v = IntegerRange(10, 10)
                val s = IntegerRange(128, 135)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (10)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 10; found $v")
                }
            }

            it("[10, 15] >> [-9,-9] => [0,15]") {
                val v = IntegerRange(10, 15)
                val s = IntegerRange(-9, -9)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (15)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 15; found $v")
                }
            }

            it("[-10, -5] >> [-9,-9] => [-10,-1]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(-9, -9)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-10)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -10 and ub >= -1; found $v")
                }
            }

            it("[-10, -5] >> [99,99] => [-10,-1]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(99, 99)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-10)
                        ub should be >= (-1)
                    case v ⇒
                        fail(s"expected lb <= -10 and ub >= -1; found $v")
                }
            }

            it("[-10, 5] >> [99,99] => [-10,5]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(99, 99)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-10)
                        ub should be >= (5)
                    case v ⇒
                        fail(s"expected lb <= -10 and ub >= 5; found $v")
                }
            }

            it("AnIntegerValue >> [31,31] => AnIntegerValue") {
                val v = AnIntegerValue()
                val s = IntegerRange(31, 31)

                ishr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(-1)
                        ub should ===(0)
                    case v ⇒
                        fail(s"expected [-1,0]; found $v")
                }
            }

            it("AnIntegerValue >> [0,0] => AnIntegerValue") {
                val v = AnIntegerValue()
                val s = IntegerRange(0, 0)

                ishr(-1, v, s) should be theSameInstanceAs v
            }

            it("[0, 0] >> AnIntegerValue => [0,0]") {
                val v = IntegerRange(0)
                val s = AnIntegerValue()

                ishr(-1, v, s) should be theSameInstanceAs v
            }
        }

        describe("the behavior of iadd") {

            it("[0,3] + [0,2] => [0,5]") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(0, 2)

                iadd(-1, v1, v2) should be(IntegerRange(0, 5))
                iadd(-1, v2, v1) should be(IntegerRange(0, 5))
            }

            it("[-3,-1] + [-10,-2] => [-13,-3]") {
                val v1 = IntegerRange(-3, -1)
                val v2 = IntegerRange(-10, -2)

                iadd(-1, v1, v2) should be(IntegerRange(-13, -3))
                iadd(-1, v2, v1) should be(IntegerRange(-13, -3))
            }

            it("[-1,3] + [0,2] => [-1,5]") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                iadd(-1, v1, v2) should be(IntegerRange(-1, 5))
                iadd(-1, v2, v1) should be(IntegerRange(-1, 5))
            }

            it("[0,0] + AnIntegerValue => AnIntegerValue") {
                val v1 = IntegerRange(0, 0)
                val v2 = AnIntegerValue()

                iadd(-1, v1, v2) should be(AnIntegerValue)
                iadd(-1, v2, v1) should be(AnIntegerValue)
            }

            it("[Int.MinValue,3] + [3,2] => [Int.MinValue+3,5]") {
                val v1 = IntegerRange(Int.MinValue, 3)
                val v2 = IntegerRange(3, 2)

                iadd(-1, v1, v2) should be(IntegerRange(Int.MinValue + 3, 5))
                iadd(-1, v2, v1) should be(IntegerRange(Int.MinValue + 3, 5))
            }

            it("[-3,-1] + [-3,Int.MaxValue] => [-6,Int.MaxValue-1]") {
                val v1 = IntegerRange(-3, -1)
                val v2 = IntegerRange(-3, Int.MaxValue)

                iadd(-1, v1, v2) should be(IntegerRange(-6, Int.MaxValue - 1))
                iadd(-1, v2, v1) should be(IntegerRange(-6, Int.MaxValue - 1))
            }

        }

        describe("the behavior of isub") {

            it("[0,3] - [0,2] => [-2,3]") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(0, 2)

                isub(-1, v1, v2) should be(IntegerRange(-2, 3))
            }

            it("[-3,-1] - [-10,-2] => [-1,9]") {
                val v1 = IntegerRange(-3, -1)
                val v2 = IntegerRange(-10, -2)

                isub(-1, v1, v2) should be(IntegerRange(-1, 9))
            }

            it("[-1,3] - [0,2] => [-3,3]") {
                val v1 = IntegerRange(-1, 3)
                val v2 = IntegerRange(0, 2)

                isub(-1, v1, v2) should be(IntegerRange(-3, 3))
            }

            it("[0,0] - AnIntegerValue => AnIntegerValue") {
                val v1 = IntegerRange(0, 0)
                val v2 = AnIntegerValue()

                isub(-1, v1, v2) should be(AnIntegerValue)
            }

            it("AnIntegerValue - [0,0] => AnIntegerValue") {
                val v1 = IntegerRange(0, 0)
                val v2 = AnIntegerValue()

                isub(-1, v2, v1) should be(AnIntegerValue)
            }

            it("[Int.MinValue,3] - [3,2] => AnIntegerValue") {
                val v1 = IntegerRange(Int.MinValue, 3)
                val v2 = IntegerRange(3, 2)

                isub(-1, v1, v2) should be(AnIntegerValue)
            }

            it("[3,2] - [Int.MinValue,3] => AnIntegerValue") {
                val v2 = IntegerRange(3, 2)
                val v1 = IntegerRange(Int.MinValue, 3)

                isub(-1, v2, v1) should be(AnIntegerValue)
            }

            it("[-3,-1] - [-3,Int.MaxValue] => AnIntegerValue") {
                val v1 = IntegerRange(-3, -1)
                val v2 = IntegerRange(-3, Int.MaxValue)

                isub(-1, v1, v2) should be(AnIntegerValue)
            }

            it("[-1,-1] - [-3,Int.MaxValue] => [Int.MinValue,2]") {
                val v1 = IntegerRange(-1, -1)
                val v2 = IntegerRange(-3, Int.MaxValue)

                isub(-1, v1, v2) should be(IntegerRange(Int.MinValue, 2))
            }

            it("A specific (but unknown) value - 'itself' => [0,0]") {
                val v = AnIntegerValue

                isub(-1, v, v) should be(IntegerRange(0, 0))
            }
        }

        describe("the behavior of idiv") {

            it("[1,3] / [2,2] => [0,1]") {
                val v1 = IntegerRange(1, 3)
                val v2 = IntegerRange(2, 2)

                idiv(-1, v1, v2) should be(ComputedValue(IntegerRange(0, 1)))
            }

            it("[1,3] / [1,1] => [1,3]") {
                val v1 = IntegerRange(1, 3)
                val v2 = IntegerRange(1, 1)

                idiv(-1, v1, v2) should be(ComputedValue(IntegerRange(1, 3)))
            }

            it("[1,3] / [0,0] => ThrowsException") {
                val v1 = IntegerRange(1, 3)
                val v2 = IntegerRange(0, 0)

                val result = idiv(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("[1,3] / [-1,-1] => ComputedValue") {
                val v1 = IntegerRange(1, 3)
                val v2 = IntegerRange(-1, -1)

                idiv(-1, v1, v2) should be(ComputedValue(AnIntegerValue()))
            }

            it("AnIntegerValue / [0,0] => ThrowsException") {
                val v1 = AnIntegerValue()
                val v2 = IntegerRange(0, 0)

                val result = idiv(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("AnIntegerValue / AnIntegerValue => Value and ThrowsException") {
                val v1 = AnIntegerValue()
                val v2 = AnIntegerValue()

                val result = idiv(-1, v1, v2)
                result.result shouldBe an[AnIntegerValue]
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("[-1,200] / AnIntegerValue => Value and ThrowsException") {
                val v1 = IntegerRange(-1, 200)
                val v2 = AnIntegerValue

                val result = idiv(-1, v1, v2)
                result.result shouldBe an[AnIntegerValue]
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("[Int.MinValue,-1] / Int.MaxValue => [1,1]") {
                val v1 = IntegerRange(Int.MinValue, -1)
                val v2 = IntegerRange(Int.MaxValue, Int.MaxValue)

                idiv(-1, v1, v2) should be(ComputedValue(IntegerRange(-1, 0)))
            }

            it("[Int.MinValue,Int.MaxValue] / Int.MaxValue => [1,1]") {
                val v1 = IntegerRange(Int.MinValue, Int.MaxValue)
                val v2 = IntegerRange(Int.MaxValue, Int.MaxValue)

                idiv(-1, v1, v2) should be(ComputedValue(IntegerRange(-1, 1)))
            }
        }

        describe("the behavior of irem") {

            it("AnIntegerValue % AnIntegerValue => AnIntegerValue + Exception") {
                val v1 = AnIntegerValue()
                val v2 = AnIntegerValue()

                val result = irem(-1, v1, v2)
                result.result shouldBe an[AnIntegerValue]
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is known, but the divisor is 0) [0,3] % [0,0] => Exception") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(0, 0)

                val result = irem(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is 0) AnIntegerValue % [0,0] => Exception") {
                val v1 = AnIntegerValue()
                val v2 = IntegerRange(0, 0)

                val result = irem(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is known) AnIntegerValue % [0,22] =>  [-21,21] + Exception") {
                val v1 = AnIntegerValue()
                val v2 = IntegerRange(0, 22)

                val result = irem(-1, v1, v2)
                result.result should be(IntegerRange(-21, 21))
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is known) AnIntegerValue % [2,4] =>  [-3,3]") {
                val v1 = AnIntegerValue()
                val v2 = IntegerRange(2, 4)

                val result = irem(-1, v1, v2)
                result.result should be(IntegerRange(-3, 3))
                result.throwsException should be(false)
            }

            it("(dividend and divisor are positive) [0,3] % [1,2] => [0,1]") {
                val v1 = IntegerRange(0, 3)
                val v2 = IntegerRange(1, 2)

                val result = irem(-1, v1, v2)
                val expected = ComputedValue(IntegerRange(0, 1))
                result should be(expected)
            }

            it("(dividend and divisor are negative) [-10,-3] % [-2,-1] => [-1,0]") {
                val v1 = IntegerRange(-10, -3)
                val v2 = IntegerRange(-2, -1)

                val result = irem(-1, v1, v2)
                val expected = ComputedValue(IntegerRange(-1, 0))
                result should be(expected)
            }

            it("(the dividend may be positive OR negative) [-10,3] % [1,2] => [-1,1]") {
                val v1 = IntegerRange(-10, 3)
                val v2 = IntegerRange(1, 2)

                val result = irem(-1, v1, v2)
                val expected = ComputedValue(IntegerRange(-1, 1))
                result should be(expected)
            }

            it("(the dividend and the divisor may be positive OR negative) [-10,3] % [-3,4] => [-3,3] + Exception") {
                val v1 = IntegerRange(-10, 3)
                val v2 = IntegerRange(-3, 4)

                val result = irem(-1, v1, v2)
                result.result should be(IntegerRange(-3, 3))
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend and the divisor are positive) [0,Int.MaxValue] % [16,16] => [0,15]") {
                val v1 = IntegerRange(0, Int.MaxValue)
                val v2 = IntegerRange(16, 16)

                val result = irem(-1, v1, v2)
                result.result should be(IntegerRange(0, 15))
            }

            it("(the dividend and the divisor are point values) [2,2] % [16,16] => [0,1]") {
                val v1 = IntegerRange(2, 2)
                val v2 = IntegerRange(16, 16)

                val result = irem(-1, v1, v2)
                result.result should be(IntegerRange(2, 2))
            }
        }

        describe("the behavior of iand") {

            it("[3,3] & [255,255] => [0,0]") {
                val v = IntegerRange(3, 3)
                val s = IntegerRange(255, 255)

                iand(-1, v, s) should be(IntegerRange(3, 3))
            }

            it("[4,4] & [2,2] => [0,0]") {
                val v = IntegerRange(4, 4)
                val s = IntegerRange(2, 2)

                iand(-1, v, s) should be(IntegerRange(0, 0))
            }

            it("[2,2] & [4,4] => [0,0]") {
                val v = IntegerRange(2, 2)
                val s = IntegerRange(4, 4)

                iand(-1, v, s) should be(IntegerRange(0, 0))
            }

            it("AnIntegerValue & [2,2] => [0,2]") {
                val v = AnIntegerValue
                val s = IntegerRange(2, 2)

                iand(-1, v, s) should be(IntegerRange(0, 2))
            }

            it("AnIntegerValue & [1,1] => [0,1]") {
                val v = AnIntegerValue
                val s = IntegerRange(1, 1)

                iand(-1, v, s) should be(IntegerRange(0, 1))
            }

            it("[1,1] & AnIntegerValue => [0,1]") {
                val s = AnIntegerValue
                val v = IntegerRange(1, 1)

                iand(-1, v, s) should be(IntegerRange(0, 1))
            }

            it("[2,2] & AnIntegerValue  => [0,2]") {
                val v = IntegerRange(2, 2)
                val s = AnIntegerValue

                iand(-1, v, s) should be(IntegerRange(0, 2))
            }

            it("[-2,-2] & AnIntegerValue  => [0,2]") {
                val v = IntegerRange(-2, -2)
                val s = AnIntegerValue

                iand(-1, v, s) should be(AnIntegerValue)
            }
        }

        describe("the behavior of iushr") {

            it("""(two "point" ranges where both values are larger than zero) [7,7] >>> [2,2] => -1 >>> 2 === 1073741823""") {
                val v = IntegerRange(7, 7)
                val s = IntegerRange(2, 2)
                val expected = 7 >>> 2
                iushr(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("""(two "point" ranges where the value is smaller than zero) [-1,-1] >>> [2,2] => -1 >>> 2 === 1073741823""") {
                val v = IntegerRange(-1, -1)
                val s = IntegerRange(2, 2)
                val expected = -1 >>> 2
                iushr(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("""(two "point" ranges which are both smaller than zero) [-5,-5] >>> [-2,-2] => -5 >>> -2 === 3""") {
                val v = IntegerRange(-5, -5)
                val s = IntegerRange(-2, -2)
                val expected = -5 >>> -2
                iushr(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("""(two "point" ranges where the shift value is smaller than zero) [5,5] >>> [-2,-2] => 5 >>> -2 === 0""") {
                val v = IntegerRange(5, 5)
                val s = IntegerRange(-2, -2)
                val expected = 5 >>> -2
                iushr(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("(no-shift/shift by zero) [-20, 20] >>> [0,0] => [-20,20]") {
                val v = IntegerRange(-20, 20)
                val s = IntegerRange(0, 0)

                iushr(-1, v, s) should be(IntegerRange(-20, 20))
            }

            it("[20, 20] >>> [2,4] => [1,5]") {
                val v = IntegerRange(20, 20)
                val s = IntegerRange(2, 4)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 1
                        ub should be >= 5

                    case v ⇒ fail(s"expected lb <= 1 and ub >= 5; found $v")
                }
            }

            it("[Int.MinValue, Int.MinValue+100] >>> [25,45] => [1,64]") {
                val v = IntegerRange(Int.MinValue, Int.MinValue + 100)
                val s = IntegerRange(25, 45)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 1
                        ub should be >= 64

                    case v ⇒ fail(s"expected lb <= 1 and ub >= 64; found $v")
                }
            }

            it("[2, 4] >>> [25,45] => [0,0]") {
                val v = IntegerRange(2, 4)
                val s = IntegerRange(25, 45)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 0

                    case v ⇒ fail(s"expected lb <= 0 and ub >= 0; found $v")
                }
            }

            it("[-2, 4] >>> [25,45] => [0,127]") {
                val v = IntegerRange(-2, 4)
                val s = IntegerRange(25, 45)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 127

                    case v ⇒ fail(s"expected lb <= 0 and ub >= 127; found $v")
                }
            }

            it("[5, 20] >>> [-20,1] => [2,20]") {
                val v = IntegerRange(5, 20)
                val s = IntegerRange(-20, 1)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 2
                        ub should be >= 20

                    case v ⇒ fail(s"expected lb <= 2 and ub >= 20; found $v")
                }
            }

            it("[-5, 20] >>> [-20,1] => [-5,2147483647]") {
                val v = IntegerRange(-5, 20)
                val s = IntegerRange(-20, 1)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -5
                        ub should be >= 2147483647

                    case v ⇒ fail(s"expected lb <= -5 and ub >= 2147483647; found $v")
                }
            }

            it("[-20, -5] >>> [-20,1] => [-20,2147483645]") {
                val v = IntegerRange(-20, -5)
                val s = IntegerRange(-20, 1)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -20
                        ub should be >= 2147483645

                    case v ⇒ fail(s"expected lb <= -20 and ub >= 2147483645; found $v")
                }
            }

            it("[Int.MinValue, Int.MinValue+100] >>> [25,31] => [1,64]") {
                val v = IntegerRange(Int.MinValue, Int.MinValue + 100)
                val s = IntegerRange(25, 31)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 1
                        ub should be >= 64

                    case v ⇒ fail(s"expected lb <= 1 and ub >= 64; found $v")
                }
            }

            it("[Int.MinValue+9999,Int.MaxValue] >>> [4,8] => [8388568,134217727]") {
                val v = IntegerRange(Int.MinValue + 9999, Int.MaxValue)
                val s = IntegerRange(4, 8)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 8388568
                        ub should be >= 134217727

                    case v ⇒ fail(s"expected lb <= 8388568 and ub >=134217727; found $v")
                }
            }

            it("[-1, 1] >>> [4,8] => [0, 268435455]") {
                val v = IntegerRange(-1, 1)
                val s = IntegerRange(4, 8)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 268435455

                    case v ⇒ fail(s"expected lb <= -1 and ub >=2147483647; found $v")
                }
            }

            it("[-25, 1] >>> [0,1] => [-25,2147483647]") {
                val v = IntegerRange(-25, 1)
                val s = IntegerRange(0, 1)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -25
                        ub should be >= 2147483647

                    case v ⇒ fail(s"expected lb <= -25 and ub >=2147483647; found $v")
                }
            }

            it("[25, 60] >>> [0,25] => [0,60]") {
                val v = IntegerRange(25, 60)
                val s = IntegerRange(0, 25)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 60

                    case v ⇒ fail(s"expected lb <= 0 and ub >= 60; found $v")
                }
            }

            it("[0,1] >>> [0,1] => [0,1]") {
                val v = IntegerRange(0, 1)
                val s = IntegerRange(0, 1)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 1

                    case v ⇒ fail(s"expected lb <= 0 and ub >= 60; found $v")
                }
            }

            it("[15,15] >>> [5,5] => [0,0]") {
                val v = IntegerRange(15, 15)
                val s = IntegerRange(5, 5)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(0)
                        ub should ===(0)

                    case v ⇒ fail(s"expected lb <= 0 and ub >=0; found $v")
                }
            }

            it("[-15,-15] >>> [5,5] => [134217727,134217727]") {
                val v = IntegerRange(-15, -15)
                val s = IntegerRange(5, 5)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should ===(134217727)
                        ub should ===(134217727)

                    case v ⇒ fail(s"expected lb <= 134217727 and ub >=134217727; found $v")
                }
            }

            it("[-15,15] >>> [5,5] => [0,134217727]") {
                val v = IntegerRange(-15, 15)
                val s = IntegerRange(5, 5)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= (134217727)

                    case v ⇒ fail(s"expected lb <= 0 and ub >=134217727; found $v")
                }
            }

            it("[-15,15] >>> [0,31] => [-15,2147483647]") {
                val v = IntegerRange(-15, 15)
                val s = IntegerRange(0, 31)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -15
                        ub should be >= 2147483647

                    case v ⇒ fail(s"expected lb <= -15 and ub >=2147483647; found $v")
                }
            }

            it("[5,45] >>> [0,31] => [0,45]") {
                val v = IntegerRange(5, 45)
                val s = IntegerRange(0, 31)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= 0
                        ub should be >= 45

                    case v ⇒ fail(s"expected lb <= 0 and ub >=45; found $v")
                }
            }

            it("[-45,-5] >>> [0,31] => [-45,2147483645]") {
                val v = IntegerRange(-45, -5)
                val s = IntegerRange(0, 31)

                iushr(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= -45
                        ub should be >= 2147483645

                    case v ⇒ fail(s"expected lb <= 0 and ub >=45; found $v")
                }
            }
        }

        describe("the behavior of ishl") {
            it("[10,22] << [12,31] => [-2147483648,2013265920]") {
                val v = IntegerRange(10, 22)
                val s = IntegerRange(12, 31)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (2013265920)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 2013265920; found $v")
                }
            }

            it("[1,22] << [1,4] => [2,352]") {
                val v = IntegerRange(1, 22)
                val s = IntegerRange(1, 4)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (2)
                        ub should be >= (352)
                    case v ⇒
                        fail(s"expected lb <= 2 and ub >= 352; found $v")
                }
            }

            it("[0,5] << [0,4] => [0,80]") {
                val v = IntegerRange(0, 5)
                val s = IntegerRange(0, 4)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (0)
                        ub should be >= (80)
                    case v ⇒
                        fail(s"expected lb <= 0 and ub >= 80; found $v")
                }
            }

            it("[1,5] << [4,4] => [16,80]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(4, 4)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (16)
                        ub should be >= (80)
                    case v ⇒
                        fail(s"expected lb <= 16 and ub >= 80; found $v")
                }
            }

            it("[1,5] << [-4,4] => [1,80]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(-4, 4)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (1)
                        ub should be >= (80)
                    case v ⇒
                        fail(s"expected lb <= 1 and ub >= 80; found $v")
                }
            }

            it("[1,5] << [-4,-1] => [-2147483648,1610612736]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(-4, -1)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (1610612736)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 1610612736; found $v")
                }
            }

            it("[1,5] << [-4,100] => [-2147483648,1610612736]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(-4, 100)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (1610612736)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 1610612736; found $v")
                }
            }

            it("[1,5] << [50,100] => [-2147483648,1610612736]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(50, 100)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (1610612736)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 1610612736; found $v")
                }
            }

            it("[1,5] << [0,0] => [1,5]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(0, 0)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (1)
                        ub should be >= (5)
                    case v ⇒
                        fail(s"expected lb <= 1 and ub >= 5; found $v")
                }
            }

            it("[1,5] << [Int.MaxValue,Int.MaxValue] => [-2147483648,0]") {
                val v = IntegerRange(1, 5)
                val s = IntegerRange(Int.MaxValue, Int.MaxValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (0)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 0; found $v")
                }
            }

            it("[-1,5] << [Int.MaxValue,Int.MaxValue] => [-2147483648,00]") {
                val v = IntegerRange(-1, 5)
                val s = IntegerRange(Int.MaxValue, Int.MaxValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (0)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 0; found $v")
                }
            }

            it("[-10,-5] << [Int.MaxValue,Int.MaxValue] => [-2147483648,00]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(Int.MaxValue, Int.MaxValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (0)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 0; found $v")
                }
            }

            it("[1,50] << [Int.MinValue,Int.MaxValue] => [-2147483648,2080374784]") {
                val v = IntegerRange(1, 50)
                val s = IntegerRange(Int.MinValue, Int.MaxValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (2080374784)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 2080374784; found $v")
                }
            }

            it("[-10,-5] << [Int.MinValue,Int.MinValue] => [-2147483648,1879048192]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(Int.MinValue, Int.MinValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (1879048192)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 1879048192; found $v")
                }
            }

            it("[10,50] << [Int.MinValue,Int.MinValue] => [-2147483648,2080374784]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(Int.MinValue, Int.MinValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (2080374784)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 2080374784; found $v")
                }
            }

            it("[-10,5] << [Int.MinValue,Int.MinValue] => [-2147483648,1879048192]") {
                val v = IntegerRange(-10, 5)
                val s = IntegerRange(Int.MinValue, Int.MinValue)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-2147483648)
                        ub should be >= (1879048192)
                    case v ⇒
                        fail(s"expected lb <= -2147483648 and ub >= 1879048192; found $v")
                }
            }

            it("[-100,-50] << [0,0] => [-100,-50]") {
                val v = IntegerRange(-100, -50)
                val s = IntegerRange(0, 0)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-100)
                        ub should be >= (-50)
                    case v ⇒
                        fail(s"expected lb <= -100 and ub >= -50; found $v")
                }
            }

            it("[-100,-50] << [3,24] => [-1677721600,-400]") {
                val v = IntegerRange(-100, -50)
                val s = IntegerRange(3, 24)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-1677721600)
                        ub should be >= (-400)
                    case v ⇒
                        fail(s"expected lb <= -1677721600 and ub >= -400; found $v")
                }
            }

            it("[-10,-5] << [0,24] => [-167772160,-5]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(0, 24)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-167772160)
                        ub should be >= (-5)
                    case v ⇒
                        fail(s"expected lb <= -167772160 and ub >= -5; found $v")
                }
            }

            it("[-10,-5] << [2,2] => [-40,-20]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-40)
                        ub should be >= (-20)
                    case v ⇒
                        fail(s"expected lb <= -40 and ub >= -20; found $v")
                }
            }

            it("[-10,-5] << [-2,2] => [-40,-5]") {
                val v = IntegerRange(-10, -5)
                val s = IntegerRange(-2, 2)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-40)
                        ub should be >= (-5)
                    case v ⇒
                        fail(s"expected lb <= -40 and ub >= -5; found $v")
                }
            }

            it("AnIntegerValue << [2,2] => AnIntegerValue") {
                val v = AnIntegerValue
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) should be(AnIntegerValue)
            }

            it("[2,2] << AnIntegerValue => AnIntegerValue") {
                val v = IntegerRange(2, 2)
                val s = AnIntegerValue

                ishl(-1, v, s) should be(AnIntegerValue)
            }

            it("[-1,1] << [2,2] => AnIntegerValue") {
                val v = IntegerRange(-1, 1)
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) match {
                    case (IntegerRange(lb, ub)) ⇒
                        lb should be <= (-4)
                        ub should be >= (4)
                    case v ⇒
                        fail(s"expected lb <= 4 and ub >= 4; found $v")
                }
            }

            it("""(two "point" ranges where the value is smaller than zero) [-1,-1] << [2,2] => -1 << 2""") {
                val v = IntegerRange(-1, -1)
                val s = IntegerRange(2, 2)
                val expected = -1 << 2
                ishl(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("""(two "point" ranges which are both smaller than zero) [-5,-5] << [-2,-2] => -5 << -2""") {
                val v = IntegerRange(-5, -5)
                val s = IntegerRange(-2, -2)
                val expected = -5 << -2
                ishl(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("""(two "point" ranges where the shift value is smaller than zero) [5,5] << [-2,-2] => 5 << -2""") {
                val v = IntegerRange(5, 5)
                val s = IntegerRange(-2, -2)
                val expected = 5 << -2
                ishl(-1, v, s) should be(IntegerRange(expected, expected))
            }

            it("(the shift value is constant but too large) [64,64] << [64,64] => [64,64]") {
                val v = IntegerRange(64, 64)
                val s = IntegerRange(64, 64)

                ishl(-1, v, s) should be(IntegerRange(64, 64))
            }

            it("[1,1] << [64,64] => [1,1]") {
                val v = IntegerRange(1, 1)
                val s = IntegerRange(64, 64)

                ishl(-1, v, s) should be(IntegerRange(1, 1))
            }

            it("[0,0] << [64,64] => [0,0]") {
                val v = IntegerRange(0, 0)
                val s = IntegerRange(64, 64)

                ishl(-1, v, s) should be(IntegerRange(0, 0))
            }

            it("[1,1] << [30,30] => [1073741824,1073741824]") {
                val v = IntegerRange(1, 1)
                val s = IntegerRange(30, 30)

                ishl(-1, v, s) should be(IntegerRange(1073741824, 1073741824))
            }

            it("[1,1] << [2,2] => [4,4]") {
                val v = IntegerRange(0, 2)
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) should be(IntegerRange(0, 8))
            }

            it("[0,2] << [2,2] => [0,8]") {
                val v = IntegerRange(0, 2)
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) should be(IntegerRange(0, 8))
            }

            it("[1,2] << [2,2] => [4,8]") {
                val v = IntegerRange(1, 2)
                val s = IntegerRange(2, 2)

                ishl(-1, v, s) should be(IntegerRange(4, 8))
            }

            it("[1,2] << [2,3] => [4,16]") {
                val v = IntegerRange(1, 2)
                val s = IntegerRange(2, 3)

                ishl(-1, v, s) should be(IntegerRange(4, 16))
            }

        }

        describe("the behavior of the i2b cast operator") {

            it("(byte)AnIntegerValue => [-128,+127]") {
                val v1 = AnIntegerValue
                i2b(-1, v1) should be(IntegerRange(-128, +127))
            }

            it("(byte)[-10,19] => [-10,+19]") {
                val v1 = IntegerRange(-10, 19)
                i2b(-1, v1) should be(IntegerRange(-10, +19))
            }

            it("(byte)[0,129] => [-128,+127]") {
                val v1 = IntegerRange(0, 129)
                i2b(-1, v1) should be(IntegerRange(-128, +127))
            }
        }

        describe("the behavior of the i2s cast operator") {

            it("(short)AnIntegerValue => [-Short.MinValue,Short.MaxValue]") {
                val v1 = AnIntegerValue
                i2s(-1, v1) should be(IntegerRange(Short.MinValue, Short.MaxValue))
            }

            it("(short)[0,129] => [0,129]") {
                val v1 = IntegerRange(0, 129)
                i2s(-1, v1) should be(IntegerRange(0, 129))
            }

            it("(short)[-128,+129000] => [-Short.MinValue,Short.MaxValue]") {
                val v1 = IntegerRange(-128, +129000)
                i2s(-1, v1) should be(IntegerRange(Short.MinValue, Short.MaxValue))
            }

        }

        describe("the behavior of the relational operators") {

            describe("the behavior of the greater or equal than (>=) operator") {
                it("[3,3] >= [0,2] => Yes") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 2)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, i2) should be(Yes)
                }

                it("[3,3] >= [0,3] => Yes") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 3)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, i2) should be(Yes)
                }

                it("[Int.MaxValue,Int.MaxValue] >= AnIntegerValue should be Yes") {
                    val p1 = IntegerRange(lb = Int.MaxValue, ub = Int.MaxValue)
                    val p2 = AnIntegerValue()
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Yes)
                }

                it("[0,3] >= [3,3] => Unknown") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 3)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, i2, p1) should be(Unknown)
                }

                it("[2,3] >= [1,4] => Unknown") {
                    val i1 = IntegerRange(lb = 2, ub = 3)
                    val i2 = IntegerRange(lb = 1, ub = 4)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, i1, i2) should be(Unknown)
                }

                it("[1,4] >= [2,3] => Unknown") {
                    val i1 = IntegerRange(lb = 1, ub = 4)
                    val i2 = IntegerRange(lb = 2, ub = 3)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, i1, i2) should be(Unknown)
                }

                it("[Int.MinValue,Int.MinValue] >= AnIntegerValue should be Unknown") {
                    val p1 = IntegerRange(lb = Int.MinValue, ub = Int.MinValue)
                    val p2 = AnIntegerValue()
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("[3,3] >= [4,4] => No") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val p2 = IntegerRange(lb = 4, ub = 4)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(No)
                }

                it("[-3,3] >= [4,40] => No") {
                    val p1 = IntegerRange(lb = -3, ub = 3)
                    val p2 = IntegerRange(lb = 4, ub = 40)
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p1, p2) should be(No)
                }

                it("a specific (but unknown) value compared (>=) with itself should be Yes") {
                    val p = AnIntegerValue
                    intIsGreaterThanOrEqualTo(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the greater or equal than (<=) operator") {
                it("a specific (but unknown) value compared (<=) with itself should be Yes") {
                    val p = AnIntegerValue
                    intIsLessThanOrEqualTo(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the greater than (>) operator") {

                it("[3,3] > [0,2] should be Yes") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 2)
                    intIsGreaterThan(IrrelevantPC, p1, i2) should be(Yes)
                }

                it("[3,300] > [0,2] should be Yes") {
                    val p1 = IntegerRange(lb = 3, ub = 300)
                    val i2 = IntegerRange(lb = 0, ub = 2)
                    intIsGreaterThan(IrrelevantPC, p1, i2) should be(Yes)
                }

                it("[3,3] > [0,3] should be Unknown") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 3)
                    intIsGreaterThan(IrrelevantPC, p1, i2) should be(Unknown)
                }

                it("[0,3] > [3,3] should be Unknown") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val i2 = IntegerRange(lb = 0, ub = 3)
                    intIsGreaterThan(IrrelevantPC, i2, p1) should be(Unknown)
                }

                it("[2,3] > [1,4] should be Unknown") {
                    val i1 = IntegerRange(lb = 2, ub = 3)
                    val i2 = IntegerRange(lb = 1, ub = 4)
                    intIsGreaterThan(IrrelevantPC, i1, i2) should be(Unknown)
                }

                it("[-3,3] > [3,30] should be Unknown") {
                    val p1 = IntegerRange(lb = -3, ub = 3)
                    val p2 = IntegerRange(lb = 3, ub = 30)
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(Unknown)
                }

                it("[3,3] > [4,4] should be No") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val p2 = IntegerRange(lb = 4, ub = 4)
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                }

                it("[3,3] > [3,3] should be No") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val p2 = IntegerRange(lb = 3, ub = 3)
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                    intIsGreaterThan(IrrelevantPC, p1, p1) should be(No)
                }

                it("[Int.MinValue,Int.MinValue] > AnIntegerValue should be No") {
                    val p1 = IntegerRange(lb = Int.MinValue, ub = Int.MinValue)
                    val p2 = AnIntegerValue()
                    intIsGreaterThan(IrrelevantPC, p1, p2) should be(No)
                }

                it("a specific (but unknown) value compared (>) with itself should be No") {
                    val p = AnIntegerValue
                    intIsGreaterThan(IrrelevantPC, p, p) should be(No)
                }
            }

            describe("the behavior of the greater than (<) operator") {

                it("a specific (but unknown) value compared (<) with itself should be No") {
                    val p = AnIntegerValue
                    intIsLessThan(IrrelevantPC, p, p) should be(No)
                }
            }

            describe("the behavior of the equals (==) operator") {

                it("[3,3] == [3,3] should be Yes") {
                    val p1 = IntegerRange(lb = 3, ub = 3)
                    val p2 = IntegerRange(lb = 3, ub = 3)
                    intAreEqual(IrrelevantPC, p1, p2) should be(Yes)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Yes)
                    intAreEqual(IrrelevantPC, p1, p1) should be(Yes) // reflexive
                }

                it("[2,2] == [3,3] should be No") {
                    val p1 = IntegerRange(lb = 2, ub = 2)
                    val p2 = IntegerRange(lb = 3, ub = 3)
                    intAreEqual(IrrelevantPC, p1, p2) should be(No)
                    intAreEqual(IrrelevantPC, p2, p1) should be(No) // reflexive
                }

                it("[0,3] == [4,10] should be No") {
                    val p1 = IntegerRange(lb = 0, ub = 3)
                    val p2 = IntegerRange(lb = 4, ub = 10)
                    intAreEqual(IrrelevantPC, p1, p2) should be(No)
                    intAreEqual(IrrelevantPC, p2, p1) should be(No) // reflexive
                }

                it("[0,3] == [3,3] should be Unknown") {
                    val p1 = IntegerRange(lb = 0, ub = 3)
                    val p2 = IntegerRange(lb = 3, ub = 3)
                    intAreEqual(IrrelevantPC, p1, p2) should be(Unknown)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Unknown) // reflexive
                }

                it("[0,3] == [0,3] should be Unknown") {
                    val p1 = IntegerRange(lb = 0, ub = 3)
                    val p2 = IntegerRange(lb = 0, ub = 3)
                    intAreEqual(IrrelevantPC, p1, p2) should be(Unknown)
                    intAreEqual(IrrelevantPC, p2, p1) should be(Unknown) // reflexive
                }

                it("a specific (but unknown) value compared (==) with itself should be Yes") {
                    val p = AnIntegerValue
                    intAreEqual(IrrelevantPC, p, p) should be(Yes)
                }
            }

            describe("the behavior of the not equals (!=) operator") {

                it("a specific (but unknown) value compared (!=) with itself should be Yes") {
                    val p = AnIntegerValue
                    intAreNotEqual(IrrelevantPC, p, p) should be(No)
                }

            }

            describe("the behavior of intIsSomeValueInRange") {

                it("if the precise value is unknown") {
                    val p = AnIntegerValue()
                    intIsSomeValueInRange(IrrelevantPC, p, -10, 20) should be(Unknown)
                }

                it("if no value is in the range (values are smaller)") {
                    val p = IntegerRange(-10, 10)
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -20) should be(No)
                }

                it("if no value is in the range (values are larger)") {
                    val p = IntegerRange(100, 10000)
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -20) should be(No)
                }

                it("if some value is in the range (completely enclosed)") {
                    val p = IntegerRange(-19, -10)
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }

                it("if some value is in the range (lower-end is overlapping)") {
                    val p = IntegerRange(-1000, -80)
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }

                it("if some value is in the range (higher-end is overlapping)") {
                    val p = IntegerRange(-10, 10)
                    intIsSomeValueInRange(IrrelevantPC, p, -100, -2) should be(Yes)
                }
            }
        }

    }

    describe("using IntegerRangeValues") {

        val testJAR = "classfiles/ai.jar"
        val testFolder = org.opalj.bi.TestSupport.locateTestResources(testJAR, "ai")
        val testProject = org.opalj.br.analyses.Project(testFolder)
        val IntegerValues = testProject.classFile(ObjectType("ai/domain/IntegerValuesFrenzy")).get

        describe("hanlding of complex dependent casts and moduluo operations") {

            it("the analysis should be correct in the presence of type casts (\"randomModulo\")") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("randomModulo").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(41).head should be(domain.IntegerRange(0, 0))
            }

        }

        describe("constraint propagation") {

            it("it should be able to adapt (<) the bounds of an IntegerRange value in the presences of aliasing") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("aliasingMax5").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 2)
                    fail("expected two results; found: "+domain.allReturnedValues)

                val summary = domain.summarize(-1, domain.allReturnedValues.toIterable.map(_._2))
                summary should be(domain.IntegerRange(Int.MinValue, 5))
            }

            it("it should be able to adapt (<=) the bounds of an IntegerRange value in the presences of aliasing") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("aliasingMax6").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 2)
                    fail("expected two results; found: "+domain.allReturnedValues)

                val summary = domain.summarize(-1, domain.allReturnedValues.toIterable.map(_._2))
                summary should be(domain.IntegerRange(Int.MinValue, 6))
            }

            it("it should be able to adapt (>=) the bounds of an IntegerRange value in the presences of aliasing") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("aliasingMinM1").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 2)
                    fail("expected two results; found: "+domain.allReturnedValues)

                val summary = domain.summarize(-1, domain.allReturnedValues.toIterable.map(_._2))
                summary should be(domain.IntegerRange(-1, Int.MaxValue))
            }

            it("it should be able to adapt (>) the bounds of an IntegerRange value in the presences of aliasing") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("aliasingMin0").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 2)
                    fail("expected two results; found: "+domain.allReturnedValues)

                val summary = domain.summarize(-1, domain.allReturnedValues.toIterable.map(_._2))
                summary should be(domain.IntegerRange(0, Int.MaxValue))
            }

            it("it should be able to collect a switch statement's cases and use that information to calculate a result") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("someSwitch").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 1)
                    fail("expected one result; found: "+domain.allReturnedValues)

                domain.allReturnedValues.head._2 should be(domain.IntegerRange(0, 8))
            }

            it("it should be able to detect contradicting conditions") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("someComparisonThatReturns5").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 2)
                    fail("expected one result; found: "+domain.allReturnedValues)

                val summary = domain.summarize(-1, domain.allReturnedValues.toIterable.map(_._2))
                summary should be(domain.IntegerRange(5, 5))
            }

            it("it should be able to track integer values such that it is possible to potentially identify an array index out of bounds exception") {
                val domain = new IntegerRangesTestDomain(20) // the array has a maximum size of 10
                val method = IntegerValues.findMethod("array10").get
                val result = BaseAI(IntegerValues, method, domain)
                if (domain.allReturnedValues.size != 1)
                    fail("expected one result; found: "+domain.allReturnedValues)

                // we don't know the size of the array
                domain.allReturnedValues.head._2 abstractsOver (
                    domain.InitializedArrayValue(2, List(10), ArrayType(IntegerType))) should be(true)

                // get the loop counter at the "icmple instruction" which controls the 
                // loops that initializes the array
                result.operandsArray(20).tail.head should be(domain.IntegerRange(0, 11))
            }

            it("it should be possible to identify dead code - even for complex conditions") {
                val domain = new IntegerRangesTestDomain
                val method = IntegerValues.findMethod("deadCode").get
                val result = BaseAI(IntegerValues, method, domain)
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

            it("an idempotent operation (+0) should not affect constraint propagation (cfDependentValuesAdd)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValuesAdd").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(21).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(29).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(31).head should be(domain.IntegerRange(99, 99))
            }

            it("an idempotent operation (-0) should not affect constraint propagation (cfDependentValuesSub)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValuesSub").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(21).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(29).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(31).head should be(domain.IntegerRange(99, 99))
            }

            it("an idempotent operation (*1) should not affect constraint propagation (cfDependentValuesMult) ") {
                val domain = new IntegerRangesTestDomain(IntegerRangeValues.MaxCardinalityOfIntegerRanges)
                val method = IntegerValues.findMethod("cfDependentValuesMult").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(15).head should be(domain.IntegerRange(Int.MinValue, 0))

                result.operandsArray(38).head should be(domain.IntegerRange(Int.MinValue, 100))
                result.operandsArray(46).head should be(domain.IntegerRange(Int.MinValue, 100))
                result.operandsArray(48).head should be(domain.IntegerRange(Int.MinValue, 100))

                result.operandsArray(68).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(76).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(78).head should be(domain.IntegerRange(99, 99))

            }

            it("an idempotent operation (/1) should not affect constraint propagation (cfDependentValuesDiv) ") {
                val domain = new IntegerRangesTestDomain(IntegerRangeValues.MaxCardinalityOfIntegerRanges)
                val method = IntegerValues.findMethod("cfDependentValuesDiv").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(31).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(39).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(41).head should be(domain.IntegerRange(99, 99))
            }

            it("an idempotent operation (& -1) should not affect constraint propagation (cfDependentValuesBitwiseAnd) ") {
                val domain = new IntegerRangesTestDomain(IntegerRangeValues.MaxCardinalityOfIntegerRanges)
                val method = IntegerValues.findMethod("cfDependentValuesBitwiseAnd").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(30).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(38).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(40).head should be(domain.IntegerRange(99, 99))
            }

            it("an idempotent operation (| 0) should not affect constraint propagation (cfDependentValuesBitwiseOr) ") {
                val domain = new IntegerRangesTestDomain(IntegerRangeValues.MaxCardinalityOfIntegerRanges)
                val method = IntegerValues.findMethod("cfDependentValuesBitwiseOr").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(27).head should be(domain.IntegerRange(100, 100))
                result.operandsArray(35).head should be(domain.IntegerRange(Int.MinValue, 98))
                result.operandsArray(37).head should be(domain.IntegerRange(99, 99))

                result.operandsArray(39).head should be(domain.IntegerRange(101, Int.MaxValue))
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v1)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues1_v1").get
                val result = BaseAI(IntegerValues, method, domain)

                if (result.operandsArray(37) != null) {
                    result.operandsArray(37).head should be(domain.AnIntegerValue)
                    result.operandsArray(41).head should be(domain.IntegerRange(0, 0))
                } else {
                    // OK - the code is dead, however, we cannot identify this, but the
                    // above is a safe approximation!
                }
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v2)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues1_v2").get
                val result = BaseAI(IntegerValues, method, domain)

                if (result.operandsArray(37) != null) {
                    result.operandsArray(37).head should be(domain.AnIntegerValue)
                    result.operandsArray(41).head should be(domain.IntegerRange(0, 0))
                } else {
                    // OK - the code is dead, however, we cannot identify this, but the
                    // above is a safe approximation!
                }
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues1_v3)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues1_v3").get
                val result = BaseAI(IntegerValues, method, domain)

                if (result.operandsArray(38) != null) {
                    result.operandsArray(38).head should be(domain.AnIntegerValue)
                    result.operandsArray(42).head should be(domain.IntegerRange(0, 0))
                } else {
                    // OK - the code is dead, however, we cannot identify this, but the
                    // above is a safe approximation!
                }
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues2)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues2").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(38).head should be(domain.AnIntegerValue)
                result.operandsArray(42).head should be(domain.IntegerRange(0, 0))
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues3)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues3").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(45).head should be(domain.AnIntegerValue)
                result.operandsArray(49).head should be(domain.IntegerRange(0, 0))
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues4)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues4").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(46).head should be(domain.IntegerRange(2, 2))
                result.operandsArray(50).head should be(domain.AnIntegerValue)
                result.operandsArray(54).head should be(domain.AnIntegerValue)
                if (result.operandsArray(50).head eq result.operandsArray(54).head)
                    fail("unequal values are made equal")
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues5)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues5").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(47).head should be(domain.IntegerRange(2, 2))
                result.operandsArray(51).head should be(domain.IntegerRange(0, 1))
                result.operandsArray(55).head should be(domain.AnIntegerValue)
            }

            it("it should not happen that a constraint (if...) affects a value that was created by the same instruction (pc), but at a different point in time (cfDependentValues6)") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("cfDependentValues6").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(77).head should be(domain.IntegerRange(0, 0))
                result.operandsArray(81).head should be(domain.AnIntegerValue)
                result.operandsArray(85).head should be(domain.AnIntegerValue)
                result.operandsArray(89).head should be(domain.IntegerRange(0, 0))

                result.operandsArray(97).head should be(domain.AnIntegerValue)
                result.operandsArray(101).head should be(domain.IntegerRange(0, 0))
                result.operandsArray(105).head should be(domain.AnIntegerValue)
                result.operandsArray(109).head should be(domain.IntegerRange(0, 0))

                result.operandsArray(117).head should be(domain.AnIntegerValue)
                result.operandsArray(121).head should be(domain.AnIntegerValue)
                result.operandsArray(125).head should be(domain.IntegerRange(0, 0))
                result.operandsArray(129).head should be(domain.IntegerRange(0, 0))
            }

            it("it should not perform useless evaluations") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("complexLoop").get
                val result = BaseAI(IntegerValues, method, domain)
                result.operandsArray(35).head should be(domain.IntegerRange(0, 2))
                // when we perform a depth-first evaluation we do not want to 
                // evaluate the same instruction with the same abstract state
                // multiple times
                result.evaluated.head should be(24)
                result.evaluated.tail.head should be(23)
                result.evaluated.tail.tail.head should be(20)
            }

            it("it should handle casts correctly") {
                val domain = new IntegerRangesTestDomain(8)
                val method = IntegerValues.findMethod("casts").get
                val result = BaseAI(IntegerValues, method, domain)
                // we primarily test that the top-level domain value is updated 
                result.operandsArray(26).head should be(domain.IntegerRange(-128, 126))
            }

            it("it should handle cases where we have more complex aliasing") {
                val domain = new IntegerRangesTestDomain(4)
                val method = IntegerValues.findMethod("moreComplexAliasing").get
                val result = BaseAI(IntegerValues, method, domain)

                result.operandsArray(20).head should be(domain.AnIntegerValue)
            }
        }
    }
}
