/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''enclosing method'' attribute.
 */
trait EnclosingMethod_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type EnclosingMethod_attribute <: Attribute

    def EnclosingMethod_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        class_index:          Constant_Pool_Index,
        method_index:         Constant_Pool_Index
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt
        EnclosingMethod_attribute(
            cp,
            ap_name_index,
            ap_descriptor_index,
            attribute_name_index,
            in.readUnsignedShort,
            in.readUnsignedShort
        )
    }

    registerAttributeReader(EnclosingMethodAttribute.Name -> parserFactory())
}
