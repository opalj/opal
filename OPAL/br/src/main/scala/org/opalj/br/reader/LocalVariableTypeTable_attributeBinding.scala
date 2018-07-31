/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.LocalVariableTypeTable_attributeReader

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
    val LocalVariableTypeTableEntryManifest: ClassTag[LocalVariableTypeTableEntry] = implicitly

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
        attribute_name_index:      Constant_Pool_Index,
        local_variable_type_table: LocalVariableTypes
    ) =
        new LocalVariableTypeTable(local_variable_type_table)
}

