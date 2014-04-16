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
package de.tud.cs.st
package bat
package resolved
package reader

import reflect.ClassTag

import de.tud.cs.st.bat.reader.LocalVariableTypeTable_attributeReader

/**
 * The factory methods to create local variable type tables and their entries.
 *
 * @author Michael Eichberg
 */
trait LocalVariableTypeTable_attributeBinding
        extends LocalVariableTypeTable_attributeReader
        with ConstantPoolBinding
        with AttributeBinding {

    type LocalVariableTypeTable_attribute = de.tud.cs.st.bat.resolved.LocalVariableTypeTable

    type LocalVariableTypeTableEntry = de.tud.cs.st.bat.resolved.LocalVariableType
    val LocalVariableTypeTableEntryManifest: ClassTag[LocalVariableTypeTableEntry] = implicitly

    def LocalVariableTypeTableEntry(
        start_pc: Int,
        length: Int,
        name_index: Constant_Pool_Index,
        signature_index: Constant_Pool_Index,
        index: Int)(
            implicit cp: Constant_Pool): LocalVariableType = {

        new LocalVariableType(
            start_pc,
            length,
            name_index.asString,
            signature_index.asFieldTypeSignature,
            index)
    }

    def LocalVariableTypeTable_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        local_variable_type_table: LocalVariableTypes)(
            implicit constant_pool: Constant_Pool) =
        new LocalVariableTypeTable(local_variable_type_table)
}


