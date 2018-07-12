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

    type LineNumberTable_attribute >: Null <: Attribute

    def LineNumberTable_attribute(
        constant_pool:        Constant_Pool,
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
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val table_length = in.readUnsignedShort()
        if (table_length > 0 || reifyEmptyAttributes) {
            val data = new Array[Byte](table_length * 4)
            in.readFully(data)
            LineNumberTable_attribute(cp, attribute_name_index, data)
        } else {
            null
        }

    }

    registerAttributeReader(LineNumberTableAttribute.Name → parserFactory())
}
