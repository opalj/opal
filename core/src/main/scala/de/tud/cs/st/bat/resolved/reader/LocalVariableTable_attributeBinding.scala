/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

import de.tud.cs.st.bat.reader.LocalVariableTable_attributeReader

/**
 * The factory methods to create local variable tables and their entries.
 *
 * @author Michael Eichberg
 */
trait LocalVariableTable_attributeBinding
        extends LocalVariableTable_attributeReader
        with ConstantPoolBinding
        with AttributeBinding {

    type LocalVariableTable_attribute = de.tud.cs.st.bat.resolved.LocalVariableTable
    type LocalVariableTableEntry = de.tud.cs.st.bat.resolved.LocalVariable
    val LocalVariableTableEntryManifest: ClassTag[LocalVariable] = implicitly

    def LocalVariableTableEntry(start_pc: Int,
                                length: Int,
                                name_index: Constant_Pool_Index,
                                descriptor_index: Constant_Pool_Index,
                                index: Int)(
                                    implicit cp: Constant_Pool): LocalVariable = {
        new LocalVariable(
            start_pc,
            length,
            name_index.asString,
            descriptor_index.asFieldType,
            index)
    }

    def LocalVariableTable_attribute(attribute_name_index: Constant_Pool_Index,
                                     attribute_length: Int,
                                     local_variable_table: LocalVariables)(
                                         implicit constant_pool: Constant_Pool): LocalVariableTable =
        new LocalVariableTable(local_variable_table)

}


