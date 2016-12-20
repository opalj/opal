/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import scala.collection.mutable

/**
 * This class is used to build up a constant pool.
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
class ConstantPoolBuffer {

    private[this] var nextIndex = 1
    private[this] val buffer = mutable.HashMap.empty[Constant_Pool_Entry, Constant_Pool_Index]
    //the first item is null because the constant_pool starts with the index 1
    buffer(null) = 0

    private[this] def getOrElseUpdate(entry: Constant_Pool_Entry, entry_size: Int): Int = {
        buffer.getOrElseUpdate(
            entry,
            {
                val index = nextIndex
                buffer(entry) = index
                nextIndex += entry_size
                index
            }
        )
    }

    def CPEClass(referenceType: ReferenceType): Int = {
        val typeName =
            if (referenceType.isObjectType)
                referenceType.asObjectType.fqn
            else
                referenceType.toJVMTypeName

        getOrElseUpdate(CONSTANT_Class_info(CPEUtf8(typeName)), 1)
    }

    def CPEDouble(value: Double): Int = {
        getOrElseUpdate(CONSTANT_Double_info(ConstantDouble(value)), 2)
    }

    def CPEFloat(value: Float): Int = {
        getOrElseUpdate(CONSTANT_Float_info(ConstantFloat(value)), 1)
    }

    def CPEInteger(value: Int): Int = {
        getOrElseUpdate(CONSTANT_Integer_info(ConstantInteger(value)), 1)
    }

    def CPELong(value: Long): Int = getOrElseUpdate(CONSTANT_Long_info(ConstantLong(value)), 2)

    def CPEString(value: String): Int = getOrElseUpdate(CONSTANT_String_info(CPEUtf8(value)), 1)

    def CPEUtf8(value: String): Int = getOrElseUpdate(CONSTANT_Utf8_info(value), 1)

    def CPENameAndType(name: String, tpe: String): Int = {
        val indexName = CPEUtf8(name)
        val indexType = CPEUtf8(tpe)
        getOrElseUpdate(CONSTANT_NameAndType_info(indexName, indexType), 1)
    }

    def CPEFieldRef(
        objectType: ObjectType,
        fieldName:  String,
        fieldType:  String
    ): Int = {
        getOrElseUpdate(
            CONSTANT_Fieldref_info(CPEClass(objectType), CPENameAndType(fieldName, fieldType)), 1
        )
    }

    def CPEMethodRef(
        referenceType: ReferenceType,
        methodName:    String,
        descriptor:    String
    ): Int = {
        val class_index = CPEClass(referenceType)
        val name_and_type_index = CPENameAndType(methodName, descriptor)
        getOrElseUpdate(CONSTANT_Methodref_info(class_index, name_and_type_index), 1)
    }

    def CPEInterfaceMethodRef(
        objectType: ReferenceType,
        methodName: String,
        descriptor: String
    ): Int = {
        getOrElseUpdate(
            CONSTANT_InterfaceMethodref_info(
                CPEClass(objectType),
                CPENameAndType(methodName, descriptor)
            ),
            1
        )
    }

    def CPEMethodHandle(methodHandle: MethodHandle): Int = methodHandle match {
        case GetFieldMethodHandle(declType, name, fieldType) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(1, CPEFieldRef(declType, name, fieldType.toJVMTypeName)),
                1
            )
        case GetStaticMethodHandle(declType, name, fieldType) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(2, CPEFieldRef(declType, name, fieldType.toJVMTypeName)),
                1
            )

        case PutFieldMethodHandle(declType, name, fieldType) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(3, CPEFieldRef(declType, name, fieldType.toJVMTypeName)),
                1
            )

        case PutStaticMethodHandle(declType, name, fieldType) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(4, CPEFieldRef(declType, name, fieldType.toJVMTypeName)),
                1
            )

        case InvokeVirtualMethodHandle(receiverType, name, descriptor) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(
                    5, CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                ),
                1
            )

        case InvokeStaticMethodHandle(receiverType, isInterface, name, descriptor) ⇒
            val methodRef =
                if (isInterface)
                    CPEInterfaceMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                else
                    CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
            getOrElseUpdate(CONSTANT_MethodHandle_info(6, methodRef), 1)

        case InvokeSpecialMethodHandle(receiverType, isInterface, name, descriptor) ⇒
            val methodRef =
                if (isInterface)
                    CPEInterfaceMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                else
                    CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
            getOrElseUpdate(CONSTANT_MethodHandle_info(7, methodRef), 1)

        case NewInvokeSpecialMethodHandle(receiverType, name, descriptor) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(
                    8, CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                ),
                1
            )

        case InvokeInterfaceMethodHandle(receiverType, name, descriptor) ⇒
            getOrElseUpdate(
                CONSTANT_MethodHandle_info(
                    9, CPEInterfaceMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                ),
                1
            )
    }

    def CPEMethodType(descriptor: String): Int = {
        getOrElseUpdate(
            CONSTANT_MethodType_info(getOrElseUpdate(CONSTANT_Utf8_info(descriptor), 1)),
            1
        )
    }

    def CPEInvokeDynamic(
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
        val indexOfNameAndType = CPENameAndType(name, descriptor)
        getOrElseUpdate(CONSTANT_InvokeDynamic_info(indexOfBootstrapMethod, indexOfNameAndType), 1)
    }

    def toArray = {
        val cp = new Array[Constant_Pool_Entry](nextIndex)
        buffer.foreach { e ⇒
            val (cpe, index) = e
            cp(index) = cpe
        }
        cp
    }
}
