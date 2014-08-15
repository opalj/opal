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

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br.{ ComputationalType, ComputationalTypeInt }

/**
 * This domain enables the tracking of an integer value's range.
 *
 * The cardinality of the range can be configured to facilitate different needs with regard to the
 * desired precision ([[maxSizeOfIntegerRanges]]).
 * Often, a very small cardinality (e.g., between 2 and 8) may be
 * completely sufficient and a large cardinality does not add the overall precision
 * significantly.
 *
 * This domain supports the most common math operations.
 *
 * ==Constraint Propagation==
 *
 * Additionally, it supports constraint propagation (e.g.,
 * [[intEstablishValue]], [[intEstablishIsLessThan]],...) w.r.t. those values
 * that were created at the same point in time by the same operation. For example,
 * in case of the following sequence:
 *  - pcA+0/t1: `iadd` (Stack: 1 :: AnIntegerValue :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+1/t2: `dup` (Stack: v(pcA/t1) :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+2/t3: `iflt` true:+10 (Stack: v(pcA/t1) :: v(pcA/t1) :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+3/t4: ... '''(Stack: v(pcA/t1) >= 0''' :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+XYZ...
 *  - pcA+12/t5: ... '''(Stack: v(pcA/t1) < 0''' :: ...; Registers: &lt;ignored&gt;)
 *
 * Hence, the test (`iflt`) of the topmost stack value against the constant 0
 * also constrained the second-top most stack value, because it was created at
 * the same point in time.
 * In case of this domain the ''reference'' of the '''Domain(Integer)Value'''
 * is used to identify those values that were created at the same point in time.
 *
 * ==Origin of an IntegerRangeValue==
 *
 * IntegerRangeValues provide implicit, limited information about the origin of the value;
 * i.e., about the instruction which ''created'' the value. This information is
 * implicitly encoded by the object reference as ''every creation, update and join of an
 * `IntegerRangeValue` always creates a new instance''. I.e, `IntegerRangeValue`s are
 * ''explicitly not cached or reused''.
 * In case that the value was passed to the method as a parameter the origin is also
 * implicitly available since the value can be found in the registers values associated
 * with the very first instruction.
 *
 * Hence, two integer (range) values (`ir1`,`ir2`) are reference equal (`eq` in Scala)
 * iff both values were created by the '''same instruction at the same time'''.
 *
 * E.g., consider the following fictitious sequence:
 *  - iconst2 ...
 *    -     Stack: EMPTY
 *    -     Locals: EMPTY
 *  - dup ...
 *    -     Stack: IntegerRangeValue(2,2)@123456;
 *    -     Locals: EMPTY
 *  - istore_0 ...
 *    -     Stack: IntegerRangeValue(2,2)@123456 <- IntegerRangeValue(2,2)@123456;
 *    -     Locals: EMPTY
 *  - iconst2 ...
 *    -     Stack: IntegerRangeValue(2,2)@123456;
 *    -     Locals: 0=IntegerRangeValue(2,2)@123456, 1=EMPTY
 *  - istore_1 ...
 *    -     Stack: IntegerRangeValue(2,2)@654321 <- IntegerRangeValue(2,2)@123456;
 *    -     Locals: 0=IntegerRangeValue(2,2)@123456, 1=EMPTY
 *  - ...
 *    -     Stack: IntegerRangeValue(2,2)@123456;
 *    -     Locals: 0=IntegerRangeValue(2,2)@123456, 1=IntegerRangeValue(2,2)@654321
 *
 * Additionally, if the sequence would be part of a loop, the next iteration would
 * create new `IntegerRangeValue`s. Hence, to identify the instruction that
 * created or constrained the respective value, it is necessary to identify the
 * memory layout that first (w.r.t. the evaluation order) contained the value and the
 * instruction that was immediately executed before is then the responsible
 * instruction. In case that the value was constrained at some point the identified
 * instruction may be a switch or an if instruction, as that instruction added
 * additional information.
 *
 * ==Implementation Requirements==
 *
 * Implementations are required to create new instances of `IntegerRangeValue`s and
 * `AnIntegerValue` '''whenever''' a computation related to the value is performed.
 * Even if the result of the computation is the original value (e.g., SomeValue +0
 * or SomeValue-0).
 *
 * If this property is not satisfied the implemented constraint propagation mechanism
 * will produce unpredictable results as it may constrain unrelated values!
 * This is true for concrete ranges as well as `AnIntegerValue`s.
 *
 * @author Michael Eichberg
 */
