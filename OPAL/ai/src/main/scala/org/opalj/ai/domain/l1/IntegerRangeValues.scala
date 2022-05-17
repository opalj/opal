/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.Int.{MinValue => MinInt}
import scala.Int.{MaxValue => MaxInt}

import org.opalj.value.IsIntegerValue
import org.opalj.br.CTIntType

/**
 * This domain represents integer values using ranges.
 *
 * The cardinality of the range can be configured to satisfy different needs with
 * regard to the desired precision ([[maxCardinalityOfIntegerRanges]]).
 * Often, a very small cardinality (e.g., between 2 and 8) may be
 * completely sufficient and a large cardinality does not add the overall precision
 * significantly and just increases the analysis time.
 *
 * ==Constraint Propagation==
 * This domain facilitates and performs constraint propagation (e.g.,
 * [[intEstablishValue]], [[intEstablishIsLessThan]],...).
 * '''Two integer (range) values (`ir1`,`ir2`) are reference equal (`eq` in Scala)
 * iff both represent the same runtime value.'''
 *
 * In other words, the implementation ensures that two int values that are known to
 * have the same value – even though the precise value may not be known – are
 * represented using the same object.
 * Furthermore, two int values that are '''not''' known to represent the same value at
 * runtime are always represented using different objects.
 * For example, consider the following sequence:
 *  - pcA+0/t1: `iadd` (Stack: 1 :: AnIntegerValue :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+1/t2: `dup` (Stack: v(pcA/t1) :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+2/t3: `iflt` true:+10 (Stack: v(pcA/t1) :: v(pcA/t1) :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+3/t4: ... '''(Stack: v(pcA/t1) >= 0''' :: ...; Registers: &lt;ignored&gt;)
 *  - pcA+XYZ...
 *  - pcA+12/t5: ... '''(Stack: v(pcA/t1) < 0''' :: ...; Registers: &lt;ignored&gt;)
 *
 * Here, the test (`iflt`) of the topmost stack value against the constant 0
 * constraints the second topmost stack value. Both (abstract) values are guaranteed
 * to represent the same value at runtime even though the concrete value
 * may be unknown. In this case, the value was even created at the same point in time.
 *
 * In case of this domain the ''reference'' of the '''Domain(Integer)Value'''
 * is used to identify those values that were created at the same point in time and
 * hence, have the same properties.
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
 * create new `IntegerRangeValue`s.
 *
 * ==Implementation Requirements==
 * Subclasses are required to create new instances of `IntegerRangeValue`s and
 * `AnIntegerValue` '''whenever''' a computation is performed that may affect the
 * runtime value.
 * If this property is not satisfied the implemented constraint propagation mechanism
 * will produce unpredictable results as it may constrain unrelated values!
 * This is true for concrete ranges as well as `AnIntegerValue`s.
 *
 * @author Michael Eichberg
 * @author Christos Votskos
 * @author David Becker
 */
