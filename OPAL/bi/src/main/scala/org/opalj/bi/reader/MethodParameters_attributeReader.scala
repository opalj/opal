/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * A generic reader for Java 8's `MethodParameters` attribute.
 */
trait MethodParameters_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type MethodParameters_attribute >: Null <: Attribute

    type MethodParameter <: AnyRef
    implicit val methodParameterType: ClassTag[MethodParameter] // TODO: Replace in Scala 3 by `type MethodParameter : ClassTag`
    type MethodParameters = ArraySeq[MethodParameter]

    def MethodParameters_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        parameters:           MethodParameters
    ): MethodParameters_attribute

    def MethodParameter(
        constant_pool: Constant_Pool,
        name_index:    Constant_Pool_Index,
        access_flags:  Int
    ): MethodParameter

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * MethodParameters_attribute {
     *      u2 attribute_name_index;
     *      u4 attribute_length;
     *      u1 parameters_count;
     *      {   u2 name_index;
     *          u2 access_flags;
     *      } parameters[parameters_count];
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt()
        val parameters_count = in.readUnsignedByte
        if (parameters_count > 0 || reifyEmptyAttributes) {
            MethodParameters_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(parameters_count) {
                    MethodParameter(cp, in.readUnsignedShort, in.readUnsignedShort)
                }
            )
        } else {
            null
        }
    }

    registerAttributeReader(MethodParametersAttribute.Name -> parserFactory())
}