trait IntegerRangeValues extends IntegerValuesDomain with ConcreteIntegerValues {
    domain: JoinStabilization with IdentityBasedAliasBreakUpDetection with Configuration with VMLevelExceptionsFactory ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum number of values captured by an integer value range.
     */
    protected def maxCardinalityOfIntegerRanges: Long = 16l

    /**
     * Abstracts over all values with computational type `integer`.
     */
    sealed trait IntegerLikeValue extends Value with IsIntegerValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeInt

        def newInstance: DomainValue
    }

    /**
     * Represents an (unknown) integer value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnIntegerValue extends IntegerLikeValue { this: DomainValue ⇒ }

    /**
     * Represents a range of integer values. The range's bounds are inclusive.
     * Unless a range has only one value it is impossible to tell whether or not
     * a value that is in the range will potentially occur at runtime.
     */
    abstract class IntegerRange extends IntegerLikeValue { this: DomainValue ⇒

        val lowerBound: Int // inclusive

        val upperBound: Int // inclusive

        /**
         * Creates a new `IntegerRange` that contains the given value.
         */
        def update(newValue: Int): DomainValue
    }

    /**
     * Creates a new IntegerRange value with the lower and upper bound set to the
     * given value.
     */
    def IntegerRange(value: Int): DomainValue = IntegerRange(value, value)

    /**
     * Creates a new IntegerRange value with the given bounds.
     */
    def IntegerRange(lb: Int, ub: Int): DomainValue

    object IntegerRange {
        def unapply(v: IntegerRange): Option[(Int, Int)] =
            Some((v.lowerBound, v.upperBound))
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
            f: Int ⇒ T)(orElse: ⇒ T): T =
        value match {
            case v: IntegerRange if v.lowerBound == v.upperBound ⇒ f(v.lowerBound)
            case _ ⇒ orElse
        }

    @inline final override def intValueOption(value: DomainValue): Option[Int] =
        value match {
            case v: IntegerRange if v.lowerBound == v.upperBound ⇒ Some(v.lowerBound)
            case _ ⇒ None
        }

    @inline protected final def intValues[T](
        value1: DomainValue, value2: DomainValue)(
            f: (Int, Int) ⇒ T)(
                orElse: ⇒ T): T = {
        intValue(value1) { v1 ⇒
            intValue(value2) { v2 ⇒ f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    override def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = {
        if (value1 eq value2)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes

        value1 match {
            case IntegerRange(lb1, ub1) ⇒
                value2 match {
                    case IntegerRange(lb2, ub2) ⇒
                        if (ub1 < lb2 || lb1 > ub2)
                            No
                        else if (lb1 == lb2 && lb1 == ub2 && ub1 == ub2 /* or...*/ )
                            Yes
                        else
                            Unknown
                    case _ ⇒
                        Unknown
                }
            case _ ⇒ Unknown
        }
    }

    override def intIsSomeValueInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            Yes
        else
            value match {
                case IntegerRange(lb, ub) ⇒
                    if (ub >= lowerBound && lb <= upperBound)
                        Yes
                    else
                        No
                case _ ⇒ Unknown
            }
    }

    override def intIsSomeValueNotInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int,
        upperBound: Int): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            No
        else
            value match {
                case IntegerRange(lb, ub) ⇒
                    if (lb < lowerBound || ub > upperBound)
                        Yes
                    else
                        No
                case _ ⇒ Unknown
            }
    }

    override def intIsLessThan(pc: PC, left: DomainValue, right: DomainValue): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return No

        right match {
            case IntegerRange(rightLB, rightUB) ⇒
                if (rightUB == Int.MinValue)
                    No
                else
                    left match {
                        case IntegerRange(leftLB, leftUB) ⇒
                            if (leftUB < rightLB)
                                Yes
                            else if (leftLB > rightUB ||
                                ( /*"for point ranges":*/
                                    leftLB == leftUB &&
                                    rightLB == rightUB &&
                                    leftLB == rightLB))
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
            case IntegerRange(rightLB, rightUB) ⇒
                if (rightLB == Int.MaxValue)
                    Yes
                else
                    left match {
                        case IntegerRange(leftLB, leftUB) ⇒
                            if (leftUB <= rightLB)
                                Yes
                            else if (leftLB > rightUB)
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

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        value match {
            case IntegerRange(`theValue`, `theValue`) ⇒
                (operands, locals)
            case _ ⇒
                updateMemoryLayout(
                    value, IntegerRange(theValue, theValue),
                    operands, locals)
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
            // Given that the values are determined to be EQUAL, every subsequent 
            // constraint applies to both values (independent of 
            // the implicit origin).
            value1 match {
                case IntegerRange(lb1, ub1) ⇒
                    value2 match {
                        case IntegerRange(lb2, ub2) ⇒
                            val lb = Math.max(lb1, lb2)
                            val ub = Math.min(ub1, ub2)
                            val newValue = IntegerRange(lb, ub)

                            val (operands1, locals1) =
                                updateMemoryLayout(value1, newValue, operands, locals)
                            updateMemoryLayout(value2, newValue, operands1, locals1)

                        case _ ⇒
                            // value1 remains unchanged
                            updateMemoryLayout(value2, value1, operands, locals)
                    }
                case _ ⇒
                    updateMemoryLayout(value1, value2, operands, locals)
            }
    }

    override def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        // Given that we cannot represent multiple ranges, our possibilities are 
        // severely limited w.r.t. representing values that are not equal. 
        // Only if one range just represents a single value
        // and this value is a boundary value of the other range it is possible
        // to establish "something"
        val (lb1, ub1) = value1 match {
            case IntegerRange(lb1, ub1) ⇒ (lb1, ub1)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = value2 match {
            case IntegerRange(lb2, ub2) ⇒ (lb2, ub2)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        if (lb1 == ub1) {
            if (lb2 == ub1)
                updateMemoryLayout(value2, IntegerRange(lb2 + 1, ub2), operands, locals)
            else if (ub2 == lb1)
                updateMemoryLayout(value2, IntegerRange(lb2, ub2 - 1), operands, locals)
            else
                (operands, locals)
        } else if (lb2 == ub2) {
            if (lb1 == ub2)
                updateMemoryLayout(value1, IntegerRange(lb1 + 1, ub1), operands, locals)
            else if (ub1 == lb2)
                updateMemoryLayout(value1, IntegerRange(lb1, ub1 - 1), operands, locals)
            else
                (operands, locals)
        } else {
            (operands, locals)
        }
    }

    /**
     * @note This function is ONLY defined if a corresponding test (`value1 < value2`)
     *      returned [[org.opalj.util.Unknown]].
     *      I.e., the domain values are real ranges (not single values, e.g., `[1,1]`)
     *      that overlap.
     */
    // Recall that this method is only called if there is something to establish!
    override def intEstablishIsLessThan(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val (lb1, ub1) = left match {
            case IntegerRange(lb1, ub1) ⇒ (lb1, ub1)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = right match {
            case IntegerRange(lb2, ub2) ⇒ (lb2, ub2)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        // establish new bounds 
        val ub = Math.min(ub2 - 1, ub1)
        val newMemoryLayout @ (operands1, locals1) =
            if (ub != ub1)
                updateMemoryLayout(left, IntegerRange(lb1, ub), operands, locals)
            else
                (operands, locals)

        val lb = Math.max(lb1 + 1, lb2)
        if (lb != lb2)
            updateMemoryLayout(
                right,
                IntegerRange(lb, ub2),
                operands1,
                locals1)
        else
            newMemoryLayout
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val (lb1, ub1) = left match {
            case IntegerRange(lb1, ub1) ⇒ (lb1, ub1)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = right match {
            case IntegerRange(lb2, ub2) ⇒ (lb2, ub2)
            case _                      ⇒ (Int.MinValue, Int.MaxValue)
        }
        // establish new bounds 
        val ub = Math.min(ub2, ub1)
        val newMemoryLayout @ (operands1, locals1) =
            if (ub != ub1) {
                val newV1 = IntegerRange(lb1, ub)
                updateMemoryLayout(left, newV1, operands, locals)
            } else
                (operands, locals)

        val lb = Math.max(lb1, lb2)
        if (lb != lb2) {
            val newV2 = IntegerRange(lb, ub2)
            updateMemoryLayout(right, newV2, operands1, locals1)
        } else
            newMemoryLayout
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def ineg(pc: PC, value: DomainValue) = value match {
        case IntegerRange(lb, ub) ⇒
            if (lb == Int.MinValue) { // -Int.MinValue === Int.MinValue
                if (ub == Int.MinValue)
                    IntegerRange(Int.MinValue, Int.MinValue)
                else
                    IntegerValue(pc)
            } else
                IntegerRange(-ub, -lb)
        case _ ⇒ IntegerValue(pc)
    }

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerRange(lb1, ub1), IntegerRange(lb2, ub2)) ⇒
                // to identify overflows we simply do the "add" on long values
                // and check afterwards
                val lb = lb1.toLong + lb2.toLong
                val ub = ub1.toLong + ub2.toLong
                if (lb < Int.MinValue || ub > Int.MaxValue)
                    IntegerValue(pc)
                else
                    IntegerRange(lb.toInt, ub.toInt)
            case _ ⇒
                // we have to create a new instance... even if we just add "0"
                IntegerValue(pc)
        }
    }

    override def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue = {
        value match {
            case IntegerRange(lb, ub) ⇒
                val newLB = lb.toLong + increment.toLong
                val newUB = ub.toLong + increment.toLong
                if (newLB < Int.MinValue || newUB > Int.MaxValue)
                    IntegerValue(pc)
                else
                    IntegerRange(newLB.toInt, newUB.toInt)
            case _ ⇒
                IntegerValue(pc)
        }
    }

    override def isub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        if (left eq right)
            return IntegerRange(0, 0)

        (left, right) match {
            case (IntegerRange(llb, lub), IntegerRange(rlb, rub)) ⇒
                // to identify overflows we simply do the "add" on long values
                // and check afterwards
                val lb = llb.toLong - rub.toLong
                val ub = lub.toLong - rlb.toLong
                if (lb < Int.MinValue || ub > Int.MaxValue)
                    IntegerValue(pc)
                else
                    IntegerRange(lb.toInt, ub.toInt)
            case _ ⇒
                // we have to create a new instance... even if we just subtract "0"
                IntegerValue(pc)
        }
    }

    override def idiv(
        pc: PC,
        numerator: DomainValue,
        denominator: DomainValue): IntegerValueOrArithmeticException = {
        denominator match {
            case IntegerRange(1, 1) ⇒
                ComputedValue(numerator.asInstanceOf[IntegerLikeValue].newInstance)
            case IntegerRange(lb, ub) if lb > 0 || ub < 0 ⇒
                // no div by "0"
                ComputedValue(IntegerValue(pc))
            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(
                        IntegerValue(pc), ArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(pc))
        }
    }

    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case IntegerRange(lb1, ub1) ⇒
                if (lb1 == 0 && ub1 == 0) IntegerRange(0, 0)
                else value2 match {
                    case IntegerRange(lb2, ub2) ⇒
                        // to identify overflows we simply do the "mul" on long values
                        // and check afterwards
                        val lb1l = lb1.toLong
                        val ub1l = ub1.toLong
                        val lb2l = lb2.toLong
                        val ub2l = ub2.toLong
                        val ub =
                            Math.max(lb1l * lb2l, ub1l * ub2l)
                        val lb =
                            Math.min(
                                Math.min(
                                    Math.min(lb1l * lb2l, ub1l * ub2l),
                                    ub1l * lb2l),
                                lb1l * ub2l)

                        if (lb < Int.MinValue || ub > Int.MaxValue)
                            IntegerValue(pc)
                        else
                            IntegerRange(lb.toInt, ub.toInt)
                    case _ ⇒
                        IntegerValue(pc)
                }

            case _ ⇒
                value2 match {
                    case IntegerRange(0, 0) ⇒ IntegerRange(0, 0)
                    case _ ⇒
                        IntegerValue(pc)
                }
        }
    }

    override def irem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): IntegerValueOrArithmeticException = {
        right match {
            case IntegerRange(rightLB, rightUB) if rightLB > 0 || rightUB < 0 ⇒
                // no div by "0"
                ComputedValue(IntegerValue(pc))
            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(
                        IntegerValue(pc), ArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(pc))
        }
    }

    override def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def i2b(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerRange(lb, ub) if lb >= Byte.MinValue && ub <= Byte.MaxValue ⇒
                value
            case _ ⇒
                IntegerRange(Byte.MinValue, Byte.MaxValue)
        }

    override def i2c(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerRange(lb, ub) if lb >= 0 && ub <= 65535 ⇒
                value
            case _ ⇒
                IntegerRange(0, 65535)
        }

    override def i2s(pc: PC, value: DomainValue): DomainValue =
        value match {
            case IntegerRange(lb, ub) if lb >= Short.MinValue && ub <= Short.MaxValue ⇒
                value
            case _ ⇒
                IntegerRange(Short.MinValue, Short.MaxValue)
        }
}

