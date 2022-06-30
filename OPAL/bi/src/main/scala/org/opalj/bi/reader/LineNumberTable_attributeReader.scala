/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the ''LineNumberTable'' attribute.
 */
trait LineNumberTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LineNumberTable_attribute >: Null <: Attribute

    type LineNumberTableEntry <: AnyRef
    implicit val lineNumberTableEntryType: ClassTag[LineNumberTableEntry] // TODO: Replace in Scala 3 by `type LineNumberTableEntry : ClassTag`
    type LineNumbers = ArraySeq[LineNumberTableEntry]

    def LineNumberTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    LineNumbers
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt()
        val line_number_table_length = in.readUnsignedShort
        if (line_number_table_length > 0 || reifyEmptyAttributes) {
            LineNumberTable_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(line_number_table_length) {
                    LineNumberTableEntry(in.readUnsignedShort, in.readUnsignedShort)
                }
            )
        } else
            null
    }

    registerAttributeReader(LineNumberTableAttribute.Name -> parserFactory())
}
