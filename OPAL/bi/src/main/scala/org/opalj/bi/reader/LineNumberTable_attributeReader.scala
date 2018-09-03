/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.collection.immutable.RefArray
import org.opalj.control.fillRefArray

/**
 * Generic parser for the ''LineNumberTable'' attribute.
 */
trait LineNumberTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LineNumberTable_attribute >: Null <: Attribute

    type LineNumberTableEntry <: AnyRef
    type LineNumbers = RefArray[LineNumberTableEntry]

    def LineNumberTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    LineNumbers,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): LineNumberTable_attribute

    def LineNumberTableEntry(start_pc: Int, line_number: Int): LineNumberTableEntry

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * LineNumberTable_attribute {
     *   u2 attribute_name_index;
     *   u4 attribute_length;
     *   u2 line_number_table_length;
     *   {  u2 start_pc;
     *      u2 line_number;
     *   }  line_number_table[line_number_table_length];
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
        /*val attribute_length =*/ in.readInt()
        val line_number_table_length = in.readUnsignedShort
        if (line_number_table_length > 0 || reifyEmptyAttributes) {
            LineNumberTable_attribute(
                cp,
                attribute_name_index,
                fillRefArray(line_number_table_length) {
                    LineNumberTableEntry(in.readUnsignedShort, in.readUnsignedShort)
                },
                as_name_index,
                as_descriptor_index
            )
        } else
            null
    }

    registerAttributeReader(LineNumberTableAttribute.Name → parserFactory())
}
