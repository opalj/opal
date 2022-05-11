/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.Type
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ShortType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.FloatType
import org.opalj.br.DoubleType
import org.opalj.br.FieldType
import org.opalj.br.VoidType

/**
 * Defines additional, generally useful factory methods to create `DomainValue`s.
 *
 * @author Michael Eichberg
 */
trait TypedValuesFactory {
    domain: ValuesDomain with ReferenceValuesFactory with PrimitiveValuesFactory =>

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no precise value or source information.
     * However, the value is guaranteed to be `null` or properly initialized.
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * The framework uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by the framework.
     */
    def TypedValue(origin: ValueOrigin, valueType: Type): DomainValue =
        (valueType.id: @scala.annotation.switch) match {
            case BooleanType.id => BooleanValue(origin)
            case ByteType.id    => ByteValue(origin)
            case ShortType.id   => ShortValue(origin)
            case CharType.id    => CharValue(origin)
            case IntegerType.id => IntegerValue(origin)
            case FloatType.id   => FloatValue(origin)
            case LongType.id    => LongValue(origin)
            case DoubleType.id  => DoubleValue(origin)
            case VoidType.id    => throw DomainException("cannot create void typed value")
            case _              => ReferenceValue(origin, valueType.asReferenceType)
        }

    /**
     * Creates a `DomainValue` that represents a value with the given type
     * and which is initialized using the JVM's default value for that type.
     * E.g., for `IntegerValue`s the value is set to `0`. In case of a
     * `ReferenceType` the value is the [[ReferenceValuesFactory#NullValue]].
     */
    final def DefaultValue(origin: ValueOrigin, theType: FieldType): DomainValue = {
        (theType.id: @scala.annotation.switch) match {
            case BooleanType.id => BooleanValue(origin, false)
            case ByteType.id    => ByteValue(origin, 0)
            case CharType.id    => CharValue(origin, 0)
            case ShortType.id   => ShortValue(origin, 0)
            case IntegerType.id => IntegerValue(origin, 0)
            case FloatType.id   => FloatValue(origin, 0.0f)
            case LongType.id    => LongValue(origin, 0L)
            case DoubleType.id  => DoubleValue(origin, 0.0d)
            case VoidType.id    => throw DomainException("cannot create void typed value")
            case _              => NullValue(origin)
        }
    }
}
