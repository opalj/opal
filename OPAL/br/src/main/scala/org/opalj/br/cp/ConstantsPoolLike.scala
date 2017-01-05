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

/**
 *
 * @note    The subclasses define in which case which exceptions may be thrown!
 *
 * @author  Michael Eichberg
 */
trait ConstantsPoolLike {
    def CPEClass(referenceType: ReferenceType, requiresUByteIndex: Boolean): Int
    def CPEFloat(value: Float, requiresUByteIndex: Boolean): Int
    def CPEInteger(value: Int, requiresUByteIndex: Boolean): Int
    def CPEString(value: String, requiresUByteIndex: Boolean): Int
    def CPEMethodHandle(methodHandle: MethodHandle, requiresUByteIndex: Boolean): Int
    def CPEMethodType(descriptor: String, requiresUByteIndex: Boolean): Int
    def CPEDouble(value: Double): Int
    def CPELong(value: Long): Int
    def CPEUtf8(value: String): Int

    def CPENameAndType(name: String, tpe: String): Int

    def CPEFieldRef(objectType: ObjectType, fieldName: String, fieldType: String): Int
    def CPEMethodRef(referenceType: ReferenceType, methodName: String, descriptor: String): Int
    def CPEInterfaceMethodRef(objectType: ReferenceType, name: String, descriptor: String): Int

    def CPEInvokeDynamic(bootstrapMethod: BootstrapMethod, name: String, descriptor: String): Int

    def CPEUtf8OfCPEClass(referenceType: ReferenceType): Int = {
        val typeName =
            if (referenceType.isObjectType)
                referenceType.asObjectType.fqn // "just", e.g., "java/lang/Object"
            else // an array type including L and ; in case of reference types
                referenceType.toJVMTypeName
        CPEUtf8(typeName)
    }

    /**
     * @return  A pair of ints where the first value is the method handle's tag and the second one
     *          is the constant pool index of the constant pool entry that the
     *          CONSTANT_MethodHandle should reference.
     */
    def CPERefOfCPEMethodHandle(
        methodHandle: MethodHandle
    ): (Int /*TAG*/ , Int /*Constant_Pool_Index*/ ) = {
        methodHandle match {
            case GetFieldMethodHandle(declType, name, fieldType) ⇒
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (1, cpFieldRef)

            case GetStaticMethodHandle(declType, name, fieldType) ⇒
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (2, cpFieldRef)

            case PutFieldMethodHandle(declType, name, fieldType) ⇒
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (3, cpFieldRef)

            case PutStaticMethodHandle(declType, name, fieldType) ⇒
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (4, cpFieldRef)

            case InvokeVirtualMethodHandle(receiverType, name, descriptor) ⇒
                val cpMethodRef = CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                (5, cpMethodRef)

            case InvokeStaticMethodHandle(receiverType, isInterface, name, descriptor) ⇒
                val methodRef =
                    if (isInterface)
                        CPEInterfaceMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                    else
                        CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                (6, methodRef)

            case InvokeSpecialMethodHandle(receiverType, isInterface, name, descriptor) ⇒
                val methodRef =
                    if (isInterface)
                        CPEInterfaceMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                    else
                        CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                (7, methodRef)

            case NewInvokeSpecialMethodHandle(receiverType, name, descriptor) ⇒
                val cpMethodRef = CPEMethodRef(receiverType, name, descriptor.toJVMDescriptor)
                (8, cpMethodRef)

            case InvokeInterfaceMethodHandle(receiverType, name, descriptor) ⇒
                val jvmDescriptor = descriptor.toJVMDescriptor
                val cpMethodRef = CPEInterfaceMethodRef(receiverType, name, jvmDescriptor)
                (9, cpMethodRef)
        }
    }

    @throws[ConstantPoolException]
    def CPEntryForBootstrapArgument(bootstrapArgument: BootstrapArgument): Int = {
        bootstrapArgument match {
            case ConstantString(value)        ⇒ CPEString(value, requiresUByteIndex = false)
            case ConstantClass(refType)       ⇒ CPEClass(refType, requiresUByteIndex = false)
            case ConstantInteger(value)       ⇒ CPEInteger(value, requiresUByteIndex = false)
            case ConstantFloat(value)         ⇒ CPEFloat(value, requiresUByteIndex = false)
            case ConstantLong(value)          ⇒ CPELong(value)
            case ConstantDouble(value)        ⇒ CPEDouble(value)
            case JVMMethodDescriptor(jvmMD)   ⇒ CPEMethodType(jvmMD, requiresUByteIndex = false)
            case gfmh: GetFieldMethodHandle   ⇒ CPEMethodHandle(gfmh, requiresUByteIndex = false)
            case gsmh: GetStaticMethodHandle  ⇒ CPEMethodHandle(gsmh, requiresUByteIndex = false)
            case pfmh: PutFieldMethodHandle   ⇒ CPEMethodHandle(pfmh, requiresUByteIndex = false)
            case psmh: PutStaticMethodHandle  ⇒ CPEMethodHandle(psmh, requiresUByteIndex = false)
            case mcmh: MethodCallMethodHandle ⇒ CPEMethodHandle(mcmh, requiresUByteIndex = false)
        }
    }
}

