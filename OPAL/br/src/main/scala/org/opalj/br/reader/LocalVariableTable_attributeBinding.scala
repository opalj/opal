/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.LocalVariableTable_attributeReader

/**
 * The factory methods to create local variable tables and their entries.
 *
 * @author Michael Eichberg
 */
trait LocalVariableTable_attributeBinding
    extends LocalVariableTable_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type LocalVariableTable_attribute = br.LocalVariableTable
    type LocalVariableTableEntry = br.LocalVariable
    override val LocalVariableTableEntryManifest: ClassTag[LocalVariable] = implicitly

    override def LocalVariableTableEntry(
        cp:               Constant_Pool,
        start_pc:         Int,
        length:           Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        index:            Int
    ): LocalVariable = {
        new LocalVariable(
            start_pc,
            length,
            cp(name_index).asString,
            cp(descriptor_index).asFieldType,
            index
        )
    }

    override def LocalVariableTable_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: LocalVariables,
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index
    ): LocalVariableTable =
        new LocalVariableTable(local_variable_table)

}

