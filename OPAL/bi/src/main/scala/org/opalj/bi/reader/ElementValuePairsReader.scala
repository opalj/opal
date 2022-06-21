/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for an annotation's element-value pairs.
 */
trait ElementValuePairsReader extends AnnotationsAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ElementValue <: AnyRef
    implicit val elementValueType: ClassTag[ElementValue] // TODO: Replace in Scala 3 by `type ElementValue : ClassTag`
    type ElementValues = ArraySeq[ElementValue]

    type ElementValuePair <: AnyRef
    implicit val elementValuePairType: ClassTag[ElementValuePair] // TODO: Replace in Scala 3 by `type ExceptionValuePair : ClassTag`
    type ElementValuePairs = ArraySeq[ElementValuePair]

    def ElementValuePair(
        constant_pool:      Constant_Pool,
        element_name_index: Constant_Pool_Index,
        element_value:      ElementValue
    ): ElementValuePair

    def ByteValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def CharValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def DoubleValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def FloatValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def IntValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def LongValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def ShortValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def BooleanValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def StringValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def ClassValue(
        constant_pool:     Constant_Pool,
        const_value_index: Constant_Pool_Index
    ): ElementValue

    def EnumValue(
        constant_pool:    Constant_Pool,
        type_name_index:  Constant_Pool_Index,
        const_name_index: Constant_Pool_Index
    ): ElementValue

    def AnnotationValue(
        constant_pool: Constant_Pool,
        annotation:    Annotation
    ): ElementValue

    def ArrayValue(
        constant_pool: Constant_Pool,
        values:        ElementValues
    ): ElementValue

    //
    // IMPLEMENTATION
    //

    def ElementValuePairs(cp: Constant_Pool, in: DataInputStream): ElementValuePairs = {
        fillArraySeq(in.readUnsignedShort) {
            ElementValuePair(cp, in)
        }
    }

    def ElementValuePair(cp: Constant_Pool, in: DataInputStream): ElementValuePair = {
        ElementValuePair(cp, in.readUnsignedShort, ElementValue(cp, in))
    }

    /**
     * Parses an element value.
     *
     * '''From the Specification'''
     * <pre>
     * element_value {
     *    u1 tag;
     *    union {
     *      u2   const_value_index;
     *
     *      {
     *        u2 type_name_index;
     *        u2 const_name_index;
     *      } enum_const_value;
     *
     *      u2 class_info_index;
     *
     *      annotation annotation_value;
     *
     *      {
     *        u2    num_values;
     *        element_value values[num_values];
     *      } array_value;
     *    } value;
     * }
     * </pre>
     */
    def ElementValue(cp: Constant_Pool, in: DataInputStream): ElementValue = {
        val tag = in.readByte
        (tag: @scala.annotation.switch) match {
            case 'B' => ByteValue(cp, in.readUnsignedShort)
            case 'C' => CharValue(cp, in.readUnsignedShort)
            case 'D' => DoubleValue(cp, in.readUnsignedShort)
            case 'F' => FloatValue(cp, in.readUnsignedShort)
            case 'I' => IntValue(cp, in.readUnsignedShort)
            case 'J' => LongValue(cp, in.readUnsignedShort)
            case 'S' => ShortValue(cp, in.readUnsignedShort)
            case 'Z' => BooleanValue(cp, in.readUnsignedShort)
            case 's' => StringValue(cp, in.readUnsignedShort)
            case 'e' => EnumValue(cp, in.readUnsignedShort, in.readUnsignedShort)
            case 'c' => ClassValue(cp, in.readUnsignedShort)
            case '@' => AnnotationValue(cp, Annotation(cp, in))
            case '[' => ArrayValue(cp, fillArraySeq(in.readUnsignedShort)(ElementValue(cp, in)))
        }
    }
}
