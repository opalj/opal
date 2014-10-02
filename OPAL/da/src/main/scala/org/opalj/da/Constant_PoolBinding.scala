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
package da

import bi.reader.{ Constant_PoolReader, Constant_PoolAbstractions }

import reflect.ClassTag

/**
 * Representation of the constant pool as specified by the JVM Spec.
 * (This representation does not provide any abstraction.)
 *
 * @author Michael Eichberg
 */
trait Constant_PoolBinding extends Constant_PoolReader with Constant_PoolAbstractions {

    protected[this] def createDeferredActionsStore(): DeferredActionsStore = {
        new scala.collection.mutable.ArrayBuffer[ClassFile ⇒ ClassFile] with Constant_Pool_Entry {
            def Constant_Type_Value = throw new UnsupportedOperationException()
            def toNode(implicit cp: Constant_Pool) = throw new UnsupportedOperationException()
            def toString(implicit cp: Constant_Pool) = throw new UnsupportedOperationException()
            def toLDCString(implicit cp: Constant_Pool) = throw new UnsupportedOperationException()
        }
    }

    // ______________________________________________________________________________________________
    //
    // REPRESENTATION OF THE CONSTANT POOL
    // ______________________________________________________________________________________________
    //

    type Constant_Pool_Entry = org.opalj.da.Constant_Pool_Entry
    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    type CONSTANT_Class_info = org.opalj.da.CONSTANT_Class_info
    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = da.CONSTANT_Class_info(i)

    type CONSTANT_Double_info = org.opalj.da.CONSTANT_Double_info
    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    type CONSTANT_Float_info = org.opalj.da.CONSTANT_Float_info
    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    type CONSTANT_Integer_info = org.opalj.da.CONSTANT_Integer_info
    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    type CONSTANT_Long_info = org.opalj.da.CONSTANT_Long_info
    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    type CONSTANT_Utf8_info = org.opalj.da.CONSTANT_Utf8_info
    def CONSTANT_Utf8_info(s: String): CONSTANT_Utf8_info = new CONSTANT_Utf8_info(s)

    type CONSTANT_String_info = org.opalj.da.CONSTANT_String_info
    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    type CONSTANT_Fieldref_info = org.opalj.da.CONSTANT_Fieldref_info
    def CONSTANT_Fieldref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index): CONSTANT_Fieldref_info =
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)

    type CONSTANT_Methodref_info = org.opalj.da.CONSTANT_Methodref_info
    def CONSTANT_Methodref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index): CONSTANT_Methodref_info =
        new CONSTANT_Methodref_info(class_index, name_and_type_index)

    type CONSTANT_InterfaceMethodref_info = org.opalj.da.CONSTANT_InterfaceMethodref_info
    def CONSTANT_InterfaceMethodref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index): CONSTANT_InterfaceMethodref_info =
        new CONSTANT_InterfaceMethodref_info(
            class_index, name_and_type_index
        )

    type CONSTANT_NameAndType_info = org.opalj.da.CONSTANT_NameAndType_info
    def CONSTANT_NameAndType_info(
        name_index: Constant_Pool_Index, descriptor_index: Constant_Pool_Index): CONSTANT_NameAndType_info =
        new CONSTANT_NameAndType_info(name_index, descriptor_index)

    type CONSTANT_InvokeDynamic_info = org.opalj.da.CONSTANT_InvokeDynamic_info
    def CONSTANT_InvokeDynamic_info(
        bootstrap_method_attr_index: Int,
        name_and_type_index: Constant_Pool_Index): org.opalj.da.ClassFileReader.CONSTANT_InvokeDynamic_info =
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)

    type CONSTANT_MethodHandle_info = org.opalj.da.CONSTANT_MethodHandle_info
    def CONSTANT_MethodHandle_info(
        reference_kind: Int,
        reference_index: Constant_Pool_Index): org.opalj.da.ClassFileReader.CONSTANT_MethodHandle_info =
        new CONSTANT_MethodHandle_info(reference_kind, reference_index)

    type CONSTANT_MethodType_info = org.opalj.da.CONSTANT_MethodType_info
    def CONSTANT_MethodType_info(
        descriptor_index: Constant_Pool_Index): org.opalj.da.ClassFileReader.CONSTANT_MethodType_info =
        new CONSTANT_MethodType_info(descriptor_index)

}

