/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

/**
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
    def CPEMethodType(descriptor: MethodDescriptor, requiresUByteIndex: Boolean): Int
    def CPEDouble(value: Double): Int
    def CPELong(value: Long): Int
    def CPEUtf8(value: String): Int

    def CPENameAndType(name: String, tpe: String): Int

    def CPEFieldRef(objectType: ObjectType, fieldName: String, fieldType: String): Int

    def CPEMethodRef(
        referenceType: ReferenceType,
        methodName:    String,
        descriptor:    MethodDescriptor
    ): Int

    def CPEInterfaceMethodRef(
        objectType: ReferenceType,
        name:       String,
        descriptor: MethodDescriptor
    ): Int

    def CPEInvokeDynamic(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor
    ): Int

    def CPEDynamic(
        bootstrapMethod:    BootstrapMethod,
        name:               String,
        descriptor:         FieldType,
        requiresUByteIndex: Boolean
    ): Int

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
            case GetFieldMethodHandle(declType, name, fieldType) =>
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (1, cpFieldRef)

            case GetStaticMethodHandle(declType, name, fieldType) =>
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (2, cpFieldRef)

            case PutFieldMethodHandle(declType, name, fieldType) =>
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (3, cpFieldRef)

            case PutStaticMethodHandle(declType, name, fieldType) =>
                val cpFieldRef = CPEFieldRef(declType, name, fieldType.toJVMTypeName)
                (4, cpFieldRef)

            case InvokeVirtualMethodHandle(receiverType, name, descriptor) =>
                val cpMethodRef = CPEMethodRef(receiverType, name, descriptor)
                (5, cpMethodRef)

            case InvokeStaticMethodHandle(receiverType, isInterface, name, descriptor) =>
                val methodRef =
                    if (isInterface)
                        CPEInterfaceMethodRef(receiverType, name, descriptor)
                    else
                        CPEMethodRef(receiverType, name, descriptor)
                (6, methodRef)

            case InvokeSpecialMethodHandle(receiverType, isInterface, name, descriptor) =>
                val methodRef =
                    if (isInterface)
                        CPEInterfaceMethodRef(receiverType, name, descriptor)
                    else
                        CPEMethodRef(receiverType, name, descriptor)
                (7, methodRef)

            case NewInvokeSpecialMethodHandle(receiverType, descriptor) =>
                val cpMethodRef = CPEMethodRef(receiverType, "<init>", descriptor)
                (8, cpMethodRef)

            case InvokeInterfaceMethodHandle(receiverType, name, descriptor) =>
                val cpMethodRef = CPEInterfaceMethodRef(receiverType, name, descriptor)
                (9, cpMethodRef)
        }
    }

    @throws[ConstantPoolException]
    def CPEntryForBootstrapArgument(bootstrapArgument: BootstrapArgument): Int = {
        bootstrapArgument match {
            case ConstantString(value)        => CPEString(value, requiresUByteIndex = false)
            case ConstantClass(refType)       => CPEClass(refType, requiresUByteIndex = false)
            case ConstantInteger(value)       => CPEInteger(value, requiresUByteIndex = false)
            case ConstantFloat(value)         => CPEFloat(value, requiresUByteIndex = false)
            case ConstantLong(value)          => CPELong(value)
            case ConstantDouble(value)        => CPEDouble(value)
            case md: MethodDescriptor         => CPEMethodType(md, requiresUByteIndex = false)
            case gfmh: GetFieldMethodHandle   => CPEMethodHandle(gfmh, requiresUByteIndex = false)
            case gsmh: GetStaticMethodHandle  => CPEMethodHandle(gsmh, requiresUByteIndex = false)
            case pfmh: PutFieldMethodHandle   => CPEMethodHandle(pfmh, requiresUByteIndex = false)
            case psmh: PutStaticMethodHandle  => CPEMethodHandle(psmh, requiresUByteIndex = false)
            case mcmh: MethodCallMethodHandle => CPEMethodHandle(mcmh, requiresUByteIndex = false)
            case DynamicConstant(bootstrapMethod, name, descriptor) =>
                CPEDynamic(bootstrapMethod, name, descriptor, requiresUByteIndex = false)
        }
    }
}
