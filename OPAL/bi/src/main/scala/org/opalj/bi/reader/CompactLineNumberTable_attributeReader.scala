/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''LineNumberTable'' attribute that does not unpack the
 * line number table.
 */
trait CompactLineNumberTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LineNumberTable_attribute >: Null <: Attribute

    def LineNumberTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    Array[Byte]
    ): LineNumberTable_attribute

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
     *
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
        val table_length = in.readUnsignedShort()
        if (table_length > 0 || reifyEmptyAttributes) {
            val data = new Array[Byte](table_length * 4)
            in.readFully(data)
            LineNumberTable_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                data
            )
        } else {
            null
        }

    }

    registerAttributeReader(LineNumberTableAttribute.Name -> parserFactory())
}
