/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.collection.immutable.RefArray
import org.opalj.br.collection.mutable.InstructionsBuilder
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.DUP
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.IAND
import org.opalj.br.instructions.ICONST_0
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.LoadClass_W
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.br.instructions.LoadDynamic2_W
import org.opalj.br.instructions.LoadDynamic_W
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadString_W
import org.opalj.br.instructions.TypeConversionInstructions

/**
 * Provides functionality to produce bytecode that loads a bootstrap argument. Loading of dynamic
 * constants is rewritten to not use ldc/ldc_w/ldc2_w instructions where possible.
 *
 * @author Dominik Helm
 */
trait BootstrapArgumentLoading {

    /**
     * Generate instructions to load a constant argument for a bootstrap method.
     *
     * @param argument The argument to be loaded
     * @param instructions The instruction builder the instructions are appended to
     * @param boxed If true, ensures the argument is of a reference type, boxing it if necessary
     *
     * @return The maximum stack height the generated instructions require
     */
    def loadBootstrapArgument(
        argument:     ConstantValue[_],
        instructions: InstructionsBuilder,
        boxed:        Boolean             = false
    ): Int = {
        argument match {
            case DynamicConstant(bootstrapMethod, name, descriptor) ⇒
                loadDynamicConstant(bootstrapMethod, name, descriptor, instructions, boxed)
            case _ ⇒
                instructions ++= LoadConstantInstruction(argument, wide = true)
                if (boxed && argument.runtimeValueType.isBaseType)
                    instructions ++= argument.runtimeValueType.asBaseType.boxValue
                argument.runtimeValueType.computationalType.operandSize
        }
    }

    /**
     * Generate instructions to load a dynamic constant.
     *
     * @param bootstrapMethod The constant's bootstrap method
     * @param name The constant's name information, passed as the bootstrap method's 2. argument
     * @param descriptor The constant's type, passed as the bootstrap method's 3. argument
     * @param instructions The instruction builder the instructions are appended to
     * @param boxed If true, ensures the constant is of a reference type, boxing it if necessary
     *
     * @return The maximum stack height the generated instructions require
     */
    def loadDynamicConstant(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      FieldType,
        instructions:    InstructionsBuilder,
        boxed:           Boolean             = false
    ): Int = {

        def dynamicLoad(): LoadConstantInstruction[_] = {
            if (descriptor.computationalType.isCategory2) LoadDynamic2_W(bootstrapMethod, name, descriptor)
            else LoadDynamic_W(bootstrapMethod, name, descriptor)
        }

        bootstrapMethod match {
            case BootstrapMethod(InvokeStaticMethodHandle(ObjectType.ConstantBootstraps, false, methodName, _), args: RefArray[ConstantValue[_]] @unchecked) ⇒

                methodName match {
                    case "arrayVarHandle" ⇒
                        val maxStack = loadClassType(args.head, instructions)

                        instructions ++= INVOKESTATIC(
                            ObjectType.MethodHandles,
                            isInterface = false,
                            "arrayElementVarHandle",
                            MethodDescriptor(ObjectType.Class, ObjectType.VarHandle)
                        )

                        maxStack

                    case "enumConstant" ⇒
                        instructions ++= GETSTATIC(descriptor.asObjectType, name, descriptor)
                        1

                    case "explicitCast" ⇒
                        val argument = args.head
                        val argType = argument.runtimeValueType
                        val bsaStack = loadBootstrapArgument(argument, instructions)

                        descriptor match {
                            case ObjectType.Object ⇒ // Nothing to do here, but box if required
                                if (boxed && argType.isBaseType)
                                    instructions ++= argType.asBaseType.boxValue
                                bsaStack

                            case rt: ReferenceType ⇒
                                instructions ++= CHECKCAST(rt)
                                bsaStack

                            case bt: BaseType ⇒
                                val isBool = bt.isBooleanType
                                val targetType = if (isBool) IntegerType else bt

                                val baseType = if (argType.isReferenceType) {
                                    instructions ++= ObjectType.unboxValue(argType)
                                    ObjectType.primitiveType(argType.asObjectType).get.asNumericType
                                } else argType.asNumericType

                                if (baseType != targetType) {
                                    instructions ++= baseType.convertTo(targetType.asNumericType)
                                }

                                // explicitCast requires booleans to be converted from ints based on
                                // the least significant bit
                                if (isBool) {
                                    instructions ++= ICONST_1
                                    instructions ++= IAND
                                }

                                if (boxed)
                                    instructions ++= bt.boxValue

                                bsaStack + (if (isBool) 1 else 0)
                        }

                    case "fieldVarHandle" ⇒
                        fieldVarHandle(
                            name,
                            args.head,
                            loadClassType(args(1), instructions),
                            isStaticField = false,
                            instructions
                        )

                    case "getStaticFinal" ⇒
                        if (args.isEmpty) { // Simple version loads field from class of field type
                            val declClass = if (descriptor.isObjectType) descriptor.asObjectType
                            else descriptor.asBaseType.WrapperType
                            instructions ++= GETSTATIC(declClass, name, descriptor)
                            if (boxed && descriptor.isBaseType)
                                instructions ++= descriptor.asBaseType.boxValue
                            descriptor.computationalType.operandSize
                        } else args.head match {
                            case ConstantClass(ct) ⇒
                                instructions ++= GETSTATIC(ct.asObjectType, name, descriptor)
                                if (boxed && descriptor.isBaseType)
                                    instructions ++= descriptor.asBaseType.boxValue
                                descriptor.computationalType.operandSize
                            case dc: DynamicConstant ⇒
                                doGetStatic(dc, name, descriptor, instructions, boxed)
                        }

                    case "invoke" ⇒
                        val methodHandle = args.head
                        val arguments = args.tail
                        invokeMethodHandle(methodHandle, arguments, descriptor, instructions, boxed)

                    case "nullConstant" ⇒
                        instructions ++= ACONST_NULL
                        1

                    case "primitiveClass" ⇒
                        val classType = FieldType(name).asBaseType.WrapperType
                        instructions ++= GETSTATIC(classType, "TYPE", ObjectType.Class)
                        1

                    case "staticFieldVarHandle" ⇒
                        fieldVarHandle(
                            name,
                            args.head,
                            loadClassType(args(1), instructions),
                            isStaticField = true,
                            instructions
                        )

                    case _ ⇒
                        instructions ++= dynamicLoad()
                        descriptor.computationalType.operandSize
                }

            case _ ⇒
                instructions ++= dynamicLoad()
                descriptor.computationalType.operandSize
        }

    }

