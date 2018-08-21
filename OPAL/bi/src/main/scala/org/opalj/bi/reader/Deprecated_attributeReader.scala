/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic infrastructure for reading the "@deprecated" attribute.
 */
trait Deprecated_attributeReader extends AttributeReader {

    type Deprecated_attribute <: Attribute

    def Deprecated_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): Deprecated_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * Deprecated_attribute {
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
        Deprecated_attribute(cp, attribute_name_index, as_name_index, as_descriptor_index)
    }

    registerAttributeReader(DeprecatedAttribute.Name → parserFactory())
}
