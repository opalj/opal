/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package canonical
package reader

import scala.reflect.ClassTag

import de.tud.cs.st.bat.reader.Constant_PoolReader

/**
  * Representation of the constant pool as specified by the JVM Specification.
  * (This representation does not provide any abstraction.)
  *
  * @author Michael Eichberg
  */
trait Constant_PoolBinding extends Constant_PoolReader {

    type Constant_Pool_Entry = ConstantPoolEntries.Constant_Pool_Entry
    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    type CONSTANT_Class_info = ConstantPoolEntries.CONSTANT_Class_info

    type CONSTANT_Fieldref_info = ConstantPoolEntries.CONSTANT_Fieldref_info

    type CONSTANT_Methodref_info = ConstantPoolEntries.CONSTANT_Methodref_info

    type CONSTANT_InterfaceMethodref_info = ConstantPoolEntries.CONSTANT_InterfaceMethodref_info

    type CONSTANT_String_info = ConstantPoolEntries.CONSTANT_String_info

    type CONSTANT_Integer_info = ConstantPoolEntries.CONSTANT_Integer_info

    type CONSTANT_Float_info = ConstantPoolEntries.CONSTANT_Float_info

    type CONSTANT_Long_info = ConstantPoolEntries.CONSTANT_Long_info

    type CONSTANT_Double_info = ConstantPoolEntries.CONSTANT_Double_info

    type CONSTANT_NameAndType_info = ConstantPoolEntries.CONSTANT_NameAndType_info

    type CONSTANT_Utf8_info = ConstantPoolEntries.CONSTANT_Utf8_info

    type CONSTANT_MethodHandle_info = ConstantPoolEntries.CONSTANT_MethodHandle_info

    type CONSTANT_MethodType_info = ConstantPoolEntries.CONSTANT_MethodType_info

    type CONSTANT_InvokeDynamic_info = ConstantPoolEntries.CONSTANT_InvokeDynamic_info

    import ConstantPoolEntries._

    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = new CONSTANT_Class_info(i)

    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    def CONSTANT_Utf8_info(s: String): CONSTANT_Utf8_info = new CONSTANT_Utf8_info(s)

    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    def CONSTANT_Fieldref_info(class_index: Int,
                               name_and_type_index: Int): CONSTANT_Fieldref_info =
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)

    def CONSTANT_Methodref_info(class_index: Int,
                                name_and_type_index: Int): CONSTANT_Methodref_info =
        new CONSTANT_Methodref_info(class_index, name_and_type_index)

    def CONSTANT_InterfaceMethodref_info(class_index: Int,
                                         name_and_type_index: Int): CONSTANT_InterfaceMethodref_info =
        new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)

    def CONSTANT_NameAndType_info(name_index: Int,
                                  descriptor_index: Int): CONSTANT_NameAndType_info =
        new CONSTANT_NameAndType_info(name_index, descriptor_index)

    def CONSTANT_MethodHandle_info(reference_kind: Int,
                                   reference_index: Int): CONSTANT_MethodHandle_info =
        new CONSTANT_MethodHandle_info(ReferenceKind(reference_kind), reference_index)

    def CONSTANT_MethodType_info(descriptor_index: Int): CONSTANT_MethodType_info =
        new CONSTANT_MethodType_info(descriptor_index)

    def CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index: Int,
                                    name_and_type_index: Int): CONSTANT_InvokeDynamic_info =
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)
}


