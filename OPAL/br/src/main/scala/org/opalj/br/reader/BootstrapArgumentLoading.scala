/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.br.collection.mutable.InstructionsBuilder
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.ALOAD_1
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.DUP
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.IAND
import org.opalj.br.instructions.ICONST_0
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.IFNE
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.LoadClass_W
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.br.instructions.LoadDynamic2_W
import org.opalj.br.instructions.LoadDynamic_W
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadString_W
import org.opalj.br.instructions.TypeConversionInstructions
import org.opalj.br.MethodDescriptor.JustReturnsString
import org.opalj.br.cp.Constant_Pool
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.ALOAD_2
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.ASTORE_1
import org.opalj.br.instructions.ASTORE_2
import org.opalj.br.instructions.BIPUSH
import org.opalj.br.instructions.IADD
import org.opalj.br.instructions.IFEQ
import org.opalj.br.instructions.IMUL
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.LoadInt_W
import org.opalj.br.instructions.NEW

import scala.collection.immutable.ArraySeq

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
     * @param classFile The class file the constant is in
     * @param boxed If true, ensures the argument is of a reference type, boxing it if necessary
     *
     * @return The maximum stack height the generated instructions require and the (potentially
     *         new!) class file
     */
    def loadBootstrapArgument(
        argument:     ConstantValue[_],
        instructions: InstructionsBuilder,
        classFile:    ClassFile,
        boxed:        Boolean             = false
    ): (Int, ClassFile) = {
        argument match {
            case DynamicConstant(bootstrapMethod, name, descriptor) =>
                loadDynamicConstant(
                    bootstrapMethod, name, descriptor, instructions, classFile, boxed
                )
            case _ =>
                instructions ++= LoadConstantInstruction(argument, wide = true)
                if (boxed && argument.runtimeValueType.isBaseType)
                    instructions ++= argument.runtimeValueType.asBaseType.boxValue
                (argument.runtimeValueType.computationalType.operandSize, classFile)
        }
    }

    /**
     * Generate instructions to load a dynamic constant.
     *
     * @param bootstrapMethod The constant's bootstrap method
     * @param name The constant's name information, passed as the bootstrap method's 2. argument
     * @param descriptor The constant's type, passed as the bootstrap method's 3. argument
     * @param instructions The instruction builder the instructions are appended to
     * @param classFile The class file the constant is in
     * @param boxed If true, ensures the constant is of a reference type, boxing it if necessary
     *
     * @return The maximum stack height the generated instructions require and the (potentially
     *         new!) class file
     */
    def loadDynamicConstant(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      FieldType,
        instructions:    InstructionsBuilder,
        classFile:       ClassFile,
        boxed:           Boolean             = false
    ): (Int, ClassFile) = {

        def dynamicLoad(): LoadConstantInstruction[_] = {
            if (descriptor.computationalType.isCategory2) LoadDynamic2_W(bootstrapMethod, name, descriptor)
            else LoadDynamic_W(bootstrapMethod, name, descriptor)
        }

        bootstrapMethod match {
            case BootstrapMethod(InvokeStaticMethodHandle(ObjectType.ConstantBootstraps, false, methodName, _), args: ArraySeq[ConstantValue[_]] @unchecked) =>

                methodName match {
                    case "arrayVarHandle" =>
                        val maxStackAndClassFile = loadClassType(args.head, instructions, classFile)

                        instructions ++= INVOKESTATIC(
                            ObjectType.MethodHandles,
                            isInterface = false,
                            "arrayElementVarHandle",
                            MethodDescriptor(ObjectType.Class, ObjectType.VarHandle)
                        )

                        maxStackAndClassFile

                    case "enumConstant" =>
                        instructions ++= GETSTATIC(descriptor.asObjectType, name, descriptor)
                        (1, classFile)

                    case "explicitCast" =>
                        val argument = args.head
                        val argType = argument.runtimeValueType
                        val (bsaStack, updatedClassFile) =
                            loadBootstrapArgument(argument, instructions, classFile)

                        val maxStack = descriptor match {
                            case ObjectType.Object => // Nothing to do here, but box if required
                                if (boxed && argType.isBaseType)
                                    instructions ++= argType.asBaseType.boxValue
                                bsaStack

                            case rt: ReferenceType =>
                                instructions ++= CHECKCAST(rt)
                                bsaStack

                            case bt: BaseType =>
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

                        (maxStack, updatedClassFile)

                    case "fieldVarHandle" =>
                        fieldVarHandle(
                            name,
                            args.head,
                            cf => loadClassType(args(1), instructions, cf),
                            isStaticField = false,
                            instructions,
                            classFile
                        )

                    case "getStaticFinal" =>
                        if (args.isEmpty) { // Simple version loads field from class of field type
                            val declClass = if (descriptor.isObjectType) descriptor.asObjectType
                            else descriptor.asBaseType.WrapperType
                            instructions ++= GETSTATIC(declClass, name, descriptor)
                            if (boxed && descriptor.isBaseType)
                                instructions ++= descriptor.asBaseType.boxValue
                            (descriptor.computationalType.operandSize, classFile)
                        } else args.head match {
                            case ConstantClass(ct) =>
                                instructions ++= GETSTATIC(ct.asObjectType, name, descriptor)
                                if (boxed && descriptor.isBaseType)
                                    instructions ++= descriptor.asBaseType.boxValue
                                (descriptor.computationalType.operandSize, classFile)
                            case dc: DynamicConstant =>
                                doGetStatic(dc, name, descriptor, instructions, classFile, boxed)
                        }

                    case "invoke" =>
                        val methodHandle = args.head
                        val arguments = args.tail
                        invokeMethodHandle(
                            methodHandle, arguments, descriptor, instructions, classFile, boxed
                        )

                    case "nullConstant" =>
                        instructions ++= ACONST_NULL
                        (1, classFile)

                    case "primitiveClass" =>
                        val classType = FieldType(name).asBaseType.WrapperType
                        instructions ++= GETSTATIC(classType, "TYPE", ObjectType.Class)
                        (1, classFile)

                    case "staticFieldVarHandle" =>
                        fieldVarHandle(
                            name,
                            args.head,
                            cf => loadClassType(args(1), instructions, cf),
                            isStaticField = true,
                            instructions,
                            classFile
                        )

                    case _ =>
                        instructions ++= dynamicLoad()
                        (descriptor.computationalType.operandSize, classFile)
                }

            case BootstrapMethod(InvokeStaticMethodHandle(ObjectType.ObjectMethods, false, _, _), _) =>
                val newMethodName = s"$$object_methods$$${classFile.thisType.simpleName}:pc"
                val (updatedClassFile, newMethodDescriptor) = createObjectMethodsTarget(
                    bootstrapMethod.arguments, name, newMethodName, classFile
                ).getOrElse {
                        instructions ++= dynamicLoad()
                        return (descriptor.computationalType.operandSize, classFile);
                    }

                instructions ++= LoadMethodHandle_W(InvokeStaticMethodHandle(
                    classFile.thisType,
                    classFile.isInterfaceDeclaration,
                    newMethodName,
                    newMethodDescriptor
                ))

                (1, updatedClassFile)

            case _ =>
                instructions ++= dynamicLoad()
                (descriptor.computationalType.operandSize, classFile)
        }
    }

    /**
     * Generate Instructions to get a VarHandle for a field.
     */
    private def fieldVarHandle(
        fieldName:      String,
        declaringClass: BootstrapArgument,
        loadFieldType:  ClassFile => (Int, ClassFile),
        isStaticField:  Boolean,
        instructions:   InstructionsBuilder,
        classFile:      ClassFile
    ): (Int, ClassFile) = {
        instructions ++= INVOKESTATIC( // Get suitable MethodHandles$Lookup object
            ObjectType.MethodHandles,
            isInterface = false,
            "lookup",
            MethodDescriptor.withNoArgs(ObjectType.MethodHandles$Lookup)
        )

        val (loadDeclClassStack, updatedClassFile1) =
            loadClassType(declaringClass, instructions, classFile)

        instructions ++= LoadString_W(fieldName)

        val (loadFieldTypeStack, updatedClassFile2) = loadFieldType(updatedClassFile1)

        instructions ++= INVOKEVIRTUAL(
            ObjectType.MethodHandles$Lookup,
            if (isStaticField) "findStaticVarHandle" else "findVarHandle",
            MethodDescriptor.FindVarHandleDescriptor
        )

        (Math.max(1 + loadDeclClassStack, 3 + loadFieldTypeStack), updatedClassFile2)
    }

    /**
     * Generate instructions to load a ClassObject from the given BootstrapArgument.
     */
    private def loadClassType(
        classType:    BootstrapArgument,
        instructions: InstructionsBuilder,
        classFile:    ClassFile
    ): (Int, ClassFile) = classType match {
        case ConstantClass(tpe) =>
            instructions ++= LoadClass_W(tpe)
            (1, classFile)

        case DynamicConstant(constantBSM, constantName, constantDescriptor) =>
            loadDynamicConstant(
                constantBSM, constantName, constantDescriptor, instructions, classFile
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
        classFile:     ClassFile,
        boxed:         Boolean
    ): (Int, ClassFile) = {
        val (varHandleStack, updatedClassFile) = fieldVarHandle( // Get a suitable Varhandle
            name,
            classConstant,
            cf => (loadFieldType(fieldType, instructions), cf),
            isStaticField = true,
            instructions,
            classFile
        )
        instructions ++= ICONST_0 // The get method for a static VarHandle takes empty varargs
        instructions ++= ANEWARRAY(ObjectType.Object)
        instructions ++= INVOKEVIRTUAL(
            ObjectType.VarHandle,
            "get",
            MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
        )

        adaptReturnType(fieldType, boxed, instructions)

        (Math.max(varHandleStack, 2), updatedClassFile)
    }

    /**
     * Generate instructions to invoke a given MethodHandle.
     */
    private def invokeMethodHandle(
        methodHandle: ConstantValue[_],
        arguments:    ArraySeq[ConstantValue[_]],
        returnType:   FieldType,
        instructions: InstructionsBuilder,
        classFile:    ClassFile,
        boxed:        Boolean
    ): (Int, ClassFile) = {
        // Load the MethodHandle and keep track of its return type if we know it
        val (actualReturnType, mhStack, newClassFile) = methodHandle match {
            case mh: MethodCallMethodHandle =>
                instructions ++= LoadMethodHandle_W(mh)
                (Some(mh.methodDescriptor.returnType), 1, classFile)

            case mh: FieldReadAccessMethodHandle =>
                instructions ++= LoadMethodHandle_W(mh)
                (Some(mh.fieldType), 1, classFile)

            case mh: FieldWriteAccessMethodHandle =>
                instructions ++= LoadMethodHandle_W(mh)
                (Some(VoidType), 1, classFile)

            case DynamicConstant(constantBSM, constantName, constantDescriptor) =>
                val (loadConstantStack, newClassFile) = loadDynamicConstant(
                    constantBSM, constantName, constantDescriptor, instructions, classFile
                )
                (None, loadConstantStack, newClassFile)
        }

        var updatedClassFile = newClassFile

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
            case (argument, index) =>
                instructions ++= DUP
                instructions ++= LoadConstantInstruction(index)
                val (bsaStack, newClassFile) =
                    loadBootstrapArgument(argument, instructions, updatedClassFile, boxed = true)
                updatedClassFile = newClassFile
                instructions ++= AASTORE
                argumentStack = Math.max(argumentStack, 2 + bsaStack)
        }

        instructions ++= INVOKEVIRTUAL( // Actually invoke the method handle
            ObjectType.MethodHandle,
            "invokeWithArguments",
            MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
        )

        adaptReturnType(returnType, boxed, instructions)

        (Iterator(mhStack, 1 + returnTypeStack, 2 + argumentStack).max, updatedClassFile)
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

    /**
     * Creates the code required to perform the action of a method created by
     * java.lang.runtime.ObjectMethods.bootstrap.
     *
     * @param bootstrapArgs The arguments to the bootstrap method
     * @param methodName Name of the represented method (either equals, toString or hashCode)
     * @param newMethodName The name for the newly created method
     * @return Either a tuple of the updated class file and the descriptor of the new method or None
     */
    protected[this] def createObjectMethodsTarget(
        bootstrapArgs: BootstrapArguments,
        methodName:    String,
        newMethodName: String,
        classFile:     ClassFile
    ): Option[(ClassFile, MethodDescriptor)] = {
        val recordType = bootstrapArgs.head match {
            case ConstantClass(rt) => rt.asObjectType
            case _                 => return None;
        }

        val getters = bootstrapArgs.drop(2) map {
            case mh: GetFieldMethodHandle => mh
            case _                        => return None;
        }

        /*
        Make enough space for any of the methods. Exact sizes are (-5 if there is no getter):
        - equals:   16 + getters.size * 28
        - toString: 25 + getters.size * 20 + (getters.size - 1) * 6
        - hashCode:  7 + getters.size * 18
         */
        val body = new InstructionsBuilder(20 + getters.size * 28)

        def prepareArgumentArray(hasTwoParameters: Boolean = false): Unit = {
            body ++= ICONST_1
            body ++= ANEWARRAY(ObjectType.Object)
            body ++= (if (hasTwoParameters) ASTORE_2 else ASTORE_1)
        }

        // Loads a component (boxed), maxStack = 5
        def loadComponent(
            getter:           MethodHandle,
            hasTwoParameters: Boolean      = false,
            forSecondRecord:  Boolean      = false
        ): Unit = {
            body ++= LoadConstantInstruction(getter, wide = true)

            body ++= (if (hasTwoParameters) ALOAD_2 else ALOAD_1)
            body ++= DUP
            body ++= ICONST_0
            body ++= (if (forSecondRecord) ALOAD_1 else ALOAD_0)
            body ++= AASTORE

            body ++= INVOKEVIRTUAL(
                ObjectType.MethodHandle,
                "invokeWithArguments",
                MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
            )
        }

        val targetDescriptor = methodName match {
            case "equals" =>
                body ++= ALOAD_1 // if(!other instanceof recordType) return false;
                body ++= INSTANCEOF(recordType)
                body ++= IFNE(5)
                body ++= ICONST_0
                body ++= IRETURN

                if (getters.nonEmpty)
                    prepareArgumentArray(hasTwoParameters = true)

                getters.iterator.zipWithIndex foreach {
                    case (getter, index) =>
                        loadComponent(getter, hasTwoParameters = true)
                        loadComponent(getter, hasTwoParameters = true, forSecondRecord = true)

                        body ++= INVOKESTATIC(
                            ObjectType.Objects,
                            isInterface = false,
                            "equals",
                            MethodDescriptor(
                                ArraySeq(ObjectType.Object, ObjectType.Object),
                                BooleanType
                            )
                        )
                        // if(!this.component().equals(other.component()) return false;
                        body ++= IFEQ(-32 - index * 28)
                }

                body ++= ICONST_1 // return true;
                body ++= IRETURN

                MethodDescriptor(ArraySeq(recordType, ObjectType.Object), BooleanType)

            case "toString" =>
                val components = bootstrapArgs(1) match {
                    case ConstantString(cs) => cs.split(';')
                    case _                  => return None;
                }

                def appendString(s: String): Unit = {
                    body ++= LoadString_W(s)
                    body ++= INVOKEVIRTUAL(
                        ObjectType.StringBuilder,
                        "append",
                        MethodDescriptor(ObjectType.String, ObjectType.StringBuilder)
                    )
                }

                body ++= NEW(ObjectType.StringBuilder)
                body ++= DUP

                body ++= LoadString_W(recordType.simpleName + '[')
                body ++= INVOKESPECIAL(
                    ObjectType.StringBuilder,
                    isInterface = false,
                    "<init>",
                    MethodDescriptor.JustTakes(ObjectType.String)
                )

                if (getters.nonEmpty)
                    prepareArgumentArray()

                var first = true
                components.zip(getters) foreach {
                    case (component, getter) =>
                        if (first) first = false
                        else appendString(", ")

                        appendString(component + '=')

                        loadComponent(getter)
                        body ++= INVOKEVIRTUAL(
                            ObjectType.StringBuilder,
                            "append",
                            MethodDescriptor(ObjectType.Object, ObjectType.StringBuilder)
                        )
                }

                body ++= LoadInt_W(']')
                body ++= INVOKEVIRTUAL(
                    ObjectType.StringBuilder,
                    "append",
                    MethodDescriptor(CharType, ObjectType.StringBuilder)
                )

                body ++= INVOKEVIRTUAL(ObjectType.StringBuilder, "toString", JustReturnsString)
                body ++= ARETURN

                MethodDescriptor(recordType, ObjectType.String)

            case "hashCode" =>
                body ++= ICONST_0 // int hashCode = 0;

                if (getters.nonEmpty)
                    prepareArgumentArray()

                getters foreach { getter =>
                    body ++= BIPUSH(31) // hashCode = hashCode * 31 + component.hashCode();
                    body ++= IMUL

                    loadComponent(getter)

                    body ++= INVOKESTATIC(
                        ObjectType.Objects,
                        isInterface = false,
                        "hashCode",
                        MethodDescriptor.apply(ObjectType.Object, IntegerType)
                    )
                    body ++= IADD
                }

                body ++= IRETURN // return hashCode;

                MethodDescriptor(recordType, IntegerType)

            case _ =>
                return None;
        }

        val maxStack = 6 // in all methods, the max stack is 1 value + the maxStack of loadComponent
        val maxLocals = targetDescriptor.requiredRegisters + (if (getters.isEmpty) 0 else 1)

        val attributes = if ("equals" == methodName) {
            ArraySeq(StackMapTable(ArraySeq(SameFrame(7), SameFrame(1))))
        } else {
            ArraySeq.empty
        }

        val code = Code(maxStack, maxLocals, body.result(), NoExceptionHandlers, attributes)

        // Access flags for the target method are `/* SYNTHETIC */ private static`
        val accessFlags = ACC_SYNTHETIC.mask | ACC_PRIVATE.mask | ACC_STATIC.mask

        val targetMethod = Method(accessFlags, newMethodName, targetDescriptor, ArraySeq(code))

        val updatedClassFile = classFile._UNSAFE_addMethod(targetMethod)

        Some((updatedClassFile, targetDescriptor))
    }

    /**
     * Replaces each of several characters in a String with a given corresponding character.
     */
    protected[this] def replaceChars(in: String, oldChars: String, newChars: String): String = {
        var result = in
        for ((oldC, newC) <- oldChars.zip(newChars)) {
            result = result.replace(oldC, newC)
        }
        result
    }

    /**
     * Generates a new, internal name for a method to be inserted.
     *
     * It follows the pattern:
     * `$targetMethodName${surroundingMethodName}{surroundingMethodDescriptor}:pc`, where
     * surroundingMethodDescriptor is the JVM descriptor of the method sanitized to not contain
     * characters illegal in method names (replacing /, [, ;, < and > by $, ], :, _ and _
     * respectively) and where pc is the pc of the invokedynamic that is rewritten.
     */
    protected[this] def newTargetMethodName(
        cp:                               Constant_Pool,
        surroundingMethodNameIndex:       Int,
        surroundingMethodDescriptorIndex: Int,
        pc:                               Int,
        targetMethodName:                 String
    ): String = {
        val methodName = cp(surroundingMethodNameIndex).asString match {
            case "<init>"   => "$constructor$"
            case "<clinit>" => "$static_initializer"
            case name       => name
        }
        val descriptor = cp(surroundingMethodDescriptorIndex).asMethodDescriptor.toJVMDescriptor
        val sanitizedDescriptor = replaceChars(descriptor, "/[;<>", "$]:__")
        s"$$$targetMethodName$$$methodName$sanitizedDescriptor:$pc"
    }
}
