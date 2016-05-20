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
package cp

import scala.collection.mutable.ArrayBuffer

/**
 * This class is used to build up a constant pool.
 *
 * @author Andre Pacak
 */
class ConstantPoolBuffer {
    private[this] val buffer =
        new scala.collection.mutable.HashMap[Constant_Pool_Entry, Constant_Pool_Index]
    //the first item is null because the constant_pool starts with the index 1
    buffer(null) = 0

    private def insertUnique(entry: Constant_Pool_Entry): Int = {
        if (!buffer.contains(entry)) {
            val index = buffer.size
            buffer(entry) = index
            return index;
        }
        buffer(entry)
    }

    def insertClass(referenceType: ReferenceType): Int = {
        val typeName =
            if (referenceType.isObjectType)
                referenceType.asObjectType.fqn
            else
                referenceType.toJVMTypeName
        insertUnique(CONSTANT_Class_info(
            insertUtf8(typeName)
        ))

    }
    def insertDouble(value: Double): Int = {
        insertUnique(CONSTANT_Double_info(ConstantDouble(value)))
    }
    def insertFloat(value: Float): Int = {
        insertUnique(CONSTANT_Float_info(ConstantFloat(value)))
    }
    def insertInteger(value: Int): Int = {
        insertUnique(CONSTANT_Integer_info(ConstantInteger(value)))
    }
    def insertLong(value: Long): Int = {
        insertUnique(CONSTANT_Long_info(ConstantLong(value)))
    }
    def insertString(value: String): Int = {
        insertUnique(CONSTANT_String_info(
            insertUtf8(value)
        ))
    }
    def insertUtf8(value: String): Int = {
        insertUnique(CONSTANT_Utf8_info(value))
    }
    def insertNameAndType(
        nameString: String,
        typeString: String
    ): Int = {
        val indexName = insertUtf8(nameString)
        val indexType = insertUtf8(typeString)
        insertUnique(CONSTANT_NameAndType_info(
            indexName,
            indexType
        ))
    }
    def insertFieldRef(
        objectType: ObjectType,
        fieldName:  String,
        fieldType:  String
    ): Int = {
        val indexObjectType = insertClass(objectType)
        insertUnique(CONSTANT_Fieldref_info(
            indexObjectType,
            insertNameAndType(fieldName, fieldType)
        ))
    }
    def insertMethodRef(
        referenceType: ReferenceType,
        methodName:    String,
        descriptor:    String
    ): Int = {
        val indexObjectType = insertClass(referenceType)
        insertUnique(CONSTANT_Methodref_info(
            indexObjectType,
            insertNameAndType(methodName, descriptor)
        ))
    }
    def insertInterfaceMethodRef(
        objectType: ReferenceType,
        methodName: String,
        descriptor: String
    ): Int = {
        val indexObjectType = insertClass(objectType)
        insertUnique(CONSTANT_InterfaceMethodref_info(
            indexObjectType,
            insertNameAndType(methodName, descriptor)
        ))
    }
    def insertMethodHandle(methodHandle: MethodHandle): Int = methodHandle match {
        case GetFieldMethodHandle(declType, name, fieldType) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                1,
                insertFieldRef(declType, name, fieldType.toJVMTypeName)
            ))
        case GetStaticMethodHandle(declType, name, fieldType) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                2,
                insertFieldRef(declType, name, fieldType.toJVMTypeName)
            ))
        case PutFieldMethodHandle(declType, name, fieldType) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                3,
                insertFieldRef(declType, name, fieldType.toJVMTypeName)
            ))
        case PutStaticMethodHandle(declType, name, fieldType) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                4,
                insertFieldRef(declType, name, fieldType.toJVMTypeName)
            ))
        case InvokeVirtualMethodHandle(recType, name, descriptor) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                5,
                insertMethodRef(recType, name, descriptor.toJVMDescriptor)
            ))
        case InvokeStaticMethodHandle(recType, name, descriptor) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                6,
                insertMethodRef(recType, name, descriptor.toJVMDescriptor)
            ))
        case InvokeSpecialMethodHandle(recType, name, descriptor) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                7,
                insertMethodRef(recType, name, descriptor.toJVMDescriptor)
            ))
        case NewInvokeSpecialMethodHandle(recType, name, descriptor) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                8,
                insertMethodRef(recType, name, descriptor.toJVMDescriptor)
            ))
        case InvokeInterfaceMethodHandle(recType, name, descriptor) ⇒
            insertUnique(CONSTANT_MethodHandle_info(
                9,
                insertInterfaceMethodRef(recType, name, descriptor.toJVMDescriptor)
            ))
    }
    def insertMethodType(descriptor: String): Int = {
        insertUnique(CONSTANT_MethodType_info(
            insertUnique(CONSTANT_Utf8_info(descriptor))
        ))
    }
    def insertInvokeDynamic(
        bootstrap:  BootstrapMethod,
        name:       String,
        descriptor: String,
        bmt:        BootstrapMethodsBuffer
    ): Int = {
        //need to build up bootstrap_methods
        val indexOfBootstrapMethod =
            if (bmt contains bootstrap)
                bmt.indexOf(bootstrap)
            else {
                bmt += bootstrap
                bmt.indexOf(bootstrap)
            }
        val indexOfNameAndType = insertNameAndType(name, descriptor)
        insertUnique(CONSTANT_InvokeDynamic_info(
            indexOfBootstrapMethod,
            indexOfNameAndType
        ))
    }
    def toArray = buffer.toList.sortBy { _._2 }.map { _._1 }.toArray
}