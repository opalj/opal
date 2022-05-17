/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.reflect.ClassTag

import scala.collection.immutable.SortedSet

import org.opalj.collection.SingletonSet
import org.opalj.value.IsIntegerValue
import org.opalj.br._

/**
 * This domain enables the tracking of integer values using sets. The cardinality of
 * the set can be configured to facilitate different needs with regard to the
 * desired precision. Often, a very small cardinality (e.g., between 2 or 8) may be
 * completely sufficient and a large cardinality does not significantly add to the
 * overall precision.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait IntegerSetValues
    extends IntegerValuesDomain
    with ConcreteIntegerValues
    with IntegerRangeValuesFactory {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF INTEGER LIKE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum size of values captured by an Integer set.
     *
     * In many cases a rather (4-16) small number is completely sufficient to
     * capture typical variability.
     *
     * The minimum value is 1; however 2 is the meaningful minimum value.
     */
    def maxCardinalityOfIntegerSets: Int = 8

    require(
        maxCardinalityOfIntegerSets < 127,
        "larger sets are not supported" // we want to avoid overlaps with U7BitSet
    )

    /**
     * Abstracts over all values with computational type `integer`.
     */
    abstract class IntegerLikeValue
        extends TypedValue[CTIntType]
        with IsIntegerValue {
        this: DomainTypedValue[CTIntType] =>

        final override def leastUpperType: Option[CTIntType] = Some(CTIntType)

    }

    /**
     * Represents a specific but unknown integer value.
     *
     * Models the top value of this domain's lattice.
     */
    abstract class AnIntegerValueLike extends IntegerLikeValue {
        this: DomainTypedValue[CTIntType] =>

        final override def lowerBound: Int = Int.MinValue
        final override def upperBound: Int = Int.MaxValue
        final override def constantValue: Option[Int] = None
    }

    /**
     * Represents a set of integer values.
     */
    abstract class IntegerSetLike extends IntegerLikeValue {
        this: DomainTypedValue[CTIntType] =>

        val values: SortedSet[Int]

        final override def lowerBound: Int = values.firstKey
        final override def upperBound: Int = values.lastKey
        final override def constantValue: Option[Int] = {
            if (values.size == 1) Some(values.head) else None
        }
    }

    /**
     * Creates a new [[IntegerSetLike]] value containing the given value.
     */
    def IntegerSet(value: Int): DomainTypedValue[CTIntType] = IntegerSet(SortedSet(value))

    /**
     * Creates a new [[IntegerSetLike]] value using the given set unless the set exceeds the
     * maximum cardinality.
     */
    // IMPROVE Use optimized collection (without (un)boxing).
    def IntegerSet(values: SortedSet[Int]): DomainTypedValue[CTIntType]

    type DomainBaseTypesBasedSet <: BaseTypesBasedSetLike with DomainValue
    val DomainBaseTypesBasedSet: ClassTag[DomainBaseTypesBasedSet]

    trait BaseTypesBasedSetLike extends IntegerLikeValue { this: DomainTypedValue[CTIntType] =>

        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue

        def newInstance: DomainBaseTypesBasedSet
    }

    def U7BitSet(): DomainTypedValue[CTIntType]

    abstract class U7BitSetLike extends BaseTypesBasedSetLike { this: DomainTypedValue[CTIntType] =>

        final override def lowerBound: Int = 0
        final override def upperBound: Int = Byte.MaxValue

        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue = {
            assert(this ne other)
            other match {
                case _: U7BitSetLike  => U7BitSet()
                case _: U15BitSetLike => U15BitSet()
                case _: CharSetLike   => CharValue(pc)
                case _: ByteSetLike   => ByteValue(pc)
                case _: ShortSetLike  => ShortValue(pc)
                case _                => IntegerValue(pc)
            }
        }
    }

    def U15BitSet(): DomainTypedValue[CTIntType]

    abstract class U15BitSetLike extends BaseTypesBasedSetLike { this: DomainTypedValue[CTIntType] =>

        final override def lowerBound: Int = 0
        final override def upperBound: Int = Short.MaxValue

        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue = {
            assert(this ne other)
            other match {
                case _: U7BitSetLike | _: U15BitSetLike => U15BitSet()
                case _: CharSetLike                     => CharValue(pc)
                case _: ByteSetLike                     => ShortValue(pc)
                case _: ShortSetLike                    => ShortValue(pc)
                case _                                  => IntegerValue(pc)
            }
        }
    }

    abstract class CharSetLike extends BaseTypesBasedSetLike { this: DomainTypedValue[CTIntType] =>

        final override def lowerBound: Int = 0 // Char.MinValue
        final override def upperBound: Int = Char.MaxValue

        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue = {
            assert(this ne other)
            other match {
                case _: U7BitSetLike | _: U15BitSetLike | _: CharSetLike => CharValue(pc)
                case _                                                   => IntegerValue(pc)
            }
        }
    }

    abstract class ByteSetLike extends BaseTypesBasedSetLike { this: DomainTypedValue[CTIntType] =>
        final override def lowerBound: Int = Byte.MinValue
        final override def upperBound: Int = Byte.MaxValue
        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue = {
            assert(this ne other)
            other match {
                case _: U7BitSetLike  => ByteValue(pc)
                case _: U15BitSetLike => ShortValue(pc)
                case _: ByteSetLike   => ByteValue(pc)
                case _: ShortSetLike  => ShortValue(pc)
                case _                => IntegerValue(pc)
            }
        }
    }

    abstract class ShortSetLike extends BaseTypesBasedSetLike { this: DomainTypedValue[CTIntType] =>
        final override def lowerBound: Int = Short.MinValue
        final override def upperBound: Int = Short.MaxValue
        def fuse(pc: PC, other: BaseTypesBasedSetLike): domain.DomainValue = {
            assert(this ne other)
            other match {
                case _: U7BitSetLike  => ShortValue(pc)
                case _: U15BitSetLike => ShortValue(pc)
                case _: ByteSetLike   => ShortValue(pc)
                case _: ShortSetLike  => ShortValue(pc)
                case _                => IntegerValue(pc)
            }
        }
    }

    protected[this] def approximateSet(
        origin:     ValueOrigin,
        lowerBound: Int,
        upperBound: Int
    ): DomainTypedValue[CTIntType] = {
        if (lowerBound >= 0) {
            if (upperBound <= Byte.MaxValue) U7BitSet()
            else if (upperBound <= Short.MaxValue) U15BitSet()
            else if (upperBound <= Char.MaxValue) CharValue(origin)
            else IntegerValue(origin = origin)
        } else if (lowerBound >= Byte.MinValue && upperBound <= Byte.MaxValue) ByteValue(origin)
        else if (lowerBound >= Short.MinValue && upperBound <= Short.MaxValue) ShortValue(origin)
        else IntegerValue(origin = origin)
    }

    /**
     * Creates a new IntegerSet value using the given set unless the set exceeds the
     * maximum cardinality.
     */
    def IntegerSet(origin: ValueOrigin, values: SortedSet[Int]): DomainTypedValue[CTIntType] = {
        if (values.size <= maxCardinalityOfIntegerSets)
            IntegerSet(values)
        else {
            val lb = values.firstKey
            val ub = values.lastKey
            approximateSet(origin, lb, ub)
        }
    }

    /**
     * Creates a new IntegerSet value containing all values
     * within the given bounds of the IntegerRange, as long as they don't
     * exceed `maxCardinalityOfIntegerSets`.
     */
    def IntegerRange(
        origin:     ValueOrigin,
        lowerBound: Int, upperBound: Int
    ): DomainTypedValue[CTIntType] = {
        assert(lowerBound <= upperBound)

        if (upperBound.toLong - lowerBound.toLong <= maxCardinalityOfIntegerSets)
            IntegerSet(SortedSet[Int](lowerBound to upperBound: _*))
        else
            approximateSet(origin, lowerBound, upperBound)
    }

    /**
     * Extractor for `IntegerSet` values.
     */
    object IntegerSetLike { def unapply(v: IntegerSetLike): Some[SortedSet[Int]] = Some(v.values) }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO  INTEGER VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTIONS ABOUT VALUES
    //

    @inline final override def intValue[T](
        value: DomainValue
    )(
        f: Int => T
    )(
        orElse: => T
    ): T = {
        value match {
            case IntegerSetLike(values) if values.size == 1 => f(values.head)
            case _                                          => orElse
        }
    }

    @inline final override def intValueOption(value: DomainValue): Option[Int] = {
        value match {
            case IntegerSetLike(values) if values.size == 1 => Some(values.head)
            case _                                          => None
        }
    }

    @inline protected final def intValues[T](
        value1: DomainValue, value2: DomainValue
    )(
        f: (Int, Int) => T
    )(
        orElse: => T
    ): T = {
        intValue(value1) {
            v1 => intValue(value2) { v2 => f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    override def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = {
        if (value1 eq value2)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes;

        (value1, value2) match {
            case (IntegerSetLike(v1s), IntegerSetLike(v2s)) =>
                if (v1s.size == 1 && v2s.size == 1)
                    Answer(v1s.head == v2s.head)
                else if (v1s.intersect(v2s).isEmpty)
                    No
                else
                    Unknown

            case (IntegerSetLike(vs), DomainBaseTypesBasedSet(btbs)) =>
                if (vs forall { v => v < btbs.lowerBound || v > btbs.upperBound })
                    No
                else
                    Unknown

            case (DomainBaseTypesBasedSet(btbs), IntegerSetLike(vs)) =>
                if (vs forall { v => v < btbs.lowerBound || v > btbs.upperBound })
                    No
                else
                    Unknown

            case _ =>
                Unknown
        }
    }

    override def intIsSomeValueInRange(
        pc:         PC,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return Yes;

        value match {
            case IntegerSetLike(values) =>
                Answer(
                    values.lastKey >= lowerBound && values.firstKey <= upperBound &&
                        values.exists(value => value >= lowerBound && value <= upperBound)
                )

            case DomainBaseTypesBasedSet(value) =>
                Answer(lowerBound <= value.upperBound && upperBound >= value.lowerBound)

            case _ => Unknown
        }
    }

    override def intIsSomeValueNotInRange(
        pc:         PC,
        value:      DomainValue,
        lowerBound: Int,
        upperBound: Int
    ): Answer = {
        if (lowerBound == Int.MinValue && upperBound == Int.MaxValue)
            return No;

        value match {
            case IntegerSetLike(values) =>
                Answer(values.firstKey < lowerBound || values.lastKey > upperBound)

            case DomainBaseTypesBasedSet(value) =>
                Answer(value.lowerBound < lowerBound || value.upperBound > upperBound)

            case _ => Unknown
        }
    }

    override def intIsLessThan(pc: PC, left: DomainValue, right: DomainValue): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return No;

        right match {
            case IntegerSetLike(rightValues) =>
                if (rightValues.lastKey == Int.MinValue)
                    // the right value is the smallest possible value...
                    No
                else left match {
                    case IntegerSetLike(leftValues) =>
                        if (leftValues.lastKey < rightValues.firstKey)
                            Yes
                        else if (leftValues.firstKey >= rightValues.lastKey ||
                            ( /*"for point sets":*/
                                leftValues.size == 1 && rightValues.size == 1 &&
                                leftValues.head == rightValues.head
                            ))
                            No
                        else
                            Unknown
                    case DomainBaseTypesBasedSet(left) =>
                        if (left.upperBound < rightValues.firstKey)
                            Yes
                        else if (rightValues.lastKey <= left.lowerBound)
                            No

                        else
                            Unknown

                    case _ =>
                        Unknown
                }

            case DomainBaseTypesBasedSet(right) =>
                left match {
                    case IntegerSetLike(leftValues) =>
                        if (leftValues.lastKey < right.lowerBound)
                            Yes
                        else if (leftValues.firstKey >= right.upperBound)
                            No
                        else
                            Unknown

                    case _ => Unknown
                }

            case _ => Unknown
        }
    }

    override def intIsLessThanOrEqualTo(
        pc:    PC,
        left:  DomainValue,
        right: DomainValue
    ): Answer = {
        if (left eq right)
            // this handles the case that the two values (even if the concrete value
            // is not known; i.e., AnIntegerValue) are actually exactly the same value
            return Yes;

        right match {
            case IntegerSetLike(rightValues) =>
                if (rightValues.firstKey == Int.MaxValue)
                    Yes
                else left match {
                    case IntegerSetLike(leftValues) =>
                        if (leftValues.lastKey <= rightValues.firstKey)
                            Yes
                        else if (leftValues.firstKey > rightValues.lastKey)
                            No
                        else
                            Unknown

                    case DomainBaseTypesBasedSet(left) =>
                        if (left.upperBound <= rightValues.firstKey)
                            Yes
                        else if (left.lowerBound > rightValues.lastKey)
                            No
                        else
                            Unknown

                    case _ => Unknown
                }

            case DomainBaseTypesBasedSet(right) =>
                left match {
                    case IntegerSetLike(leftValues) =>
                        if (leftValues.lastKey <= right.lowerBound)
                            Yes
                        else if (leftValues.firstKey > right.upperBound)
                            No
                        else
                            Unknown

                    case _ => Unknown
                }

            case _ =>
                left match {
                    case IntegerSetLike(leftValues) =>
                        if (leftValues.lastKey == Int.MinValue)
                            Yes
                        else
                            Unknown

                    case _ =>
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
        pc:       PC,
        theValue: Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        value match {
            case IntegerSetLike(values) if values.size == 1 && values.head == theValue =>
                (operands, locals)
            case _ =>
                updateMemoryLayout(value, IntegerSet(theValue), operands, locals)
        }
    }

    override def intEstablishAreEqual(
        pc:       PC,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        if (value1 eq value2)
            // this basically handles the case that both are "AnIntegerValue"
            return (operands, locals);

        value1 match {
            case IntegerSetLike(leftValues) =>
                value2 match {

                    case IntegerSetLike(rightValues) =>
                        val newValue = IntegerSet(pc, leftValues.intersect(rightValues))
                        val (operands1, locals1) =
                            updateMemoryLayout(value1, newValue, operands, locals)
                        updateMemoryLayout(value2, newValue, operands1, locals1)

                    case DomainBaseTypesBasedSet(value2) =>
                        // all matching values from value1
                        val filtered = leftValues.filter { v =>
                            v >= value2.lowerBound && v <= value2.upperBound
                        }
                        val newValue =
                            if (filtered.size == leftValues.size) value1 else IntegerSet(filtered)
                        val (newOperands, newLocals) =
                            updateMemoryLayout(oldValue = value2, newValue, operands, locals)

                        // If there were value removed from value 2, update it too
                        if (newValue ne value1)
                            updateMemoryLayout(oldValue = value1, newValue, newOperands, newLocals)
                        else
                            (newOperands, newLocals)

                    case _ =>
                        // value1 is unchanged (an IntegerSet value is always more precise)
                        updateMemoryLayout(oldValue = value2, value1, operands, locals)
                }
            case DomainBaseTypesBasedSet(value1) =>
                value2 match {
                    case DomainBaseTypesBasedSet(value2) =>
                        val newValue = value1.fuse(pc, value2)
                        val (os1, ls1) = updateMemoryLayout(value1, newValue, operands, locals)
                        updateMemoryLayout(value2, newValue, os1, ls1)

                    case IntegerSetLike(rightValues) =>
                        // all matching values from value2
                        val filtered = rightValues.filter { v =>
                            v >= value1.lowerBound && v <= value1.upperBound
                        }
                        val newValue =
                            if (filtered.size == rightValues.size) value2 else IntegerSet(filtered)
                        val (newOperands, newLocals) =
                            updateMemoryLayout(oldValue = value1, newValue, operands, locals)

                        // If there were value removed from value 2, update it too
                        if (newValue ne value2)
                            updateMemoryLayout(oldValue = value2, newValue, newOperands, newLocals)
                        else
                            (newOperands, newLocals)

                    case _ /*AnIntegerValue*/ =>
                        // value1 is unchanged (an IntegerSet value is always more precise)
                        updateMemoryLayout(oldValue = value2, value1, operands, locals)
                }

            case _ /*AnIntegerValue*/ =>
                // value2 is unchanged
                updateMemoryLayout(oldValue = value1, value2, operands, locals)
        }
    }

    override def intEstablishAreNotEqual(
        pc:       PC,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        assert(value1 ne value2, "the values are definitively equal; impossible refinement \"!=\"")

        intValue(value1) { v1 =>
            value2 match {
                case IntegerSetLike(values) =>
                    updateMemoryLayout(value2, IntegerSet(pc, values - v1), operands, locals)
                case _ =>
                    (operands, locals)
            }
        } {
            intValue(value2) { v2 =>
                value1 match {
                    case IntegerSetLike(values) =>
                        updateMemoryLayout(value1, IntegerSet(pc, values - v2), operands, locals)
                    case _ =>
                        (operands, locals)
                }
            } {
                (operands, locals)
            }
        }
    }

    override def intEstablishIsLessThan(
        pc:       PC,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        //        println("intEstablishIsLessThan"+System.identityHashCode(left).toHexString + left+" .... "+System.identityHashCode(right).toHexString + right)
        //        println(locals.map(v => System.identityHashCode(v).toHexString+"."+v).mkString("", ";   ", ""))

        assert(left ne right, "the values are definitively equal; impossible refinement \"<\"")

        val result = (left, right) match {
            case (IntegerSetLike(ls), IntegerSetLike(rs)) =>
                val rsMax = rs.lastKey
                val newLs = ls.filter(_ < rsMax)
                val (operands1, locals1) =
                    if (newLs.size != ls.size) {
                        if (newLs.size == 0) {
                            val message = s"constraint: $left < $right led to impossible value"
                            throw new IllegalStateException(message)
                        }
                        updateMemoryLayout(left, IntegerSet(pc, newLs), operands, locals)
                    } else {
                        (operands, locals)
                    }

                val lsMin = ls.firstKey
                val newRs = rs.filter(_ > lsMin)
                if (newRs.size != rs.size) {
                    updateMemoryLayout(right, IntegerSet(pc, newRs), operands1, locals1)
                } else {
                    (operands1, locals1)
                }

            case (IntegerSetLike(ls), DomainBaseTypesBasedSet(right)) =>
                val newLs = ls.filter(_ < right.upperBound)
                updateMemoryLayout(left, IntegerSet(pc, newLs), operands, locals)

            case (DomainBaseTypesBasedSet(left), IntegerSetLike(rs)) =>
                val newIntValue = approximateSet(pc, left.lowerBound, rs.lastKey - 1)
                updateMemoryLayout(left, newIntValue, operands, locals)

            case (DomainBaseTypesBasedSet(left), DomainBaseTypesBasedSet(right)) =>
                val newIntValue = approximateSet(pc, left.lowerBound, right.upperBound)
                updateMemoryLayout(left, newIntValue, operands, locals)

            case (_, DomainBaseTypesBasedSet(right)) =>
                updateMemoryLayout(left, right.newInstance, operands, locals)

            case (_, IntegerSetLike(rs)) =>
                val newIntValue = approximateSet(pc, rs.firstKey - 1, rs.lastKey - 1)
                updateMemoryLayout(left, newIntValue, operands, locals)

            case _ =>
                (operands, locals)
        }
        //        println(locals.map(v => System.identityHashCode(v).toHexString+"."+v).mkString("", ";   ", ""))
        result
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc:       PC,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {
        //        println("intEstablishIsLessThanOrEqualTo"+System.identityHashCode(left).toHexString + left+" .... "+System.identityHashCode(right).toHexString + right)
        //        println(locals.map(v => System.identityHashCode(v).toHexString+"."+v).mkString("", ";   ", ""))

        val result =
            (left, right) match {
                case (IntegerSetLike(ls), IntegerSetLike(rs)) =>
                    val rsMax = rs.lastKey
                    val newLs = ls.filter(_ <= rsMax)
                    val (operands1, locals1) =
                        if (newLs.size != ls.size) {
                            updateMemoryLayout(left, IntegerSet(pc, newLs), operands, locals)
                        } else {
                            (operands, locals)
                        }

                    val lsMin = ls.firstKey
                    val newRs = rs.filter(_ >= lsMin)
                    val newMemoryLayout =
                        if (newRs.size != rs.size) {
                            updateMemoryLayout(right, IntegerSet(pc, newRs), operands1, locals1)
                        } else {
                            (operands1, locals1)
                        }
                    newMemoryLayout

                case (IntegerSetLike(ls), DomainBaseTypesBasedSet(right)) =>
                    val newLs = ls.filter(_ <= right.upperBound)
                    updateMemoryLayout(left, IntegerSet(pc, newLs), operands, locals)

                case (DomainBaseTypesBasedSet(left), IntegerSetLike(rs)) =>
                    val newDomainValue = approximateSet(pc, left.lowerBound, rs.lastKey)
                    updateMemoryLayout(left, newDomainValue, operands, locals)

                case (DomainBaseTypesBasedSet(left), DomainBaseTypesBasedSet(right)) =>
                    val newIntValue = approximateSet(pc, left.lowerBound, right.upperBound)
                    updateMemoryLayout(left, newIntValue, operands, locals)

                case (_, DomainBaseTypesBasedSet(right)) =>
                    updateMemoryLayout(left, right.newInstance, operands, locals)

                case (_, IntegerSetLike(rs)) =>
                    val newIntValue = approximateSet(pc, rs.firstKey, rs.lastKey)
                    updateMemoryLayout(left, newIntValue, operands, locals)

                case _ =>
                    (operands, locals)
            }
        //        println(locals.map(v => System.identityHashCode(v).toHexString+"."+v).mkString("", ";   ", ""))
        result
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    /*override*/ def ineg(pc: PC, value: DomainValue): DomainValue = {
        value match {
            case IntegerSetLike(SingletonSet(Int.MinValue)) => value
            case IntegerSetLike(values)                     => IntegerSet(pc, values.map(-_))
            case _                                          => IntegerValue(origin = pc)
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    /*override*/ def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                // {1,2,3}+{0,1} => {1,2,3,4}
                val results =
                    for (leftValue <- leftValues; rightValue <- rightValues) yield {
                        leftValue + rightValue
                    }
                IntegerSet(pc, results)
            case _ =>
                IntegerValue(origin = pc)
        }
    }

    /*override*/ def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue = {
        value match {
            case IntegerSetLike(values) => IntegerSet(values.map(_ + increment))
            case _                      => IntegerValue(origin = pc)
        }
    }

    /*override*/ def isub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                val results =
                    for (leftValue <- leftValues; rightValue <- rightValues) yield {
                        leftValue - rightValue
                    }
                IntegerSet(pc, results)
            case _ =>
                IntegerValue(origin = pc)
        }
    }

    /*override*/ def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (IntegerSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == 0)
                    value1
                else if (leftValues.size == 1 && leftValues.head == 1)
                    value2
                else value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        val results =
                            for (leftValue <- leftValues; rightValue <- rightValues) yield {
                                leftValue * rightValue
                            }
                        IntegerSet(pc, results)

                    case _ =>
                        IntegerValue(origin = pc)

                }
            case _ =>
                value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == 0)
                            value2
                        else if (rightValues.size == 1 && rightValues.head == 1)
                            value1
                        else
                            IntegerValue(origin = pc)
                    case _ =>
                        IntegerValue(origin = pc)
                }
        }
    }

    protected[this] def createIntegerValueOrArithmeticException(
        pc:        PC,
        exception: Boolean,
        results:   SortedSet[Int]
    ): IntegerValueOrArithmeticException = {

        assert(exception || results.nonEmpty)

        if (results.nonEmpty) {
            val newValue = IntegerSet(pc, results)
            if (exception)
                ComputedValueOrException(newValue, VMArithmeticException(pc))
            else
                ComputedValue(newValue)
        } else {
            ThrowsException(VMArithmeticException(pc))
        }
    }

    /*override*/ def idiv(
        pc:          PC,
        numerator:   DomainValue,
        denominator: DomainValue
    ): IntegerValueOrArithmeticException = {
        (numerator, denominator) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                var results: SortedSet[Int] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue <- leftValues; rightValue <- rightValues) {
                    if (rightValue == 0)
                        exception = true
                    else
                        results += (leftValue / rightValue)
                }
                createIntegerValueOrArithmeticException(pc, exception, results)

            case (_, IntegerSetLike(rightValues)) =>
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(VMArithmeticException(pc))
                    else
                        ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
                } else
                    ComputedValue(IntegerValue(origin = pc))

            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(origin = pc), VMArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(origin = pc))
        }
    }

    /*override*/ def irem(
        pc:    PC,
        left:  DomainValue,
        right: DomainValue
    ): IntegerValueOrArithmeticException = {

        (left, right) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                var results: SortedSet[Int] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue <- leftValues; rightValue <- rightValues) {
                    if (rightValue == 0)
                        exception = true
                    else
                        results += (leftValue % rightValue)
                }
                createIntegerValueOrArithmeticException(pc, exception, results)

            case (_, IntegerSetLike(rightValues)) =>
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(VMArithmeticException(pc))
                    else
                        ComputedValueOrException(IntegerValue(pc), VMArithmeticException(pc))
                } else
                    ComputedValue(IntegerValue(origin = pc))

            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(IntegerValue(origin = pc), VMArithmeticException(pc))
                else
                    ComputedValue(IntegerValue(origin = pc))
        }
    }

    /*override*/ def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (IntegerSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == -1)
                    value2
                else if (leftValues.size == 1 && leftValues.head == 0)
                    value1
                else value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        val results =
                            for (leftValue <- leftValues; rightValue <- rightValues) yield {
                                leftValue & rightValue
                            }
                        IntegerSet(pc, results)

                    case _ =>
                        IntegerValue(origin = pc)

                }
            case _ =>
                value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == -1)
                            value1
                        else if (rightValues.size == 1 && rightValues.head == 0)
                            value2
                        else
                            IntegerValue(origin = pc)
                    case _ =>
                        IntegerValue(origin = pc)
                }
        }
    }

    /*override*/ def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (IntegerSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == -1)
                    value1
                else if (leftValues.size == 1 && leftValues.head == 0)
                    value2
                else value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        val results =
                            for (leftValue <- leftValues; rightValue <- rightValues) yield {
                                leftValue | rightValue
                            }
                        IntegerSet(pc, results)

                    case _ =>
                        IntegerValue(origin = pc)
                }
            case _ =>
                value2 match {
                    case (IntegerSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == -1)
                            value2
                        else if (rightValues.size == 1 && rightValues.head == 0)
                            value1
                        else
                            IntegerValue(origin = pc)

                    case _ =>
                        IntegerValue(origin = pc)
                }
        }
    }

    /*override*/ def ishl(pc: PC, value: DomainValue, shift: DomainValue): DomainValue = {
        (value, shift) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                val results = for (leftValue <- leftValues; rightValue <- rightValues) yield {
                    leftValue << rightValue
                }
                IntegerSet(pc, results)

            case _ =>
                IntegerValue(origin = pc)
        }
    }

    /*override*/ def ishr(pc: PC, value: DomainValue, shift: DomainValue): DomainValue = {
        (value, shift) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                val results = for (leftValue <- leftValues; rightValue <- rightValues) yield {
                    leftValue >> rightValue
                }
                IntegerSet(pc, results)

            case _ =>
                IntegerValue(origin = pc)
        }
    }

    /*override*/ def iushr(pc: PC, value: DomainValue, shift: DomainValue): DomainValue = {
        (value, shift) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                val results =
                    for (leftValue <- leftValues; rightValue <- rightValues) yield {
                        leftValue >>> rightValue
                    }
                IntegerSet(pc, results)

            case _ =>
                IntegerValue(origin = pc)
        }
    }

    /*override*/ def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (IntegerSetLike(leftValues), IntegerSetLike(rightValues)) =>
                val results =
                    for (leftValue <- leftValues; rightValue <- rightValues) yield {
                        leftValue ^ rightValue
                    }
                IntegerSet(pc, results)

            case _ =>
                IntegerValue(origin = pc)
        }
    }

    //
    // TYPE CONVERSION INSTRUCTIONS
    //

    /*override*/ def i2b(pc: PC, value: DomainValue): DomainTypedValue[CTIntType] =
        value match {
            case IntegerSetLike(values) => IntegerSet(pc, values.map(_.toByte.toInt))
            case _                      => ByteValue(origin = pc)
        }

    /*override*/ def i2c(pc: PC, value: DomainValue): DomainTypedValue[CTIntType] =
        value match {
            case IntegerSetLike(values) => IntegerSet(pc, values.map(_.toChar.toInt))
            case _                      => CharValue(origin = pc)
        }

    /*override*/ def i2s(pc: PC, value: DomainValue): DomainTypedValue[CTIntType] =
        value match {
            case IntegerSetLike(values) => IntegerSet(pc, values.map(_.toShort.toInt))
            case _                      => ShortValue(origin = pc)
        }

}
