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
package bi
package reader

import reflect.ClassTag

import java.io.DataInputStream

/**
 * Generic parser for the local variable type table attribute.
 *
 * @author Michael Eichberg
 */
trait LocalVariableTypeTable_attributeReader extends AttributeReader {

    //
    // ABSTRACT DEFINITIONS
    //
    type LocalVariableTypeTable_attribute <: Attribute

    type LocalVariableTypeTableEntry
    implicit val LocalVariableTypeTableEntryManifest: ClassTag[LocalVariableTypeTableEntry]

    def LocalVariableTypeTable_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        local_variable_type_table: LocalVariableTypes): LocalVariableTypeTable_attribute

    def LocalVariableTypeTableEntry(
        constant_pool: Constant_Pool,
        start_pc: Int,
        length: Int,
        name_index: Constant_Pool_Index,
        signature_index: Constant_Pool_Index,
        index: Int): LocalVariableTypeTableEntry

    //
    // IMPLEMENTATION
    //

    type LocalVariableTypes = IndexedSeq[LocalVariableTypeTableEntry]

    /* '''From the Specification'''
     * 
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
    registerAttributeReader(
        LocalVariableTypeTable_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt()

                val entriesCount = in.readUnsignedShort()
                if (entriesCount > 0)
                    LocalVariableTypeTable_attribute(
                        cp,
                        attribute_name_index,
                        attribute_length,
                        repeat(entriesCount) {
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
                else {
                    null
                }
            }
        )
    )
}
object LocalVariableTypeTable_attributeReader {

    val ATTRIBUTE_NAME = "LocalVariableTypeTable"

}