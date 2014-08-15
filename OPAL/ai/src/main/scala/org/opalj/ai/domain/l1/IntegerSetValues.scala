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

import scala.collection.immutable.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br._

/**
 * This domain enables the tracking of integer values using sets. The cardinality of
 * the set can be configured to facilitate different needs with regard to the
 * desired precision. Often, a very small cardinality (e.g., 2 or 8) may be
 * completely sufficient and a large cardinality does not significantly add to the
 * overall precision.
 *
 * @author Michael Eichberg
 */
trait IntegerSetValues extends IntegerValuesDomain with ConcreteIntegerValues {
    domain: JoinStabilization with IdentityBasedAliasBreakUpDetection with Configuration with VMLevelExceptionsFactory ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum number of values captured by an Integer set.
     *
     * In many cases a rather (4-16) small number is completely sufficient to
     * capture typically variability.
     */
    protected def maxCardinalityOfIntegerSets: Int = 8

    /**
     * Abstracts over all values with computational type `integer`.
     */
    sealed trait IntegerLikeValue extends Value with IsIntegerValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeInt

    }

    /**
     * Represents an (unknown) integer value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnIntegerValue extends IntegerLikeValue { this: DomainValue ⇒ }

    /**
     * Represents a set of integer values.
     */
    abstract class IntegerSet extends IntegerLikeValue { this: DomainValue ⇒

        val values: SortedSet[Int]

    }

    /**
     * Creates a new IntegerSet value containing the given value.
     */
    def IntegerSet(value: Int): DomainValue = IntegerSet(SortedSet(value))

    /**
     * Creates a new IntegerSet value using the given set.
     */
    def IntegerSet(values: SortedSet[Int]): DomainValue

    /**
     * Extractor for `IntegerSet` values.
     */
    object IntegerSet {
        def unapply(v: IntegerSet): Option[SortedSet[Int]] = Some(v.values)
    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO  INTEGER VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTION'S ABOUT VALUES
    //

    @inline final override def intValue[T](
        value: DomainValue)(
            f: Int ⇒ T)(
                orElse: ⇒ T): T =
        value match {
            case IntegerSet(values) if values.size == 1 ⇒ f(values.head)
            case _                                      ⇒ orElse
        }

    @inline final override def intValueOption(value: DomainValue): Option[Int] =
        value match {
            case IntegerSet(values) if values.size == 1 ⇒ Some(values.head)
            case _                                      ⇒ None
        }

    @inline protected final def intValues[T](
        value1: DomainValue, value2: DomainValue)(
            f: (Int, Int) ⇒ T)(
                orElse: ⇒ T): T = {
        intValue(value1) {
            v1 ⇒ intValue(value2) { v2 ⇒ f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    override def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = {
        if (value1 eq value2)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes

        (value1, value2) match {
            case (IntegerSet(v1s), IntegerSet(v2s)) ⇒
                if (v1s.size == 1 && v2s.size == 1)
                    Answer(v1s.head == v2s.head)
                else {
                    if (v1s.intersect(v2s).isEmpty)
                        No
                    else
                        Unknown
                }
            case _ ⇒
                Unknown
        }
    }

    override def intIsSomeValueInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            Yes
        else value match {
            case IntegerSet(values) ⇒
                Answer(values.lastKey >= lowerBound && values.firstKey <= upperBound)
            case _ ⇒
                Unknown
        }
    }

    override def intIsSomeValueNotInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            No
        else value match {
            case IntegerSet(values) ⇒
                Answer(values.firstKey < lowerBound || values.lastKey > upperBound)
            case _ ⇒
                Unknown
        }
    }

    override def intIsLessThan(pc: PC, left: DomainValue, right: DomainValue): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return No

        right match {
            case IntegerSet(rightValues) ⇒
                if (rightValues.lastKey == Int.MinValue)
                    // the right value is the smallest possible value...
                    No
                else left match {
                    case IntegerSet(leftValues) ⇒
                        if (leftValues.lastKey < rightValues.firstKey)
                            Yes
                        else if (leftValues.firstKey >= rightValues.lastKey ||
                            ( /*"for point sets":*/
                                leftValues.size == 1 && rightValues.size == 1 &&
                                leftValues.head == rightValues.head))
                            No
                        else
                            Unknown
                    case _ ⇒
                        Unknown
                }
            case _ ⇒
                Unknown
        }
    }

    override def intIsLessThanOrEqualTo(
        pc: PC,
        left: DomainValue,
        right: DomainValue): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes

        right match {
            case IntegerSet(rightValues) ⇒
                if (rightValues.firstKey == Int.MaxValue)
                    Yes
                else left match {
                    case IntegerSet(leftValues) ⇒
                        if (leftValues.lastKey <= rightValues.firstKey)
                            Yes
                        else if (leftValues.firstKey > rightValues.lastKey)
                            No
                        else
                            Unknown
                    case _ ⇒
                        Unknown
                }
            case _ ⇒
                left match {
                    case IntegerSet(leftValues) ⇒
                        if (leftValues.lastKey == Int.MinValue)
                            Yes
                        else
                            Unknown
                    case _ ⇒
                        Unknown
                }
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // ESTABLISH CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        value match {
            case IntegerSet(values) if values.size == 1 && values.head == theValue ⇒
                (operands, locals)
            case _ ⇒
                updateMemoryLayout(value, IntegerSet(theValue), operands, locals)
        }
    }

    override def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        if (value1 eq value2)
            // this basically handles the case that both are "AnIntegerValue"
            (operands, locals)
        else
            value1 match {
                case IntegerSet(leftValues) ⇒
                    value2 match {
                        case IntegerSet(rightValues) ⇒
                            val newValue = IntegerSet(leftValues.intersect(rightValues))
                            val (operands1, locals1) =
                                updateMemoryLayout(value1, newValue, operands, locals)
                            updateMemoryLayout(value2, newValue, operands1, locals1)
                        case _ ⇒
                            // value1 is unchanged
                            updateMemoryLayout(value2, value1, operands, locals)
                    }
                case _ ⇒
                    // value2 is unchanged
                    updateMemoryLayout(value1, value2, operands, locals)
            }
    }

    override def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        intValue(value1) { v1 ⇒
            value2 match {
                case IntegerSet(values) ⇒
                    updateMemoryLayout(
                        value2, IntegerSet(values - v1),
                        operands, locals)
                case _ ⇒
                    (operands, locals)
            }
        } {
            intValue(value2) { v2 ⇒
                value1 match {
                    case IntegerSet(values) ⇒
                        updateMemoryLayout(
                            value1, IntegerSet(values - v2),
                            operands, locals)
                    case _ ⇒ (operands, locals)
                }
            } {
                (operands, locals)
            }
        }
    }

    override def intEstablishIsLessThan(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        (left, right) match {
            case (IntegerSet(ls), IntegerSet(rs)) ⇒
                val rsMax = rs.lastKey
                val newLs = ls.filter(_ < rsMax)
                val (operands1, locals1) =
                    if (newLs.size != ls.size) {
                        if (newLs.size == 0)
                            println(left+"  .... "+right)
                        updateMemoryLayout(
                            left, IntegerSet(newLs),
                            operands, locals)
                    } else {
                        (operands, locals)
                    }

                val lsMin = ls.firstKey
                val newRs = rs.filter(_ > lsMin)
                val newMemoryLayout =
                    if (newRs.size != rs.size) {
                        updateMemoryLayout(
                            right, IntegerSet(newRs),
                            operands1, locals1)
                    } else {
                        (operands1, locals1)
                    }

                newMemoryLayout
            case _ ⇒
                (operands, locals)
        }
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        (left, right) match {
            case (IntegerSet(ls), IntegerSet(rs)) ⇒
                val rsMax = rs.lastKey
                val newLs = ls.filter(_ <= rsMax)
                val (operands1, locals1) =
                    if (newLs.size != ls.size) {
                        updateMemoryLayout(
                            left, IntegerSet(newLs),
                            operands, locals)
                    } else {
                        (operands, locals)
                    }

                val lsMin = ls.firstKey
                val newRs = rs.filter(_ >= lsMin)
                val newMemoryLayout =
                    if (newRs.size != rs.size) {
                        updateMemoryLayout(
                            right, IntegerSet(newRs),
                            operands1, locals1)
                    } else {
                        (operands1, locals1)
                    }

                newMemoryLayout
            case _ ⇒
                (operands, locals)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    /*override*/ def ineg(pc: PC, value: DomainValue) =
        value match {
            case IntegerSet(values) ⇒ IntegerSet(values.map(-_))
            case _                  ⇒ IntegerValue(pc)
        }

    //
    // BINARY EXPRESSIONS
    //

    /*override*/ def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue + rightValue
                    }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)
            case _ ⇒
                // we have to create a new instance... even if we just add "0"
                IntegerValue(pc)
        }
    }

    /*override*/ def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue = {
        value match {
            case IntegerSet(values) ⇒ IntegerSet(values.map(_ + increment))
            case _                  ⇒ IntegerValue(pc)
        }
    }

    /*override*/ def isub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue - rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)
            case _ ⇒
                // we have to create a new instance... even if we just add "0"
                IntegerValue(pc)
        }
    }

    /*override*/ def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue * rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    protected[this] def createIntegerValueOrArithmeticException(
        pc: PC,
        exception: Boolean,
        results: SortedSet[Int]): IntegerValueOrArithmeticException = {
        if (results.size > 0) {
            if (results.size <= maxCardinalityOfIntegerSets) {
                if (exception)
                    ComputedValueOrException(IntegerSet(results), ArithmeticException(pc))
                else
                    ComputedValue(IntegerSet(results))
            } else {
                if (exception)
                    ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(pc))
            }
        } else {
            if (exception)
                ThrowsException(ArithmeticException(pc))
            else
                throw new DomainException("no result and no exception")
        }
    }

    /*override*/ def idiv(
        pc: PC,
        numerator: DomainValue,
        denominator: DomainValue): IntegerValueOrArithmeticException = {
        (numerator, denominator) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                var results: SortedSet[Int] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue ← leftValues; rightValue ← rightValues) {
                    if (rightValue == 0)
                        exception = true
                    else
                        results += (leftValue / rightValue)
                }
                createIntegerValueOrArithmeticException(pc, exception, results)

            case (_, IntegerSet(rightValues)) ⇒
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(ArithmeticException(pc))
                    else
                        ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
                } else
                    ComputedValue(IntegerValue(pc))

            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(pc))
        }
    }

    /*override*/ def irem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): IntegerValueOrArithmeticException = {

        (left, right) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                var results: SortedSet[Int] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue ← leftValues; rightValue ← rightValues) {
                    if (rightValue == 0)
                        exception = true
                    else
                        results += (leftValue % rightValue)
                }
                createIntegerValueOrArithmeticException(pc, exception, results)

            case (_, IntegerSet(rightValues)) ⇒
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(ArithmeticException(pc))
                    else
                        ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
                } else
                    ComputedValue(IntegerValue(pc))

            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(pc), ArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(pc))
        }
    }

    /*override*/ def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue & rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    /*override*/ def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue | rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    /*override*/ def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue << rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    /*override*/ def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue >> rightValue
                }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    /*override*/ def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue >>> rightValue
                    }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    /*override*/ def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSet(leftValues), IntegerSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue ^ rightValue
                    }
                if (results.size <= maxCardinalityOfIntegerSets)
                    IntegerSet(results)
                else
                    IntegerValue(pc)

            case _ ⇒
                IntegerValue(pc)
        }
    }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    /*override*/ def i2b(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toByte.toInt))
            case _                  ⇒ IntegerValue(pc)
        }

    /*override*/ def i2c(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toChar.toInt))
            case _                  ⇒ IntegerValue(pc)
        }

    /*override*/ def i2s(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerSet(values) ⇒ IntegerSet(values.map(_.toShort.toInt))
            case _                  ⇒ IntegerValue(pc)
        }

}

