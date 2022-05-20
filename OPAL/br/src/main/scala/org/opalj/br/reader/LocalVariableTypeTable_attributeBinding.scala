/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.LocalVariableTypeTable_attributeReader

import scala.reflect.ClassTag

/**
 * The factory methods to create local variable type tables and their entries.
 *
 * @author Michael Eichberg
 */
trait LocalVariableTypeTable_attributeBinding
    extends LocalVariableTypeTable_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type LocalVariableTypeTable_attribute = br.LocalVariableTypeTable

    type LocalVariableTypeTableEntry = br.LocalVariableType
    override implicit val localVariableTypeTableEntryType = ClassTag(classOf[br.LocalVariableType])

    def LocalVariableTypeTableEntry(
        cp:              Constant_Pool,
        start_pc:        Int,
        length:          Int,
        name_index:      Constant_Pool_Index,
        signature_index: Constant_Pool_Index,
        index:           Int
    ): LocalVariableType = {
        new LocalVariableType(
            start_pc,
            length,
            cp(name_index).asString,
            cp(signature_index).asFieldTypeSignature,
            index
        )
    }

    def LocalVariableTypeTable_attribute(
        cp:                        Constant_Pool,
        ap_name_index:             Constant_Pool_Index,
        ap_descriptor_index:       Constant_Pool_Index,
        attribute_name_index:      Constant_Pool_Index,
        local_variable_type_table: LocalVariableTypes
    ): LocalVariableTypeTable = {
        new LocalVariableTypeTable(local_variable_type_table)
    }
}

