/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
    override def CPEClass(referenceType: ReferenceType, requiresUByteIndex: Boolean): Int = {
        val cpeUtf8 = CPEUtf8OfCPEClass(referenceType)
        validateIndex(constantPool(CONSTANT_Class_info(cpeUtf8)), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    override def CPEFloat(value: Float, requiresUByteIndex: Boolean): Int = {
        validateIndex(constantPool(CONSTANT_Float_info(ConstantFloat(value))), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    override def CPEInteger(value: Int, requiresUByteIndex: Boolean): Int = {
        val cpEntry = CONSTANT_Integer_info(ConstantInteger(value))
        validateIndex(constantPool(cpEntry), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    override def CPEString(value: String, requiresUByteIndex: Boolean): Int = {
        validateIndex(constantPool(CONSTANT_String_info(CPEUtf8(value))), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    override def CPEMethodHandle(methodHandle: MethodHandle, requiresUByteIndex: Boolean): Int = {
        val (tag, cpRefIndex) = CPERefOfCPEMethodHandle(methodHandle)
        validateIndex(constantPool(CONSTANT_MethodHandle_info(tag, cpRefIndex)), requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEMethodType(descriptor: String, requiresUByteIndex: Boolean): Int = {
        val cpEntry = CONSTANT_MethodType_info(constantPool(CONSTANT_Utf8_info(descriptor)))
        validateIndex(constantPool(cpEntry), requiresUByteIndex)
    }

    override def CPEMethodType(descriptor: MethodDescriptor, requiresUByteIndex: Boolean): Int = {
        CPEMethodType(descriptor.toJVMDescriptor, requiresUByteIndex)
    }

    override def CPEDouble(value: Double): Int = {
        constantPool(CONSTANT_Double_info(ConstantDouble(value)))
    }

    override def CPELong(value: Long): Int = {
        constantPool(CONSTANT_Long_info(ConstantLong(value)))
    }

    override def CPEUtf8(value: String): Int = constantPool(CONSTANT_Utf8_info(value))

    override def CPENameAndType(name: String, tpe: String): Int = {
        val nameIndex = CPEUtf8(name)
        val typeIndex = CPEUtf8(tpe)
        constantPool(CONSTANT_NameAndType_info(nameIndex, typeIndex))
    }

    override def CPEFieldRef(
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

    override def CPEMethodRef(
        objectType: ReferenceType,
        name:       String,
        descriptor: MethodDescriptor
    ): Int = {
        CPEMethodRef(objectType, name, descriptor.toJVMDescriptor)
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

    override def CPEInterfaceMethodRef(
        objectType: ReferenceType,
        name:       String,
        descriptor: MethodDescriptor
    ): Int = {
        CPEInterfaceMethodRef(objectType, name, descriptor.toJVMDescriptor)
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

    override def CPEInvokeDynamic(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor
    ): Int = {
        CPEInvokeDynamic(bootstrapMethod, name, descriptor.toJVMDescriptor)
    }

    @throws[ConstantPoolException]
    def CPEDynamic(
        bootstrapMethod:    BootstrapMethod,
        name:               String,
        descriptor:         String,
        requiresUByteIndex: Boolean
    ): Int = {
        val indexOfBootstrapMethod = bootstrapMethods.indexOf(bootstrapMethod)
        if (indexOfBootstrapMethod == -1) {
            throw new ConstantPoolException(s"the bootstrap method $bootstrapMethod is unknown")
        }
        val cpNameAndTypeIndex = CPENameAndType(name, descriptor)
        validateIndex(
            constantPool(CONSTANT_Dynamic_info(indexOfBootstrapMethod, cpNameAndTypeIndex)),
            requiresUByteIndex
        )
    }

    @throws[ConstantPoolException]
    override def CPEDynamic(
        bootstrapMethod:    BootstrapMethod,
        name:               String,
        descriptor:         FieldType,
        requiresUByteIndex: Boolean
    ): Int = {
        CPEDynamic(bootstrapMethod, name, descriptor.toJVMTypeName, requiresUByteIndex)
    }

}
