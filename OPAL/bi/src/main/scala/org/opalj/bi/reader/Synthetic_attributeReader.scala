/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * The Synthetic attribute is an attribute in the attributes table
 * of a ClassFile, field_info or method_info structure.
 */
trait Synthetic_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Synthetic_attribute <: Attribute

    def Synthetic_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index
    ): Synthetic_attribute

    //
    // IMPLEMENTATION
    //
    /**
     * <pre>
     * Synthetic_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
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
        Synthetic_attribute(cp, ap_name_index, ap_descriptor_index, attribute_name_index)
    }

    registerAttributeReader(SyntheticAttribute.Name -> parserFactory())
}