    /**
     * Generate Instructions to get a VarHandle for a field.
     */
    private def fieldVarHandle(
        fieldName:      String,
        declaringClass: BootstrapArgument,
        loadFieldType:  ⇒ Int,
        isStaticField:  Boolean,
        instructions:   InstructionsBuilder
    ): Int = {
        instructions ++= INVOKESTATIC( // Get suitable MethodHandles$Lookup object
            ObjectType.MethodHandles,
            isInterface = false,
            "lookup",
            MethodDescriptor.withNoArgs(ObjectType.MethodHandles$Lookup)
        )

        val loadDeclClassStack = loadClassType(declaringClass, instructions)

        instructions ++= LoadString_W(fieldName)

        val loadFieldTypeStack = loadFieldType

        instructions ++= INVOKEVIRTUAL(
            ObjectType.MethodHandles$Lookup,
            if (isStaticField) "findStaticVarHandle" else "findVarHandle",
            MethodDescriptor.FindVarHandleDescriptor
        )

        Math.max(1 + loadDeclClassStack, 3 + loadFieldTypeStack)
    }

    /**
     * Generate instructions to load a ClassObject from the given BootstrapArgument.
     */
    private def loadClassType(
        classType:    BootstrapArgument,
        instructions: InstructionsBuilder
    ): Int = classType match {
        case ConstantClass(tpe) ⇒
            instructions ++= LoadClass_W(tpe)
            1

        case DynamicConstant(constantBSM, constantName, constantDescriptor) ⇒
            loadDynamicConstant(
                constantBSM, constantName, constantDescriptor, instructions
            )
    }

    /**
     * Generate instruction to load a Class object for the given FieldType.
     */
    private def loadFieldType(fieldType: FieldType, instructions: InstructionsBuilder): Int = {
        if (fieldType.isBaseType)
            instructions ++= GETSTATIC(fieldType.asBaseType.WrapperType, "TYPE", ObjectType.Class)
        else
            instructions ++= LoadClass_W(fieldType.asReferenceType)

        1
    }

