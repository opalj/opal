/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''enclosing method'' attribute.
 */
trait EnclosingMethod_attributeReader extends AttributeReader {

    type EnclosingMethod_attribute <: Attribute

    def EnclosingMethod_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        class_index:          Constant_Pool_Index,
        method_index:         Constant_Pool_Index,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): EnclosingMethod_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * EnclosingMethod_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 class_index
     *  u2 method_index;
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt
        EnclosingMethod_attribute(
            cp,
            attribute_name_index,
            in.readUnsignedShort,
            in.readUnsignedShort,
            as_name_index,
            as_descriptor_index
        )
    }

    registerAttributeReader(EnclosingMethodAttribute.Name → parserFactory())
}
