///* BSD 2-Clause License:
// * Copyright (c) 2009 - 2014
// * Software Technology Group
// * Department of Computer Science
// * Technische Universität Darmstadt
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice,
// *    this list of conditions and the following disclaimer.
// *  - Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// * 
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// * POSSIBILITY OF SUCH DAMAGE.
// */
//package org.opalj
//package ai
//package domain
//package l1
//
//import scala.collection.immutable.SortedSet
//
//import org.opalj.util.{ Answer, Yes, No, Unknown }
//
//import br._
//
///**
// * This domain enables the tracking of integer values using sets. The cardinality of
// * the set can be configured to facilitate different needs with regard to the
// * desired precision. Often, a very small cardinality (e.g., 2 or 8) may be
// * completely sufficient and a large cardinality does not make the precision more
// * precise.
// *
// * @author Michael Eichberg
// */
//trait IntegerSetValues
//        extends Domain
//        with ConcreteIntegerValues {
//    this: Configuration ⇒
//
//    // -----------------------------------------------------------------------------------
//    //
//    // REPRESENTATION OF INTEGER LIKE VALUES
//    //
//    // -----------------------------------------------------------------------------------
//
//    /**
//     * Determines the maximum number of values captured by an Integer set.
//     */
//    protected def maxCardinalityOfIntegerValuesSet: Int = 2
//
//    /**
//     * Abstracts over all values with computational type `integer`.
//     */
//    sealed trait IntegerLikeValue extends Value with IsIntegerValue { this: DomainValue ⇒
//
//        final def computationalType: ComputationalType = ComputationalTypeInt
//
//    }
//
//    /**
//     * Represents an (unknown) integer value.
//     *
//     * Models the top value of this domain's lattice.
//     */
//    trait AnIntegerValue extends IntegerLikeValue { this: DomainValue ⇒ }
//
//    /**
//     * Represents a set of integer values.
//     */
//    abstract class IntegerSet extends IntegerLikeValue { this: DomainValue ⇒
//
//        val values: SortedSet[Int]
//
//    }
//
//    /**
//     * Creates a new IntegerSet value containing the given value.
//     */
//    def IntegerSet(value: Int): DomainValue
//
//    def IntegerSet(values: SortedSet[Int]): DomainValue
//
//    object IntegerSet {
//        def unapply(v: IntegerSet): Option[SortedSet[Int]] = Some(v.values)
//    }
//
//    // -----------------------------------------------------------------------------------
//    //
//    // COMPUTATIONS RELATED TO  INTEGER VALUES
//    //
//    // -----------------------------------------------------------------------------------
//
//    // TODO Rename and move to a "common domain"
//    protected[this] def updateIntegerSetValue(
//        oldValue: DomainValue,
//        newValue: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) =
//        (
//            operands.map { operand ⇒ if (operand eq oldValue) newValue else operand },
//            locals.map { local ⇒ if (local eq oldValue) newValue else local }
//        )
//
//    //
//    // QUESTION'S ABOUT VALUES
//    //
//
//    @inline final override def intValue[T](
//        value: DomainValue)(
//            f: Int ⇒ T)(orElse: ⇒ T): T =
//        value match {
//            case IntegerSet(values) if values.size == 1 ⇒ f(values.head)
//            case _                                      ⇒ orElse
//        }
//
//    @inline final override def intValueOption(value: DomainValue): Option[Int] =
//        value match {
//            case IntegerSet(values) if values.size == 1 ⇒ Some(values.head)
//            case _                                      ⇒ None
//        }
//
//    @inline protected final def intValues[T](
//        value1: DomainValue, value2: DomainValue)(
//            f: (Int, Int) ⇒ T)(orElse: ⇒ T): T = {
//        intValue(value1) {
//            v1 ⇒ intValue(value2) { v2 ⇒ f(v1, v2) } { orElse }
//        } {
//            orElse
//        }
//    }
//
//    override def intAreEqual(value1: DomainValue, value2: DomainValue): Answer = {
//        intValue(value1) {
//            v1 ⇒ intValue(value2) { v2 ⇒ Answer(v1 == v2) } { Unknown }
//        } {
//            Unknown
//        }
//    }
//
//    override def intIsSomeValueInRange(
//        value: DomainValue,
//        lowerBound: Int,
//        upperBound: Int): Answer = {
//        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
//            Yes
//        else
//            value match {
//                case IntegerSet(values) ⇒
//                    if (values.lastKey >= lowerBound && values.firstKey <= upperBound)
//                        Yes
//                    else
//                        No
//                case _ ⇒ Unknown
//            }
//    }
//
//    override def intIsSomeValueNotInRange(
//        value: DomainValue,
//        lowerBound: Int,
//        upperBound: Int): Answer = {
//        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
//            No
//        else
//            value match {
//                case IntegerSet(values) ⇒
//                    if (values.firstKey <= lowerBound || values.lastKey >= upperBound)
//                        Yes
//                    else
//                        No
//                case _ ⇒ Unknown
//            }
//    }
//
//    override def intIsLessThan(
//        left: DomainValue,
//        right: DomainValue): Answer = {
//
//        right match {
//            case IntegerSet(rightValues) ⇒
//                if (rightValues.lastKey == Int.MinValue)
//                    No
//                else
//                    left match {
//                        case IntegerSet(leftValues) ⇒
//                            if (leftValues.lastKey < rightValues.firstKey)
//                                Yes
//                            else if (leftValues.firstKey > rightValues.lastKey ||
//                                ( /*"for point sets":*/
//                                    leftValues.size == 1 &&
//                                    rightValues.size == 1 &&
//                                    leftValues.head == rightValues.head))
//                                No
//                            else
//                                Unknown
//                        case _ ⇒
//                            Unknown
//                    }
//            case _ ⇒
//                Unknown
//        }
//    }
//
//    override def intIsLessThanOrEqualTo(
//        left: DomainValue,
//        right: DomainValue): Answer = {
//
//        right match {
//            case IntegerSet(rightValues) ⇒
//                if (rightValues.firstKey == Int.MaxValue)
//                    Yes
//                else
//                    left match {
//                        case IntegerSet(leftValues) ⇒
//                            if (leftValues.lastKey <= rightValues.firstKey)
//                                Yes
//                            else if (leftValues.firstKey > rightValues.lastKey)
//                                No
//                            else
//                                Unknown
//                        case _ ⇒
//                            Unknown
//                    }
//            case _ ⇒
//                Unknown
//        }
//    }
//
//    override def intEstablishValue(
//        pc: PC,
//        theValue: Int,
//        value: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) = {
//        value match {
//            case IntegerSet(values) if values.size == 1 && values.head == theValue ⇒
//                (operands, locals)
//            case _ ⇒
//                updateIntegerSetValue(value, IntegerSet(theValue), operands, locals)
//        }
//    }
//
//    override def intEstablishAreEqual(
//        pc: PC,
//        value1: DomainValue,
//        value2: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) = {
//        if (value1 eq value2)
//            // this basically handles the case that both are "AnIntegerValue"
//            (operands, locals)
//        else
//            value1 match {
//                case IntegerSet(leftValues) ⇒
//                    value2 match {
//                        case IntegerSet(rightValues) ⇒
//                            val newValue = IntegerSet(leftValues.intersect(rightValues))
//                            updateIntegerSetValue(value1, newValue, operands, locals)
//                            updateIntegerSetValue(value2, newValue, operands, locals)
//                        case _ ⇒
//                            // value1 is unchanged
//                            updateIntegerSetValue(value2, value1, operands, locals)
//                    }
//                case _ ⇒
//                    // value2 is unchanged
//                    updateIntegerSetValue(value1, value2, operands, locals)
//            }
//    }
//
//    override def intEstablishAreNotEqual(
//        pc: PC,
//        value1: DomainValue,
//        value2: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) = {
//        // Given that we cannot represent multiple ranges, our possibilities are 
//        // severely limited w.r.t. representing values that are not equal. 
//        // Only if one range just represents a single value
//        // and this value is a boundary value of the other range it is possible
//        // to establish "something"
//        intValue(value1) { v1 ⇒
//            value2 match {
//                case IntegerSet(values) ⇒
//                    updateIntegerSetValue(value2, IntegerSet(values - v1), operands, locals)
//                case _ ⇒ (operands, locals)
//            }
//        } {
//            intValue(value2) { v2 ⇒
//                value1 match {
//                    case IntegerSet(values) ⇒
//                        updateIntegerSetValue(value1, IntegerSet(values - v2), operands, locals)
//                    case _ ⇒ (operands, locals)
//                }
//            } {
//                (operands, locals)
//            }
//        }
//    }
//
//    /**
//     * @note This function is ONLY defined if a corresponding test (`value1 < value2`)
//     *      returned [[org.opalj.util.Unknown]].
//     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
//     *      that overlap.
//     */
//    // Recall that this method is only called if there is something to establish!
//    override def intEstablishIsLessThan(
//        pc: PC,
//        left: DomainValue,
//        right: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) = {
//
//        // e.g, {1,2,3,4} < { 3,4,5,6} => "unchanged"
//        // e.g, {1,2,3,4} < { 3,4} => {1,2,3} and { 3,4}
////        (left, right) match {
////            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
////                val newLeftValues = leftValues.filter(_ >= rightValues.firstKey)
////                val newRightValues = rightValues.filter(_ >= leftValues.lastKey)
////            case _ ⇒ IntegerValue(pc)
////        }
//    }
//
//    override def intEstablishIsLessThanOrEqualTo(
//        pc: PC,
//        left: DomainValue,
//        right: DomainValue,
//        operands: Operands,
//        locals: Locals): (Operands, Locals) = {
//
//        val (lb1, ub1) = left match {
//            case IntegerRange(lb1, ub1) ⇒ (lb1, ub1)
//            case _                      ⇒ (Int.MinValue, Int.MaxValue)
//        }
//        val (lb2, ub2) = right match {
//            case IntegerRange(lb2, ub2) ⇒ (lb2, ub2)
//            case _                      ⇒ (Int.MinValue, Int.MaxValue)
//        }
//        // establish new bounds 
//        val ub = Math.min(ub2, ub1)
//        val newMemoryLayout @ (operands1, locals1) =
//            if (ub != ub1) {
//                val newV1 = IntegerRange(lb1, ub)
//                updateIntegerRangeValue(left, newV1, operands, locals)
//            } else
//                (operands, locals)
//
//        val lb = Math.max(lb1, lb2)
//        if (lb != lb2) {
//            val newV2 = IntegerRange(lb, ub2)
//            updateIntegerRangeValue(right, newV2, operands1, locals1)
//        } else
//            newMemoryLayout
//    }
//
//    // -----------------------------------------------------------------------------------
//    //
//    // HANDLING OF COMPUTATIONS
//    //
//    // -----------------------------------------------------------------------------------
//
//    //
//    // UNARY EXPRESSIONS
//    //
//    /*override*/ def ineg(pc: PC, value: DomainValue) =
//        value match {
//            case IntegerSet(values) ⇒ IntegerSet(values.map(-_))
//            case _                  ⇒ value
//        }
//
//    //
//    // BINARY EXPRESSIONS
//    //
//
//    /*override*/ def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue + rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                // we have to create a new instance... even if we just add "0"
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue = {
//        value match {
//            case IntegerSet(values) ⇒ IntegerSet(values.map(_ + increment))
//            case _                  ⇒ value
//        }
//    }
//
//    /*override*/ def isub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
//        (left, right) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue - rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                // we have to create a new instance... even if we just add "0"
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def idiv(
//        pc: PC,
//        numerator: DomainValue,
//        denominator: DomainValue): IntegerLikeValueOrArithmeticException = {
//        (numerator, denominator) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                var results: SortedSet[Int] = SortedSet.empty
//                var exception: Boolean = false
//                for (leftValue ← leftValues; rightValue ← rightValues) {
//                    if (rightValue == 0)
//                        exception = true
//                    else
//                        results += (leftValue / rightValue)
//                }
//                if (results.size > 0) {
//                    if (exception)
//                        ComputedValueOrException(IntegerSet(results), ArithmeticException(pc))
//                    else
//                        ComputedValue(IntegerSet(results))
//                } else {
//                    if (exception)
//                        ThrowsException(ArithmeticException(pc))
//                    else
//                        throw new DomainException("no result and no exception")
//                }
//
//            case (_, IntegerSet(rightValues)) ⇒
//                if (rightValues contains (0)) {
//                    if (rightValues.size == 1)
//                        ThrowsException(ArithmeticException(pc))
//                    else
//                        ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
//                } else
//                    ComputedValue(IntegerValue(pc))
//
//            case _ ⇒
//                if (throwArithmeticExceptions)
//                    ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
//                else
//                    ComputedValue(IntegerValue(pc))
//        }
//    }
//
//    /*override*/ def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue * rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def irem(
//        pc: PC,
//        left: DomainValue,
//        right: DomainValue): IntegerLikeValueOrArithmeticException = {
//
//        (left, right) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                var results: SortedSet[Int] = SortedSet.empty
//                var exception: Boolean = false
//                for (leftValue ← leftValues; rightValue ← rightValues) {
//                    if (rightValue == 0)
//                        exception = true
//                    else
//                        results += (leftValue % rightValue)
//                }
//                if (results.size > 0) {
//                    if (exception)
//                        ComputedValueOrException(IntegerSet(results), ArithmeticException(pc))
//                    else
//                        ComputedValue(IntegerSet(results))
//                } else {
//                    if (exception)
//                        ThrowsException(ArithmeticException(pc))
//                    else
//                        throw new DomainException("no result and no exception")
//                }
//
//            case (_, IntegerSet(rightValues)) ⇒
//                if (rightValues contains (0)) {
//                    if (rightValues.size == 1)
//                        ThrowsException(ArithmeticException(pc))
//                    else
//                        ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
//                } else
//                    ComputedValue(IntegerValue(pc))
//
//            case _ ⇒
//                if (throwArithmeticExceptions)
//                    ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
//                else
//                    ComputedValue(IntegerValue(pc))
//        }
//    }
//
//    /*override*/ def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue & rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue | rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue << rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                    leftValue >> rightValue
//                }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results =
//                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                        leftValue >>> rightValue
//                    }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    /*override*/ def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
//        (value1, value2) match {
//            case (IntegerSet(leftValues), IntegerSet(rightValues)) if leftValues.size * rightValues.size <= maxCardinalityOfIntegerValuesSet ⇒
//                val results =
//                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
//                        leftValue ^ rightValue
//                    }
//                IntegerSet(results)
//            case _ ⇒
//                IntegerValue(pc)
//        }
//    }
//
//    //
//    // TYPE CONVERSION INSTRUCTIONS
//    //
//
//    /*override*/ def i2b(pc: PC, value: DomainValue): DomainValue =
//        value match {
//            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toByte.toInt))
//            case _                  ⇒ IntegerValue(pc)
//        }
//
//    /*override*/ def i2c(pc: PC, value: DomainValue): DomainValue =
//        value match {
//            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toChar.toInt))
//            case _                  ⇒ IntegerValue(pc)
//        }
//
//    /*override*/ def i2s(pc: PC, value: DomainValue): DomainValue =
//        value match {
//            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toShort.toInt))
//            case _                  ⇒ IntegerValue(pc)
//        }
//
//    /*override*/ def i2d(pc: PC, value: DomainValue): DomainValue =
//        intValue(value) { DoubleValue(pc, _) } { DoubleValue(pc) }
//
//    /*override*/ def i2f(pc: PC, value: DomainValue): DomainValue =
//        intValue(value) { FloatValue(pc, _) } { FloatValue(pc) }
//
//    /*override*/ def i2l(pc: PC, value: DomainValue): DomainValue =
//        intValue(value) { LongValue(pc, _) } { LongValue(pc) }
//
//}
//
