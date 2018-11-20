/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.REF_getField
import org.opalj.bi.REF_getStatic
import org.opalj.bi.REF_invokeInterface
import org.opalj.bi.REF_invokeSpecial
import org.opalj.bi.REF_invokeStatic
import org.opalj.bi.REF_invokeVirtual
import org.opalj.bi.REF_newInvokeSpecial
import org.opalj.bi.REF_putField
import org.opalj.bi.REF_putStatic
import org.opalj.bi.ReferenceKind

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
    override def runtimeValueType: ObjectType = ObjectType.MethodHandle

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
        receiverType:     ObjectType,
        methodDescriptor: MethodDescriptor
) extends MethodCallMethodHandle {

    final override val name = "<init>"

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
