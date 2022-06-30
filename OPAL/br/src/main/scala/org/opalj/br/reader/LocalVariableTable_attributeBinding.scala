/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.LocalVariableTable_attributeReader

import scala.reflect.ClassTag

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
    override implicit val localVariableTableEntryType: ClassTag[LocalVariableTableEntry] = ClassTag(classOf[br.LocalVariable])

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
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: LocalVariables
    ): LocalVariableTable =
        new LocalVariableTable(local_variable_table)

}

