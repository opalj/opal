/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import scala.annotation.switch
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.br.MethodDescriptor.DefaultConstructorDescriptor

import scala.collection.immutable.ArraySeq

/**
 * Provides helper methods to facilitate the generation of classes.
 * In particular, functionality to create transparent proxy classes is provided.
 *
 * @author Arne Lottmann
 */
object ClassFileFactory {

    /**
     * Name used to store the final receiver object in generated proxy classes.
     */
    final val ReceiverFieldName = "$receiver"

    /**
     * This is the default name for the factory method of a proxy class that is created by
     * the [[Proxy]] method. If the name of the method to be proxified is equal to this
     * name, [[AlternativeFactoryMethodName]] is used instead.
     */
    final val DefaultFactoryMethodName = "$newInstance"

    /**
     * Alternative name for the factory method of proxy classes created by the [[Proxy]]
     * method. This name is only used if the proxified method's name is equal to
     * [[DefaultFactoryMethodName]].
     */
    final val AlternativeFactoryMethodName = "$createInstance"

    /**
     * Creates a class that acts as a proxy for the specified class.
     * The proxy implements a single method – e.g., as defined by a so-called
     * "Functional Interface" - that calls the specified method;
     * creating a proxy for `java.lang.Object`'s methods is not supported. Additionally,
     * further marker interfaces (e.g., `java.io.Serializable`) may be implemented.
     *
     * The generated class uses the following template:
     * {{{
     * class <definingType.objectType>
     *  extends <definingType.theSuperclassType>
     *  implements <definingType.theSuperinterfaceTypes> {
     *
     *  private final <ReceiverType> receiver;
     *
     *  // possible additional fields for static parameters
     *
     *  public "<init>"( <ReceiverType> receiver) { // the constructor
     *      this.receiver = receiver;
     *  }
     *
     *  public <methodDescriptor.returnType> <methodName> <methodDescriptor.parameterTypes>{
     *     return/*<= if the return type is not void*/ this.receiver.<receiverMethodName>(<parameters>)
     *  }
     *
     *  // possibly multiple bridge methods (multiple only in case of altLambdaMetaFactory usages)
     * }
     * }}}
     *
     * The class, the constructor and the method are public. The field which holds
     * the receiver object is private and final unless the receiver method is static.
     * In this case no receiver field is generated and the constructor
     * does not take an argument of the receiver's type.
     *
     * In addition to the receiver field, additional fields holding '''static parameters'''
     * are created if all parameters found in `methodDescriptor` are present, in the same
     * order, at the end of `receiverMethodDescriptor`'s parameters, but
     * `receiverMethodDescriptor` has more parameters that precede the parameters found in
     * `methodDescriptor`.
     *
     * E.g., given the following two descriptors:
     * {{{
     * val methodDescriptor =
     *  MethodDescriptor(IntegerType, IntegerType)
     * val receiverMethodDescriptor =
     *  MethodDescriptor(IndexedSeq(DoubleType, IntegerType), IntegerType)
     * }}}
     * one additional field and constructor parameter of type `double` will be created.
     * This case occurs for example with Java 8 lambda expressions that capture local
     * variables, which are prepended to the regular parameter list.
     *
     * If any of the parameters or the return type of `methodDescriptor` are
     * generic types, the generated proxy will need to create a bridge method to be valid.
     * Therefore, in these cases, `bridgeMethodDescriptor` must be specified.
     * It must be identical to `methodDescriptor` except for all occurrences of generic
     * types, which must be replaced with `ObjectType.Object`.
     * For example, consider the Java interface `java.util.Comparator` that defines the
     * generic type `T` and uses it in its `int compare(T, T)` method. This would require
     * a bridge method `int compare(Object, Object)`. The appropriate method descriptors
     * for, for example, `Comparator<String>` would be:
     * {{{
     * // Uses "String"
     * methodDescriptor =
     *  MethodDescriptor(IndexedSeq(ObjectType.String, ObjectType.String), IntegerType)
     * // Uses "Object"
     * bridgeMethodDescriptor =
     *  MethodDescriptor(IndexedSeq(ObjectType.Object, ObjectType.Object), IntegerType)
     * }}}
     *
     * The created class will always have its synthetic access flag set, as well as the
     * [[VirtualTypeFlag]] attribute.
     *
     * @note The used class file version is 52. (StackMapTables are, however, still not required
     *       because the code contains no relevant control-flow.)
     *
     * @note It is expected that `methodDescriptor` and `receiverMethodDescriptor` are
     *       "compatible", i.e., it would be possible to have the method described by
     *       `methodDescriptor` forward to `receiverMethodDescriptor`.
     *
     * This requires that for their return types, one of the following statements holds true:
     *
     * - `methodDescriptor`'s return type is [[VoidType]] (so no returning is necessary)
     * - `receiverMethodDescriptor`'s return type is assignable to `methodDescriptor`'s
     *      (e.g., a "smaller" numerical type, (un)boxable, a subtype, etc)
     * - `receiverMethodDescriptor` returns `Object`: in this case, we assume that `Object`
     *   stands for "generic return type" and expect the receiver method to return an
     *   object of a type compatible to the forwarder method's return type
     *
     * Additionally, the parameter lists must satisfy one of these conditions:
     *
     * - they are identical
     * - the descriptors have the same numbers of parameters and `methodDescriptor`'s
     *      parameter types can be widened/boxed/unboxed to match `receiverMethodDescriptor`'s
     *   parameter types
     * - `methodDescriptor`'s first parameter is of the same type as `receiverType`,
     *   and the remaining parameters are compatible to `receiverMethodDescriptor`'s
     *   entire parameter list (this is, effectively, an explicit `this` and occurs for
     *   example with references to instance methods: e.g., `String::isEmpty`, a zero
     *   argument method, could be turned into the Predicate method `test(String)`)
     * - the last `n` parameters of `receiverMethodDescriptor` are identical to the
     *   parameters of `methodDescriptor`, where `n = methodDescriptor.parametersCount`
     *   (this is the case if a lambda expression captures local variables)
     * - `receiverMethodDescriptor`'s single parameter is of type `Object[]` (in this case,
     *   `methodDescriptor`'s arguments will be collected into an `Object[]` prior to
     *   forwarding)
     *
     * Examples of compatible method descriptors are:
     * {{{
     * // ------------- First Example
     * methodDescriptor =
     *  MethodDescriptor(IntegerType, VoidType)
     * receiverMethodDescriptor =
     *  MethodDescriptor(ObjectType.Integer, VoidType)
     *  // or MethodDescriptor(ObjectType.Object, ByteType)
     *
     * // ------------- Second Example
     * methodDescriptor =
     *  MethodDescriptor(ObjectType.String, BooleanType)
     * receiverMethodDescriptor =
     *  MethodDescriptor.JustReturnsBoolean // IF receiverType == ObjectType.String
     *
     * // ------------- Third Example
     * methodDescriptor =
     *  MethodDescriptor(IndexedSeq(ByteType, ByteType, ObjectType.Integer), IntegerType)
     * receiverMethodDescriptor =
     *  MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object) // generic method
     *
     * // ------------- Fourth Example
     * methodDescriptor =
     *  MethodDescriptor(IntegerType, LongType)
     * receiverMethodDescriptor =
     *  MethodDescriptor(IndexedSeq(ByteType, ByteType, IntegerType), IntegerType)
     * }}}
     *
     * @param definingType The defining type; if the type is `Serializable`, the interface '''has
     *                     to be a direct super interface'''.
     *
     * @param invocationInstruction the opcode of the invocation instruction
     *          (`INVOKESPECIAL.opcode`,`INVOKEVIRTUAL.opcode`,
     *          `INVOKESTATIC.opcode`,`INVOKEINTERFACE.opcode`)
     *          used to call call the method on the receiver.
     */
    def Proxy(
        caller:                  ObjectType,
        callerIsInterface:       Boolean,
        definingType:            TypeDeclaration,
        methodName:              String,
        methodDescriptor:        MethodDescriptor,
        receiverType:            ObjectType,
        receiverIsInterface:     Boolean,
        implMethod:              MethodCallMethodHandle,
        invocationInstruction:   Opcode,
        samMethodType:           MethodDescriptor,
        bridgeMethodDescriptors: MethodDescriptors
    ): ClassFile = {

        val interfaceMethodParametersCount = methodDescriptor.parametersCount
        val implMethodParameters = implMethod.methodDescriptor.parameterTypes

        val receiverField =
            if (invocationInstruction == INVOKESTATIC.opcode ||
                isNewInvokeSpecial(invocationInstruction, implMethod.name) ||
                isVirtualMethodReference(
                    invocationInstruction,
                    receiverType,
                    implMethod.methodDescriptor,
                    methodDescriptor
                )) {
                ArraySeq.empty
            } else {
                ArraySeq(createField(fieldType = receiverType, name = ReceiverFieldName))
            }
        val additionalFieldsForStaticParameters =
            implMethodParameters.dropRight(interfaceMethodParametersCount).zipWithIndex.map[FieldTemplate] { p =>
                val (fieldType, index) = p
                createField(fieldType = fieldType, name = s"staticParameter$index")
            }
        val fields: ArraySeq[FieldTemplate] =
            receiverField ++ additionalFieldsForStaticParameters

        val constructor: MethodTemplate = createConstructor(definingType, fields)

        val factoryMethodName: String =
            if (methodName == DefaultFactoryMethodName) {
                AlternativeFactoryMethodName
            } else {
                DefaultFactoryMethodName
            }

        /* We don't need to analyze the type hierarchy because the "Serializable" interface is
         * directly implemented by the  defining type.
         * From "java...LambdaMetaFactory":
         *      When FLAG_SERIALIZABLE is set in flags, the function objects will implement
         *      Serializable, and will have a writeReplace method that returns an appropriate
         *      SerializedLambda. The caller class must have an appropriate $deserializeLambda$
         *      method, as described in SerializedLambda.
         */
        val isSerializable = definingType.theSuperinterfaceTypes.contains(ObjectType.Serializable)

        val methods = new Array[AnyRef](
            3 /* proxy method, constructor, factory */ +
                bridgeMethodDescriptors.length + // bridge methods
                (if (isSerializable) 2 else 0) // writeReplace and $deserializeLambda$ if Serializable
        )
        methods(0) = proxyMethod(
            definingType.objectType,
            methodName,
            methodDescriptor,
            additionalFieldsForStaticParameters,
            receiverType,
            receiverIsInterface,
            implMethod,
            invocationInstruction
        )
        methods(1) = constructor
        methods(2) = createFactoryMethod(
            definingType.objectType,
            fields.map[FieldType](_.fieldType),
            factoryMethodName
        )

        bridgeMethodDescriptors.iterator.zipWithIndex.foreach {
            case (bridgeMethodDescriptor, i) =>
                methods(3 + i) = createBridgeMethod(
                    methodName,
                    bridgeMethodDescriptor,
                    methodDescriptor,
                    definingType.objectType
                )
        }

        // Add a writeReplace and $deserializeLambda$ method if the class isSerializable
        if (isSerializable) {
            methods(3 + bridgeMethodDescriptors.length) = createWriteReplaceMethod(
                definingType,
                methodName,
                samMethodType,
                implMethod,
                methodDescriptor,
                additionalFieldsForStaticParameters
            )
            methods(4 + bridgeMethodDescriptors.length) = createDeserializeLambdaProxy(
                caller,
                callerIsInterface
            )
        }

        // We need a version 52 classfile, because prior version don't support an INVOKESTATIC
        // instruction on a static interface method.
        // Given that none of the generated methods contains any control-flow instructions
        // (gotos, ifs, switches, ...) we don't have to create a StackmapTableAttribute.
        ClassFile(
            0, 52,
            bi.ACC_SYNTHETIC.mask | bi.ACC_PUBLIC.mask | bi.ACC_SUPER.mask,
            definingType.objectType,
            definingType.theSuperclassType,
            definingType.theSuperinterfaceTypes.toArraySeq,
            fields,
            ArraySeq.unsafeWrapArray(methods).asInstanceOf[MethodTemplates],
            ArraySeq(VirtualTypeFlag)
        )
    }

    def DeserializeLambdaProxy(
        definingType:       TypeDeclaration,
        bootstrapArguments: BootstrapArguments,
        staticMethodName:   String
    ): ClassFile = {
        /*
            Instructions of LambdaDeserialize::bootstrap. This method will be reimplemented in the
            constructor of the new LambdaDeserializeProxy class.

            PC  Line  Instruction
            0   36    invokestatic java.lang.invoke.MethodHandles { java.lang.invoke.MethodHandles$Lookup lookup () }
            3   |     astore_2
            4   38    ldc java.lang.Object.class
            6   39    ldc java.lang.invoke.SerializedLambda.class
            8   |     invokestatic java.lang.invoke.MethodType { java.lang.invoke.MethodType methodType (java.lang.Class, java.lang.Class) }
            11  |     astore_3
            12  42    aload_2
            13  43    aload_0
            14  |     invokevirtual java.lang.invoke.SerializedLambda { java.lang.String getImplMethodName () }
            17  44    aload_3
            18  |     iconst_1
            19  |     anewarray java.lang.invoke.MethodHandle
            22  |     dup
            23  |     iconst_0
            24  45    aconst_null
            25  |     aastore
            26  |     invokestatic scala.runtime.LambdaDeserialize { java.lang.invoke.CallSite bootstrap (java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle[]) }
            29  |     astore 4
            31  59    aload 4
            33  |     invokevirtual java.lang.invoke.CallSite { java.lang.invoke.MethodHandle getTarget () }
            36  |     aload_0
            37  |     invokevirtual java.lang.invoke.MethodHandle { java.lang.Object invoke (java.lang.invoke.SerializedLambda) }
            40  |     areturn
            */
        // val a = new ArrayBuffer[Object](100)
        var buildMethodType: Array[Instruction] = Array() // IMPROVE [L10] Use ArrayBuffer

        bootstrapArguments.iterator.zipWithIndex.foreach { ia =>
            val (arg, idx) = ia
            val staticHandle = arg.asInstanceOf[InvokeStaticMethodHandle]

            // lookup.findStatic parameters
            def getParameterTypeInstruction(t: Type): Array[Instruction] =
                if (t.isReferenceType) {
                    Array(LDC(ConstantClass(t.asReferenceType)), null)
                } else if (t.isBaseType) {
                    // Primitive type handling
                    Array(
                        GETSTATIC(t.asBaseType.WrapperType, "TYPE", ObjectType.Class),
                        null, null
                    )
                } else {
                    // Handling for void type
                    Array(
                        GETSTATIC(
                            VoidType.WrapperType,
                            "TYPE",
                            ObjectType.Class
                        ), null, null
                    )
                }

            buildMethodType ++= Array(DUP)
            buildMethodType ++= getParameterTypeInstruction(staticHandle.methodDescriptor.returnType) // rtype

            if (staticHandle.methodDescriptor.parametersCount == 0) {
                // We have ZERO parameters, call MethodType.methodType with return parameter only
                // IMPROVE: check if LDC method type can be used
                buildMethodType ++= Array(
                    INVOKESTATIC(
                        ObjectType.MethodType,
                        false,
                        "methodType",
                        MethodDescriptor(
                            ArraySeq(ObjectType.Class),
                            ObjectType.MethodType
                        )
                    ), null, null
                )
            } else if (staticHandle.methodDescriptor.parametersCount == 1) {
                // We have ONE parameters, call MethodType.methodType with return and one parameter only
                buildMethodType ++= getParameterTypeInstruction(staticHandle.methodDescriptor.parameterType(0))
                buildMethodType ++= Array(
                    INVOKESTATIC(
                        ObjectType.MethodType,
                        false,
                        "methodType",
                        MethodDescriptor(
                            ArraySeq(ObjectType.Class, ObjectType.Class),
                            ObjectType.MethodType
                        )
                    ), null, null
                )
            } else {
                // We have MULTIPLE parameters
                buildMethodType ++= getParameterTypeInstruction(staticHandle.methodDescriptor.parameterType(0))
                buildMethodType ++= Array(
                    // The first parameter has its own parameter field
                    ICONST_4,
                    ANEWARRAY(ObjectType.Class), null, null
                )

                // The following parameters are put into an array of Class
                staticHandle.methodDescriptor.parameterTypes.tail.zipWithIndex.foreach { pt =>
                    val (param, i) = pt
                    buildMethodType ++= Array(
                        DUP,
                        BIPUSH(i), null // Use BIPUSH instead of ICONST_<i>, it is equivalent, see https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.iconst_i
                    ) ++ getParameterTypeInstruction(param) ++ Array(
                            AASTORE
                        )
                }

                buildMethodType ++= Array(
                    INVOKESTATIC(
                        ObjectType.MethodType,
                        false,
                        "methodType",
                        MethodDescriptor(
                            ArraySeq(
                                ObjectType.Class,
                                ObjectType.Class,
                                ArrayType(ObjectType.Class)
                            ),
                            ObjectType.MethodType
                        )
                    ), null, null
                )
            }

            buildMethodType ++= Array(
                LDC(ConstantString(staticHandle.name)), null, // method name
                LDC(ConstantClass(staticHandle.receiverType)), null // reference class
            )

            // Now we have to call lookup.getStatic
            buildMethodType ++= Array(
                ALOAD_2, // Load the lookup
                INVOKEVIRTUAL(
                    ObjectType.MethodHandles$Lookup,
                    "getStatic",
                    MethodDescriptor(
                        ArraySeq(
                            ObjectType.Class,
                            ObjectType.String,
                            ObjectType.MethodType
                        ),
                        ObjectType.MethodHandle
                    )
                ), null, null,
                LoadInt(idx), null,
                AASTORE
            )
        }

        val instructions: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.MethodHandles,
                    false,
                    "lookup",
                    MethodDescriptor.withNoArgs(ObjectType.MethodHandles$Lookup)
                ), null, null,
                ASTORE_2,
                LoadClass(ObjectType.Object), null,
                LoadClass(ObjectType.SerializedLambda), null,
                INVOKESTATIC(
                    ObjectType.MethodType,
                    false,
                    "methodType",
                    MethodDescriptor(
                        ArraySeq(ObjectType.Class, ObjectType.Class), ObjectType.MethodType
                    )
                ), null, null,
                ASTORE_3,
                ALOAD_2,
                ALOAD_0,
                INVOKEVIRTUAL(
                    ObjectType.SerializedLambda,
                    "getImplMethodName",
                    MethodDescriptor.JustReturnsString
                ), null, null,
                ALOAD_3,
                LoadInt(bootstrapArguments.length), null,
                ANEWARRAY(ObjectType.MethodHandle), null, null
            ) ++
                // *** START Add lookup for each argument ***
                buildMethodType ++
                // *** END Add lookup for each argument ***
                Array(
                    INVOKESTATIC(
                        ObjectType.ScalaLambdaDeserialize,
                        false,
                        "bootstrap",
                        MethodDescriptor(
                            ArraySeq(
                                ObjectType.MethodHandles$Lookup,
                                ObjectType.String,
                                ObjectType.MethodType,
                                ArrayType(ObjectType.MethodHandle)
                            ),
                            ObjectType.CallSite
                        )
                    ), null, null,
                    ASTORE(4), null,
                    ALOAD(4), null,
                    INVOKEVIRTUAL(
                        ObjectType.CallSite,
                        "getTarget",
                        MethodDescriptor.withNoArgs(ObjectType.MethodHandle)
                    ), null, null,
                    ALOAD_0,
                    INVOKEVIRTUAL(
                        ObjectType.MethodHandle,
                        "invoke",
                        MethodDescriptor(
                            ObjectType.SerializedLambda, // Parameter
                            ObjectType.Object // Return
                        )
                    ), null, null,
                    ARETURN
                )

        val maxStack = Code.computeMaxStack(instructions)

        val methods = ArraySeq(
            Method(
                bi.ACC_PUBLIC.mask | bi.ACC_STATIC.mask,
                staticMethodName,
                MethodDescriptor(
                    ArraySeq(ObjectType.SerializedLambda),
                    ObjectType.Object
                ),
                ArraySeq(
                    Code(maxStack, maxLocals = 5, instructions, NoExceptionHandlers, NoAttributes)
                )
            )
        )

        // We need a version 52 classfile, because prior version don't support an INVOKESTATIC
        // instruction on a static interface method.
        // Given that none of the generated methods contains any control-flow instructions
        // (gotos, ifs, switches, ...) we don't have to create a StackmapTableAttribute.
        ClassFile(
            0, 52,
            bi.ACC_SYNTHETIC.mask | bi.ACC_PUBLIC.mask | bi.ACC_SUPER.mask,
            definingType.objectType,
            definingType.theSuperclassType,
            definingType.theSuperinterfaceTypes.toArraySeq,
            NoFieldTemplates, // Class fields
            methods,
            ArraySeq(VirtualTypeFlag)
        )
    }

    /**
     * Returns true if the method invocation described by the given Opcode and method name
     * is a "NewInvokeSpecial" invocation (i.e., a reference to a constructor, like so:
     * `Object::new`).
     */
    def isNewInvokeSpecial(opcode: Opcode, methodName: String): Boolean = {
        opcode == INVOKESPECIAL.opcode && methodName == "<init>"
    }

    /**
     * Returns true if the given parameters identify a Java 8 method reference to an
     * instance or interface method (i.e., a reference to a virtual method, like so:
     * `ArrayList::size` or `List::size`). In this case, the resulting functional interface's method
     * has one parameter more than the referenced method because the referenced method's
     * implicit `this` parameter becomes explicit.
     */
    def isVirtualMethodReference(
        opcode:                         Opcode,
        targetMethodDeclaringType:      ObjectType,
        targetMethodDescriptor:         MethodDescriptor,
        proxyInterfaceMethodDescriptor: MethodDescriptor
    ): Boolean = {
        (opcode == INVOKEVIRTUAL.opcode || opcode == INVOKEINTERFACE.opcode) &&
            targetMethodDescriptor.parametersCount + 1 ==
            proxyInterfaceMethodDescriptor.parametersCount &&
            (proxyInterfaceMethodDescriptor.parameterType(0) eq targetMethodDeclaringType)
    }

    /**
     * Creates a field of the specified type with the given name.
     */
    def createField(
        accessFlags: Int        = bi.ACC_PRIVATE.mask | bi.ACC_FINAL.mask,
        name:        String,
        fieldType:   FieldType,
        attributes:  Attributes = NoAttributes
    ): FieldTemplate = {
        Field(accessFlags, name, fieldType, attributes)
    }

    /**
     * Creates a public constructor that initializes the given fields.
     *
     * For every `Field` in `fields` the constructor will have one parameter of the same
     * type. The parameter list will have the same order as `fields`.
     * The generated constructor will call the superclass' default constructor; i.e.,
     * the type `definingType.theSuperclassType` has to have a default constructor.
     * Additionally, bytecode is generated to populate the `fields` from the constructor
     * arguments.
     *
     * @see [[callSuperDefaultConstructor]]
     * @see [[copyParametersToInstanceFields]]
     */
    def createConstructor(
        definingType: TypeDeclaration,
        fields:       FieldTemplates
    ): MethodTemplate = {
        // it doesn't make sense that the superClassType is not defined
        val theSuperclassType = definingType.theSuperclassType.get
        val theType = definingType.objectType
        val instructions =
            callSuperDefaultConstructor(theSuperclassType) ++
                copyParametersToInstanceFields(theType, fields) :+
                RETURN
        val maxStack =
            1 /* for `this` when invoking the super constructor*/ + (
                if (fields.isEmpty)
                    0 // nothing extra needed if no fields are being set
                /*
                 * For below: we're only setting one field at a time, so we'll only need
                 * two additional stack slots if there's at least one field to be set that
                 * needs two spaces. Otherwise, we just need one more space.
                 */
                else if (fields.exists(_.fieldType.computationalType.operandSize == 2))
                    2
                else
                    1
            )
        val maxLocals = 1 + fields.iterator.map(_.fieldType.computationalType.operandSize).sum

        Method(
            bi.ACC_PUBLIC.mask,
            "<init>",
            MethodDescriptor(fields.map[FieldType](_.fieldType), VoidType),
            ArraySeq(Code(maxStack, maxLocals, instructions, NoExceptionHandlers, NoAttributes))
        )
    }

    /**
     * Returns the instructions necessary to perform a call to the constructor of the
     * given superclass.
     */
    def callSuperDefaultConstructor(theSuperclassType: ObjectType): Array[Instruction] = {
        Array(
            ALOAD_0,
            INVOKESPECIAL(theSuperclassType, false, "<init>", DefaultConstructorDescriptor),
            null, null
        )
    }

    /**
     * Creates an array of instructions that populates the given `fields` in `declaringType`
     * from local variables (constructor parameters).
     *
     * This method assumes that it creates instructions for a constructor whose parameter
     * list matches the given `fields` in terms of order and field types.
     *
     * It further assumes that none of the `fields` provided as arguments are
     * static fields, as it would make little sense to initialize static fields through
     * the constructor.
     */
    def copyParametersToInstanceFields(
        declaringType: ObjectType,
        fields:        FieldTemplates
    ): Array[Instruction] = {

        val requiredInstructions =
            computeNumberOfInstructionsForParameterLoading(fields.map[FieldType](_.fieldType), 1) +
                fields.size + // ALOAD_0  for each field
                (3 * fields.size) // PUTFIELD for each field
        val instructions = new Array[Instruction](requiredInstructions)

        var currentInstructionPC = 0
        var nextLocalVariableIndex = 1

        fields foreach { f =>
            instructions(currentInstructionPC) = ALOAD_0
            currentInstructionPC = ALOAD_0.indexOfNextInstruction(currentInstructionPC, false)
            val llvi = LoadLocalVariableInstruction(f.fieldType, nextLocalVariableIndex)
            nextLocalVariableIndex += f.fieldType.computationalType.operandSize
            instructions(currentInstructionPC) = llvi
            currentInstructionPC = llvi.indexOfNextInstruction(currentInstructionPC, false)
            val putField = PUTFIELD(declaringType, f.name, f.fieldType)
            instructions(currentInstructionPC) = putField
            currentInstructionPC = putField.indexOfNextInstruction(currentInstructionPC, false)
        }

        instructions
    }

    /**
     * Computes the number of instructions required to put the list of parameters given
     * as `fieldTypes` onto the stack.
     */
    private def computeNumberOfInstructionsForParameterLoading(
        fieldTypes:          FieldTypes,
        localVariableOffset: Int
    ): Int = {
        var numberOfInstructions = 0
        var localVariableIndex = localVariableOffset
        fieldTypes foreach { ft =>
            numberOfInstructions += (if (localVariableIndex <= 3) 1 else 2)
            localVariableIndex += ft.computationalType.operandSize
        }
        numberOfInstructions
    }

    /**
     * Creates a factory method with the appropriate instructions to create and
     * return an instance of `typeToCreate`.
     *
     * `typeToCreate` must have a constructor with a parameter list that exactly matches
     * `fieldTypes`. It also must not define a method named `factoryMethodName` with a
     * parameter list matching `fieldTypes`.
     *
     * @see [[createConstructor]]
     */
    def createFactoryMethod(
        typeToCreate:      ObjectType,
        fieldTypes:        FieldTypes,
        factoryMethodName: String
    ): MethodTemplate = {
        val numberOfInstructionsForParameterLoading: Int =
            computeNumberOfInstructionsForParameterLoading(fieldTypes, 0)
        val numberOfInstructions =
            3 + // NEW
                1 + // DUP
                numberOfInstructionsForParameterLoading +
                3 + // INVOKESPECIAL
                1 // ARETURN
        val maxLocals = fieldTypes.iterator.map(_.computationalType.operandSize.toInt).sum
        val maxStack = maxLocals + 2 // new + dup makes two extra on the stack
        val instructions = new Array[Instruction](numberOfInstructions)
        var currentPC: Int = 0
        instructions(currentPC) = NEW(typeToCreate)
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        instructions(currentPC) = DUP
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        var currentVariableIndex = 0
        fieldTypes.foreach { t =>
            val instruction = LoadLocalVariableInstruction(t, currentVariableIndex)
            currentVariableIndex += t.computationalType.operandSize
            instructions(currentPC) = instruction
            currentPC = instruction.indexOfNextInstruction(currentPC, false)
        }
        instructions(currentPC) =
            INVOKESPECIAL(typeToCreate, false, "<init>", MethodDescriptor(fieldTypes, VoidType))
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        instructions(currentPC) = ARETURN
        val body = Code(maxStack, maxLocals, instructions)

        Method(
            bi.ACC_PUBLIC.mask | bi.ACC_STATIC.mask,
            factoryMethodName,
            MethodDescriptor(fieldTypes, typeToCreate),
            ArraySeq(body)
        )
    }

    /**
     * Creates the `writeReplace` method for a lambda proxy class. It is used
     * to create a `SerializedLambda` object which holds the serialized lambda.
     *
     * The parameters of the SerializedLambda class map are as following:
     *   capturingClass = definingType
     *   functionalInterfaceClass = definingType.theSuperinterfaceTypes.head (This is the functional interface)
     *   functionalInterfaceMethodName = functionalInterfaceMethodName
     *   functionalInterfaceMethodSignature = samMethodType.parameterType
     *   implMethodKind = implMethod match to REF_
     *   implClass = implMethod.receiverType
     *   implMethodName = implMethod.name
     *   implMethodSignature = implMethod.methodDescriptor
     *   instantiatedMethodType = instantiatedMethodType
     *   capturedArgs = methodDescriptor.parameterTypes (factoryParameters, fields in Proxy)
     *
     * @see https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/SerializedLambda.html
     *      and
     *      https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/LambdaMetafactory.html#altMetafactory-java.lang.invoke.MethodHandles.Lookup-java.lang.String-java.lang.invoke.MethodType-java.lang.Object...-
     *
     * @param definingType The lambda proxyclass type.
     * @param methodName The name of the functional interface method.
     * @param samMethodType Includes the method signature for the functional interface method.
     * @param implMethod The lambda method inside the class defining the lambda.
     * @param additionalFieldsForStaticParameters The static fields that are put into the capturedArgs.
     * @return A SerializedLambda object containing all information to be able to serialize this lambda.
     */
    def createWriteReplaceMethod(
        definingType:                        TypeDeclaration,
        methodName:                          String,
        samMethodType:                       MethodDescriptor,
        implMethod:                          MethodCallMethodHandle,
        instantiatedMethodType:              MethodDescriptor,
        additionalFieldsForStaticParameters: FieldTemplates
    ): MethodTemplate = {
        var instructions: Array[Instruction] = Array(
            NEW(ObjectType.SerializedLambda), null, null,
            DUP,
            LoadClass(definingType.objectType), null,
            LDC(ConstantString(definingType.theSuperinterfaceTypes.head.fqn)), null,
            LDC(ConstantString(methodName)), null,
            LDC(ConstantString(samMethodType.toJVMDescriptor)), null,
            LDC(ConstantInteger(implMethod.referenceKind.referenceKind)), null,
            LDC(ConstantString(implMethod.receiverType.asObjectType.fqn)), null,
            LDC(ConstantString(implMethod.name)), null,
            LDC(ConstantString(implMethod.methodDescriptor.toJVMDescriptor)), null,
            LDC(ConstantString(instantiatedMethodType.toJVMDescriptor)), null,
            // Add the capturedArgs
            BIPUSH(additionalFieldsForStaticParameters.length), null,
            ANEWARRAY(ObjectType.Object), null, null
        )

        additionalFieldsForStaticParameters.zipWithIndex.foreach {
            case (x, i) =>
                instructions ++= Array(
                    DUP,
                    BIPUSH(i), null,
                    ALOAD_0,
                    GETFIELD(definingType.objectType, x.name, x.fieldType), null, null
                )
                if (x.fieldType.isBaseType) {
                    instructions ++= x.fieldType.asBaseType.boxValue
                }
                instructions :+= AASTORE
        }

        instructions ++= Array(
            INVOKESPECIAL(
                ObjectType.SerializedLambda,
                isInterface = false,
                "<init>",
                MethodDescriptor(
                    ArraySeq(
                        ObjectType.Class,
                        ObjectType.String,
                        ObjectType.String,
                        ObjectType.String,
                        IntegerType,
                        ObjectType.String,
                        ObjectType.String,
                        ObjectType.String,
                        ObjectType.String,
                        ArrayType(ObjectType.Object)
                    ),
                    VoidType
                )
            ), null, null,
            ARETURN
        )

        val maxStack = Code.computeMaxStack(instructions)
        val body = Code(maxStack, 1, instructions)
        Method(
            bi.ACC_PUBLIC.mask | bi.ACC_SYNTHETIC.mask,
            "writeReplace",
            MethodDescriptor.JustReturnsObject,
            ArraySeq(body)
        )
    }

    /**
     * Creates a static proxy method used by the `\$deserializeLambda$` method.
     *
     * @param caller The class where the lambda is implemented.
     * @param callerIsInterface `true `if the class is an interface, false if not.
     * @return The static proxy method relaying the `\$deserializedLambda$` invocation to the actual
     *         class that implements the lambda.
     */
    def createDeserializeLambdaProxy(
        caller:            ObjectType,
        callerIsInterface: Boolean
    ): MethodTemplate = {
        val deserializedLambdaMethodDescriptor = MethodDescriptor(
            ObjectType.SerializedLambda,
            ObjectType.Object
        )
        val instructions: Array[Instruction] = Array(
            ALOAD_0,
            INVOKESTATIC(
                caller,
                callerIsInterface,
                "$deserializeLambda$",
                deserializedLambdaMethodDescriptor
            ), null, null,
            ARETURN
        )
        val maxStack = Code.computeMaxStack(instructions)
        val body = Code(maxStack, 1, instructions)
        Method(
            bi.ACC_PUBLIC.mask | bi.ACC_STATIC.mask | bi.ACC_SYNTHETIC.mask,
            "$deserializeLambda$",
            deserializedLambdaMethodDescriptor,
            ArraySeq(body)
        )
    }

    /**
     * Creates a proxy method with name `methodName` and descriptor `methodDescriptor` and
     * the bytecode instructions to execute the method `receiverMethod` in `receiverType`.
     *
     * The `methodDescriptor`s have to be identical in terms of parameter types and have to
     * be compatible w.r.t. the return type.
     */
    def proxyMethod(
        definingType:          ObjectType,
        methodName:            String,
        methodDescriptor:      MethodDescriptor,
        staticParameters:      Seq[FieldTemplate],
        receiverType:          ObjectType,
        receiverIsInterface:   Boolean,
        implMethod:            MethodCallMethodHandle,
        invocationInstruction: Opcode
    ): MethodTemplate = {

        val code =
            createProxyMethodBytecode(
                definingType, methodDescriptor, staticParameters,
                receiverType, receiverIsInterface, implMethod,
                invocationInstruction
            )

        Method(bi.ACC_PUBLIC.mask, methodName, methodDescriptor, ArraySeq(code))
    }

    /**
     * Creates the bytecode instructions for the proxy method.
     *
     * These instructions will setup the stack with the variables required to call the
     * `receiverMethod`, perform the appropriate invocation instruction (one of
     * INVOKESTATIC, INVOKEVIRTUAL, or INVOKESPECIAL), and return from the proxy method.
     *
     * @see [[parameterForwardingInstructions]]
     */
    private def createProxyMethodBytecode(
        definingType:          ObjectType, // type of "this"
        methodDescriptor:      MethodDescriptor, // the parameters of the current method
        staticParameters:      Seq[FieldTemplate],
        receiverType:          ObjectType,
        receiverIsInterface:   Boolean,
        implMethod:            MethodCallMethodHandle,
        invocationInstruction: Opcode
    ): Code = {

        assert(!receiverIsInterface || invocationInstruction != INVOKEVIRTUAL.opcode)

        // If we have a constructor, we have to fix the method descriptor. Usually, the instruction
        // doesn't have a return type. For the proxy method, it is important to return the instance
        // so we have to patch the correct return type into the method descriptor.
        val fixedImplDescriptor: MethodDescriptor =
            if (implMethod.name == "<init>" && invocationInstruction == INVOKESPECIAL.opcode) {
                MethodDescriptor(implMethod.methodDescriptor.parameterTypes, receiverType)
            } else {
                implMethod.methodDescriptor
            }

        val isVirtualMethodReference = this.isVirtualMethodReference(
            invocationInstruction,
            receiverType,
            fixedImplDescriptor,
            methodDescriptor
        )
        // if the receiver method is not static, we need to push the receiver object
        // onto the stack, which we can retrieve from the receiver field on `this`
        // unless we have a method reference where the receiver will be explicitly
        // provided
        val loadReceiverObject: Array[Instruction] =
            if (invocationInstruction == INVOKESTATIC.opcode || isVirtualMethodReference) {
                Array()
            } else if (implMethod.name == "<init>") {
                Array(
                    NEW(receiverType), null, null,
                    DUP
                )
            } else {
                Array(
                    ALOAD_0,
                    GETFIELD(definingType, ReceiverFieldName, receiverType), null, null
                )
            }

        // `this` occupies variable 0, since the proxy method is never static
        val variableOffset = 1

        val forwardParametersInstructions =
            parameterForwardingInstructions(
                methodDescriptor, fixedImplDescriptor, variableOffset,
                staticParameters, definingType
            )

        val forwardingCallInstruction: Array[Instruction] = (invocationInstruction: @switch) match {
            case INVOKESTATIC.opcode =>
                Array(
                    INVOKESTATIC(
                        implMethod.receiverType.asObjectType, receiverIsInterface,
                        implMethod.name, fixedImplDescriptor
                    ), null, null
                )

            case INVOKESPECIAL.opcode =>
                val methodDescriptor =
                    if (implMethod.name == "<init>") {
                        MethodDescriptor(fixedImplDescriptor.parameterTypes, VoidType)
                    } else {
                        fixedImplDescriptor
                    }
                val invoke = INVOKESPECIAL(
                    implMethod.receiverType.asObjectType, receiverIsInterface,
                    implMethod.name, methodDescriptor
                )
                Array(invoke, null, null)

            case INVOKEINTERFACE.opcode =>
                val invoke = INVOKEINTERFACE(implMethod.receiverType.asObjectType, implMethod.name, fixedImplDescriptor)
                Array(invoke, null, null, null, null)

            case INVOKEVIRTUAL.opcode =>
                val invoke = INVOKEVIRTUAL(implMethod.receiverType, implMethod.name, fixedImplDescriptor)
                Array(invoke, null, null)
        }

        val forwardingInstructions: Array[Instruction] =
            forwardParametersInstructions ++ forwardingCallInstruction

        val returnAndConvertInstructionsArray: Array[Instruction] =
            if (methodDescriptor.returnType.isVoidType) {
                if (fixedImplDescriptor.returnType.isVoidType)
                    Array(RETURN)
                else
                    Array(
                        if (fixedImplDescriptor.returnType.operandSize == 1) POP else POP2,
                        RETURN
                    )
            } else {
                returnAndConvertInstructions(
                    methodDescriptor.returnType.asFieldType,
                    fixedImplDescriptor.returnType.asFieldType
                )
            }

        val bytecodeInstructions: Array[Instruction] =
            loadReceiverObject ++ forwardingInstructions ++ returnAndConvertInstructionsArray

        val receiverObjectStackSize =
            if (invocationInstruction == INVOKESTATIC.opcode) 0 else 1

        val parametersStackSize = implMethod.methodDescriptor.requiredRegisters

        val returnValueStackSize = methodDescriptor.returnType.operandSize

        val maxStack =
            1 + // Required if, e.g., we first have to create and initialize an object;
                // which is done by "dup"licating the new created, but not yet initialized
                // object reference on the stack.
                math.max(receiverObjectStackSize + parametersStackSize, returnValueStackSize)

        val maxLocals =
            1 +
                receiverObjectStackSize + parametersStackSize + returnValueStackSize

        Code(maxStack, maxLocals, bytecodeInstructions, NoExceptionHandlers, NoAttributes)
    }

    /**
     * Generates an array of instructions that fill the operand stack with all parameters
     * required by `receiverMethodDescriptor` from the parameters of
     * `calledMethodDescriptor`. For that reason, it is expected that both method
     * descriptors have compatible parameter and return types: i.e., that
     * `forwarderMethodDescriptor`'s parameters can be widened or (un)boxed to fit into
     * `receiverMethodDescriptor`'s parameters, and that `receiverMethodDescriptor`'s return
     * type can be widened or (un)boxed to fit into `forwarderMethodDescriptor`'s return type.
     *
     * If `receiverMethodDescriptor` has more parameters than `forwarderMethodDescriptor`,
     * the missing parameters must be provided in `staticParameters`.
     *
     * @see [[org.opalj.br.instructions.LoadLocalVariableInstruction]]
     */
    def parameterForwardingInstructions(
        forwarderMethodDescriptor: MethodDescriptor,
        receiverMethodDescriptor:  MethodDescriptor,
        variableOffset:            Int,
        staticParameters:          Seq[FieldTemplate],
        definingType:              ObjectType
    ): Array[Instruction] = try {
        val receiverParameters = receiverMethodDescriptor.parameterTypes
        val forwarderParameters = forwarderMethodDescriptor.parameterTypes

        var lvIndex = variableOffset

        val receiverTakesObjectArray =
            receiverParameters.size == 1 && receiverParameters(0) == ArrayType(ObjectType.Object)
        val callerDoesNotTakeObjectArray =
            forwarderParameters.isEmpty || forwarderParameters(0) != ArrayType(ObjectType.Object)

        if (receiverTakesObjectArray && callerDoesNotTakeObjectArray) {

            // now we need to construct a new object[] array on the stack
            // and shove everything (boxed, if necessary) in there
            val arraySize = forwarderParameters.size

            var numberOfInstructions = 3 + // need to create a new object array
                LoadConstantInstruction(arraySize).length
            forwarderParameters.zipWithIndex foreach { p =>
                val (t, i) = p
                val loadInstructions = if (lvIndex > 3) 2 else 1
                // primitive types need 3 for a boxing (invokestatic)
                val conversionInstructions = if (t.isBaseType) 3 else 0
                val storeInstructions =
                    1 + // dup
                        LoadConstantInstruction(i).length +
                        1 // aastore
                numberOfInstructions += loadInstructions + conversionInstructions +
                    storeInstructions
                lvIndex += t.computationalType.operandSize
            }

            val instructions = new Array[Instruction](numberOfInstructions)
            var nextIndex = 0

            lvIndex = variableOffset

            instructions(nextIndex) = LoadConstantInstruction(arraySize)
            nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)
            instructions(nextIndex) = ANEWARRAY(ObjectType.Object)
            nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)

            forwarderParameters.zipWithIndex foreach { p =>
                val (t, i) = p

                instructions(nextIndex) = DUP
                nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)

                instructions(nextIndex) = LoadConstantInstruction(i)
                nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)

                val llv = LoadLocalVariableInstruction(t, lvIndex)
                lvIndex += t.computationalType.operandSize

                instructions(nextIndex) = llv
                nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)

                if (t.isBaseType) {
                    // boxing always needs one instruction
                    val boxInstructions = t.asBaseType.boxValue
                    val boxInstruction = boxInstructions(0)
                    instructions(nextIndex) = boxInstruction
                    nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)
                }

                instructions(nextIndex) = AASTORE
                nextIndex = instructions(nextIndex).indexOfNextInstruction(nextIndex, false)
            }

            instructions

        } else {
            val staticParametersCount = staticParameters.size
            val forwarderParametersCount = forwarderParameters.size
            val receiverParametersCount = receiverParameters.size

            var forwarderParameterIndex = 0
            var numberOfInstructions = 4 * staticParametersCount
            var receiverParametersOffset = staticParametersCount
            if (forwarderParametersCount > receiverParametersCount) {
                /* This is the case of an implicit receiver becoming explicit in the
                 * forwarder, hence we get one instruction for loading the first
                 * parameter. Subsequently we need to ignore that parameter, though.
                 */
                numberOfInstructions += 1
                lvIndex += forwarderParameters(0).computationalType.operandSize
                forwarderParameterIndex = 1
                receiverParametersOffset = -1
            }
            while (forwarderParameterIndex < forwarderParametersCount) {
                val ft = forwarderParameters(forwarderParameterIndex)
                val rt = receiverParameters(forwarderParameterIndex + receiverParametersOffset)
                val loadInstructions = if (lvIndex > 3) 2 else 1
                lvIndex += ft.computationalType.operandSize
                val conversionInstructions =
                    if (rt != ft) {
                        if (rt.isBaseType && ft.isBaseType) {
                            if (rt.isIntLikeType && ft.isIntLikeType &&
                                rt.asIntLikeType.isWiderThan(ft.asIntLikeType)) {
                                0 // smaller int values can directly be stored in "wider" types
                            } else {
                                1 // we only do safe conversions => 1 instruction
                            }
                        } else if ((rt.isBaseType && ft.isObjectType) || (ft.isBaseType && rt.isObjectType)) {
                            // can (un)box to fit => invokestatic/invokevirtual
                            3
                        } else if (rt.isReferenceType && ft.isReferenceType && (rt ne ObjectType.Object)) {
                            3 // checkcast
                        } else 0
                    } else 0
                numberOfInstructions += loadInstructions + conversionInstructions
                forwarderParameterIndex += 1
            }

            val instructions = new Array[Instruction](numberOfInstructions)
            lvIndex = variableOffset

            var currentIndex = 0
            var receiverParameterIndex = 0
            if (receiverParametersOffset == -1) {
                // we have an explicit receiver in the forwarder parameters
                val llv = LoadLocalVariableInstruction(forwarderParameters(0), 1)
                instructions(currentIndex) = llv
                currentIndex = llv.indexOfNextInstruction(currentIndex, false)
                lvIndex += 1
            }
            while (receiverParameterIndex < receiverParametersCount) {
                val rt = receiverParameters(receiverParameterIndex)
                if (receiverParameterIndex < staticParametersCount) {
                    val sp = staticParameters(receiverParameterIndex)
                    instructions(currentIndex) = ALOAD_0
                    currentIndex = ALOAD_0.indexOfNextInstruction(currentIndex, false)
                    val gf = GETFIELD(definingType, sp.name, sp.fieldType)
                    instructions(currentIndex) = gf
                    currentIndex = gf.indexOfNextInstruction(currentIndex, false)
                } else {
                    val ft = forwarderParameters(receiverParameterIndex - receiverParametersOffset)
                    val llv = LoadLocalVariableInstruction(ft, lvIndex)
                    instructions(currentIndex) = llv
                    lvIndex += ft.computationalType.operandSize
                    currentIndex = llv.indexOfNextInstruction(currentIndex, false)

                    if (rt != ft) {
                        if (ft.isNumericType && rt.isNumericType) {
                            val conversion = ft.asNumericType.convertTo(rt.asNumericType)
                            conversion foreach { instr =>
                                instructions(currentIndex) = instr
                                currentIndex = instr.indexOfNextInstruction(currentIndex, false)
                            }
                        } else if (rt.isReferenceType && ft.isReferenceType) {
                            if (rt ne ObjectType.Object) {
                                val conversion = CHECKCAST(rt.asReferenceType)
                                instructions(currentIndex) = conversion
                                currentIndex = conversion.indexOfNextInstruction(currentIndex, false)
                            }
                        } else if (rt.isBaseType && ft.isObjectType) {
                            val unboxInstructions = ft.asObjectType.unboxValue
                            val unboxInstruction = unboxInstructions(0)
                            instructions(currentIndex) = unboxInstruction
                            currentIndex = unboxInstruction.indexOfNextInstruction(currentIndex, false)
                        } else if (ft.isBaseType && rt.isObjectType) {
                            val boxInstructions = ft.asBaseType.boxValue
                            val boxInstruction = boxInstructions(0)
                            instructions(currentIndex) = boxInstruction
                            currentIndex = boxInstruction.indexOfNextInstruction(currentIndex, false)
                        } else {
                            throw new UnknownError("Should not occur: "+ft+" -> "+rt)
                        }
                    }
                }
                receiverParameterIndex += 1
            }
            instructions
        }
    } catch {
        case t: Throwable =>
            OPALLogger.error(
                "internal error",
                s"${definingType.toJava}: failed to create parameter forwarding instructions for:\n\t"+
                    s"forwarder descriptor = ${forwarderMethodDescriptor.toJava} =>\n\t"+
                    s"receiver descriptor  = $receiverMethodDescriptor +\n\t "+
                    s"static parameters    = $staticParameters (variableOffset=$variableOffset)",
                t
            )(GlobalLogContext)
            throw t;
    }

    /**
     * Returns the instructions that return a value of type `typeToBeReturned`, converting
     * `typeOnStack` to it first if necessary.
     * If `typeOnStack` is `Object`, it will be treated as a generic return type and
     * converted to the required type.
     */
    @throws[IllegalArgumentException]("if `typeOnStack` is not compatible with `toBeReturnedType` and `typeOnStack` is not `Object`")
    def returnAndConvertInstructions(
        toBeReturnedType: FieldType,
        typeOnStack:      FieldType
    ): Array[Instruction] = {

        if (toBeReturnedType eq typeOnStack)
            return Array(ReturnInstruction(toBeReturnedType))

        val conversionInstructions: Array[Instruction] =
            if (typeOnStack eq ObjectType.Object) {
                if (toBeReturnedType.isBaseType) {
                    val baseType = toBeReturnedType.asBaseType
                    val wrapper = baseType.WrapperType
                    Array(CHECKCAST(wrapper), null, null) ++ wrapper.unboxValue
                } else {
                    Array(CHECKCAST(toBeReturnedType.asReferenceType), null, null)
                }
            } else if (typeOnStack.isObjectType && toBeReturnedType.isObjectType) {
                Array(CHECKCAST(toBeReturnedType.asObjectType), null, null)
            } else if (typeOnStack.isNumericType && toBeReturnedType.isNumericType) {
                typeOnStack.asNumericType.convertTo(toBeReturnedType.asNumericType)
            } else if (typeOnStack.isNumericType && toBeReturnedType.isObjectType) {
                typeOnStack.asNumericType.boxValue
            } else if (typeOnStack.isObjectType && toBeReturnedType.isNumericType) {
                typeOnStack.asObjectType.unboxValue
            } else if (typeOnStack.isBooleanType && toBeReturnedType.isObjectType) {
                typeOnStack.asBooleanType.boxValue
            } else if (typeOnStack.isObjectType && toBeReturnedType.isBooleanType) {
                typeOnStack.asObjectType.unboxValue
            } else if (typeOnStack.isArrayType && (typeOnStack.asArrayType.elementType eq ObjectType.Object)
                && toBeReturnedType.isArrayType &&
                typeOnStack.asArrayType.dimensions <= toBeReturnedType.asArrayType.dimensions) {
                Array(CHECKCAST(toBeReturnedType.asReferenceType), null, null)
            } else {
                throw new IllegalArgumentException(
                    s"incompatible types: ${toBeReturnedType.toJava} and ${typeOnStack.toJava}"
                )
            }
        conversionInstructions :+ ReturnInstruction(toBeReturnedType)
    }

    /**
     * Creates a bridge method using the given method descriptors, name, and type.
     *
     * The bridge method's parameter list and return type are dictated by
     * `bridgeMethodDescriptor`.
     * This method generates bytecode that invokes the method described by `methodName`
     * and `targetMethodDescriptor` on `declaringType`. If parameters need to be cast
     * before invocation, the appropriate bytecode will be generated as well.
     */
    def createBridgeMethod(
        methodName:                String,
        bridgeMethodDescriptor:    MethodDescriptor,
        targetMethodDescriptor:    MethodDescriptor,
        targetMethodDeclaringType: ObjectType
    ): MethodTemplate = {

        val bridgeMethodParameters = bridgeMethodDescriptor.parameterTypes
        val targetMethodParameters = targetMethodDescriptor.parameterTypes
        val bridgeMethodParametersCount = bridgeMethodParameters.size

        var numberOfInstructions = 1 // for ALOAD_0
        var parameterIndex = 0
        var lvIndex = 1
        while (parameterIndex < bridgeMethodParametersCount) {
            val parameter = bridgeMethodParameters(parameterIndex)
            val target = targetMethodParameters(parameterIndex)

            val loadInstructions = if (lvIndex > 3) 2 else 1
            val conversionInstructions = if (parameter != target) 3 else 0

            numberOfInstructions += loadInstructions + conversionInstructions

            parameterIndex += 1
            lvIndex += parameter.computationalType.operandSize.toInt
        }
        numberOfInstructions += 3 // invoke target method
        numberOfInstructions += 1 // return

        val instructions = new Array[Instruction](numberOfInstructions)

        var currentPC = 0
        instructions(currentPC) = ALOAD_0
        currentPC = ALOAD_0.indexOfNextInstruction(currentPC, false)

        parameterIndex = 0
        lvIndex = 1
        while (parameterIndex < bridgeMethodParametersCount) {
            val parameter = bridgeMethodParameters(parameterIndex)
            val target = targetMethodParameters(parameterIndex)

            val llv = LoadLocalVariableInstruction(parameter, lvIndex)
            instructions(currentPC) = llv
            currentPC = llv.indexOfNextInstruction(currentPC, false)

            if (parameter != target) {
                val cast = CHECKCAST(target.asReferenceType)
                instructions(currentPC) = cast
                currentPC = cast.indexOfNextInstruction(currentPC, false)
            }

            parameterIndex += 1
            lvIndex += parameter.computationalType.operandSize
        }

        val invokeTarget = INVOKEVIRTUAL(targetMethodDeclaringType, methodName,
            targetMethodDescriptor)
        instructions(currentPC) = invokeTarget
        currentPC = invokeTarget.indexOfNextInstruction(currentPC, false)

        val returnInstruction = ReturnInstruction(bridgeMethodDescriptor.returnType)
        instructions(currentPC) = returnInstruction

        val maxStack = targetMethodDescriptor.requiredRegisters + 1 //<= the receiver
        val maxLocals = maxStack + targetMethodDescriptor.returnType.operandSize

        Method(
            ACC_PUBLIC.mask | ACC_BRIDGE.mask | ACC_SYNTHETIC.mask,
            methodName,
            bridgeMethodDescriptor.parameterTypes,
            bridgeMethodDescriptor.returnType,
            ArraySeq(Code(maxStack, maxLocals, instructions, NoExceptionHandlers, NoAttributes))
        )
    }

    def cloneMethodSignature: MethodSignature = {
        MethodSignature("clone", MethodDescriptor.withNoArgs(ObjectType.Object))
    }

    def equalsMethodSignature: MethodSignature = {
        MethodSignature("equals", MethodDescriptor(ObjectType.Object, BooleanType))
    }

    def finalizeMethodSignature: MethodSignature = {
        MethodSignature("equals", MethodDescriptor.NoArgsAndReturnVoid)
    }

    def hashCodeMethodSignature: MethodSignature = {
        MethodSignature("hashCode", MethodDescriptor.withNoArgs(IntegerType))
    }

    def toStringSignature: MethodSignature = {
        MethodSignature("toString", MethodDescriptor.withNoArgs(ObjectType.String))
    }

    def nonFinalInterfaceOfObject(): Array[MethodSignature] = {
        Array(
            cloneMethodSignature,
            equalsMethodSignature,
            finalizeMethodSignature,
            hashCodeMethodSignature,
            toStringSignature
        )
    }
}
