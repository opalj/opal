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

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a method handle.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_MethodHandle_info(
        referenceKind:  Int,
        referenceIndex: Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_MethodHandle_ID

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asMethodHandle(cp)

    override def asConstantValue(cp: Constant_Pool): MethodHandle = asMethodHandle(cp)

    override def asMethodHandle(cp: Constant_Pool): MethodHandle = {
        (this.referenceKind: @scala.annotation.switch) match {

            case bi.REF_getField.referenceKind ⇒
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                GetFieldMethodHandle(declaringType, name, fieldType)

            case bi.REF_getStatic.referenceKind ⇒
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                GetStaticMethodHandle(declaringType, name, fieldType)

            case bi.REF_putField.referenceKind ⇒
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                PutFieldMethodHandle(declaringType, name, fieldType)

            case bi.REF_putStatic.referenceKind ⇒
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                PutStaticMethodHandle(declaringType, name, fieldType)

            case bi.REF_invokeVirtual.referenceKind ⇒
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeVirtualMethodHandle(receiverType, name, methodDescriptor)

            case bi.REF_invokeStatic.referenceKind ⇒
                val (receiverType, isInterface, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeStaticMethodHandle(receiverType, isInterface, name, methodDescriptor)

            case bi.REF_invokeSpecial.referenceKind ⇒
                val (receiverType, isInterface, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeSpecialMethodHandle(receiverType, isInterface, name, methodDescriptor)

            case bi.REF_newInvokeSpecial.referenceKind ⇒
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                NewInvokeSpecialMethodHandle(receiverType, name, methodDescriptor)

            case bi.REF_invokeInterface.referenceKind ⇒
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeInterfaceMethodHandle(receiverType, name, methodDescriptor)

        }
    }
}