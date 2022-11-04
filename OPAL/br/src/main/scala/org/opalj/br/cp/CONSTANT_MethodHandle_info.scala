/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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

            case bi.REF_getField.referenceKind =>
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                GetFieldMethodHandle(declaringType, name, fieldType)

            case bi.REF_getStatic.referenceKind =>
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                GetStaticMethodHandle(declaringType, name, fieldType)

            case bi.REF_putField.referenceKind =>
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                PutFieldMethodHandle(declaringType, name, fieldType)

            case bi.REF_putStatic.referenceKind =>
                val (declaringType, name, fieldType) = cp(referenceIndex).asFieldref(cp)
                PutStaticMethodHandle(declaringType, name, fieldType)

            case bi.REF_invokeVirtual.referenceKind =>
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeVirtualMethodHandle(receiverType, name, methodDescriptor)

            case bi.REF_invokeStatic.referenceKind =>
                val (receiverType, isInterface, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeStaticMethodHandle(receiverType, isInterface, name, methodDescriptor)

            case bi.REF_invokeSpecial.referenceKind =>
                val (receiverType, isInterface, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeSpecialMethodHandle(receiverType, isInterface, name, methodDescriptor)

            case bi.REF_newInvokeSpecial.referenceKind =>
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                assert(
                    receiverType.isObjectType,
                    "receiver for newInvokeSpecial must be objecttype"
                )
                assert(
                    name == "<init>",
                    "invalid bytecode: newInvokeSpecial name must be <init> (see JVM spc 4.4.8)"
                )
                NewInvokeSpecialMethodHandle(receiverType.asObjectType, methodDescriptor)

            case bi.REF_invokeInterface.referenceKind =>
                val (receiverType, _, name, methodDescriptor) = cp(referenceIndex).asMethodref(cp)
                InvokeInterfaceMethodHandle(receiverType, name, methodDescriptor)

        }
    }
}