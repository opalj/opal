/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the local variable type table attribute.
 */
trait LocalVariableTypeTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type LocalVariableTypeTable_attribute >: Null <: Attribute

    type LocalVariableTypeTableEntry <: AnyRef
    implicit val localVariableTypeTableEntryType: ClassTag[LocalVariableTypeTableEntry] // TODO: Replace in Scala 3 by `type LocalVariableTypeTableEntry : ClassTag`
    type LocalVariableTypes = ArraySeq[LocalVariableTypeTableEntry]

    def LocalVariableTypeTable_attribute(
        cp:                        Constant_Pool,
        ap_name_index:             Constant_Pool_Index,
        ap_descriptor_index:       Constant_Pool_Index,
        attribute_name_index:      Constant_Pool_Index,
        local_variable_type_table: LocalVariableTypes
    ): LocalVariableTypeTable_attribute

    def LocalVariableTypeTableEntry(
        constant_pool:   Constant_Pool,
        start_pc:        Int,
        length:          Int,
        name_index:      Constant_Pool_Index,
        signature_index: Constant_Pool_Index,
        index:           Int
    ): LocalVariableTypeTableEntry

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * LocalVariableTypeTable_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 local_variable_type_table_length;
     *  { u2 start_pc;
     *      u2 length;
     *      u2 name_index;
     *      u2 signature_index;
     *      u2 index;
     *  } local_variable_type_table[local_variable_type_table_length];
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
            LocalVariableTypeTable_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(entriesCount) {
                    LocalVariableTypeTableEntry(
                        cp,
                        in.readUnsignedShort,
                        in.readUnsignedShort,
                        in.readUnsignedShort,
                        in.readUnsignedShort,
                        in.readUnsignedShort
                    )
                }
            )
        } else {
            null
        }
    }

    registerAttributeReader(LocalVariableTypeTableAttribute.Name -> parserFactory())
}
