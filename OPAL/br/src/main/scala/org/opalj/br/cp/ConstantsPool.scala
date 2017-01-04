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

import scala.collection.immutable

/**
 * An immutable view of a created constants pool. The `ConstantsPool` object is typically
 * created using a [[ConstantsBuffer]]'s `build` method.
 *
 * @author  Michael Eichberg
 */
class ConstantsPool(
        val constantPool:     immutable.Map[Constant_Pool_Entry, Constant_Pool_Index],
        val bootstrapMethods: IndexedSeq[BootstrapMethod]
) extends ConstantsPoolLike {

    private[this] def validateIndex(index: Int, requiresUByteIndex: Boolean): Int = {
        if (requiresUByteIndex && index > UByte.MaxValue) {
            val message = s"the constant pool index $index is larger than ${UByte.MaxValue}"
            throw new ConstantPoolException(message)
        }
        index
    }

    @throws[ConstantPoolException]
    def CPEClass(referenceType: ReferenceType, requiresUByteIndex: Boolean): Int = {
        val cpeUtf8 = CPEUtf8OfCPEClass(referenceType)
        validateIndex(constantPool(CONSTANT_Class_info(cpeUtf8)), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEFloat(value: Float, requiresUByteIndex: Boolean): Int = {
        validateIndex(constantPool(CONSTANT_Float_info(ConstantFloat(value))), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEInteger(value: Int, requiresUByteIndex: Boolean): Int = {
        val cpEntry = CONSTANT_Integer_info(ConstantInteger(value))
        validateIndex(constantPool(cpEntry), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEString(value: String, requiresUByteIndex: Boolean): Int = {
        validateIndex(constantPool(CONSTANT_String_info(CPEUtf8(value))), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEMethodHandle(methodHandle: MethodHandle, requiresUByteIndex: Boolean): Int = {
        val (tag, cpRefIndex) = CPERefOfCPEMethodHandle(methodHandle)
        validateIndex(constantPool(CONSTANT_MethodHandle_info(tag, cpRefIndex)), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEMethodType(descriptor: String, requiresUByteIndex: Boolean): Int = {
        val cpEntry = CONSTANT_MethodType_info(constantPool(CONSTANT_Utf8_info(descriptor)))
        validateIndex(constantPool(cpEntry), requiresUByteIndex)
    }

    def CPEDouble(value: Double): Int = {
        constantPool(CONSTANT_Double_info(ConstantDouble(value)))
    }

    def CPELong(value: Long): Int = {
        constantPool(CONSTANT_Long_info(ConstantLong(value)))
    }

    def CPEUtf8(value: String): Int = constantPool(CONSTANT_Utf8_info(value))

    def CPENameAndType(name: String, tpe: String): Int = {
        val nameIndex = CPEUtf8(name)
        val typeIndex = CPEUtf8(tpe)
        constantPool(CONSTANT_NameAndType_info(nameIndex, typeIndex))
    }

    def CPEFieldRef(
        objectType: ObjectType,
        fieldName:  String,
        fieldType:  String
    ): Int = {
        val nameAndTypeRef = CPENameAndType(fieldName, fieldType)
        val cpeClass = CPEClass(objectType, false)
        constantPool(CONSTANT_Fieldref_info(cpeClass, nameAndTypeRef))
    }

    def CPEMethodRef(
        referenceType: ReferenceType,
        methodName:    String,
        descriptor:    String
    ): Int = {
        val class_index = CPEClass(referenceType, false)
        val name_and_type_index = CPENameAndType(methodName, descriptor)
        constantPool(CONSTANT_Methodref_info(class_index, name_and_type_index))
    }

    def CPEInterfaceMethodRef(
        objectType: ReferenceType,
        methodName: String,
        descriptor: String
    ): Int = {
        val class_index = CPEClass(objectType, false)
        val name_and_type_index = CPENameAndType(methodName, descriptor)
        constantPool(CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index))
    }

    def CPEInvokeDynamic(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      String
    ): Int = {
        val indexOfBootstrapMethod = bootstrapMethods.indexOf(bootstrapMethod)
        if (indexOfBootstrapMethod == -1) {
            throw new ConstantPoolException(s"the bootstrap method $bootstrapMethod is unknown")
        }
        val cpNameAndTypeIndex = CPENameAndType(name, descriptor)
        constantPool(CONSTANT_InvokeDynamic_info(indexOfBootstrapMethod, cpNameAndTypeIndex))
    }
}
