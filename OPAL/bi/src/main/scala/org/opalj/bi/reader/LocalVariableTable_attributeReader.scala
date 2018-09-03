/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.collection.immutable.RefArray
import org.opalj.control.fillRefArray

/**
 * Generic parser for the ''local variable table'' attribute.
 */
trait LocalVariableTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LocalVariableTable_attribute >: Null <: Attribute

    type LocalVariableTableEntry <: AnyRef
    type LocalVariables = RefArray[LocalVariableTableEntry]

    def LocalVariableTableEntry(
        constant_pool:    Constant_Pool,
        start_pc:         Int,
        length:           Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        index:            Int
    ): LocalVariableTableEntry

    def LocalVariableTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: LocalVariables,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): LocalVariableTable_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * LocalVariableTable_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 local_variable_table_length;
     *  {   u2 start_pc;
     *      u2 length;
     *      u2 name_index;
     *      u2 descriptor_index;
     *      u2 index;
     *  } local_variable_table[local_variable_table_length];
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
        val entriesCount = in.readUnsignedShort()
        if (entriesCount > 0 || reifyEmptyAttributes) {
            LocalVariableTable_attribute(
                cp,
                attribute_name_index,
                {
                    fillRefArray(entriesCount) {
                        LocalVariableTableEntry(
                            cp,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort
                        )
                    }
                },
                as_name_index,
                as_descriptor_index
            )
        } else {
            null
        }
    }

    registerAttributeReader(LocalVariableTableAttribute.Name → parserFactory())
}
