/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.value.IsIntegerValue
import org.opalj.br.CTIntType

/**
 * This domain enables the tracking of an integer value (a constant);
 * unknown integer values are represented using "AnIntegerValue". It basically provides support
 * for constant propagation and constant computations related to integer values.
 *
 * Given that it uses one instance to represent arbitrary integer values, '''constraint
 * propagation is not relevant'''.
 *
 * This domain may be appropriate, e.g., if you want to determine if a field/local is
 * always initialized to a specific value.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait IntegerValues extends IntegerValuesDomain with ConcreteIntegerValues {
    domain: Configuration with ExceptionsFactory =>

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

    }

    /**
     * Represents an (unknown) integer value.
     *
     * Models the top value of this domain's lattice.
     */
    trait AnIntegerValue extends IntegerLikeValue { this: DomainTypedValue[CTIntType] =>
        final override def lowerBound: Int = Int.MinValue
        final override def upperBound: Int = Int.MaxValue
        final override def constantValue: Option[Int] = None
    }

    /**
     * Represents one, concrete integer value.
     */
    abstract class TheIntegerValue extends IntegerLikeValue { this: DomainTypedValue[CTIntType] =>
        val value: Int

        final override def lowerBound: Int = value
        final override def upperBound: Int = value
    }

    object TheIntegerValue { def unapply(v: TheIntegerValue): Some[Int] = Some(v.value) }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO INTEGER VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTIONS ABOUT VALUES
    //

    @inline final override def intValue[T](
        value: DomainValue
    )(
        f: Int => T
    )(orElse: => T): T =
        value match {
            case v: TheIntegerValue => f(v.value)
            case _                  => orElse
        }

    @inline final override def intValueOption(value: DomainValue): Option[Int] =
        value match {
            case v: TheIntegerValue => Some(v.value)
            case _                  => None
        }

    @inline protected final def intValues[T](
        value1: DomainValue, value2: DomainValue
    )(
        f: (Int, Int) => T
    )(
        orElse: => T
    ): T = {
        intValue(value1) { v1 => intValue(value2) { v2 => f(v1, v2) } { orElse } } { orElse }
    }

    override def intAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer = {
        intValues(value1, value2) { (v1, v2) => Answer(v1 == v2) } { Unknown }
    }

    override def intIsSomeValueInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return Yes;

        value match {
            case TheIntegerValue(v) => Answer(v >= lowerBound && v <= upperBound)
            case _                  => Unknown
        }
    }

    override def intIsSomeValueNotInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return No;

        value match {
            case TheIntegerValue(v) => Answer(v < lowerBound || v > upperBound)
            case _                  => Unknown
        }
    }

    override def intIsLessThan(pc: Int, left: DomainValue, right: DomainValue): Answer = {
        intValues(left, right) { (v1, v2) => Answer(v1 < v2) } { Unknown }
    }

    override def intIsLessThanOrEqualTo(pc: Int, left: DomainValue, right: DomainValue): Answer = {
        intValues(left, right) { (v1, v2) => Answer(v1 <= v2) } { Unknown }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    override def ineg(pc: Int, value: DomainValue): DomainValue = {
        value match {
            case TheIntegerValue(v) => IntegerValue(pc, -v)
            case v                  => v
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    override def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue = {
        value match {
            case TheIntegerValue(v) => IntegerValue(pc, v + increment)
            case v                  => v
        }
    }

    override def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        intValues(value1, value2) { (v1, v2) =>
            IntegerValue(pc, v1 + v2)
        } {
            IntegerValue(origin = pc)
        }
    }

    override def isub(pc: Int, left: DomainValue, right: DomainValue): DomainValue = {
        intValues(left, right) { (l, r) =>
            IntegerValue(pc, l - r)
        } {
            IntegerValue(origin = pc)
        }
    }

    override def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (_, TheIntegerValue(0))                  => value2
            case (_, TheIntegerValue(1))                  => value1
            case (TheIntegerValue(0), _)                  => value1
            case (TheIntegerValue(1), _)                  => value2

            case (TheIntegerValue(l), TheIntegerValue(r)) => IntegerValue(pc, l * r)

            case _                                        => IntegerValue(origin = pc)
        }
    }

    override def idiv(
        pc:          Int,
        numerator:   DomainValue,
        denominator: DomainValue
    ): IntegerValueOrArithmeticException = {

        (numerator, denominator) match {
            case (_, TheIntegerValue(0))                  => ThrowsException(VMArithmeticException(pc))
            case (TheIntegerValue(n), TheIntegerValue(d)) => ComputedValue(IntegerValue(pc, n / d))
            case (_, TheIntegerValue(_ /*<=> not 0*/ ))   => ComputedValue(IntegerValue(origin = pc))
            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(origin = pc), VMArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(origin = pc))
        }
    }

    override def irem(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {

        (left, right) match {
            case (_, TheIntegerValue(0))                  => ThrowsException(VMArithmeticException(pc))
            case (TheIntegerValue(n), TheIntegerValue(d)) => ComputedValue(IntegerValue(pc, n % d))
            case (_, TheIntegerValue(_ /*<=> not 0*/ ))   => ComputedValue(IntegerValue(origin = pc))
            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(origin = pc), VMArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(origin = pc))
        }
    }

    override def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        (value1, value2) match {
            case (_, TheIntegerValue(-1))                 => value1
            case (_, TheIntegerValue(0))                  => value2
            case (TheIntegerValue(-1), _)                 => value2
            case (TheIntegerValue(0), _)                  => value1

            case (TheIntegerValue(l), TheIntegerValue(r)) => IntegerValue(pc, l & r)

            case _                                        => IntegerValue(origin = pc)
        }

    override def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        (value1, value2) match {
            case (_, TheIntegerValue(-1))                 => value2
            case (_, TheIntegerValue(0))                  => value1
            case (TheIntegerValue(-1), _)                 => value1
            case (TheIntegerValue(0), _)                  => value2

            case (TheIntegerValue(l), TheIntegerValue(r)) => IntegerValue(pc, l | r)

            case _                                        => IntegerValue(origin = pc)
        }

    override def ishl(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        intValues(value, shift) { (v1, v2) =>
            IntegerValue(pc, v1 << v2)
        } {
            IntegerValue(origin = pc)
        }
    }

    override def ishr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        intValues(value, shift) { (v1, v2) =>
            IntegerValue(pc, v1 >> v2)
        } {
            IntegerValue(origin = pc)
        }
    }

    override def iushr(pc: Int, value: DomainValue, shift: DomainValue): DomainValue = {
        intValues(value, shift) { (v1, v2) =>
            IntegerValue(pc, v1 >>> v2)
        } {
            IntegerValue(origin = pc)
        }
    }

    override def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        intValues(value1, value2) { (v1, v2) =>
            IntegerValue(pc, v1 ^ v2)
        } {
            IntegerValue(origin = pc)
        }
    }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    override def i2b(pc: Int, value: DomainValue): DomainValue =
        value match {
            case TheIntegerValue(v) => IntegerValue(pc, v.toByte.toInt)
            case v                  => v
        }

    override def i2c(pc: Int, value: DomainValue): DomainValue =
        value match {
            case TheIntegerValue(v) => IntegerValue(pc, v.toChar.toInt)
            case v                  => v
        }

    override def i2s(pc: Int, value: DomainValue): DomainValue =
        value match {
            case TheIntegerValue(v) => IntegerValue(pc, v.toShort.toInt)
            case v                  => v
        }

}
