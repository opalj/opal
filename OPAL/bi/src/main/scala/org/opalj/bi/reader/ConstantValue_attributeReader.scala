/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Defines a template method to read in a constant value attribute.
 *
 * '''From the Specification'''
 *
 * The ConstantValue attribute is a fixed-length attribute in the attributes
 * table of a field_info structure.
 *
 * <pre>
 * ConstantValue_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u2 constantvalue_index;
 * }
 * </pre>
 */
trait ConstantValue_attributeReader extends AttributeReader {

    type ConstantValue_attribute <: Attribute

    def ConstantValue_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        constantvalue_index:  Constant_Pool_Index
    ): ConstantValue_attribute

    //
    // IMPLEMENTATION
    //

    private[this] def parserFactory() = (
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt
        ConstantValue_attribute(cp, attribute_name_index, in.readUnsignedShort)
    }

    registerAttributeReader(ConstantValueAttribute.Name → parserFactory())
}
