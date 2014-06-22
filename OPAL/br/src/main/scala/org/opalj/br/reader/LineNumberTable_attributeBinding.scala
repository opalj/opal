/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package reader

import reflect.ClassTag

import bi.reader.LineNumberTable_attributeReader

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

    def LineNumberTable_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        line_number_table: LineNumbers): UnpackedLineNumberTable =
        new UnpackedLineNumberTable(line_number_table)

    def LineNumberTableEntry(start_pc: Int, line_number: Int) =
        new LineNumber(start_pc, line_number)

    /**
     * Merge all line number tables and create a single sorted line number table.
     */
    registerAttributesPostProcessor { attributes ⇒
        val (lineNumberTables, otherAttributes) =
            attributes partition {
                _ match {
                    case lnt: UnpackedLineNumberTable ⇒ true
                    case _                            ⇒ false
                }
            }
        lineNumberTables match {
            case Seq()    ⇒ attributes
            case Seq(lnt) ⇒ attributes
            case lnts ⇒ {
                val mergedTables =
                    lnts.map(_.asInstanceOf[UnpackedLineNumberTable].lineNumbers).flatten
                val sortedTable =
                    mergedTables.sortWith((ltA, ltB) ⇒ ltA.startPC < ltB.startPC)
                new UnpackedLineNumberTable(sortedTable) +: otherAttributes
            }
        }
    }
}

import bi.reader.CompactLineNumberTable_attributeReader

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

    def LineNumberTable_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        line_number_table: Array[Byte]): CompactLineNumberTable =
        new CompactLineNumberTable(line_number_table)

    /**
     * Merge all line number tables and create a single sorted line number table.
     */
    registerAttributesPostProcessor { attributes ⇒
        val (lineNumberTables, otherAttributes) =
            attributes partition {
                _ match {
                    case lnt: UnpackedLineNumberTable ⇒ true
                    case _                            ⇒ false
                }
            }
        lineNumberTables match {
            case Seq()    ⇒ attributes
            case Seq(lnt) ⇒ attributes
            case lnts     ⇒ throw new UnsupportedOperationException("contact Michael Eichberg for support")
        }
    }
}



