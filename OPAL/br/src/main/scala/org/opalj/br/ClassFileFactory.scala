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

import instructions._

/**
 * Provides a method to generate a proxy class file that implements a single method.
 *
 * @author Arne Lottmann
 */
object ClassFileFactory {

    final val ReceiverFieldName = "receiver";

    /**
     * Creates a class that acts as a proxy for the specified class and that implements
     * a single method that calls the specified method.
     *
     * I.e., a class is generated using the following template:
     * {{{
     * class <definingType.objectType>
     *  extends <definingType.theSuperclassType>
     *  implements <definingType.theSuperinterfaceTypes> {
     *
     *  private final <ReceiverType> receiver;
     *
     *  public "<init>"( <ReceiverType> receiver) { // the constructor
     *      this.receiver = receiver;
     *  }
     *
     *  public <methodDescriptor.returnType> <methodName> <methodDescriptor.parameterTypes>{
     *     return/*<= if the return type is not void*/ this.receiver.<receiverMethodName>(<parameters>)
     *  }
     *  }
     * }}}
     * The class, the constructor and the method will be public. The field which holds
     * the receiver object is private.
     *
     * If the receiver method is static, an empty '''default constructor''' is created
     * and no field is generated.
     *
     * The synthetic access flag is always set, as well as the [[VirtualTypeFlag]]
     * attribute.
     *
     * The used class file version is 49.0 (Java 5) (Using this version, we are not
     * required to create the stack map table attribute to create a valid class file.)
     */
    def Proxy(
        definingType: TypeDeclaration,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        isStaticCall: Boolean,
        isPrivateCall: Boolean,
        isInterfaceCall: Boolean): ClassFile = {

        val fields: IndexedSeq[Field] =
            if (isStaticCall) {
                IndexedSeq.empty
            } else {
                IndexedSeq(createField(fieldType = receiverType, name = ReceiverFieldName))
            }

        val constructor: Method = createConstructor(definingType, fields)

        val methods: IndexedSeq[Method] = IndexedSeq(
            proxyMethod(
                definingType.objectType,
                methodName,
                methodDescriptor,
                receiverType,
                receiverMethodName,
                receiverMethodDescriptor,
                isStaticCall,
                isPrivateCall,
                isInterfaceCall
            ),
            constructor
        )

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
     * Creates a field of the specified type with the given name.
     */
    def createField(
        accessFlags: Int = (bi.ACC_PRIVATE.mask | bi.ACC_FINAL.mask),
        name: String,
        fieldType: FieldType,
        attributes: Seq[Attribute] = Seq.empty): Field = {

        Field(accessFlags, name, fieldType, Seq.empty)
    }

    /**
     * Creates the proxy class's constructor.
     *
     * If `fields` is not empty, the constructor will have parameters
     * corresponding to these fields in the order given by the sequence. Furthermore, the
     * constructor will contain bytecode instructions that place the parameters into these
     * fields.
     *
     * @see [[putParametersToFieldsInstructions]]
     */
    private[br] def createConstructor(
        definingType: TypeDeclaration,
        fields: IndexedSeq[Field]): Method = {
        // it doesn't make sense that the superClassType is not defined
        val theSuperclassType = definingType.theSuperclassType.get
        val theType = definingType.objectType
        val instructions =
            callSuperDefaultConstructor(theSuperclassType) ++
                putParametersToFieldsInstructions(theType, fields) :+
                RETURN
        val maxStack =
            if (fields.isEmpty)
                0
            else
                1 + fields.map(_.fieldType.computationalType.operandSize).max
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
    private def callSuperDefaultConstructor(
        theSuperclassType: ObjectType): Array[Instruction] =
        Array(
            ALOAD_0,
            INVOKESPECIAL(theSuperclassType,
                "<init>",
                NoArgumentAndNoReturnValueMethodDescriptor)
        )

    /**
     * Creates an array of instructions that populate the given `fields` in `declaringType`
     * from local variables (constructor parameters).
     *
     * This method assumes that it creates instructions for a constructor whose parameter
     * list matches the given `fields` in terms of order and field types.
     *
     * This method further assumes that none of the `fields` provided as arguments are
     * static fields, as it would make little sense to initialize static fields through
     * the constructor.
     */
    private def putParametersToFieldsInstructions(
        declaringType: ObjectType,
        fields: IndexedSeq[Field]): Array[Instruction] = {

        val variablesWithBytecodeIndices: Array[(Field, Int)] = new Array(fields.size)

        // variable index starts at 1 because 0 is `this`
        fields.zipWithIndex.foldLeft(1) { (variableIndex, fieldWithIndex) ⇒
            val (field, index) = fieldWithIndex
            variablesWithBytecodeIndices(index) = (field, variableIndex)

            // advance the variableIndex for the next iteration of the foldLeft method
            // by the amount of words (1 or 2) taken up by this parameter
            variableIndex + field.fieldType.computationalType.operandSize
        }

        variablesWithBytecodeIndices.flatMap { fieldWithIndex ⇒
            val (field, variableIndex) = fieldWithIndex
            val array = new Array[Instruction](if (variableIndex > 3) 4 else 3)
            array(0) = ALOAD_0
            val llv = LoadLocalVariableInstruction(field.fieldType, variableIndex)
            array(1) = llv
            val nextIndex = llv.indexOfNextInstruction(1, false)
            array(nextIndex) = PUTFIELD(declaringType, field.name, field.fieldType)
            array
        }
    }

    /**
     * Creates a proxy method with name `methodName` and descriptor `methodDescriptor` and
     * the bytecode instructions to execute the method `receiverMethod` in `receiverType`.
     *
     * It is expected that `methodDescriptor` and `receiverMethodDescriptor` are identical
     * in terms of parameter types and return type.
     *
     * @see [[createProxyMethodBytecode]], [[parameterForwardingInstructions]]
     */
    private def proxyMethod(
        definingType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        isStaticCall: Boolean,
        isPrivateCall: Boolean,
        isInterfaceCall: Boolean): Method = {

        val code =
            createProxyMethodBytecode(
                definingType, methodName, methodDescriptor,
                receiverType, receiverMethodName, receiverMethodDescriptor,
                isStaticCall, isPrivateCall, isInterfaceCall)

        Method(bi.ACC_PUBLIC.mask, methodName, methodDescriptor, Seq(code))
    }

    /**
     * Creates the bytecode instructions for the proxy method.
     *
     * These instructions will populate the stack with the variables required to call the
     * `receiverMethod`, perform the appropriate invocation instruction (one of
     * INVOKESTATIC, INVOKEVIRTUAL, or INVOKESPECIAL), and return from the proxy method.
     *
     * It is expected that `methodDescriptor` and `receiverMethodDescriptor` are identical
     * in terms of parameter types and return type.
     *
     * @see [[parameterForwardingInstructions]]
     */
    private[br] def createProxyMethodBytecode(
        definingType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        receiverType: ObjectType,
        receiverMethodName: String,
        receiverMethodDescriptor: MethodDescriptor,
        isStaticCall: Boolean,
        isPrivateCall: Boolean,
        isInterfaceCall: Boolean): Code = {

        // if the receiver method is not static, we need to push the receiver object
        // onto the stack, which we can retrieve from the receiver field on `this`
        val loadReceiverObject: Array[Instruction] =
            if (isStaticCall) {
                Array()
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
            methodDescriptor, receiverMethodDescriptor, variableOffset)

        val forwardingCallInstruction: Array[Instruction] =
            if (isStaticCall) {
                Array(
                    INVOKESTATIC(receiverType, receiverMethodName, receiverMethodDescriptor),
                    null,
                    null
                )
            } else if (isPrivateCall) {
                Array(
                    INVOKESPECIAL(receiverType, receiverMethodName, receiverMethodDescriptor),
                    null,
                    null
                )
            } else if (isInterfaceCall) {
                Array(
                    INVOKEINTERFACE(receiverType, receiverMethodName, receiverMethodDescriptor),
                    null,
                    null,
                    null,
                    null
                )
            } else {
                Array(
                    INVOKEVIRTUAL(receiverType, receiverMethodName, receiverMethodDescriptor),
                    null,
                    null
                )
            }

        val forwardingInstructions: Array[Instruction] =
            forwardParametersInstructions ++ forwardingCallInstruction

        val returnInstruction: Instruction = ReturnInstruction(methodDescriptor.returnType)

        val bytecodeInstructions: Array[Instruction] =
            loadReceiverObject ++ forwardingInstructions :+ returnInstruction

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
                methodDescriptor.returnType.computationalType.operandSize
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
     * descriptors are identical in terms of parameters and return types.
     *
     * @see [[LoadLocalVariableInstruction]]
     */
    private def parameterForwardingInstructions(
        calledMethodDescriptor: MethodDescriptor,
        receiverMethodDescriptor: MethodDescriptor,
        variableOffset: Int): Array[Instruction] = {

        var lvIndex = variableOffset
        receiverMethodDescriptor.parameterTypes.flatMap { pt ⇒
            val array = new Array[Instruction](if (lvIndex > 3) 2 else 1)
            array(0) = LoadLocalVariableInstruction(pt, lvIndex)
            lvIndex += pt.computationalType.operandSize
            array
        }.toArray
    }
}