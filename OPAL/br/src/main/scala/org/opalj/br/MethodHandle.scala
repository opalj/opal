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

import org.opalj.bi.ReferenceKind
import org.opalj.bi.REF_getField
import org.opalj.bi.REF_getStatic
import org.opalj.bi.REF_putField
import org.opalj.bi.REF_putStatic
import org.opalj.bi.REF_invokeVirtual
import org.opalj.bi.REF_invokeStatic
import org.opalj.bi.REF_invokeSpecial
import org.opalj.bi.REF_newInvokeSpecial
import org.opalj.bi.REF_invokeInterface

/**
 * A method handle.
 *
 * @author Michael Eichberg
 */
sealed abstract class MethodHandle extends ConstantValue[MethodHandle] {

    final override def value: this.type = this

    /**
     * Returns `ObjectType.MethodHandle`;
     * the type of the value pushed onto the stack by an ldc(_w) instruction.
     */
    override def valueType: ObjectType = ObjectType.MethodHandle

    def isInvokeStaticMethodHandle: Boolean = false

    override def valueToString: String = this.toString

    def toJava: String

    def referenceKind: ReferenceKind
}

sealed abstract class FieldAccessMethodHandle extends MethodHandle {

    def declaringClassType: ObjectType
    def name: String
    def fieldType: FieldType

    def asVirtualField = VirtualField(declaringClassType, name, fieldType)

    override def toJava: String = {
        s"${getClass.getSimpleName}: ${declaringClassType.toJava}.$name:${fieldType.toJava}"
    }
}

sealed abstract class FieldReadAccessMethodHandle extends FieldAccessMethodHandle

sealed abstract class FieldWriteAccessMethodHandle extends FieldAccessMethodHandle

case class GetFieldMethodHandle(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
) extends FieldReadAccessMethodHandle {
    override def referenceKind: ReferenceKind = REF_getField
}

case class GetStaticMethodHandle(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
) extends FieldReadAccessMethodHandle {
    override def referenceKind: ReferenceKind = REF_getStatic
}

case class PutFieldMethodHandle(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
) extends FieldWriteAccessMethodHandle {
    override def referenceKind: ReferenceKind = REF_putField
}

case class PutStaticMethodHandle(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
) extends FieldWriteAccessMethodHandle {
    override def referenceKind: ReferenceKind = REF_putStatic
}

sealed abstract class MethodCallMethodHandle extends MethodHandle {

    def receiverType: ReferenceType
    def name: String
    def methodDescriptor: MethodDescriptor

    override def toJava: String = {
        s"${getClass.getSimpleName}: ${methodDescriptor.toJava(receiverType.toJava, name)}"
    }

    def opcodeOfUnderlyingInstruction: Opcode
}

object MethodCallMethodHandle {

    def unapply(
        handle: MethodCallMethodHandle
    ): Option[(ReferenceType, String, MethodDescriptor)] = {
        Some((handle.receiverType, handle.name, handle.methodDescriptor))
    }

}

case class InvokeVirtualMethodHandle(
        receiverType:     ReferenceType,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    override def opcodeOfUnderlyingInstruction: Opcode = instructions.INVOKEVIRTUAL.opcode

    override def referenceKind: ReferenceKind = REF_invokeVirtual
}

case class InvokeStaticMethodHandle(
        receiverType:     ReferenceType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    override def opcodeOfUnderlyingInstruction: Opcode = instructions.INVOKESTATIC.opcode

    final override def isInvokeStaticMethodHandle: Boolean = true

    override def referenceKind: ReferenceKind = REF_invokeStatic
}

case class InvokeSpecialMethodHandle(
        receiverType:     ReferenceType,
        isInterface:      Boolean,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    override def opcodeOfUnderlyingInstruction: Opcode = instructions.INVOKESPECIAL.opcode

    override def referenceKind: ReferenceKind = REF_invokeSpecial
}

case class NewInvokeSpecialMethodHandle(
        receiverType:     ReferenceType,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    override def opcodeOfUnderlyingInstruction: Opcode = instructions.INVOKESPECIAL.opcode

    override def referenceKind: ReferenceKind = REF_newInvokeSpecial
}

case class InvokeInterfaceMethodHandle(
        receiverType:     ReferenceType,
        name:             String,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    override def opcodeOfUnderlyingInstruction: Opcode = instructions.INVOKEINTERFACE.opcode

    override def referenceKind: ReferenceKind = REF_invokeInterface
}
