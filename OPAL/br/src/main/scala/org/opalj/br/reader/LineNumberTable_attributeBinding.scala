/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import reflect.ClassTag

import org.opalj.bi.reader.LineNumberTable_attributeReader

/**
 * Implements the factory methods to create line number tables and their entries.
 *
 * @author Michael Eichberg
 */
trait UnpackedLineNumberTable_attributeBinding
    extends LineNumberTable_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type LineNumberTableEntry = br.LineNumber
    val LineNumberTableEntryManifest: ClassTag[LineNumber] = implicitly

    type LineNumberTable_attribute = br.UnpackedLineNumberTable

    override def LineNumberTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    LineNumbers,
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index
    ): UnpackedLineNumberTable = {
        new UnpackedLineNumberTable(line_number_table)
    }

    def LineNumberTableEntry(start_pc: Int, line_number: Int): br.LineNumber = {
        new LineNumber(start_pc, line_number)
    }

}

import org.opalj.bi.reader.CompactLineNumberTable_attributeReader

/**
 * Implements the factory methods to create line number tables.
 *
 * @author Michael Eichberg
 */
trait CompactLineNumberTable_attributeBinding
    extends CompactLineNumberTable_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type LineNumberTable_attribute = br.CompactLineNumberTable

    override def LineNumberTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    Array[Byte],
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index
    ): CompactLineNumberTable = {
        new CompactLineNumberTable(line_number_table)
    }

    /**
     * Merge all line number tables and create a single sorted line number table.
     */
    registerAttributesPostProcessor { attributes ⇒
        val (lineNumberTables, _ /*otherAttributes*/ ) =
            attributes partition {
                case _: CompactLineNumberTable ⇒ true
                case _                         ⇒ false
            }
        if (lineNumberTables.isEmpty || lineNumberTables.tail.isEmpty)
            // we have at most one line number table
            attributes
        else throw new UnsupportedOperationException(
            "multiple line number tables are not yet supported; contact the OPAL team"
        )

    }
}
