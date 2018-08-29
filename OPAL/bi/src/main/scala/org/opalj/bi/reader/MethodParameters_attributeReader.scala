/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.collection.immutable.AnyRefArray
import org.opalj.control.fillAnyRefArray

/**
 * A generic reader for Java 8's `MethodParameters` attribute.
 */
trait MethodParameters_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type MethodParameters_attribute >: Null <: Attribute

    type MethodParameter <: AnyRef
    type MethodParameters = AnyRefArray[MethodParameter]

    def MethodParameters_attribute(
        constant_pool:        Constant_Pool,
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
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val parameters_count = in.readUnsignedByte
        if (parameters_count > 0 || reifyEmptyAttributes) {
            MethodParameters_attribute(
                cp,
                attribute_name_index,
                fillAnyRefArray(parameters_count) {
                    MethodParameter(cp, in.readUnsignedShort, in.readUnsignedShort)
                }
            )
        } else {
            null
        }
    }

    registerAttributeReader(MethodParametersAttribute.Name → parserFactory())
}