trait IntegerRangeValues
    extends IntegerValuesDomain
    with IntegerRangeValuesFactory
    with ConcreteIntegerValues {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory =>

    // -----------------------------------------------------------------------------------
    //
    // CONFIGURATION OPTIONS
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum number of values captured by an integer value range.
     *
     * This setting is only used when true ranges are merged; in case of a join of two
     * concrete values we will always create an [[IntegerRangeLike]] value. If the cardinality
     * is exceeded, we will also first create ranges based on the boundaries determined
     * by the defaul data types (byte,short,char).
     *
     * This setting can be adapted at runtime.
     */
    def maxCardinalityOfIntegerRanges: Long = 16L

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `integer`.
     */
    sealed trait IntegerLikeValue
        extends TypedValue[CTIntType]
        with IsIntegerValue {
        this: DomainTypedValue[CTIntType] =>

        final override def leastUpperType: Option[CTIntType] = Some(CTIntType)

        def newInstance: DomainValue
    }

    /**
     * Represents an (unknown) integer value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnIntegerValueLike extends IntegerLikeValue {
        this: DomainTypedValue[CTIntType] =>
        final override def lowerBound: Int = Int.MinValue
        final override def upperBound: Int = Int.MaxValue
        final override def constantValue: Option[ValueOrigin] = None
    }

    /**
     * Represents a range of integer values. The range's bounds are inclusive.
     * Unless a range has only one value it is impossible to tell whether or not
     * a value that is in the range will potentially occur at runtime.
     */
    abstract class IntegerRangeLike extends IntegerLikeValue {
        this: DomainTypedValue[CTIntType] =>

        val lowerBound: Int // inclusive

        val upperBound: Int // inclusive

    }

    /**
     * Creates a new IntegerRange value with the lower and upper bound set to the
     * given value.
     */
    def IntegerRange(value: Int): DomainTypedValue[CTIntType] = {
        IntegerRange(value, value)
    }

    /**
     * Creates a new IntegerRange value with the given bounds.
     */
    def IntegerRange(lowerBound: Int, upperBound: Int): DomainTypedValue[CTIntType]

    /**
     * Creates a new IntegerRange value with the given bounds.
     */
    final def IntegerRange(
        origin:     ValueOrigin,
        lowerBound: Int, upperBound: Int
    ): DomainTypedValue[CTIntType] = {
        IntegerRange(lowerBound, upperBound)
    }

    /**
     * Extractor for `IntegerRange` values.
     */
    object IntegerRangeLike {
        def unapply(v: IntegerRangeLike): Some[(Int, Int)] = Some((v.lowerBound, v.upperBound))
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
        value: DomainValue
    )(
        f: Int => T
    )(orElse: => T): T =
        value match {
            case v: IntegerRangeLike if v.lowerBound == v.upperBound => f(v.lowerBound)
            case _                                                   => orElse
        }

    @inline final override def intValueOption(value: DomainValue): Option[Int] = // IMPROVE Define and use IntOption
        value match {
            case v: IntegerRangeLike if v.lowerBound == v.upperBound => Some(v.lowerBound)
            case _                                                   => None
        }

    @inline protected final def intValues[T](
        value1: DomainValue, value2: DomainValue
    )(
        f: (Int, Int) => T
    )(
        orElse: => T
    ): T = {
        intValue(value1) { v1 =>
            intValue(value2) { v2 => f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    override def intAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer = {
        if (value1 eq value2)
            // This handles the case that the two values are actually exactly the same value
            // (even if the concrete value is not known; i.e., they are both AnIntegerValue
            // or a range).
            return Yes

        value1 match {
            case IntegerRangeLike(lb1, ub1) =>
                value2 match {
                    case IntegerRangeLike(lb2, ub2) =>
                        if (ub1 < lb2 || lb1 > ub2)
                            No
                        else if (lb1 == lb2 && lb1 == ub2 && ub1 == ub2 /* or...*/ )
                            Yes
                        else
                            Unknown
                    case _ =>
                        Unknown
                }
            case _ => Unknown
        }
    }

    override def intIsSomeValueInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            Yes
        else
            value match {
                case IntegerRangeLike(lb, ub) => Answer(ub >= lowerBound && lb <= upperBound)
                case _                        => Unknown
            }
    }

    override def intIsSomeValueNotInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            No
        else
            value match {
                case IntegerRangeLike(lb, ub) => Answer(lb < lowerBound || ub > upperBound)
                case _                        => Unknown
            }
    }

    override def intIsLessThan(pc: Int, left: DomainValue, right: DomainValue): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return No

        right match {
            case IntegerRangeLike(rightLB, rightUB) =>
                if (rightUB == Int.MinValue)
                    No
                else
                    left match {
                        case IntegerRangeLike(leftLB, leftUB) =>
                            if (leftUB < rightLB)
                                Yes
                            else if (leftLB >= rightUB)
                                No
                            else
                                Unknown
                        case _ =>
                            Unknown
                    }
            case _ =>
                left match {
                    case IntegerRangeLike(Int.MaxValue, _ /* Int.MaxValue*/ ) => No
                    case _ => Unknown
                }
        }
    }

    override def intIsLessThanOrEqualTo(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer = {

        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes;

        right match {
            case IntegerRangeLike(rightLB, rightUB) =>
                if (rightLB == Int.MaxValue)
                    Yes
                else
                    left match {
                        case IntegerRangeLike(leftLB, leftUB) =>
                            if (leftUB <= rightLB)
                                Yes
                            else if (leftLB > rightUB)
                                No
                            else
                                Unknown
                        case _ =>
                            Unknown
                    }
            case _ =>
                left match {
                    case IntegerRangeLike(_, Int.MinValue) => Yes
                    case _                                 => Unknown
                }
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    override def intEstablishValue(
        pc:       Int,
        theValue: Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        value match {
            case IntegerRangeLike(`theValue`, `theValue`) =>
                // "nothing to do"
                (operands, locals)
            case _ =>
                updateMemoryLayout(
                    value, IntegerRange(theValue, theValue),
                    operands, locals
                )
        }
    }

    override def intEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        if (value1 eq value2)
            // this basically handles the case that both are "AnIntegerValue"
            (operands, locals)
        else
            // Given that the values are determined to be EQUAL, every subsequent
            // constraint applies to both values (independent of
            // the implicit origin).
            value1 match {
                case IntegerRangeLike(lb1, ub1) =>
                    value2 match {
                        case IntegerRangeLike(lb2, ub2) =>
                            val lb = Math.max(lb1, lb2)
                            val ub = Math.min(ub1, ub2)
                            val newValue = IntegerRange(lb, ub)

                            val (operands1, locals1) =
                                updateMemoryLayout(value1, newValue, operands, locals)
                            updateMemoryLayout(value2, newValue, operands1, locals1)

                        case _ =>
                            // value1 remains unchanged
                            updateMemoryLayout(value2, value1, operands, locals)
                    }
                case _ =>
                    updateMemoryLayout(value1, value2, operands, locals)
            }
    }

    override def intEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        // Given that we cannot represent multiple ranges, our possibilities are
        // severely limited w.r.t. representing values that are not equal.
        // Only if one range just represents a single value
        // and this value is a boundary value of the other range it is possible
        // to establish "something"
        val (lb1, ub1) = value1 match {
            case IntegerRangeLike(lb1, ub1) => (lb1, ub1)
            case _                          => (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = value2 match {
            case IntegerRangeLike(lb2, ub2) => (lb2, ub2)
            case _                          => (Int.MinValue, Int.MaxValue)
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

    override def intEstablishIsLessThan(
        pc:       Int,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val (lb1, ub1) = left match {
            case IntegerRangeLike(lb1, ub1) => (lb1, ub1)
            case _                          => (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = right match {
            case IntegerRangeLike(lb2, ub2) => (lb2, ub2)
            case _                          => (Int.MinValue, Int.MaxValue)
        }
        // establish new bounds // e.g. l=(0,0) & r=(-10,0)
        assert(lb1 < ub2, s"the value $left cannot be less than $right")

        val newUB1 = Math.min(ub1, ub2 - 1)
        val newMemoryLayout @ (operands1, locals1) =
            if (newUB1 != ub1)
                updateMemoryLayout(left, IntegerRange(lb1, newUB1), operands, locals)
            else
                (operands, locals)

        val lb = Math.max(lb1 + 1, lb2)
        if (lb != lb2)
            updateMemoryLayout(
                right,
                IntegerRange(lb, ub2),
                operands1,
                locals1
            )
        else
            newMemoryLayout
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc:       Int,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val (lb1, ub1) = left match {
            case IntegerRangeLike(lb1, ub1) => (lb1, ub1)
            case _                          => (Int.MinValue, Int.MaxValue)
        }
        val (lb2, ub2) = right match {
            case IntegerRangeLike(lb2, ub2) => (lb2, ub2)
            case _                          => (Int.MinValue, Int.MaxValue)
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
    override def ineg(pc: Int, value: DomainValue) = value match {
        case IntegerRangeLike(_, Int.MinValue) /* => lb is also Int.MinValue*/ => value
        case IntegerRangeLike(Int.MinValue, _) => IntegerValue(pc)
        case IntegerRangeLike(lb, ub) => IntegerRange(-ub, -lb)
        case _ => IntegerValue(pc)
    }

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case IntegerRangeLike(lb1, ub1) =>
                if (lb1 == 0 && ub1 == 0)
                    value2
                else
                    value2 match {
                        case IntegerRangeLike(lb2, ub2) =>
                            if (lb2 == 0 && ub2 == 0)
                                value1
                            else {
                                // to identify overflows we simply do the "add" on long values
                                // and check afterwards
                                val lb: Long = lb1.toLong + lb2.toLong
                                val ub: Long = ub1.toLong + ub2.toLong
                                if (lb < Int.MinValue || ub > Int.MaxValue)
                                    IntegerValue(pc)
                                else
                                    IntegerRange(lb.toInt, ub.toInt)
                            }
                        case _ =>
                            IntegerValue(pc)
                    }

            case _ =>
                value2 match {
                    case IntegerRangeLike(0, 0) => value1
                    case _                      => IntegerValue(pc)
                }
        }
    }

    override def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue = {
        if (increment == 0)
            return value;

        value match {
            case IntegerRangeLike(lb, ub) =>
                val newLB = lb.toLong + increment.toLong
                val newUB = ub.toLong + increment.toLong
                if (newLB < Int.MinValue || newUB > Int.MaxValue)
                    IntegerValue(pc)
                else
                    IntegerRange(newLB.toInt, newUB.toInt)
            case _ =>
                IntegerValue(pc)
        }
    }

    override def isub(pc: Int, left: DomainValue, right: DomainValue): DomainValue = {
        if (left eq right) {
            return left /* or right*/ match {
                case IntegerRangeLike(0, 0) => left
                case _                      => IntegerRange(0, 0)
            }
        }

        right match {
            case IntegerRangeLike(0, 0) =>
                left
            case IntegerRangeLike(rlb, rub) =>
                left match {
                    case IntegerRangeLike(llb, lub) =>

                        // to identify overflows we simply do the "add" on long values
                        // and check afterwards
                        val lb = llb.toLong - rub.toLong
                        val ub = lub.toLong - rlb.toLong
                        if (lb < Int.MinValue || ub > Int.MaxValue)
                            IntegerValue(pc)
                        else
                            IntegerRange(lb.toInt, ub.toInt)
                    case _ =>
                        IntegerValue(pc)
                }

            case _ =>
                // we don't know what we are subtracting
                IntegerValue(pc)
        }
    }

    override def idiv(
        pc:          Int,
        numerator:   DomainValue,
        denominator: DomainValue
    ): IntegerValueOrArithmeticException = {

        def genericResult() = {
            val value =
                if (numerator eq denominator)
                    IntegerRange(1)
                else
                    IntegerValue(pc)

            if (throwArithmeticExceptions)
                ComputedValueOrException(value, VMArithmeticException(pc))
            else
                ComputedValue(value)
        }

        denominator match {
            case IntegerRangeLike(1, 1) => ComputedValue(numerator)
            case IntegerRangeLike(dlb, dub) =>
                if (dlb > 0) {
                    // no div by "0"
                    numerator match {
                        case IntegerRangeLike(nlb, nub) =>
                            ComputedValue(IntegerRange(nlb / dlb, nub / dlb))
                        case _ =>
                            ComputedValue(IntegerValue(pc))
                    }
                } else if (dlb == 0 && dub == 0) {
                    ThrowsException(VMArithmeticException(pc))
                } else if (dub < 0) {
                    ComputedValue(IntegerValue(pc))
                } else {
                    genericResult()
                }

            // IMPROVE [IntegerRangeValues] handling of divisors that span values in the range[-X,+Y]

            case _ =>
                genericResult()
        }
    }

    override def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, IntegerRangeLike(0, 0)) => value2
            case (_, IntegerRangeLike(1, 1)) => value1
            case (IntegerRangeLike(0, 0), _) => value1
            case (IntegerRangeLike(1, 1), _) => value2

            case (IntegerRangeLike(lb1, ub1), IntegerRangeLike(lb2, ub2)) =>
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
                            ub1l * lb2l
                        ),
                        lb1l * ub2l
                    )

                if (lb < Int.MinValue || ub > Int.MaxValue)
                    IntegerValue(pc)
                else
                    IntegerRange(lb.toInt, ub.toInt)

            case _ =>
                IntegerValue(pc)
        }
    }

    override def irem(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {

        right match {
            case IntegerRangeLike(0, 0)   => ThrowsException(VMArithmeticException(pc))
            case IntegerRangeLike(1, 1)   => ComputedValue(IntegerRange(0))
            case IntegerRangeLike(-1, -1) => ComputedValue(IntegerRange(0))

            case IntegerRangeLike(rightLB, rightUB) =>
                def result(newValue: DomainValue) =
                    if (rightLB > 0 || rightUB < 0 || !throwArithmeticExceptions)
                        ComputedValue(newValue)
                    else
                        ComputedValueOrException(newValue, VMArithmeticException(pc))

                left match {

                    case IntegerRangeLike(leftLB, leftUB) =>
                        if (leftLB == leftUB && rightLB == rightUB) {
                            // two point ranges...
                            val result = leftLB % rightLB
                            ComputedValue(IntegerRange(result, result))
                        } else if (rightLB == Int.MinValue) {
                            // the following "else" relies on a divisor > Int.MinValue
                            result(IntegerValue(origin = pc))
                        } else {
                            // From the spec:
                            // [...] the result of the remainder operation can be negative only if
                            // the dividend is negative and can be positive only if the dividend
                            // is positive[...]
                            val maxDividend = Math.max(Math.abs(leftLB), Math.abs(leftUB))
                            var maxDivisor = Math.max(Math.abs(rightLB), Math.abs(rightUB))
                            if (maxDividend < maxDivisor /* e.g., [0..3] % [1..8] */ )
                                maxDivisor = maxDividend + 1
                            val newValue =
                                if (leftLB < 0) {
                                    if (leftUB < 0)
                                        IntegerRange(-(maxDivisor - 1), 0)
                                    else
                                        IntegerRange(-(maxDivisor - 1), maxDivisor - 1)
                                } else
                                    IntegerRange(0, maxDivisor - 1)
                            result(newValue)
                        }

                    case _ /*AnIntegerValue*/ if rightLB > Int.MinValue =>
                        val maxDivisor = Math.max(Math.abs(rightLB), Math.abs(rightUB))
                        val newValue = IntegerRange(-(maxDivisor - 1), maxDivisor - 1)
                        result(newValue)
                    case _ /*AnIntegerValue*/ =>
                        result(IntegerValue(origin = pc))
                }

            case _ => // right is "AnIntegerValue"
                val newValue =
                    if (left eq right) // "a value % itself is always 0 unless the value is 0"
                        IntegerValue(pc, 0)
                    else
                        IntegerValue(pc)
                if (throwArithmeticExceptions)
                    ComputedValueOrException(newValue, VMArithmeticException(pc))
                else
                    ComputedValue(newValue)
        }
    }

    override def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        if (value1 eq value2)
            return value1 // or value2

        (value1, value2) match {
            case (_, IntegerRangeLike(0, 0))   => value2
            case (_, IntegerRangeLike(-1, -1)) => value1
            case (IntegerRangeLike(0, 0), _)   => value1
            case (IntegerRangeLike(-1, -1), _) => value2

            case (IntegerRangeLike(vlb, vub), IntegerRangeLike(slb, sub)) =>
                if (vlb == vub && slb == sub) {
                    // "two point values"
                    val r = vlb & slb
                    IntegerRange(r, r)
                } else if (vlb >= 0) {
                    IntegerRange(0, vub)
                } else if (slb >= 0) {
                    IntegerRange(0, sub)
                } else
                    IntegerValue(pc)

            // IMPROVE [IntegerRangeValues] General handling of "and" for two integer range values
            case (_, IntegerRangeLike(lb2, ub2)) if (lb2 >= 0) => IntegerRange(0, ub2)
            case (IntegerRangeLike(lb1, ub1), _) if (lb1 >= 0) => IntegerRange(0, ub1)
            case _                                             => IntegerValue(pc)
        }
    }

    override def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        if (value1 eq value2)
            return value1

        (value1, value2) match {
            case (_, IntegerRangeLike(0, 0))   => value1
            case (_, IntegerRangeLike(-1, -1)) => value2
            case (IntegerRangeLike(0, 0), _)   => value2
            case (IntegerRangeLike(-1, -1), _) => value1

            case (IntegerRangeLike(llb, lub), IntegerRangeLike(rlb, rub)) =>
                // We have two "arbitrary" ranges.
                // Recall that negative values always have a "1" in the highest Bit
                // and that -1 is the value where all 32 bits are "1".
                if (llb == lub && rlb == rub) {
                    val result = llb | rlb
                    IntegerRange(result, result)
                } else if (lub >= 0 && rub < 0) {
                    IntegerRange(rlb, -1)
                } else if (lub < 0 && rub >= 0) {
                    IntegerRange(llb, -1)
                } else if (lub < 0 && rub < 0) {
                    val lb = Math.max(llb, rlb)
                    IntegerRange(lb, -1)
                } else {
                    // both values are positive (lbs are >= 0) and at least one
                    // value is not just 0.
                    import java.lang.Integer.{numberOfLeadingZeros => nlz}
                    val max = Math.max(rub, lub)
                    val nlzMax = nlz(max)
                    val ub =
                        if (nlzMax == 1)
                            Int.MaxValue
                        else // nlzMax >= 2
                            (1 << (33 - nlzMax)) - 1
                    val lb = if (llb < rlb) llb else rlb
                    IntegerRange(lb, ub)
                }

            case _ =>
                IntegerValue(pc)
        }
    }

    override def ishl(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        // RECALL THAT ONLY THE FIVE LOWEST BITS OF THE SHIFT VALUE ARE CONSIDERED!
        // I.E. THE SHIFT IS ALWAYS BETWEEN 0 AND 31 BITS
        (value, shift) match {
            case (_, IntegerRangeLike(0, 0)) => value
            case (IntegerRangeLike(0, 0), _) => value

            case (IntegerRangeLike(vlb, vub), IntegerRangeLike(slb, sub)) =>
                val maxShift = if (sub > 31 || sub < 0) 31 else sub
                val minShift = if (slb >= 0 && slb <= 31) slb else 0

                if (vlb == vub && slb == sub) {
                    val r = vlb << slb
                    IntegerRange(r)
                } else if (vlb >= 0) {
                    // max : Long will never be negative, since shifting a 64 bit value at most 31
                    // times always results into a positive value
                    val max = vub.toLong << maxShift

                    if (max <= Int.MaxValue)
                        IntegerRange(vlb << minShift, max.toInt)
                    else
                        // `lb` is "(1 << maxShift) | Int.MinValue", since the smallest
                        // value has the most trailing zeros that will be shifted.
                        // By using the `or` operation after (1 << maxShift) the sign
                        // bit is set to one, since the smallest value is negative.
                        //
                        // `ub` is "(-1 << minShift) & Int.MaxValue", since the biggest
                        // number after a shift always has the pattern 0111..1100..0.
                        // The number of trailing zeros must be minShift, the sign
                        // bit 0 and bits between sign bit and next zero bit have to
                        // be 1.
                        // To ensure that the number is always positive the sign bit
                        // is set to zero by using "& Int.MaxValue".
                        IntegerRange((1 << maxShift) | MinInt, (-1 << minShift) & Int.MaxValue)
                } else {
                    IntegerRange((1 << maxShift) | MinInt, (-1 << minShift) & MaxInt)
                }

            case (IntegerRangeLike(-1, -1), _) => IntegerRange(Int.MinValue, -1)
            case (_, IntegerRangeLike(31, 31)) =>
                // the value is either Int.MinValue or 0
                IntegerRange(Int.MinValue, 0)

            case _ => IntegerValue(pc)
        }
    }

    override def ishr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        (value, shift) match {
            case (_, IntegerRangeLike(0, 0))   => value
            case (IntegerRangeLike(0, 0), _)   => value

            // In this case a signed shift does not change the value ([-1,-1]).
            case (IntegerRangeLike(-1, -1), _) => value
            case (_, IntegerRangeLike(31, 31)) => IntegerRange(-1, 0)

            case (IntegerRangeLike(vlb, vub), IntegerRangeLike(slb, sub)) if vlb == vub && slb == sub =>
                val r = vlb >> slb
                IntegerRange(r)

            case (IntegerRangeLike(vlb, vub), IntegerRangeLike(slb, sub)) =>
                // We have one "arbitrary" range of numbers to shift and one range that
                // should be between 0 and 31. Every number above 31 or any negative number
                // does not make sense, since only the five least significant bits are used
                // for shifting.
                val maxShift = if (sub >= 0 && sub <= 31) sub else 31
                val minShift = if (slb >= 0 && slb <= 31) slb else 0

                if (vlb >= 0) {
                    val lb = vlb >> maxShift
                    val ub = vub >> minShift
                    IntegerRange(lb, ub)

                } else if (vlb < 0 && vub >= 0) {
                    val lb = vlb >> minShift
                    val ub = vub >> minShift
                    IntegerRange(lb, ub)

                } else { // case vub < 0
                    val lb = vlb >> minShift
                    val ub = vub >> maxShift
                    IntegerRange(lb, ub)
                }

            case _ => IntegerValue(pc)
        }

    }

    override def iushr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        (value, shift) match {
            case (IntegerRangeLike(0, 0), _) => value
            case (_, IntegerRangeLike(0, 0)) => value

            case (IntegerRangeLike(vlb, vub), IntegerRangeLike(slb, sub)) =>
                if (vlb == vub && slb == sub) {
                    val result = vlb >>> slb
                    IntegerRange(result)
                } else {
                    // Recall: the shift value is at most "31" (a corresponding)
                    // bit mask is always implicitly applied to the shift value.

                    // IMPROVE [IntegerRangeValues] log suspicious shift value
                    val maxShift = if (sub > 31 || sub < 0) 31 else sub
                    val minShift = if (slb >= 0 && slb <= 31) slb else 0

                    if (vlb >= 0) {
                        val lb = vlb >>> maxShift
                        val ub = vub >>> minShift
                        IntegerRange(lb, ub)

                    } else if (vlb < 0 && vub >= 0) {
                        if (minShift == 0)
                            IntegerRange(vlb, -1 >>> 1)
                        else
                            IntegerRange(0, -1 >>> minShift)

                    } else { // last case: vub < 0
                        val lb = if (minShift == 0) vlb else vub >>> maxShift
                        val ub =
                            if (minShift != 0)
                                vub >>> minShift
                            else if (maxShift > 0)
                                vub >>> 1
                            else
                                vub

                        IntegerRange(lb, ub)
                    }
                }

            case _ =>
                IntegerValue(pc)
        }
    }

    override def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        if (value1 eq value2)
            return IntegerValue(pc, 0);

        (value1, value2) match {

            case (IntegerRangeLike(v1lb, v1ub), IntegerRangeLike(v2lb, v2ub)) =>
                if (v1lb == v1ub && v2lb == v2ub)
                    IntegerRange(v1lb ^ v2lb)

                else if (v1lb >= 0 && v2lb >= 0) {
                    val smallerUB = Math.min(v1ub, v2ub)
                    val smallerUBLZ = Integer.numberOfLeadingZeros(smallerUB)

                    // Calculate the upper bound
                    // Idea: 1. Get the leading bit sequence of the larger value.
                    //       (leading means: all bits up until the highest-order
                    //        bit of the lower value)
                    //       2. Set all subsequent bits to "1"
                    // Example:
                    // ub1 = 0101 0000
                    // ub2 = 0000 1010
                    // ub^ = 0101 1111
                    val ub = Math.max(v1ub, v2ub) | (Int.MaxValue >>> (smallerUBLZ - 1))

                    val lb =
                        // Calculate the lower bound _if the ranges are not overlapping_
                        // Idea: 1. Get the leading bit sequence of the lower bound of the
                        //       range with the larger values.
                        //       (leading means: all bits up until the highest-order
                        //        bit of the upper bound of the range of smaller values)
                        //       2. Set all subsequent bits to "0"
                        // Example:
                        // ub1 = 0101 0000 (ub1 = 80(dez))
                        // lb1 = 0100 1110 (lb1 = 78(dez))
                        // ub2 = 0000 0100 (ub1 = 4(dez))
                        // lb2 = 0000 0001 (lb1 = 1(dez))
                        // lb^ = 0100 1000 (lb  = 72(dez))
                        if (v1ub < v2lb || v2ub < v1lb) {
                            // the ranges are non-overlapping
                            val largerLB = Math.max(v1lb, v2lb)
                            largerLB & (-1 << (32 - smallerUBLZ))
                        } else {
                            // the ranges are overlapping
                            0
                        }

                    IntegerRange(lb, ub)

                } else if (v1ub < 0 && v2ub < 0) {
                    def numberOfLeadingOnes(value: Int) = Integer.numberOfLeadingZeros(~value)

                    val vlbLOs = numberOfLeadingOnes(v1lb)
                    val slbLOs = numberOfLeadingOnes(v2lb)

                    // Example (the ranges are not overlapping)
                    // lb1 = 1010 1101 ... ub1 = 1110 0000
                    // lb2 = 1111 0111 ... ub2 = 1111 1101
                    // ub^ = 0101 1111 ... lb^ = 0000 0000
                    val ub =
                        if (vlbLOs != slbLOs)
                            ~Math.min(v1lb, v2lb) | (-1 >>> (Math.max(vlbLOs, slbLOs) - 1))
                        else
                            (1 << (32 - vlbLOs)) - 1

                    val lb =
                        if (v1ub < v2lb) {
                            ~v1ub & (-1 << (32 - numberOfLeadingOnes(v2lb)))
                        } else if (v2ub < v1lb) {
                            ~v2ub & (-1 << (32 - numberOfLeadingOnes(v1lb)))
                        } else
                            // the ranges are overlapping
                            0

                    IntegerRange(lb, ub)

                } else if (v1ub < 0 && v2lb >= 0) {
                    val subBits = 32 - Integer.numberOfLeadingZeros(v2ub)

                    // The min value is calculated by setting subBits to zero,
                    // since min neg value contains the most trailing zeros.
                    // The first right shift preserves the leading bits, since
                    // the do not change.
                    val lb = (v1lb >> subBits) << subBits

                    // The max value is calculated by setting subBits to one,
                    // since this change can result into the max number. The
                    // or operation leading preserves the leading bits, since
                    // those bits do not change.
                    val ub = v1ub | ((1 << subBits) - 1)

                    IntegerRange(lb, ub)
                } else if (v2ub < 0 && v1lb >= 0) {
                    val vubBits = 32 - Integer.numberOfLeadingZeros(v1ub)

                    // same idea like above
                    val lb = (v2lb >> vubBits) << vubBits
                    val ub = v2ub | ((1 << vubBits) - 1)

                    IntegerRange(lb, ub)
                } else if (v1lb < 0 && v1ub >= 0 && v2lb >= 0) {
                    val vubBits = 32 - Integer.numberOfLeadingZeros(v1ub)
                    val subBits = 32 - Integer.numberOfLeadingZeros(v2ub)

                    // same idea like above
                    val lb = (v1lb >> subBits) << subBits
                    val ub = (1 << (if (vubBits > subBits) vubBits else subBits)) - 1

                    IntegerRange(lb, ub)
                } else if (v2lb < 0 && v2ub >= 0 && v1lb >= 0) {
                    val vubBits = 32 - Integer.numberOfLeadingZeros(v1ub)
                    val subBits = 32 - Integer.numberOfLeadingZeros(v2ub)

                    // same idea like first case: vlb >= 0 && slb >= 0
                    val lb = (v2lb >> vubBits) << vubBits
                    val ub = (1 << (if (subBits > vubBits) subBits else vubBits)) - 1

                    IntegerRange(lb, ub)
                } else if (v1lb < 0 && v1ub >= 0 && v2ub < 0) {
                    val vlbBits = 32 - Integer.numberOfLeadingZeros(~v1lb)
                    val vubBits = 32 - Integer.numberOfLeadingZeros(v1ub)
                    val slbBits = 32 - Integer.numberOfLeadingZeros(~v2lb)

                    // same idea like first case: vlb >= 0 && slb >= 0
                    val lb = (v2lb >> vubBits) << vubBits
                    val ub = (1 << (if (vlbBits > slbBits) vlbBits else slbBits)) - 1

                    IntegerRange(lb, ub)
                } else if (v2lb < 0 && v2ub >= 0 && v1ub < 0) {
                    val vlbBits = 32 - Integer.numberOfLeadingZeros(~v1lb)
                    val slbBits = 32 - Integer.numberOfLeadingZeros(~v2lb)
                    val subBits = 32 - Integer.numberOfLeadingZeros(v2ub)

                    // same idea like first case: vlb >= 0 && slb >= 0
                    val lb = (v1lb >> subBits) << subBits
                    val ub = (1 << (if (slbBits > vlbBits) slbBits else vlbBits)) - 1
                    IntegerRange(lb, ub)

                } else { // case [-,+] [-,+]
                    val vlbBits = 32 - Integer.numberOfLeadingZeros(~v1lb)
                    val vubBits = 32 - Integer.numberOfLeadingZeros(v1ub)
                    val slbBits = 32 - Integer.numberOfLeadingZeros(~v2lb)
                    val subBits = 32 - Integer.numberOfLeadingZeros(v2ub)

                    // The min value can be calculated by either using the min neg
                    // value of range s and max pos value of range v or vice versa.
                    val lbCand1 = -1 << (if (slbBits > vubBits) slbBits else vubBits)
                    val lbCand2 = -1 << (if (subBits > vlbBits) subBits else vlbBits)
                    val lb = Math.min(lbCand1, lbCand2)

                    // The max value can be calculated by either using the min neg
                    // value of range s and v or the max value of range s and v.
                    val ubCand1 = (1 << (if (slbBits > vlbBits) slbBits else vlbBits)) - 1
                    val ubCand2 = (1 << (if (subBits > vubBits) subBits else vubBits)) - 1
                    val ub = Math.max(ubCand1, ubCand2)

                    IntegerRange(lb, ub)
                }

            case _ =>
                IntegerValue(pc)
        }
    }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def i2b(pc: Int, value: DomainValue): DomainValue =
        value match {
            case IntegerRangeLike(lb, ub) if lb >= Byte.MinValue && ub <= Byte.MaxValue =>
                value
            case _ =>
                IntegerRange(Byte.MinValue, Byte.MaxValue)
        }

    override def i2c(pc: Int, value: DomainValue): DomainValue =
        value match {
            case IntegerRangeLike(lb, ub) if lb >= 0 && ub <= Char.MaxValue =>
                value
            case _ =>
                IntegerRange(0, 65535)
        }

    override def i2s(pc: Int, value: DomainValue): DomainValue =
        value match {
            case IntegerRangeLike(lb, ub) if lb >= Short.MinValue && ub <= Short.MaxValue =>
                value
            case _ =>
                IntegerRange(Short.MinValue, Short.MaxValue)
        }
}
/**
 * Defines common constants related to integer ranges.
 */
object IntegerRangeValues {

    /**
     * The largest cardinality that makes sense.
     */
    final val AbsoluteMaxCardinalityOfIntegerRanges = Int.MaxValue.toLong + (-(Int.MinValue.toLong))

}
