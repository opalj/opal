/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package constraints

/**
 * Constraints related to values.
 *
 * @author Michael Eichberg
 */
trait ValueConstraint

trait NumericValueConstraint extends ValueConstraint

/**
 * States that the underlying, but unknown value is an element of the specified range of values.
 */
case class NumericRange[@specialized(Int, Long, Float, Double, Boolean) T <: AnyVal](
        lowerBound: T,
        upperBound: T
) extends NumericValueConstraint

object NumericRange {
    final val PositiveIntegerValue: NumericRange[Int] = NumericRange(0, Int.MaxValue)
}

/**
 * States that the underlying, but unknown value is an element of the specified set.
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
