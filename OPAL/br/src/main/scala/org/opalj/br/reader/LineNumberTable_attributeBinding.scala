/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.LineNumberTable_attributeReader
import org.opalj.bi.reader.CompactLineNumberTable_attributeReader

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

    type LineNumberTable_attribute = br.UnpackedLineNumberTable

    override def LineNumberTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    LineNumbers
    ): UnpackedLineNumberTable = {
        new UnpackedLineNumberTable(line_number_table)
    }

    def LineNumberTableEntry(start_pc: Int, line_number: Int): br.LineNumber = {
        new LineNumber(start_pc, line_number)
    }

}

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
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    Array[Byte]
    ): CompactLineNumberTable = {
        new CompactLineNumberTable(line_number_table)
    }

    /**
     * Merge all line number tables and create a single sorted line number table.
     */
    registerAttributesPostProcessor { attributes =>
        if (attributes.count(_.isInstanceOf[CompactLineNumberTable]) <= 1)
            // we have at most one line number table
            attributes
        else throw new UnsupportedOperationException(
            "multiple line number tables are not yet supported; contact the OPAL team"
        )

    }
}
