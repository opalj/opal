/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import scala.language.implicitConversions

import scala.reflect.ClassTag
import scala.collection.mutable

import org.opalj.bi.reader.Constant_PoolReader

/**
 * A representation of the constant pool.
 *
 * @note The constant pool is considered to be static; i.e., references between
 *    constant pool entries are always resolved at most once and the results are cached.
 *    Hence, after reading the constant pool the constant pool is treated as
 *    immutable; the referenced constant pool entry must not change.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
trait ConstantPoolBinding extends Constant_PoolReader {

    type ClassFile <: br.ClassFile

    implicit def cpIndexTocpEntry(
        index: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): Constant_Pool_Entry = {
        cp(index)
    }

    type Constant_Pool_Entry = cp.Constant_Pool_Entry

    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    type CONSTANT_Class_info = cp.CONSTANT_Class_info
    type CONSTANT_Double_info = cp.CONSTANT_Double_info
    type CONSTANT_Float_info = cp.CONSTANT_Float_info
    type CONSTANT_Integer_info = cp.CONSTANT_Integer_info
    type CONSTANT_Long_info = cp.CONSTANT_Long_info
    type CONSTANT_Utf8_info = cp.CONSTANT_Utf8_info
    type CONSTANT_String_info = cp.CONSTANT_String_info

    type CONSTANT_Fieldref_info = cp.CONSTANT_Fieldref_info
    type CONSTANT_Methodref_info = cp.CONSTANT_Methodref_info
    type CONSTANT_InterfaceMethodref_info = cp.CONSTANT_InterfaceMethodref_info
    type CONSTANT_NameAndType_info = cp.CONSTANT_NameAndType_info

    type CONSTANT_MethodHandle_info = cp.CONSTANT_MethodHandle_info
    type CONSTANT_MethodType_info = cp.CONSTANT_MethodType_info
    type CONSTANT_InvokeDynamic_info = cp.CONSTANT_InvokeDynamic_info

    type CONSTANT_Module_info = cp.CONSTANT_Module_info
    type CONSTANT_Package_info = cp.CONSTANT_Package_info

    //
    // IMPLEMENTATION OF THE CONSTANT POOL READER'S FACTORY METHODS
    //

    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    def CONSTANT_Utf8_info(r: Array[Byte], s: String): CONSTANT_Utf8_info = {
        new CONSTANT_Utf8_info(s)
    }

    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = new CONSTANT_Class_info(i)

    def CONSTANT_Fieldref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
    ): CONSTANT_Fieldref_info = {
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)
    }

    def CONSTANT_Methodref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
    ): CONSTANT_Methodref_info = {
        new CONSTANT_Methodref_info(class_index, name_and_type_index)
    }

    def CONSTANT_InterfaceMethodref_info(
        class_index:         Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index
    ): CONSTANT_InterfaceMethodref_info = {
        new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)
    }

    def CONSTANT_NameAndType_info(
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index
    ): CONSTANT_NameAndType_info = {
        new CONSTANT_NameAndType_info(name_index, descriptor_index)
    }

    def CONSTANT_MethodHandle_info(
        reference_kind:  Int,
        reference_index: Int
    ): CONSTANT_MethodHandle_info = {
        new CONSTANT_MethodHandle_info(reference_kind, reference_index)
    }

    def CONSTANT_MethodType_info(
        descriptor_index: Constant_Pool_Index
    ): CONSTANT_MethodType_info = {
        new CONSTANT_MethodType_info(descriptor_index)
    }

    def CONSTANT_InvokeDynamic_info(
        bootstrap_method_attr_index: Constant_Pool_Index,
        name_and_type_index:         Constant_Pool_Index
    ): CONSTANT_InvokeDynamic_info = {
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)
    }

    def CONSTANT_Module_info(name_index: Constant_Pool_Index): CONSTANT_Module_info = {
        new CONSTANT_Module_info(name_index)
    }

    def CONSTANT_Package_info(name_index: Constant_Pool_Index): CONSTANT_Package_info = {
        new CONSTANT_Package_info(name_index)
    }

    protected[this] def createDeferredActionsStore(): DeferredActionsStore = {
        new mutable.ArrayBuffer[ClassFile ⇒ ClassFile] with Constant_Pool_Entry {}
    }

}
