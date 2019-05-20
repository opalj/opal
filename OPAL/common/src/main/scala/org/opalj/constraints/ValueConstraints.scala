/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package constraints

/**
 * Constraints related to values.
 *
 * @author Michael Eichberg
 */
trait ValueConstraint

trait NumericValueConstraint extends ValueConstraint

/**
 * States that the underlying, unknown value is an element of the specified range of values.
 */
case class NumericRange[@specialized(Int, Long, Float, Double, Boolean) T <: AnyVal](
        lowerBound: T,
        upperBound: T
) extends NumericValueConstraint

object NumericRange {
    final val PositiveIntegerValue: NumericRange[Int] = NumericRange(0, Int.MaxValue)
}

/**
 * States that the underlying, unknown value is an element of the specified set.
 */
case class NumericSet[@specialized(Int, Long, Float, Double, Boolean) T <: AnyVal](
        values: Set[T]
) extends NumericValueConstraint

trait ReferenceValueConstraint extends ValueConstraint

/**
 * The respective value is guaranteed to be null.
 */
case object NullValue extends ReferenceValueConstraint

/**
 * The respective value is guaranteed to be null.
 */
case object NonNullValue extends ReferenceValueConstraint

/**
 * Models constraints related to an object (graph).
 */
case class ObjectConstraint(
        objectReference:  ReferenceValueConstraint,
        fieldConstraints: Map[String, ValueConstraint]
) extends ReferenceValueConstraint

/**
 * @param arrayValues Constraint which is satisified by all elements of the array (on first access).
 */
case class ArrayConstraint(
        arrayReference: ReferenceValueConstraint,
        arraySize:      NumericValueConstraint   = NumericRange.PositiveIntegerValue,
        arrayValues:    ValueConstraint
) extends ReferenceValueConstraint

object ArrayConstraint {
    final val NonNullArrayOfNonNullValues = {
        ArrayConstraint(arrayReference = NonNullValue, arrayValues = NonNullValue)
    }
}

/**
 * States that the underlying, but unknown value is unconstrained except of those constraints
 * given by the value's known type.
 */
case object ConstraintByType extends ValueConstraint
    with NumericValueConstraint
    with ReferenceValueConstraint
