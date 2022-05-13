/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the ''local variable table'' attribute.
 */
trait LocalVariableTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LocalVariableTable_attribute >: Null <: Attribute

    type LocalVariableTableEntry <: AnyRef
    implicit val localVariableTableEntryType: ClassTag[LocalVariableTableEntry] // TODO: Replace in Scala 3 by `type LocalVariableTableEntry : ClassTag`
    type LocalVariables = ArraySeq[LocalVariableTableEntry]

    def LocalVariableTableEntry(
        cp:               Constant_Pool,
        start_pc:         Int,
        length:           Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        index:            Int
    ): LocalVariableTableEntry

    def LocalVariableTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: LocalVariables
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt()
        val entriesCount = in.readUnsignedShort()
        if (entriesCount > 0 || reifyEmptyAttributes) {
            LocalVariableTable_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                {
                    fillArraySeq(entriesCount) {
                        LocalVariableTableEntry(
                            cp,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort,
                            in.readUnsignedShort
                        )
                    }
                }
            )
        } else {
            null
        }
    }

    registerAttributeReader(LocalVariableTableAttribute.Name -> parserFactory())
}
