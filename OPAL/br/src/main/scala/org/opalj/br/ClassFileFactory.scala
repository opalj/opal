/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import scala.IndexedSeq

import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_SYNTHETIC

import instructions.AASTORE
import instructions.ALOAD_0
import instructions.ANEWARRAY
import instructions.ARETURN
import instructions.CHECKCAST
import instructions.DUP
import instructions.GETFIELD
import instructions.INVOKEINTERFACE
import instructions.INVOKESPECIAL
import instructions.INVOKESTATIC
import instructions.INVOKEVIRTUAL
import instructions.Instruction
import instructions.LoadConstantInstruction
import instructions.LoadLocalVariableInstruction
import instructions.NEW
import instructions.PUTFIELD
import instructions.RETURN
import instructions.ReturnInstruction

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
    final val ReceiverFieldName = "$receiver";

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
     * The proxy implements a single, method – e.g., as defined by a so-called
     * "Functional Interface" - that calls the specified method;
     * creating a proxy for `java.lang.Object`'s methods is not supported.
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
     *  // possibly a bridge method
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
     * @note The used class file version is 49.0 (Java 5) (Using this version, we are not
     * required to create the stack map table attribute to create a valid class file.)
     *
     * @note It is expected that `methodDescriptor` and `receiverMethodDescriptor` are
     * "compatible", i.e. it would be possible to have the method described by
     * `methodDescriptor` forward to `receiverMethodDescriptor`.
     *
     * This requires that for their return types, one of the following statements holds true:
     *
     * - `methodDescriptor`'s return type is [[VoidType]] (so no returning is necessary)
     * - `receiverMethodDescriptor`'s return type is assignable to `methodDescriptor`'s
     *      (e.g. a "smaller" numerical type, (un)boxable, a subtype, etc)
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
     *   example with references to instance methods: e.g. `String::isEmpty`, a zero
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
     * @param invocationInstruction the opcode of the invocation instruction
     *          (`INVOKESPECIAL.opcode`,`INVOKEVIRTUAL.opcode`,
     *          `INVOKESTATIC.opcode`,`INVOKEINTERFACE.opcode`)
     *          used to call call the method on the receiver.
     */
    def Proxy(
        definingType: TypeDeclaration,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        invocationInstruction: Opcode,
        bridgeMethodDescriptor: Option[MethodDescriptor] = None): ClassFile = {

        val interfaceMethodParametersCount = methodDescriptor.parametersCount
        val receiverParameters = receiverMethodDescriptor.parameterTypes

        val receiverField =
            if (invocationInstruction == INVOKESTATIC.opcode ||
                isNewInvokeSpecial(invocationInstruction, receiverMethodName) ||
                isVirtualMethodReference(
                    invocationInstruction,
                    receiverType,
                    receiverMethodDescriptor,
                    methodDescriptor)) {
                IndexedSeq.empty
            } else {
                IndexedSeq(createField(fieldType = receiverType, name = ReceiverFieldName))
            }
        val additionalFieldsForStaticParameters =
            receiverParameters.dropRight(interfaceMethodParametersCount).
                zipWithIndex.map { p ⇒
                    val (fieldType, index) = p
                    createField(fieldType = fieldType, name = s"staticParameter${index}")
                }
        val fields: IndexedSeq[Field] =
            receiverField ++ additionalFieldsForStaticParameters

        val constructor: Method = createConstructor(definingType, fields)

        val factoryMethodName: String =
            if (methodName == DefaultFactoryMethodName)
                AlternativeFactoryMethodName
            else
                DefaultFactoryMethodName

        val methods: IndexedSeq[Method] = IndexedSeq(
            proxyMethod(
                definingType.objectType,
                methodName,
                methodDescriptor,
                additionalFieldsForStaticParameters,
                receiverType,
                receiverMethodName,
                receiverMethodDescriptor,
                invocationInstruction
            ),
            constructor,
            createFactoryMethod(
                definingType.objectType,
                fields.map(_.fieldType),
                factoryMethodName)
        ) ++ bridgeMethodDescriptor.map { bridgeMethodDescriptor ⇒
                IndexedSeq(createBridgeMethod(
                    methodName,
                    bridgeMethodDescriptor,
                    methodDescriptor,
                    definingType.objectType))
            }.getOrElse(IndexedSeq.empty)

        ClassFile(0, 49,
            bi.ACC_SYNTHETIC.mask | bi.ACC_PUBLIC.mask | bi.ACC_SUPER.mask,
            definingType.objectType,
            definingType.theSuperclassType,
            definingType.theSuperinterfaceTypes.toSeq,
            fields,
            methods,
            IndexedSeq(VirtualTypeFlag))
    }

    /**
     * Returns true if the method invocation described by the given Opcode and method name
     * is a "NewInvokeSpecial" invocation (i.e. a reference to a constructor, like so:
     * `Object::new`).
     */
    def isNewInvokeSpecial(opcode: Opcode, methodName: String): Boolean =
        opcode == INVOKESPECIAL.opcode && methodName == "<init>"

    /**
     * Returns true if the given parameters identify a Java 8 method reference to an
     * instance or interface method (i.e. a reference to a virtual method, like so:
     * `ArrayList::size` or `List::size`). In this case, the resulting functional interface's method
     * has one parameter more than the referenced method because the referenced method's
     * implicit `this` parameter becomes explicit.
     */
    def isVirtualMethodReference(
        opcode: Opcode,
        targetMethodDeclaringType: ObjectType,
        targetMethodDescriptor: MethodDescriptor,
        proxyInterfaceMethodDescriptor: MethodDescriptor): Boolean = {
        (opcode == INVOKEVIRTUAL.opcode || opcode == INVOKEINTERFACE.opcode) &&
            targetMethodDescriptor.parametersCount + 1 ==
            proxyInterfaceMethodDescriptor.parametersCount &&
            (proxyInterfaceMethodDescriptor.parameterType(0) eq targetMethodDeclaringType)
    }

    /**
     * Creates a field of the specified type with the given name.
     */
    def createField(
        accessFlags: Int = (bi.ACC_PRIVATE.mask | bi.ACC_FINAL.mask),
        name: String,
        fieldType: FieldType,
        attributes: Seq[Attribute] = Seq.empty): Field = {

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
        fields: IndexedSeq[Field]): Method = {
        // it doesn't make sense that the superClassType is not defined
        val theSuperclassType = definingType.theSuperclassType.get
        val theType = definingType.objectType
        val instructions =
            callSuperDefaultConstructor(theSuperclassType) ++
                copyParametersToInstanceFields(theType, fields) :+
                RETURN
        val maxStack =
            1 + ( // for `this` when invoking the super constructor
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
        val maxLocals = 1 + fields.map(_.fieldType.computationalType.operandSize).sum

        Method(
            bi.ACC_PUBLIC.mask,
            "<init>",
            MethodDescriptor(fields.map(_.fieldType), VoidType),
            Seq(Code(maxStack, maxLocals, instructions, IndexedSeq.empty, Seq.empty))
        )
    }

    /**
     * Returns the instructions necessary to perform a call to the constructor of the
     * given superclass.
     */
    def callSuperDefaultConstructor(
        theSuperclassType: ObjectType): Array[Instruction] =
        Array(
            ALOAD_0,
            INVOKESPECIAL(
                theSuperclassType,
                "<init>", NoArgumentAndNoReturnValueMethodDescriptor),
            null,
            null
        )

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
        fields: IndexedSeq[Field]): Array[Instruction] = {

        val requiredInstructions =
            computeNumberOfInstructionsForParameterLoading(fields.map(_.fieldType), 1) +
                (fields.size) + // ALOAD_0  for each field
                (3 * fields.size) // PUTFIELD for each field
        val instructions = new Array[Instruction](requiredInstructions)

        var currentInstructionPC = 0
        var nextLocalVariableIndex = 1

        fields foreach { f ⇒
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
        fieldTypes: Seq[FieldType],
        localVariableOffset: Int): Int = {
        var numberOfInstructions = 0
        var localVariableIndex = localVariableOffset
        fieldTypes foreach { ft ⇒
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
        typeToCreate: ObjectType,
        fieldTypes: IndexedSeq[FieldType],
        factoryMethodName: String): Method = {
        val numberOfInstructionsForParameterLoading: Int =
            computeNumberOfInstructionsForParameterLoading(fieldTypes, 0)
        val numberOfInstructions =
            3 + // NEW
                1 + // DUP
                numberOfInstructionsForParameterLoading +
                3 + // INVOKESPECIAL
                1 // ARETURN
        val maxLocals = fieldTypes.map(_.computationalType.operandSize.toInt).sum
        val maxStack = maxLocals + 2 // new + dup makes two extra on the stack
        val instructions = new Array[Instruction](numberOfInstructions)
        var currentPC: Int = 0
        instructions(currentPC) = NEW(typeToCreate)
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        instructions(currentPC) = DUP
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        var currentVariableIndex = 0
        fieldTypes.foreach { t ⇒
            val instruction = LoadLocalVariableInstruction(t, currentVariableIndex)
            currentVariableIndex += t.computationalType.operandSize
            instructions(currentPC) = instruction
            currentPC = instruction.indexOfNextInstruction(currentPC, false)
        }
        instructions(currentPC) = INVOKESPECIAL(
            typeToCreate,
            "<init>",
            MethodDescriptor(fieldTypes, VoidType))
        currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
        instructions(currentPC) = ARETURN
        val body = Code(
            maxStack, maxLocals,
            instructions,
            IndexedSeq.empty, Seq.empty
        )

        Method(
            bi.ACC_PUBLIC.mask | bi.ACC_STATIC.mask,
            factoryMethodName,
            MethodDescriptor(fieldTypes, typeToCreate),
            Seq(body)
        )
    }

    /**
     * Creates a proxy method with name `methodName` and descriptor `methodDescriptor` and
     * the bytecode instructions to execute the method `receiverMethod` in `receiverType`.
     *
     * It is expected that `methodDescriptor` and `receiverMethodDescriptor` are identical
     * in terms of parameter types and return type.
     */
    def proxyMethod(
        definingType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        staticParameters: Seq[Field],
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        invocationInstruction: Opcode): Method = {

        val code =
            createProxyMethodBytecode(
                definingType, methodName, methodDescriptor, staticParameters,
                receiverType, receiverMethodName, receiverMethodDescriptor,
                invocationInstruction)

        Method(bi.ACC_PUBLIC.mask, methodName, methodDescriptor, Seq(code))
    }

    /**
     * Creates the bytecode instructions for the proxy method.
     *
     * These instructions will setup the stack with the variables required to call the
     * `receiverMethod`, perform the appropriate invocation instruction (one of
     * INVOKESTATIC, INVOKEVIRTUAL, or INVOKESPECIAL), and return from the proxy method.
     *
     * It is expected that `methodDescriptor` and `receiverMethodDescriptor` are identical
     * in terms of parameter types and return type.
     *
     * @see [[parameterForwardingInstructions]]
     */
    private def createProxyMethodBytecode(
        definingType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        staticParameters: Seq[Field],
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        invocationInstruction: Opcode): Code = {

        // if the receiver method is not static, we need to push the receiver object
        // onto the stack, which we can retrieve from the receiver field on `this`
        val loadReceiverObject: Array[Instruction] =
            if (invocationInstruction == INVOKESTATIC.opcode ||
                isVirtualMethodReference(
                    invocationInstruction,
                    receiverType,
                    receiverMethodDescriptor,
                    methodDescriptor)) {
                Array()
            } else if (receiverMethodName == "<init>") {
                Array(
                    NEW(receiverType),
                    null,
                    null,
                    DUP
                )
            } else {
                Array(
                    ALOAD_0,
                    GETFIELD(definingType, ReceiverFieldName, receiverType),
                    null,
                    null
                )
            }

        // `this` occupies variable 0, since the proxy method is never static
        val variableOffset = 1

        val forwardParametersInstructions = parameterForwardingInstructions(
            methodDescriptor, receiverMethodDescriptor, variableOffset,
            staticParameters, definingType)

        val forwardingCallInstruction: Array[Instruction] =
            (invocationInstruction: @scala.annotation.switch) match {
                case INVOKESTATIC.opcode ⇒
                    Array(
                        INVOKESTATIC(receiverType, receiverMethodName, receiverMethodDescriptor),
                        null,
                        null
                    )
                case INVOKESPECIAL.opcode ⇒
                    val methodDescriptor =
                        if (receiverMethodName == "<init>") {
                            MethodDescriptor(
                                receiverMethodDescriptor.parameterTypes,
                                VoidType
                            )
                        } else {
                            receiverMethodDescriptor
                        }
                    Array(
                        INVOKESPECIAL(receiverType, receiverMethodName, methodDescriptor),
                        null,
                        null
                    )
                case INVOKEINTERFACE.opcode ⇒
                    Array(
                        INVOKEINTERFACE(receiverType, receiverMethodName, receiverMethodDescriptor),
                        null,
                        null,
                        null,
                        null
                    )
                case INVOKEVIRTUAL.opcode ⇒
                    Array(
                        INVOKEVIRTUAL(receiverType, receiverMethodName, receiverMethodDescriptor),
                        null,
                        null
                    )
            }

        val forwardingInstructions: Array[Instruction] =
            forwardParametersInstructions ++ forwardingCallInstruction

        val returnAndConvertInstructionsArray: Array[Instruction] =
            if (methodDescriptor.returnType.isVoidType)
                Array(RETURN)
            else
                returnAndConvertInstructions(
                    methodDescriptor.returnType.asFieldType,
                    receiverMethodDescriptor.returnType.asFieldType)

        val bytecodeInstructions: Array[Instruction] =
            loadReceiverObject ++ forwardingInstructions ++ returnAndConvertInstructionsArray

        val receiverObjectStackSize =
            if (loadReceiverObject.isEmpty) {
                0
            } else {
                1
            }

        val parametersStackSize =
            receiverMethodDescriptor.parameterTypes.map(_.computationalType.operandSize).sum

        val returnValueStackSize =
            if (methodDescriptor.returnType != VoidType) {
                methodDescriptor.returnType.computationalType.operandSize.toInt
            } else {
                0
            }

        val maxStack =
            math.max(
                (receiverObjectStackSize) + parametersStackSize,
                returnValueStackSize)

        val maxLocals = 1 + receiverObjectStackSize + parametersStackSize +
            returnValueStackSize

        Code(maxStack, maxLocals,
            bytecodeInstructions,
            IndexedSeq.empty,
            Seq.empty)
    }

    /**
     * Generates an array of instructions that fill the operand stack with all parameters
     * required by `receiverMethodDescriptor` from the parameters of
     * `calledMethodDescriptor`. For that reason, it is expected that both method
     * descriptors have compatible parameter and return types, i.e. that
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
        receiverMethodDescriptor: MethodDescriptor,
        variableOffset: Int,
        staticParameters: Seq[Field],
        definingType: ObjectType): Array[Instruction] = {

        val receiverParameters = receiverMethodDescriptor.parameterTypes
        val forwarderParameters = forwarderMethodDescriptor.parameterTypes

        var lvIndex = variableOffset

        val receiverTakesObjectArray = receiverParameters.size == 1 &&
            receiverParameters(0) == ArrayType(ObjectType.Object)
        val callerDoesNotTakeObjectArray = forwarderParameters.size == 0 ||
            forwarderParameters(0) != ArrayType(ObjectType.Object)

        if (receiverTakesObjectArray && callerDoesNotTakeObjectArray) {

            // now we need to construct a new object[] array on the stack
            // and shove everything (boxed, if necessary) in there
            val arraySize = forwarderParameters.size

            var numberOfInstructions = 3 + // need to create a new object array
                LoadConstantInstruction(arraySize).length
            forwarderParameters.zipWithIndex foreach { p ⇒
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

            forwarderParameters.zipWithIndex foreach { p ⇒
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
                    val box = t.asBaseType.boxValue(0)
                    instructions(nextIndex) = box
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
                                0
                                // there's no need to convert from a smaller 
                                //integer type to a larger one
                            } else {
                                1 // we only do safe conversions => 1 instruction
                            }
                        } else if ((rt.isBaseType && ft.isObjectType) ||
                            (ft.isBaseType && rt.isObjectType)) {
                            // can (un)box to fit => invokestatic/invokevirtual
                            3
                        } else if (rt.isReferenceType &&
                            ft.isReferenceType &&
                            rt != ObjectType.Object) {
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
                            conversion foreach { instr ⇒
                                instructions(currentIndex) = instr
                                currentIndex = instr.indexOfNextInstruction(currentIndex, false)
                            }
                        } else if (rt.isReferenceType && ft.isReferenceType) {
                            val conversion = CHECKCAST(rt.asReferenceType)
                            instructions(currentIndex) = conversion
                            currentIndex = conversion.indexOfNextInstruction(currentIndex, false)
                        } else if (rt.isBaseType && ft.isObjectType) {
                            val conv = ft.asObjectType.unboxValue(0)
                            instructions(currentIndex) = conv
                            currentIndex = conv.indexOfNextInstruction(currentIndex, false)
                        } else if (ft.isBaseType && rt.isObjectType) {
                            val conv = ft.asBaseType.boxValue(0)
                            instructions(currentIndex) = conv
                            currentIndex = conv.indexOfNextInstruction(currentIndex, false)
                        } else throw new UnknownError("Should not occur: "+ft+" → "+rt)
                    }
                }
                receiverParameterIndex += 1
            }

            instructions
        }
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
        typeOnStack: FieldType): Array[Instruction] = {

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
            } else {
                throw new IllegalArgumentException(
                    "types are incompatible: "+
                        s"${toBeReturnedType.toJava} and ${typeOnStack.toJava}"
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
        methodName: String,
        bridgeMethodDescriptor: MethodDescriptor,
        targetMethodDescriptor: MethodDescriptor,
        targetMethodDeclaringType: ObjectType): Method = {

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
            lvIndex = parameter.computationalType.operandSize.toInt
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

        val maxStack =
            targetMethodDescriptor.parameterTypes.map(
                _.computationalType.operandSize).sum + 1
        val maxLocals =
            maxStack +
                {
                    val returnType = targetMethodDescriptor.returnType
                    if (!returnType.isVoidType)
                        returnType.computationalType.operandSize.toInt
                    else
                        0
                }

        Method(
            ACC_PUBLIC.mask | ACC_BRIDGE.mask | ACC_SYNTHETIC.mask,
            methodName,
            bridgeMethodDescriptor.parameterTypes,
            bridgeMethodDescriptor.returnType,
            Seq(Code(maxStack, maxLocals, instructions, IndexedSeq.empty, Seq.empty))
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
