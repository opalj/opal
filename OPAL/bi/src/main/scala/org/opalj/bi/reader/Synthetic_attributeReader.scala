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

    type Synthetic_attribute <: Attribute

    def Synthetic_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): Synthetic_attribute

    /**
     * <pre>
     * Synthetic_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
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
        Synthetic_attribute(cp, attribute_name_index, as_name_index, as_descriptor_index)
    }

    registerAttributeReader(SyntheticAttribute.Name → parserFactory())
}