    /**
     * Generate instructions to read a static field from a VarHandle for a dynamically loaded class.
     */
    private def doGetStatic(
        classConstant: DynamicConstant,
        name:          String,
        fieldType:     FieldType,
        instructions:  InstructionsBuilder,
        boxed:         Boolean
    ): Int = {
        val varHandleStack = fieldVarHandle( // Get a suitable Varhandle
            name,
            classConstant,
            loadFieldType(fieldType, instructions),
            isStaticField = true,
            instructions
        )
        instructions ++= ICONST_0 // The get method for a static VarHandle takes empty varargs
        instructions ++= ANEWARRAY(ObjectType.Object)
        instructions ++= INVOKEVIRTUAL(
            ObjectType.VarHandle,
            "get",
            MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
        )

        adaptReturnType(fieldType, boxed, instructions)

        Math.max(varHandleStack, 2)
    }

    /**
     * Generate instructions to invoke a given MethodHandle.
     */
    private def invokeMethodHandle(
        methodHandle: ConstantValue[_],
        arguments:    RefArray[ConstantValue[_]],
        returnType:   FieldType,
        instructions: InstructionsBuilder,
        boxed:        Boolean
    ): Int = {
        // Load the MethodHandle and keep track of its return type if we know it
        val (actualReturnType, mhStack) = methodHandle match {
            case mh: MethodCallMethodHandle ⇒
                instructions ++= LoadMethodHandle_W(mh)
                (Some(mh.methodDescriptor.returnType), 1)

            case mh: FieldReadAccessMethodHandle ⇒
                instructions ++= LoadMethodHandle_W(mh)
                (Some(mh.fieldType), 1)

            case mh: FieldWriteAccessMethodHandle ⇒
                instructions ++= LoadMethodHandle_W(mh)
                (Some(VoidType), 1)

            case DynamicConstant(constantBSM, constantName, constantDescriptor) ⇒
                val loadConstantStack = loadDynamicConstant(
                    constantBSM, constantName, constantDescriptor, instructions
                )
                (None, loadConstantStack)
        }

        // If the return type is unknown or doesn't match the required, adapt it
        val returnTypeStack = if (actualReturnType.isEmpty || actualReturnType.get != returnType) {
            instructions ++= DUP

            instructions ++= INVOKEVIRTUAL( // Get the full method type
                ObjectType.MethodHandle,
                "type",
                MethodDescriptor.withNoArgs(ObjectType.MethodType)
            )

            val fieldTypeStack = loadFieldType(returnType, instructions) // Load the new return type

            instructions ++= INVOKEVIRTUAL( // Adapt the method type
                ObjectType.MethodType,
                "changeReturnType",
                MethodDescriptor(ObjectType.Class, ObjectType.MethodType)
            )

            instructions ++= INVOKEVIRTUAL( // Adapt the method handle
                ObjectType.MethodHandle,
                "asType",
                MethodDescriptor(ObjectType.MethodType, ObjectType.MethodHandle)
            )

            1 + fieldTypeStack
        } else 0

        // Load the bootstrap arguments into a varargs array
        instructions ++= LoadConstantInstruction(arguments.size)

        instructions ++= ANEWARRAY(ObjectType.Object)

        var argumentStack = 0
        arguments.iterator.zipWithIndex foreach {
            case (argument, index) ⇒
                instructions ++= DUP
                instructions ++= LoadConstantInstruction(index)
                val bsaStack = loadBootstrapArgument(argument, instructions, boxed = true)
                instructions ++= AASTORE
                argumentStack = Math.max(argumentStack, 2 + bsaStack)
        }

        instructions ++= INVOKEVIRTUAL( // Actually invoke the method handle
            ObjectType.MethodHandle,
            "invokeWithArguments",
            MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
        )

        adaptReturnType(returnType, boxed, instructions)

        Iterator(mhStack, 1 + returnTypeStack, 2 + argumentStack).max
    }

    private def adaptReturnType(
        returnType:   FieldType,
        boxed:        Boolean,
        instructions: InstructionsBuilder
    ): Unit = {
        if (returnType.isBaseType) {
            val wrapper = returnType.asBaseType.WrapperType
            instructions ++= CHECKCAST(wrapper)
            if (!boxed)
                instructions ++= ObjectType.unboxValue(wrapper)
        } else
            instructions ++= CHECKCAST(returnType.asReferenceType)
    }
}
