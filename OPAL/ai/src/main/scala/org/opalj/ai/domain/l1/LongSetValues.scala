/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.collection.immutable.SortedSet

import org.opalj.value.IsLongValue
import org.opalj.br._

/**
 * This domain enables the tracking of long values using sets. The cardinality of
 * the set can be configured to facilitate different needs with regard to the
 * desired precision.
 *
 * This domain supports constraint propagation as every two values that are not
 * guaranteed to have the same value at runtime are always represented using a
 * unique instance of [[LongValue]].
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait LongSetValues extends LongValuesDomain with ConcreteLongValues {
    domain: IntegerRangeValuesFactory with CorrelationalDomain with Configuration with ExceptionsFactory =>

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum number of values captured by a set of Long values.
     *
     * Often, a very small cardinality (e.g., 2 or 8) may be
     * completely sufficient and a large cardinality does not significantly add to the
     * overall precision.
     */
    protected def maxCardinalityOfLongSets: Int = 5

    /**
     * Abstracts over all values with computational type `long`.
     */
    sealed trait LongValue extends TypedValue[LongType] with IsLongValue {
        this: DomainTypedValue[LongType] =>

        final override def leastUpperType: Option[LongType] = Some(LongType)

    }

    /**
     * Represents a specific, but unknown long value. Two values that are both unknown
     * and which may be different at runtime are always represented using two different
     * instances of `ALongValue`.
     *
     * Models the top value of this domain's lattice.
     */
    trait ALongValueLike extends LongValue { this: DomainTypedValue[LongType] => }

    /**
     * Represents a set of long values.
     */
    abstract class LongSetLike extends LongValue { this: DomainTypedValue[LongType] =>

        val values: SortedSet[Long]

    }

    /**
     * Factory method to create a new [[LongSetLike]] value containing just the given value.
     */
    def LongSet(value: Long): DomainTypedValue[LongType] = LongSet(SortedSet(value))

    /**
     * Factory method to create a new [[LongSetLike]] value using the given set.
     */
    def LongSet(values: SortedSet[Long]): DomainTypedValue[LongType]

    /**
     * Extractor for `LongSet` values.
     */
    object LongSetLike {

        def unapply(v: LongSetLike): Option[SortedSet[Long]] = Some(v.values)

    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTIONS ABOUT VALUES
    //

    @inline final override def longValue[T](
        value: DomainValue
    )(
        f: Long => T
    )(
        orElse: => T
    ): T =
        value match {
            case LongSetLike(values) if values.size == 1 => f(values.head)
            case _                                       => orElse
        }

    @inline final override def longValueOption(value: DomainValue): Option[Long] =
        value match {
            case LongSetLike(values) if values.size == 1 => Some(values.head)
            case _                                       => None
        }

    @inline protected final def withLongValuesOrElse[T](
        value1: DomainValue, value2: DomainValue
    )(
        f: (Long, Long) => T
    )(
        orElse: => T
    ): T = {
        longValue(value1) {
            v1 => longValue(value2) { v2 => f(v1, v2) } { orElse }
        } {
            orElse
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
    /*override*/ def lneg(pc: PC, value: DomainValue): DomainValue =
        value match {
            case LongSetLike(values) => LongSet(values.map(v => -v))
            case _                   => LongValue(origin = pc)
        }

    //
    // RELATIONAL OPERATORS
    //

    /*override*/ def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        left match {
            case (LongSetLike(leftValues)) =>
                right match {
                    case (LongSetLike(rightValues)) =>
                        val lb =
                            if (leftValues.head < rightValues.last)
                                -1
                            else if (leftValues.head == rightValues.last)
                                0
                            else
                                1
                        val ub =
                            if (leftValues.last > rightValues.head)
                                1
                            else if (leftValues.last == rightValues.head)
                                0
                            else
                                -1
                        IntegerRange(pc, lb, ub)
                    case _ =>
                        if (leftValues.size == 1) {
                            if (leftValues.head == Long.MinValue)
                                IntegerRange(pc, -1, 0)
                            else if (leftValues.head == Long.MaxValue)
                                IntegerRange(pc, 0, 1)
                            else
                                IntegerRange(pc, -1, 1)
                        } else
                            IntegerRange(pc, -1, 1)
                }
            case _ =>
                right match {
                    case (LongSetLike(rightValues)) =>
                        if (rightValues.size == 1) {
                            if (rightValues.head == Long.MinValue)
                                IntegerRange(pc, 0, 1)
                            else if (rightValues.head == Long.MaxValue)
                                IntegerRange(pc, -1, 0)
                            else
                                IntegerRange(pc, -1, 1)
                        } else
                            IntegerRange(pc, -1, 1)
                    case _ =>
                        IntegerRange(pc, -1, 1)
                }
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    /*override*/ def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSetLike(leftValues), LongSetLike(rightValues)) =>
                val results = for (l <- leftValues; r <- rightValues) yield l + r
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(origin = pc)
            case _ =>
                LongValue(origin = pc)
        }
    }

    /*override*/ def lsub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (LongSetLike(leftValues), LongSetLike(rightValues)) =>
                val results = for (l <- leftValues; r <- rightValues) yield l - r
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(origin = pc)
            case _ =>
                LongValue(origin = pc)
        }
    }

    /*override*/ def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (LongSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == 0L)
                    value1
                else if (leftValues.size == 1 && leftValues.head == 1L)
                    value2
                else value2 match {
                    case (LongSetLike(rightValues)) =>
                        val results = for (l <- leftValues; r <- rightValues) yield l * r

                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
            case _ =>
                value2 match {
                    case (LongSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == 0L)
                            value2
                        else if (rightValues.size == 1 && rightValues.head == 1L)
                            value1
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
        }
    }

    protected[this] def createLongValueOrArithmeticException(
        pc:        PC,
        exception: Boolean,
        results:   SortedSet[Long]
    ): LongValueOrArithmeticException = {

        val hasResults = results.nonEmpty
        assert(exception || hasResults)

        if (hasResults) {
            if (results.size <= maxCardinalityOfLongSets) {
                if (exception)
                    ComputedValueOrException(LongSet(results), VMArithmeticException(pc))
                else
                    ComputedValue(LongSet(results))
            } else {
                if (exception)
                    ComputedValueOrException(
                        LongValue(origin = pc),
                        VMArithmeticException(pc)
                    )
                else
                    ComputedValue(LongValue(origin = pc))
            }
        } else {
            ThrowsException(VMArithmeticException(pc))
        }
    }

    /*override*/ def ldiv(
        pc:          PC,
        numerator:   DomainValue,
        denominator: DomainValue
    ): LongValueOrArithmeticException = {
        (numerator, denominator) match {
            case (LongSetLike(leftValues), LongSetLike(rightValues)) =>
                var results: SortedSet[Long] = SortedSet.empty
                var exception: Boolean = false
                for (l <- leftValues; r <- rightValues) {
                    if (r == 0L)
                        exception = true
                    else
                        results += (l / r)
                }
                createLongValueOrArithmeticException(pc, exception, results)

            case (_, LongSetLike(rightValues)) =>
                if (rightValues contains (0L)) {
                    if (rightValues.size == 1)
                        ThrowsException(VMArithmeticException(pc))
                    else
                        ComputedValueOrException(
                            LongValue(origin = pc),
                            VMArithmeticException(pc)
                        )
                } else
                    ComputedValue(LongValue(origin = pc))

            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(
                        LongValue(origin = pc),
                        VMArithmeticException(pc)
                    )
                else
                    ComputedValue(LongValue(origin = pc))
        }
    }

    /*override*/ def lrem(
        pc:    PC,
        left:  DomainValue,
        right: DomainValue
    ): LongValueOrArithmeticException = {

        (left, right) match {
            case (LongSetLike(leftValues), LongSetLike(rightValues)) =>
                var results: SortedSet[Long] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue <- leftValues; rightValue <- rightValues) {
                    if (rightValue == 0L)
                        exception = true
                    else
                        results += (leftValue % rightValue)
                }
                createLongValueOrArithmeticException(pc, exception, results)

            case (_, LongSetLike(rightValues)) =>
                if (rightValues contains (0L)) {
                    if (rightValues.size == 1)
                        ThrowsException(VMArithmeticException(pc))
                    else
                        ComputedValueOrException(
                            LongValue(origin = pc),
                            VMArithmeticException(pc)
                        )
                } else
                    ComputedValue(LongValue(origin = pc))

            case _ =>
                if (throwArithmeticExceptions)
                    ComputedValueOrException(
                        LongValue(origin = pc),
                        VMArithmeticException(pc)
                    )
                else
                    ComputedValue(LongValue(origin = pc))
        }
    }

    /*override*/ def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (LongSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == -1L)
                    value2
                else if (leftValues.size == 1 && leftValues.head == 0L)
                    value1
                else value2 match {
                    case (LongSetLike(rightValues)) =>
                        val results = for (l <- leftValues; r <- rightValues) yield l & r
                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
            case _ =>
                value2 match {
                    case (LongSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == -1L)
                            value1
                        else if (rightValues.size == 1 && rightValues.head == 0L)
                            value2
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
        }
    }

    /*override*/ def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        value1 match {
            case (LongSetLike(leftValues)) =>
                if (leftValues.size == 1 && leftValues.head == -1L)
                    value1
                else if (leftValues.size == 1 && leftValues.head == 0L)
                    value2
                else value2 match {
                    case (LongSetLike(rightValues)) =>
                        val results = for (l <- leftValues; r <- rightValues) yield l | r
                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
            case _ =>
                value2 match {
                    case (LongSetLike(rightValues)) =>
                        if (rightValues.size == 1 && rightValues.head == -1L)
                            value2
                        else if (rightValues.size == 1 && rightValues.head == 0L)
                            value1
                        else
                            LongValue(origin = pc)
                    case _ =>
                        LongValue(origin = pc)
                }
        }
    }

    /*override*/ def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSetLike(leftValues), LongSetLike(rightValues)) =>
                val results = for (l <- leftValues; r <- rightValues) yield l ^ r
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(origin = pc)

            case _ =>
                LongValue(origin = pc)
        }
    }

}

