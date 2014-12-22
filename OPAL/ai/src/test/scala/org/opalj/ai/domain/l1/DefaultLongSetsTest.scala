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

import scala.collection.immutable.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.{ ObjectType, ArrayType }

/**
 * Tests the LongSets Domain.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
@RunWith(classOf[JUnitRunner])
class DefaultLongSetsTest extends FunSpec with Matchers with ParallelTestExecution {

    final val IrrelevantPC = Int.MinValue

    class LongSetsTestDomain(
        override val maxCardinalityOfLongSets: Int = Int.MaxValue)
            extends CorrelationalDomain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelFloatValues
            with l1.DefaultIntegerRangeValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultReferenceValuesBinding
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l1.DefaultLongSetValues // <----- The one we are going to test
            with l0.DefaultPrimitiveValuesConversions
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with PredefinedClassHierarchy
            with RecordLastReturnedValues

    describe("central properties of domains that use LongSet values") {

        val theDomain = new LongSetsTestDomain
        import theDomain._
    }

    describe("operations involving LongSet values") {

        describe("the behavior of join if we exceed the max cardinality") {

            val theDomain = new LongSetsTestDomain(8)
            import theDomain._

            it("(join of two sets with positive values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongValue)\"") {
                val v1 = LongSet(SortedSet[Long](0, 2, 4, 9))
                val v2 = LongSet(SortedSet[Long](1, 3, 5, 6, 7))
                v1.join(-1, v2) should be(StructuralUpdate(LongValue))
                v2.join(-1, v1) should be(StructuralUpdate(LongValue))
            }

            it("(join of two sets with positive values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongSet(0, 1, 2, 3, 4, 5, 6, 9))\"") {
                val v1 = LongSet(SortedSet[Long](0, 2, 4, 6, 9))
                val v2 = LongSet(SortedSet[Long](1, 3, 5, 6))
                v1.join(-1, v2) should be(StructuralUpdate(LongSet(SortedSet[Long](0, 1, 2, 3, 4, 5, 6, 9))))
                v2.join(-1, v1) should be(StructuralUpdate(LongSet(SortedSet[Long](0, 1, 2, 3, 4, 5, 6, 9))))
            }

            it("(join of two sets with positive and negative values that exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongValue)\"") {
                val v1 = LongSet(SortedSet[Long](0, 2, 4, 9))
                val v2 = LongSet(SortedSet[Long](1, 3, 5, 6, 7))
                v1.join(-1, v2) should be(StructuralUpdate(LongValue))
                v2.join(-1, v1) should be(StructuralUpdate(LongValue))
            }

            it("(join of two sets with positive and negative values that do not exceed the cardinality); i1 join i2 => \"StructuralUpdate(LongSet(-10, -7, -3, -1, 0, 5, 6, 9))\"") {
                val v1 = LongSet(SortedSet[Long](-7, -3, 0, 6, 9))
                val v2 = LongSet(SortedSet[Long](-10, -1, 5, 6))
                v1.join(-1, v2) should be(StructuralUpdate(LongSet(SortedSet[Long](-10, -7, -3, -1, 0, 5, 6, 9))))
                v2.join(-1, v1) should be(StructuralUpdate(LongSet(SortedSet[Long](-10, -7, -3, -1, 0, 5, 6, 9))))
            }

        }

        val theDomain = new LongSetsTestDomain
        import theDomain._

        describe("the behavior of the join operation if we do not exceed the max. spread") {

            it("(join with itself) val ir = LongSet(...); ir join ir => \"NoUpdate\"") {
                val v = LongSet(0)
                v.join(-1, v) should be(NoUpdate)
            }

            it("(join of disjoint sets) {Long.MinValue,-1} join {1,Long.MaxValue} => {Long.MinValue,-1,1,Long.MaxValue}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, -1))
                val v2 = LongSet(SortedSet[Long](1, Long.MaxValue))

                v1.join(-1, v2) should be(StructuralUpdate(LongSet(SortedSet[Long](Long.MinValue, -1, 1, Long.MaxValue))))
                v2.join(-1, v1) should be(StructuralUpdate(LongSet(SortedSet[Long](Long.MinValue, -1, 1, Long.MaxValue))))
            }

            it("(join of intersecting LongSets) {-1,1} join {0,1} => {-1,0,1}") {
                val v1 = LongSet(SortedSet[Long](-1, 1))
                val v2 = LongSet(SortedSet[Long](0, 1))

                v1.join(-1, v2) should be(StructuralUpdate(LongSet(SortedSet[Long](-1, 0, 1))))
                v2.join(-1, v1) should be(StructuralUpdate(LongSet(SortedSet[Long](-1, 0, 1))))
            }

            it("(join of two LongSets with the same values) {-1,1} join {-1,1} => \"MetaInformationUpdate\"") {
                val v1 = LongSet(SortedSet[Long](-1, 1))
                val v2 = LongSet(SortedSet[Long](-1, 1))

                v1.join(-1, v2) should be('isMetaInformationUpdate)
            }

        }

        describe("the behavior of the \"summarize\" function") {

            it("it should be able to handle intersecting LongSets") {
                val v1 = LongSet(SortedSet[Long](-3, -2))
                val v2 = LongSet(SortedSet[Long](-2, -1))

                summarize(-1, Iterable(v1, v2)) should be(LongSet(SortedSet[Long](-3, -2, -1)))
                summarize(-1, Iterable(v2, v1)) should be(LongSet(SortedSet[Long](-3, -2, -1)))
            }

            it("it should be able to handle disjunct LongSets") {
                val v1 = LongSet(SortedSet[Long](-3, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](-2, -1))

                summarize(-1, Iterable(v1, v2)) should be(LongSet(SortedSet[Long](-3, -2, -1, Long.MaxValue)))
                summarize(-1, Iterable(v2, v1)) should be(LongSet(SortedSet[Long](-3, -2, -1, Long.MaxValue)))
            }

            it("a summary involving some LongValueValue should result in LongValue") {
                val v1 = LongSet(SortedSet[Long](-3, Long.MaxValue))
                val v2 = LongValue(-1 /*PC*/ )

                summarize(-1, Iterable(v1, v2)) should be(LongValue())
                summarize(-1, Iterable(v2, v1)) should be(LongValue())
            }

            it("should calculate the correct summary if Long.MaxValue is involved") {
                val v1 = LongSet(SortedSet[Long](-3, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](-2, Long.MaxValue))
                summarize(-1, Iterable(v1, v2)) should be(LongSet(SortedSet[Long](-3, -2, Long.MaxValue)))
                summarize(-1, Iterable(v2, v1)) should be(LongSet(SortedSet[Long](-3, -2, Long.MaxValue)))
            }

            it("should calculate the correct summary if Long.MinValue is involved") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, 0))
                val v2 = LongSet(SortedSet[Long](Long.MinValue, 0))
                summarize(-1, Iterable(v1, v2)) should be(LongSet(SortedSet[Long](Long.MinValue, 0)))
                summarize(-1, Iterable(v2, v1)) should be(LongSet(SortedSet[Long](Long.MinValue, 0)))
            }
        }

        describe("the behavior of lmul") {

            it("{0,3} * {0,2} => {0,6}") {
                val v1 = LongSet(SortedSet[Long](0, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](0, 6)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](0, 6)))
            }

            it("{-3,-1} * {-10,-2} => {2,6,10,30}") {
                val v1 = LongSet(SortedSet[Long](-3, -1))
                val v2 = LongSet(SortedSet[Long](-10, -2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](2, 6, 10, 30)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](2, 6, 10, 30)))
            }

            it("{-1,3} * {0,2} => {-2,0,6}") {
                val v1 = LongSet(SortedSet[Long](-1, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](-2, 0, 6)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](-2, 0, 6)))
            }

            it("{-3,3} * {-2,2} => {-6,6}") {
                val v1 = LongSet(SortedSet[Long](-3, 3))
                val v2 = LongSet(SortedSet[Long](-2, 2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](-6, 6)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](-6, 6)))
            }

            it("{Long.MinValue} * {0} => {0}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue))
                val v2 = LongSet(SortedSet[Long](0))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("{Long.MaxValue} * {0} => {0}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](0))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("{Long.MinValue} * {2} => {Long.MinValue*2}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue))
                val v2 = LongSet(SortedSet[Long](2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MinValue * 2)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MinValue * 2)))
            }

            it("{Long.MaxValue} * {2} => {Long.MaxValue*2}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](2))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue * 2)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MaxValue * 2)))
            }

            it("{0,Long.MaxValue} * {Long.MinValue,0} => {Long.MaxValue*Long.MinValue,0}") {
                val v1 = LongSet(SortedSet[Long](0, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](Long.MinValue, 0))

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue * Long.MinValue, 0)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MaxValue * Long.MinValue, 0)))
            }

            it("The result of the mul of a set s and {1} should be s itself; {2,4} * {1} => {2,4}") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](1))

                lmul(-1, v1, v2) should be(v1)
                lmul(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value * {0} should be {0}") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongValue()

                lmul(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lmul(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }
        }

        describe("the behavior of lor") {

            it("LongValue | {8,19} => LongValue") {
                val v1 = LongValue()
                val v2 = LongSet(SortedSet[Long](8, 19))

                lor(-1, v1, v2) should be(LongValue)
                lor(-1, v2, v1) should be(LongValue)
            }

            it("{Long.MinValue,Long.MaxValue} | {8,19} => {Long.MinValue+8, Long.MinValue+19, Long.MaxValue}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](8, 19))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MinValue + 8, Long.MinValue + 19, Long.MaxValue)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MinValue + 8, Long.MinValue + 19, Long.MaxValue)))
            }

            it("{Long.MaxValue-2,Long.MaxValue-1} | {Long.MaxValue-1,Long.MaxValue} => {Long.MaxValue-1, Long.MaxValue}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue - 2, Long.MaxValue - 1))
                val v2 = LongSet(SortedSet[Long](Long.MaxValue - 1, Long.MaxValue))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue - 1, Long.MaxValue)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MaxValue - 1, Long.MaxValue)))
            }

            it("{3} | {8,19} => {11,19}") {
                val v1 = LongSet(SortedSet[Long](3))
                val v2 = LongSet(SortedSet[Long](8, 19))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](11, 19)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](11, 19)))
            }

            it("{0} | {0} => {0}") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongSet(SortedSet[Long](0))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("{0} | {1} => {1}") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongSet(SortedSet[Long](1))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](1)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](1)))
            }

            it("{1} | {1} => {1}") {
                val v1 = LongSet(SortedSet[Long](1))
                val v2 = LongSet(SortedSet[Long](1))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](1)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](1)))
            }

            it("{1, 3} | {7, 15} => {7, 15}") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](7, 15))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](7, 15)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](7, 15)))
            }

            it("{8} | {2, 7} => {10, 15}") {
                val v1 = LongSet(SortedSet[Long](8))
                val v2 = LongSet(SortedSet[Long](2, 7))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](10, 15)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](10, 15)))
            }

            it("{Long.MaxValue} | {0} => {Long.MaxValue}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](0))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MaxValue)))
            }

            it("The result of the or of a set s and {0} should be s itself; {2,4} | {0} => {2,4}") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](0))

                lor(-1, v1, v2) should be(v1)
                lor(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value | {-1} should be {-1}") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](-1))

                lor(-1, v1, v2) should be(LongSet(SortedSet[Long](-1)))
                lor(-1, v2, v1) should be(LongSet(SortedSet[Long](-1)))
            }
        }

        describe("the behavior of lxor") {

            it("LongValue ^ {8,19} => LongValue") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](8, 19))

                lxor(-1, v1, v2) should be(LongValue)
                lxor(-1, v2, v1) should be(LongValue)
            }

            it("{Long.MinValue,Long.MaxValue} ^ {8,19} => {Long.MinValue+8,Long.MinValue+19,Long.MaxValue-19,Long.MaxValue-8}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](8, 19))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MinValue + 8, Long.MinValue + 19, Long.MaxValue - 19, Long.MaxValue - 8)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MinValue + 8, Long.MinValue + 19, Long.MaxValue - 19, Long.MaxValue - 8)))
            }

            it("{Long.MaxValue-2,Long.MaxValue-1} ^ {Long.MaxValue-1,Long.MaxValue} => {0,1,2,3}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue - 2, Long.MaxValue - 1))
                val v2 = LongSet(SortedSet[Long](Long.MaxValue - 1, Long.MaxValue))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](0, 1, 2, 3)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](0, 1, 2, 3)))
            }

            it("{3} ^ {8,19} => {11,16}") {
                val v1 = LongSet(SortedSet[Long](3))
                val v2 = LongSet(SortedSet[Long](8, 19))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](11, 16)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](11, 16)))
            }

            it("{0} ^ {0} => {0}") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongSet(SortedSet[Long](0))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("{0} ^ {1} => {1}") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongSet(SortedSet[Long](1))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](1)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](1)))
            }

            it("{1} ^ {1} => {0}") {
                val v1 = LongSet(SortedSet[Long](1))
                val v2 = LongSet(SortedSet[Long](1))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("{1, 3} ^ {7, 15} => {4,6,12,14}") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](7, 15))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](4, 6, 12, 14)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](4, 6, 12, 14)))
            }

            it("{8} ^ {2, 7} => {15}") {
                val v1 = LongSet(SortedSet[Long](8))
                val v2 = LongSet(SortedSet[Long](2, 7))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](10, 15)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](10, 15)))
            }

            it("{Long.MaxValue} ^ {0} => {Long.MaxValue}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](0))

                lxor(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue)))
                lxor(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MaxValue)))
            }
        }

        describe("the behavior of ladd") {

            it("{0,3} + {0,2} => {0,2,3,5}") {
                val v1 = LongSet(SortedSet[Long](0, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                ladd(-1, v1, v2) should be(LongSet(SortedSet[Long](0, 2, 3, 5)))
                ladd(-1, v2, v1) should be(LongSet(SortedSet[Long](0, 2, 3, 5)))
            }

            it("{-3,-1} + {-10,-2} => {-13,-11,-5,-3}") {
                val v1 = LongSet(SortedSet[Long](-3, -1))
                val v2 = LongSet(SortedSet[Long](-10, -2))

                ladd(-1, v1, v2) should be(LongSet(SortedSet[Long](-13, -11, -5, -3)))
                ladd(-1, v2, v1) should be(LongSet(SortedSet[Long](-13, -11, -5, -3)))
            }

            it("{-1,3} + {0,2} => {-1,1,3,5}") {
                val v1 = LongSet(SortedSet[Long](-1, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                ladd(-1, v1, v2) should be(LongSet(SortedSet[Long](-1, 1, 3, 5)))
                ladd(-1, v2, v1) should be(LongSet(SortedSet[Long](-1, 1, 3, 5)))
            }

            it("{0} + LongValue => LongValue") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongValue()

                ladd(-1, v1, v2) should be(LongValue)
                ladd(-1, v2, v1) should be(LongValue)
            }

            it("{Long.MinValue,3} + {3,2} => {Long.MinValue+2,Long.MinValue+3,5,6}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, 3))
                val v2 = LongSet(SortedSet[Long](3, 2))

                ladd(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MinValue + 2, Long.MinValue + 3, 5, 6)))
                ladd(-1, v2, v1) should be(LongSet(SortedSet[Long](Long.MinValue + 2, Long.MinValue + 3, 5, 6)))
            }

            it("{-3,-1} + {-3,Long.MaxValue} => {-6,-4,Long.MaxValue-3,Long.MaxValue-1}") {
                val v1 = LongSet(SortedSet[Long](-3, -1))
                val v2 = LongSet(SortedSet[Long](-3, Long.MaxValue))

                ladd(-1, v1, v2) should be(LongSet(SortedSet[Long](-6, -4, Long.MaxValue - 3, Long.MaxValue - 1)))
                ladd(-1, v2, v1) should be(LongSet(SortedSet[Long](-6, -4, Long.MaxValue - 3, Long.MaxValue - 1)))
            }

        }

        describe("the behavior of lsub") {

            it("{0,3} - {0,2} => {-2,0,1,3}") {
                val v1 = LongSet(SortedSet[Long](0, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                lsub(-1, v1, v2) should be(LongSet(SortedSet[Long](-2, 0, 1, 3)))
            }

            it("{-3,-1} - {-10,-2} => {-1,1,7,9}") {
                val v1 = LongSet(SortedSet[Long](-3, -1))
                val v2 = LongSet(SortedSet[Long](-10, -2))

                lsub(-1, v1, v2) should be(LongSet(SortedSet[Long](-1, 1, 7, 9)))
            }

            it("{-1,3} - {0,2} => {-3,-1,1,3}") {
                val v1 = LongSet(SortedSet[Long](-1, 3))
                val v2 = LongSet(SortedSet[Long](0, 2))

                lsub(-1, v1, v2) should be(LongSet(SortedSet[Long](-3, -1, 1, 3)))
            }

            it("{0} - LongValue => LongValue") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongValue()

                lsub(-1, v1, v2) should be(LongValue)
            }

            it("LongValue - {0} => LongValue") {
                val v1 = LongSet(SortedSet[Long](0))
                val v2 = LongValue()

                lsub(-1, v2, v1) should be(LongValue)
            }

            it("{Long.MinValue,3} - {3,2} => {0,1,Long.MinValue-2,Long.MinValue-3}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, 3))
                val v2 = LongSet(SortedSet[Long](3, 2))

                lsub(-1, v1, v2) should be(LongSet(SortedSet[Long](0, 1, Long.MinValue - 2, Long.MinValue - 3)))
            }

            it("{Long.MaxValue,3} - {-3,2} => {Long.MaxValue+3,1,6,Long.MaxValue-2}") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue, 3))
                val v2 = LongSet(SortedSet[Long](-3, 2))

                lsub(-1, v1, v2) should be(LongSet(SortedSet[Long](Long.MaxValue + 3, 1, 6, Long.MaxValue - 2)))
            }

        }

        describe("the behavior of ldiv") {

            it("{1,3} / {2} => {0,1}") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](2))

                ldiv(-1, v1, v2) should be(ComputedValue(LongSet(SortedSet[Long](0, 1))))
            }

            it("{1,3} / {1} => {1,3}") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](1))

                ldiv(-1, v1, v2) should be(ComputedValue(LongSet(SortedSet[Long](1, 3))))
            }

            it("{1,3} / {0} => ThrowsException") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](0))

                val result = ldiv(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{1,3} / {-1} => {-3,-1}") {
                val v1 = LongSet(SortedSet[Long](1, 3))
                val v2 = LongSet(SortedSet[Long](-1))

                ldiv(-1, v1, v2) should be(ComputedValue(LongSet(SortedSet[Long](-3, -1))))
            }

            it("LongValue / {0} => ThrowsException") {
                val v1 = LongValue()
                val v2 = LongSet(SortedSet[Long](0))

                val result = ldiv(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("LongValue / LongValue => Value and ThrowsException") {
                val v1 = LongValue()
                val v2 = LongValue()

                val result = ldiv(-1, v1, v2)
                result.result should be { LongValue }
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{-1,200} / LongValue => Value and ThrowsException") {
                val v1 = LongSet(SortedSet[Long](-1, 200))
                val v2 = LongValue

                val result = ldiv(-1, v1, v2)
                result.result should be { LongValue }
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("{Long.MinValue,-1} / Long.MaxValue => {-1,0}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, -1))
                val v2 = LongSet(SortedSet[Long](Long.MaxValue, Long.MaxValue))

                ldiv(-1, v1, v2) should be(ComputedValue(LongSet(SortedSet[Long](-1, 0))))
            }

            it("{Long.MinValue,Long.MaxValue} / Long.MaxValue => {-1,1}") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](Long.MaxValue, Long.MaxValue))

                ldiv(-1, v1, v2) should be(ComputedValue(LongSet(SortedSet[Long](-1, 1))))
            }
        }

        describe("the behavior of lrem") {

            it("LongValue % LongValue => LongValue + Exception") {
                val v1 = LongValue()
                val v2 = LongValue()

                val result = lrem(-1, v1, v2)
                result.result should be { LongValue }
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is known, but the divisor is 0) {0,3} % {0} => Exception") {
                val v1 = LongSet(SortedSet[Long](0, 3))
                val v2 = LongSet(SortedSet[Long](0))

                val result = lrem(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is 0) LongValue % {0} => Exception") {
                val v1 = LongValue()
                val v2 = LongSet(SortedSet[Long](0))

                val result = lrem(-1, v1, v2)
                result.hasResult should be(false)
                result.exceptions match {
                    case SObjectValue(ObjectType.ArithmeticException) ⇒ /*OK*/
                    case v ⇒ fail(s"expected ArithmeticException; found $v")
                }
            }

            it("(the dividend is unknown, but the divisor is known) LongValue % {2} => LongValue") {
                val v1 = LongValue()
                val v2 = LongSet(SortedSet[Long](2))

                val result = lrem(-1, v1, v2)
                result.result should be(LongValue)
                result.throwsException should be(false)
            }

            it("(dividend and divisor are positive) {0,3} % {1,2} => {0,1}") {
                val v1 = LongSet(SortedSet[Long](0, 3))
                val v2 = LongSet(SortedSet[Long](1, 2))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](0, 1)))
            }

            it("(dividend and divisor are negative) {-10,-3} % {-2,-1} => {-1,0}") {
                val v1 = LongSet(SortedSet[Long](-10, -3))
                val v2 = LongSet(SortedSet[Long](-2, -1))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](-1, 0)))
            }

            it("(the dividend may be positive OR negative) {-10,3} % {1,2} => {0,1}") {
                val v1 = LongSet(SortedSet[Long](-10, 3))
                val v2 = LongSet(SortedSet[Long](1, 2))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](0, 1)))
            }

            it("(the dividend and the divisor may be positive OR negative) {-10,3} % {-3,4} => {-2,-1,0,3}") {
                val v1 = LongSet(SortedSet[Long](-10, 3))
                val v2 = LongSet(SortedSet[Long](-3, 4))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](-2, -1, 0, 3)))
            }

            it("(the dividend and the divisor are positive) {0,Long.MaxValue} % {16} => {0,15}") {
                val v1 = LongSet(SortedSet[Long](0, Long.MaxValue))
                val v2 = LongSet(SortedSet[Long](16))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](0, 15)))
            }

            it("(the dividend and the divisor are single values) {2} % {16} => {2}") {
                val v1 = LongSet(SortedSet[Long](2))
                val v2 = LongSet(SortedSet[Long](16))

                val result = lrem(-1, v1, v2)
                result.result should be(LongSet(SortedSet[Long](2)))
            }
        }

        describe("the behavior of land") {

            it("{3} & {255} => {0}") {
                val v1 = LongSet(SortedSet[Long](3))
                val v2 = LongSet(SortedSet[Long](255))

                land(-1, v1, v2) should be(LongSet(SortedSet[Long](3)))
                land(-1, v2, v1) should be(LongSet(SortedSet[Long](3)))
            }

            it("{4} & {2} => {0}") {
                val v1 = LongSet(SortedSet[Long](4))
                val v2 = LongSet(SortedSet[Long](2))

                land(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                land(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }

            it("LongValue & {2} => LongValue") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](2))

                land(-1, v1, v2) should be(LongValue)
                land(-1, v2, v1) should be(LongValue)
            }

            it("{-2} & LongValue  => LongValue") {
                val v1 = LongSet(SortedSet[Long](-2))
                val v2 = LongValue

                land(-1, v1, v2) should be(LongValue)
                land(-1, v2, v1) should be(LongValue)
            }

            it("The result of the and of a set s and {-1} should be s itself; {2,4} & {-1} => {2,4}") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](-1))

                land(-1, v1, v2) should be(v1)
                land(-1, v2, v1) should be(v1)
            }

            it("A specific (but unknown) value & {0} should be {0}") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](0))

                land(-1, v1, v2) should be(LongSet(SortedSet[Long](0)))
                land(-1, v2, v1) should be(LongSet(SortedSet[Long](0)))
            }
        }

        describe("the behavior of lshl") {

            it("LongValue << {2} => LongValue") {
                val v = LongValue
                val s = LongSet(SortedSet[Long](2))

                lshl(-1, v, s) should be(LongValue)
            }

            it("{2} << LongValue => LongValue") {
                val v = LongSet(SortedSet[Long](2))
                val s = LongValue

                lshl(-1, v, s) should be(LongValue)
            }

            it("{-1,1} << {2} => {-4,4}") {
                val v = LongSet(SortedSet[Long](-1, 1))
                val s = LongSet(SortedSet[Long](2))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](-4, 4)))
            }

            it("{64} << {64} => {64}") {
                val v = LongSet(SortedSet[Long](64))
                val s = LongSet(SortedSet[Long](64))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](64)))
            }

            it("{1} << {64} => {1}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](64))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](1)))
            }

            it("{0} << {64} => {0}") {
                val v = LongSet(SortedSet[Long](0))
                val s = LongSet(SortedSet[Long](64))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](0)))
            }

            it("{1} << {30} => {1073741824}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](30))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](1073741824)))
            }

            it("{1} << {2} => {4}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](2))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](4)))
            }

            it("{0,2} << {2} => {0,8}") {
                val v = LongSet(SortedSet[Long](0, 2))
                val s = LongSet(SortedSet[Long](2))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](0, 8)))
            }

            it("{1,2} << {2} => {4,8}") {
                val v = LongSet(SortedSet[Long](1, 2))
                val s = LongSet(SortedSet[Long](2))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](4, 8)))
            }

            it("{1,2} << {2,3} => {4,8,16}") {
                val v = LongSet(SortedSet[Long](1, 2))
                val s = LongSet(SortedSet[Long](2, 3))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](4, 8, 16)))
            }

            it("{Long.MinValue,-64,Long.MaxValue} << {2,3} => {-512,-256,-8,-4,0}") {
                val v = LongSet(SortedSet[Long](Long.MinValue, -64, Long.MaxValue))
                val s = LongSet(SortedSet[Long](2, 3))

                lshl(-1, v, s) should be(LongSet(SortedSet[Long](-512, -256, -8, -4, 0)))
            }

        }

        describe("the behavior of lshr") {

            it("LongValue >> {2} => LongValue") {
                val v = LongValue
                val s = LongSet(SortedSet[Long](2))

                lshr(-1, v, s) should be(LongValue)
            }

            it("{2} >> LongValue => LongValue") {
                val v = LongSet(SortedSet[Long](2))
                val s = LongValue

                lshr(-1, v, s) should be(LongValue)
            }

            it("{-1,1} >> {2} => {-1,0}") {
                val v = LongSet(SortedSet[Long](-1, 1))
                val s = LongSet(SortedSet[Long](2))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](-1, 0)))
            }

            it("{256} >> {64} => {256}") {
                val v = LongSet(SortedSet[Long](256))
                val s = LongSet(SortedSet[Long](64))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](256)))
            }

            it("{256} >> {8} => {1}") {
                val v = LongSet(SortedSet[Long](256))
                val s = LongSet(SortedSet[Long](8))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](1)))
            }

            it("{256} >> {9} => {0}") {
                val v = LongSet(SortedSet[Long](256))
                val s = LongSet(SortedSet[Long](9))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](0)))
            }

            it("{0} >> {64} => {0}") {
                val v = LongSet(SortedSet[Long](0))
                val s = LongSet(SortedSet[Long](64))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](0)))
            }

            it("{1} >> {30} => {0}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](30))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](0)))
            }

            it("{1} >> {2} => {0}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](2))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](0)))
            }

            it("{1} >> {0} => {1}") {
                val v = LongSet(SortedSet[Long](1))
                val s = LongSet(SortedSet[Long](0))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](1)))
            }

            it("{32,64} >> {2} => {8,16}") {
                val v = LongSet(SortedSet[Long](32, 64))
                val s = LongSet(SortedSet[Long](2))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](8, 16)))
            }

            it("{Long.MinValue,Long.MaxValue} >> {48,63} => {-32768,-1,0,32767}") {
                val v = LongSet(SortedSet[Long](Long.MinValue, Long.MaxValue))
                val s = LongSet(SortedSet[Long](48, 63))

                lshr(-1, v, s) should be(LongSet(SortedSet[Long](-32768, -1, 0, 32767)))
            }

        }

        describe("the behaviour of lcmp") {

            it("compare two single-element sets where v1 < v2; lcmp({2}, {4}) => [-1,-1]") {
                val v1 = LongSet(SortedSet[Long](2))
                val v2 = LongSet(SortedSet[Long](4))

                lcmp(-1, v1, v2) should be(IntegerRange(-1))
            }

            it("compare two single-element sets where v1 = v2; lcmp({2}, {2}) => [0,0]") {
                val v1 = LongSet(SortedSet[Long](2))
                val v2 = LongSet(SortedSet[Long](2))

                lcmp(-1, v1, v2) should be(IntegerRange(0))
            }

            it("compare two single-element sets where v1 > v2; lcmp({4}, {2}) => [1,1]") {
                val v1 = LongSet(SortedSet[Long](4))
                val v2 = LongSet(SortedSet[Long](2))

                lcmp(-1, v1, v2) should be(IntegerRange(1))
            }

            it("compare a specific (but unknown) LongValue with {Long.MinValue} where v1 can't be < v2; lcmp(LongValue, {Long.MinValue}) => [0,1]") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](Long.MinValue))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare a specific (but unknown) LongValue with {Long.MaxValue} where v1 can't be > v2; lcmp({LongValue, {Long.MaxValue}) => [-1,0]") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](Long.MaxValue))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the single-element set v1 containing Long.MinValue with a specific (but unknown) LongValue where v1 can't be > v2; lcmp({Long.MinValue}, LongValue) => [-1,0]") {
                val v1 = LongSet(SortedSet[Long](Long.MinValue))
                val v2 = LongValue

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the single-element set v1 containing Long.MaxValue with a specific (but unknown) LongValue where v1 can't be < v2; lcmp({Long.MaxValue}, LongValue) => [0,1]") {
                val v1 = LongSet(SortedSet[Long](Long.MaxValue))
                val v2 = LongValue

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare the multi-element set v1 with a specific (but unknown) LongValue where no information can be deduced; lcmp({-2,0,2}, LongValue) => [-1,1]") {
                val v1 = LongSet(SortedSet[Long](-2, 0, 2))
                val v2 = LongValue

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }

            it("compare a specific (but unknown) LongValue with the multi-element set v2 where no information can be deduced; lcmp(LongValue, {-2,0,2}) => [-1,1]") {
                val v1 = LongValue
                val v2 = LongSet(SortedSet[Long](-2, 0, 2))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }

            it("compare two multi-element sets where the smallest element of v1 is greater than the largest element of v2; lcmp({2,4,6}, {-4,-2,0}) => [1,1]") {
                val v1 = LongSet(SortedSet[Long](2, 4, 6))
                val v2 = LongSet(SortedSet[Long](-4, -2, 0))

                lcmp(-1, v1, v2) should be(IntegerRange(1, 1))
            }

            it("compare two multi-element sets where the greatest element of v1 is less than the smallest element of v2; lcmp({2,4}, {6,8}) => [-1,-1]") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](6, 8))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, -1))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.last overlaps with v2.head; lcmp({2,4}, {4}) => [-1,0]") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](4))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))                
            }

            it("compare two multi-element sets where v1.last overlaps with v2.head; lcmp({2,4}, {4,5}) => [-1,0]") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](4, 5))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.head overlaps with v2.last; lcmp({4}, {-4,4}) => [0,1]") {
                val v1 = LongSet(SortedSet[Long](4))
                val v2 = LongSet(SortedSet[Long](-4, 4))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare two multi-element sets where v1.head overlaps with v2.last; lcmp({2,4}, {-4,2}) => [0,1]") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](-4, 2))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare the single-element set v1 with the multi-element set v2 where v1.head overlaps with v2.head; lcmp({-7},{-7,-5}) => [-1,0]") {
                val v1 = LongSet(SortedSet[Long](-7))
                val v2 = LongSet(SortedSet[Long](-7, -5))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 0))
            }

            it("compare the multi-element set v1 with the single-element set v2 where v1.head overlaps with v2.head; lcmp({2,4}, {2}) => [0,1]") {
                val v1 = LongSet(SortedSet[Long](2, 4))
                val v2 = LongSet(SortedSet[Long](2))

                lcmp(-1, v1, v2) should be(IntegerRange(0, 1))
            }

            it("compare two multi-element sets where v1.last overlaps with v2.last; lcmp({-2,0},{-1,0}) => [-1,1]") {
                val v1 = LongSet(SortedSet[Long](-2, 0))
                val v2 = LongSet(SortedSet[Long](-1, 0))

                lcmp(-1, v1, v2) should be(IntegerRange(-1, 1))
            }
        }
    }
}
