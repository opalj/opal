/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.value.IsBooleanValue
import org.opalj.value.IsByteValue
import org.opalj.value.IsCharValue
import org.opalj.value.IsIntegerValue
import org.opalj.value.IsShortValue
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.CTIntType
import org.opalj.br.IntegerType
import org.opalj.br.ShortType

/**
 * Domain that performs computations related to integer values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelIntegerValues extends Domain { this: Configuration =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over values with computational type `integer`.
     */
    protected[this] trait ComputationalTypeIntegerValue[T <: CTIntType] extends TypedValue[T] {
        this: DomainTypedValue[T] =>

        override def summarize(pc: Int): DomainValue = this

    }

    trait BooleanValue extends ComputationalTypeIntegerValue[BooleanType] with IsBooleanValue {
        this: DomainTypedValue[BooleanType] =>

        final override def leastUpperType: Option[BooleanType] = Some(BooleanType)

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.BooleanValue(vo)
        }
    }

    trait ByteValue extends ComputationalTypeIntegerValue[ByteType] with IsByteValue {
        this: DomainTypedValue[ByteType] =>

        final override def leastUpperType: Option[ByteType] = Some(ByteType)

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.ByteValue(vo)
        }

    }

    trait CharValue extends ComputationalTypeIntegerValue[CharType] with IsCharValue {
        this: DomainTypedValue[CharType] =>

        final override def leastUpperType: Option[CharType] = Some(CharType)

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.CharValue(vo)
        }

    }

    trait ShortValue extends ComputationalTypeIntegerValue[ShortType] with IsShortValue {
        this: DomainTypedValue[ShortType] =>

        final override def leastUpperType: Option[ShortType] = Some(ShortType)

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.ShortValue(vo)
        }

    }

    trait IntegerValue extends ComputationalTypeIntegerValue[IntegerType] with IsIntegerValue {
        this: DomainTypedValue[IntegerType] =>

        final override def leastUpperType: Option[IntegerType] = Some(IntegerType)

        final override def lowerBound: Int = Int.MinValue
        final override def upperBound: Int = Int.MaxValue

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue = {
            target.IntegerValue(vo)
        }

    }

    //
    // QUESTION'S ABOUT VALUES
    //

    override def intAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer =
        Unknown

    override def intIsSomeValueInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer =
        Unknown

    override def intIsSomeValueNotInRange(
        pc:         Int,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer =
        Unknown

    override def intIsLessThan(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer =
        Unknown

    override def intIsLessThanOrEqualTo(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer =
        Unknown

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //

    override def ineg(pc: Int, value: DomainValue): DomainValue = IntegerValue(pc)

    //
    // BINARY EXPRESSIONS
    //

    override def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def idiv(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(IntegerValue(pc))
    }

    override def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def irem(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {
        if (throwArithmeticExceptions)
            ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
        else
            ComputedValue(IntegerValue(pc))
    }

    override def ishl(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ishr(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def isub(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iushr(pc: Int, left: DomainValue, right: DomainValue): DomainValue =
        IntegerValue(pc)

    override def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def iinc(pc: Int, left: DomainValue, right: Int): DomainValue =
        IntegerValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //
    override def i2b(pc: Int, value: DomainValue): DomainValue = ByteValue(pc)

    override def i2c(pc: Int, value: DomainValue): DomainValue = CharValue(pc)

    override def i2s(pc: Int, value: DomainValue): DomainValue = ShortValue(pc)

}

