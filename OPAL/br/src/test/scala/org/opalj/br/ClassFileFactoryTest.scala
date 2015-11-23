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
import org.junit.runner.RunWith
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.TestSupport.locateTestResources
import org.scalatest.FunSpec
import org.scalatest.Matchers
import analyses.Project
import instructions.AASTORE
import instructions.ALOAD
import instructions.ALOAD_0
import instructions.ALOAD_1
import instructions.ALOAD_2
import instructions.ALOAD_3
import instructions.ANEWARRAY
import instructions.ARETURN
import instructions.BIPUSH
import instructions.CHECKCAST
import instructions.DLOAD
import instructions.DLOAD_0
import instructions.DLOAD_1
import instructions.DLOAD_2
import instructions.DLOAD_3
import instructions.DRETURN
import instructions.DUP
import instructions.F2D
import instructions.FLOAD
import instructions.FLOAD_0
import instructions.FLOAD_1
import instructions.FLOAD_2
import instructions.FLOAD_3
import instructions.FRETURN
import instructions.GETFIELD
import instructions.I2L
import instructions.I2S
import instructions.ICONST_0
import instructions.ICONST_1
import instructions.ICONST_2
import instructions.ICONST_3
import instructions.ICONST_4
import instructions.ICONST_5
import instructions.ILOAD
import instructions.ILOAD_0
import instructions.ILOAD_1
import instructions.ILOAD_2
import instructions.ILOAD_3
import instructions.INVOKEINTERFACE
import instructions.INVOKESPECIAL
import instructions.INVOKESTATIC
import instructions.INVOKEVIRTUAL
import instructions.IRETURN
import instructions.Instruction
import instructions.InvocationInstruction
import instructions.L2D
import instructions.L2F
import instructions.LLOAD
import instructions.LLOAD_0
import instructions.LLOAD_1
import instructions.LLOAD_2
import instructions.LLOAD_3
import instructions.LRETURN
import instructions.LoadLocalVariableInstruction
import instructions.NEW
import instructions.PUTFIELD
import instructions.RETURN
import instructions.ReturnInstruction
import org.scalatest.junit.JUnitRunner
import org.opalj.br.instructions.INVOKEDYNAMIC

/**
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ClassFileFactoryTest extends FunSpec with Matchers {
    val testProject = Project(locateTestResources("classfiles/proxy.jar", "br"))
    val lambdasProject = Project(locateTestResources("classfiles/Lambdas.jar", "br"))

    val StaticMethods = ObjectType("proxy/StaticMethods")
    val InstanceMethods = ObjectType("proxy/InstanceMethods")
    val Constructors = ObjectType("proxy/Constructors")
    val PrivateInstanceMethods = ObjectType("proxy/PrivateInstanceMethods")
    val InterfaceMethods = ObjectType("proxy/InterfaceMethods")

    private def getMethods(
        theClass:   ObjectType,
        repository: ClassFileRepository
    ): Iterable[(ObjectType, Method)] =
        repository.classFile(theClass).map { cf ⇒
            cf.methods.map((theClass, _))
        }.getOrElse(Iterable.empty)

    private def checkAndReturnMethod(
        classFile: ClassFile
    )(
        filter: Method ⇒ Boolean
    ): Method = {
        val methods = classFile.methods.filter(filter)
        methods should have size (1)
        val method = methods.head
        method.body should be('defined)
        method
    }

    private def checkAndReturnConstructor(classFile: ClassFile): Method =
        checkAndReturnMethod(classFile)(_.isConstructor)

    private def checkAndReturnFactoryMethod(classFile: ClassFile): Method =
        checkAndReturnMethod(classFile)(m ⇒
            m.isStatic && m.isPublic &&
                (m.name == "$newInstance" || m.name == "$createInstance"))

    private def checkAndReturnForwardingMethod(classFile: ClassFile): Method =
        checkAndReturnMethod(classFile)(m ⇒
            !(m.isConstructor || m.name == "$newInstance" || m.name == "$createInstance"))

    describe("ClassFileFactory") {

        describe("should be able to proxify methods") {
            val instanceMethods = getMethods(InstanceMethods, testProject)
            instanceMethods should not be ('empty)
            val constructors = getMethods(Constructors, testProject)
            constructors should not be ('empty)
            val privateInstanceMethods = getMethods(PrivateInstanceMethods, testProject)
            privateInstanceMethods should not be ('empty)
            val interfaceMethods = getMethods(InterfaceMethods, testProject)
            interfaceMethods should not be ('empty)
            val staticMethods = getMethods(StaticMethods, testProject)
            staticMethods should not be ('empty)
            val methods: Iterable[(ObjectType, Method)] = instanceMethods ++
                constructors ++ privateInstanceMethods ++ interfaceMethods ++
                staticMethods

            it("with one instance field for instance methods, none for static") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    if (calleeTypeAndMethod._2.isStatic) {
                        classFile.fields should have size (0)
                    } else {
                        classFile.fields should have size (1)
                        val field = classFile.fields(0)
                        bi.ACC_FINAL.isSet(field.accessFlags) should be(true)
                        bi.ACC_FINAL.isSet(field.accessFlags) should be(true)
                        field.fieldType should be(calleeTypeAndMethod._1)
                    }
                }
            }

            it("and a constructor that sets that instance field (if present)") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    if (!calleeTypeAndMethod._2.isStatic) {
                        val constructor = checkAndReturnConstructor(classFile)
                        bi.ACC_PUBLIC.isSet(constructor.accessFlags) should be(true)
                        constructor.parameterTypes should have size (1)
                        constructor.parameterTypes(0) should be(calleeTypeAndMethod._1)

                        constructor.body should be('defined)
                        val body = constructor.body.get
                        val instructions = body.instructions
                        body.maxLocals should be(2)
                        body.maxStack should be(2)
                        instructions should be(Array(
                            ALOAD_0,
                            INVOKESPECIAL(
                                ObjectType.Object,
                                "<init>",
                                NoArgumentAndNoReturnValueMethodDescriptor
                            ),
                            null,
                            null,
                            ALOAD_0,
                            ALOAD_1,
                            PUTFIELD(
                                classFile.thisType,
                                classFile.fields(0).name,
                                classFile.fields(0).fieldType
                            ),
                            null,
                            null,
                            RETURN
                        ))
                    }
                }
            }

            it("and one static factory that calls the constructor") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    val factoryMethod = checkAndReturnFactoryMethod(classFile)
                    val constructor = checkAndReturnConstructor(classFile)
                    factoryMethod.descriptor should be(MethodDescriptor(
                        constructor.parameterTypes, classFile.thisType
                    ))
                    val maxLocals = factoryMethod.parameterTypes.map(_.computationalType.operandSize).sum
                    val maxStack = maxLocals + 2 // new + dup makes two extra on the stack
                    var currentVariableIndex = 0
                    val loadParametersInstructions: Array[Instruction] =
                        factoryMethod.parameterTypes.flatMap { t ⇒
                            val instruction = LoadLocalVariableInstruction(t, currentVariableIndex)
                            currentVariableIndex += t.computationalType.operandSize
                            if (currentVariableIndex > 3) Array(instruction, null) else Array(instruction)
                        }.toArray
                    val body = factoryMethod.body.get
                    body.maxStack should be(maxStack)
                    body.maxLocals should be(maxLocals)
                    body.instructions should be(
                        Array(
                            NEW(classFile.thisType),
                            null,
                            null,
                            DUP
                        ) ++
                            loadParametersInstructions ++
                            Array(
                                INVOKESPECIAL(classFile.thisType, "<init>", constructor.descriptor),
                                null,
                                null,
                                ARETURN
                            )
                    )
                }
            }

            it("and one forwarding method") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    bi.ACC_PUBLIC.isSet(method.accessFlags) should be(true)
                }
            }

            it("that calls the callee method with the appropriate invokeX instruction") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    if (calleeMethod.isStatic) {
                        body.instructions should contain(INVOKESTATIC(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ))
                    } else if (testProject.classFile(calleeType).get.isInterfaceDeclaration) {
                        body.instructions should contain(INVOKEINTERFACE(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ))
                    } else if (calleeMethod.isPrivate) {
                        body.instructions should contain(INVOKESPECIAL(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ))
                    } else {
                        body.instructions should contain(INVOKEVIRTUAL(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ))
                    }
                }
            }

            it("and passes all parameters correctly [barring reference type check]") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    var currentInstruction = 0
                    val body = method.body.get
                    val instructions = body.instructions

                    val parameters =
                        if (calleeTypeAndMethod._2.isStatic) {
                            method.parameterTypes
                        } else {
                            calleeTypeAndMethod._1 +: method.parameterTypes
                        }
                    parameters.foreach { requiredParameter ⇒
                        val remainingInstructions =
                            instructions.slice(currentInstruction, instructions.size)
                        val consumedInstructions =
                            requiredParameter match {
                                case IntegerType      ⇒ requireInt(remainingInstructions)
                                case ShortType        ⇒ requireInt(remainingInstructions)
                                case ByteType         ⇒ requireInt(remainingInstructions)
                                case CharType         ⇒ requireInt(remainingInstructions)
                                case BooleanType      ⇒ requireInt(remainingInstructions)
                                case FloatType        ⇒ requireFloat(remainingInstructions)
                                case DoubleType       ⇒ requireDouble(remainingInstructions)
                                case LongType         ⇒ requireLong(remainingInstructions)
                                case _: ReferenceType ⇒ requireReference(remainingInstructions)
                            }
                        currentInstruction += consumedInstructions
                    }
                }
            }

            it("and computes correct maxLocals/maxStack values") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    val body = method.body.get
                    val operandsSize: Int =
                        (if (calleeTypeAndMethod._2.isStatic) 0 else 1 /* for `this`*/ ) +
                            method.parameterTypes.map(_.computationalType.operandSize).sum

                    val returnSize: Int =
                        if (method.returnType != VoidType) {
                            method.returnType.computationalType.operandSize.toInt
                        } else {
                            0
                        }
                    val stackSize = math.max(operandsSize, returnSize)
                    body.maxStack should be(stackSize)
                    body.maxLocals should be(1 + operandsSize + returnSize)
                }
            }

            val methodWithManyParametersAndNoReturnValue = testProject.
                classFile(InstanceMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").get

            val staticMethodWithManyParametersAndNoReturnValue = testProject.
                classFile(StaticMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").get

            it("and produces correctly indexed load instructions") {
                testMethod(
                    InstanceMethods,
                    methodWithManyParametersAndNoReturnValue,
                    testProject
                ) { (classFile, calleeTypeAndMethod) ⇒
                    val calleeField = classFile.fields.head
                    val method = checkAndReturnForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    val instructions = body.instructions
                    body.maxStack should be(13)
                    body.maxLocals should be(14)
                    instructions should be(Array(
                        ALOAD_0,
                        GETFIELD(
                            classFile.thisType,
                            calleeField.name,
                            calleeField.fieldType
                        ),
                        null,
                        null,
                        DLOAD_1,
                        FLOAD_3,
                        LLOAD(4),
                        null,
                        ILOAD(6),
                        null,
                        ILOAD(7),
                        null,
                        ILOAD(8),
                        null,
                        ILOAD(9),
                        null,
                        ILOAD(10),
                        null,
                        ALOAD(11),
                        null,
                        ALOAD(12),
                        null,
                        INVOKEVIRTUAL(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ),
                        null,
                        null,
                        RETURN
                    ))
                }
                testMethod(
                    StaticMethods,
                    staticMethodWithManyParametersAndNoReturnValue,
                    testProject
                ) { (classFile, calleeTypeAndMethod) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    val instructions = body.instructions
                    body.maxLocals should be(13)
                    body.maxStack should be(12)
                    instructions should be(Array(
                        DLOAD_1,
                        FLOAD_3,
                        LLOAD(4),
                        null,
                        ILOAD(6),
                        null,
                        ILOAD(7),
                        null,
                        ILOAD(8),
                        null,
                        ILOAD(9),
                        null,
                        ILOAD(10),
                        null,
                        ALOAD(11),
                        null,
                        ALOAD(12),
                        null,
                        INVOKESTATIC(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ),
                        null,
                        null,
                        RETURN
                    ))
                }
            }

            it("and handles multiple parameters of the same type correctly") {
                val methodWithFiveDoubleParameters = testProject.
                    classFile(StaticMethods).get.findMethod(
                        "doubleDoubleDoubleDoubleDoubleAndNoReturnValue"
                    ).get
                val proxy =
                    ClassFileFactory.Proxy(
                        TypeDeclaration(
                            ObjectType("StaticMethods$ProxyWithFiveArguments"),
                            false,
                            Some(ObjectType.Object),
                            Set.empty
                        ),
                        methodWithFiveDoubleParameters.name,
                        methodWithFiveDoubleParameters.descriptor,
                        StaticMethods,
                        methodWithFiveDoubleParameters.name,
                        methodWithFiveDoubleParameters.descriptor,
                        INVOKESTATIC.opcode
                    )

                val method = checkAndReturnForwardingMethod(proxy)
                val body = method.body.get
                val instructions = body.instructions
                body.maxStack should be(10)
                body.maxLocals should be(11)
                instructions should be(Array(
                    DLOAD_1,
                    DLOAD_3,
                    DLOAD(5),
                    null,
                    DLOAD(7),
                    null,
                    DLOAD(9),
                    null,
                    INVOKESTATIC(
                        StaticMethods,
                        methodWithFiveDoubleParameters.name,
                        methodWithFiveDoubleParameters.descriptor
                    ),
                    null,
                    null,
                    RETURN
                ))
            }

            it("and creates fields for additional static parameters") {
                for {
                    (theType, method) ← methods
                } {
                    val invocationInstruction: Opcode =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            INVOKEINTERFACE.opcode
                        } else if (method.isStatic) {
                            INVOKESTATIC.opcode
                        } else if (method.isPrivate) {
                            INVOKESPECIAL.opcode
                        } else {
                            INVOKEVIRTUAL.opcode
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                Set.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            method.name,
                            MethodDescriptor(
                                IntegerType +: method.parameterTypes,
                                method.returnType
                            ),
                            invocationInstruction
                        )

                    val constructor = checkAndReturnConstructor(proxy)
                    if (method.isStatic) {
                        constructor.parameterTypes should be(IndexedSeq(IntegerType))
                    } else {
                        constructor.parameterTypes should be(IndexedSeq(theType, IntegerType))
                    }

                    val factory = checkAndReturnFactoryMethod(proxy)
                    if (method.isStatic) {
                        factory.parameterTypes should be(IndexedSeq(IntegerType))
                    } else {
                        factory.parameterTypes should be(IndexedSeq(theType, IntegerType))
                    }

                    val forwarder = checkAndReturnForwardingMethod(proxy)
                    forwarder.parameterTypes should be(method.parameterTypes)
                }
            }

            it("and correctly forwards those static parameters") {
                for {
                    (theType, method) ← methods
                } {
                    val invocationInstruction: Opcode =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            INVOKEINTERFACE.opcode
                        } else if (method.isStatic) {
                            INVOKESTATIC.opcode
                        } else if (method.isPrivate) {
                            INVOKESPECIAL.opcode
                        } else {
                            INVOKEVIRTUAL.opcode
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                Set.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            method.name,
                            MethodDescriptor(
                                IntegerType +: method.parameterTypes,
                                method.returnType
                            ),
                            invocationInstruction
                        )
                    val forwarderMethod = checkAndReturnForwardingMethod(proxy)
                    val instructions = forwarderMethod.body.get.instructions

                    var currentPC = 0
                    if (!method.isStatic) {
                        // check that the receiver is loaded while advancing the index correctly
                        if (method.isConstructor) {
                            instructions(currentPC) should be(NEW(theType))
                            currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
                            instructions(currentPC) should be(DUP)
                            currentPC = DUP.indexOfNextInstruction(currentPC, false)
                        } else {
                            instructions(currentPC) should be(ALOAD_0)
                            currentPC = ALOAD_0.indexOfNextInstruction(currentPC, false)
                            instructions(currentPC) should be(
                                GETFIELD(proxy.thisType, ClassFileFactory.ReceiverFieldName, theType)
                            )
                            currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
                        }
                    }
                    instructions(currentPC) should be(ALOAD_0)
                    currentPC = ALOAD_0.indexOfNextInstruction(currentPC, false)
                    instructions(currentPC) should be(
                        GETFIELD(proxy.thisType, "staticParameter0", IntegerType)
                    )
                    currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
                    instructions(currentPC) should not be (null)
                }
            }

            it("and returns correctly") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) ⇒
                    val method = checkAndReturnForwardingMethod(classFile)
                    val body = method.body.get
                    val instructions = body.instructions
                    instructions.last should be(method.returnType match {
                        case VoidType         ⇒ RETURN
                        case IntegerType      ⇒ IRETURN
                        case ShortType        ⇒ IRETURN
                        case ByteType         ⇒ IRETURN
                        case CharType         ⇒ IRETURN
                        case BooleanType      ⇒ IRETURN
                        case LongType         ⇒ LRETURN
                        case FloatType        ⇒ FRETURN
                        case DoubleType       ⇒ DRETURN
                        case _: ReferenceType ⇒ ARETURN
                    })
                }
                for {
                    (theType, method) ← methods if method.returnType != VoidType
                } {
                    val invocationInstruction: Opcode =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            INVOKEINTERFACE.opcode
                        } else if (method.isStatic) {
                            INVOKESTATIC.opcode
                        } else if (method.isPrivate) {
                            INVOKESPECIAL.opcode
                        } else {
                            INVOKEVIRTUAL.opcode
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                Set.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            method.name,
                            MethodDescriptor(
                                method.parameterTypes,
                                ObjectType.Object
                            ),
                            invocationInstruction
                        )
                    val forwarderMethod = checkAndReturnForwardingMethod(proxy)
                    val instructions = forwarderMethod.body.get.instructions

                    val indexOfInvocation =
                        instructions.indexWhere(_.isInstanceOf[InvocationInstruction])
                    var currentPC =
                        instructions(indexOfInvocation).indexOfNextInstruction(indexOfInvocation, false)

                    val returnType = method.returnType

                    if (returnType.isBaseType) {
                        // cast & unbox
                        val baseType = returnType.asBaseType
                        val wrapper = baseType.WrapperType
                        val cast = CHECKCAST(wrapper)
                        instructions(currentPC) should be(cast)
                        currentPC = cast.indexOfNextInstruction(currentPC, false)
                        instructions(currentPC) should be(
                            INVOKEVIRTUAL(
                                wrapper,
                                s"${baseType.toJava}Value",
                                new NoArgumentMethodDescriptor(baseType)
                            )
                        )
                    } else if (returnType.isReferenceType) {
                        // just cast
                        val cast = CHECKCAST(returnType.asReferenceType)
                        instructions(currentPC) should be(cast)
                    }
                    currentPC = instructions(currentPC).indexOfNextInstruction(currentPC, false)
                    instructions(currentPC) should be(ReturnInstruction(returnType))
                }
            }
        }

        describe("should be able to create correct proxies for method reference invokedynamics") {

            describe("lambda expressions should not be identified as method references") {
                val Lambdas = lambdasProject.allProjectClassFiles.find(
                    _.fqn == "lambdas/Lambdas"
                ).get
                it("they are not constructor references") {
                    for {
                        MethodWithBody(body) ← Lambdas.methods
                        instruction ← body.instructions
                        if instruction.isInstanceOf[INVOKEDYNAMIC]
                        invokedynamic = instruction.asInstanceOf[INVOKEDYNAMIC]
                    } {
                        val targetMethodHandle = invokedynamic.bootstrapMethod.
                            bootstrapArguments(1).asInstanceOf[MethodCallMethodHandle]
                        val proxyInterfaceMethodDescriptor = invokedynamic.bootstrapMethod.
                            bootstrapArguments(2).asInstanceOf[MethodDescriptor]
                        assert(!ClassFileFactory.isVirtualMethodReference(
                            targetMethodHandle.opcodeOfUnderlyingInstruction,
                            targetMethodHandle.receiverType.asObjectType,
                            targetMethodHandle.methodDescriptor,
                            proxyInterfaceMethodDescriptor
                        ))
                    }
                }

                it("nor are they virtual method calls") {
                    for {
                        MethodWithBody(body) ← Lambdas.methods
                        instruction ← body.instructions
                        if instruction.isInstanceOf[INVOKEDYNAMIC]
                        invokedynamic = instruction.asInstanceOf[INVOKEDYNAMIC]
                    } {
                        val targetMethodHandle = invokedynamic.bootstrapMethod.
                            bootstrapArguments(1).asInstanceOf[MethodCallMethodHandle]
                        assert(!ClassFileFactory.isNewInvokeSpecial(
                            targetMethodHandle.opcodeOfUnderlyingInstruction,
                            targetMethodHandle.name
                        ))
                    }
                }
            }

            val MethodReferences = lambdasProject.allProjectClassFiles.find(
                _.fqn == "lambdas/MethodReferences"
            ).get

            describe("references to constructors") {
                it("should be correctly identified") {
                    val newValueMethod = MethodReferences.findMethod("newValue").get
                    val indy = newValueMethod.body.get.instructions.find(
                        _.isInstanceOf[INVOKEDYNAMIC]
                    ).get.asInstanceOf[INVOKEDYNAMIC]
                    val targetMethod = indy.bootstrapMethod.bootstrapArguments(1).
                        asInstanceOf[MethodCallMethodHandle]
                    val opcode = targetMethod.opcodeOfUnderlyingInstruction
                    val methodName = targetMethod.name
                    assert(ClassFileFactory.isNewInvokeSpecial(opcode, methodName))
                }

                val SomeType = ObjectType("SomeType")
                val proxy = ClassFileFactory.Proxy(
                    TypeDeclaration(
                        ObjectType("MethodRefConstructorProxy"),
                        false,
                        Some(ObjectType.Object),
                        Set.empty
                    ),
                    "get",
                    MethodDescriptor(ObjectType.String, SomeType),
                    SomeType,
                    "<init>",
                    MethodDescriptor(ObjectType.String, SomeType),
                    INVOKESPECIAL.opcode
                )

                val proxyMethod = proxy.findMethod("get").get

                it("should result in a proxy method that creates an instance of the object first") {
                    proxyMethod.body.get.instructions.slice(0, 4) should be(
                        Array(
                            NEW(SomeType),
                            null,
                            null,
                            DUP
                        )
                    )
                }
            }

            describe("references to instance methods") {
                it("should be correctly identified") {
                    val filterOutEmptyValuesMethod = MethodReferences.findMethod(
                        "filterOutEmptyValues"
                    ).get
                    val invokedynamic = filterOutEmptyValuesMethod.body.get.instructions.
                        find(_.isInstanceOf[INVOKEDYNAMIC]).get.asInstanceOf[INVOKEDYNAMIC]
                    val targetMethodHandle = invokedynamic.bootstrapMethod.
                        bootstrapArguments(1).asInstanceOf[MethodCallMethodHandle]
                    val proxyInterfaceMethodDescriptor = invokedynamic.bootstrapMethod.
                        bootstrapArguments(2).asInstanceOf[MethodDescriptor]
                    assert(ClassFileFactory.isVirtualMethodReference(
                        targetMethodHandle.opcodeOfUnderlyingInstruction,
                        targetMethodHandle.receiverType.asObjectType,
                        targetMethodHandle.methodDescriptor,
                        proxyInterfaceMethodDescriptor
                    ))
                }

                val SomeOtherType = ObjectType("SomeOtherType")
                val proxy = ClassFileFactory.Proxy(
                    TypeDeclaration(
                        ObjectType("InstanceMethodRefProxy"),
                        false,
                        Some(ObjectType.Object),
                        Set.empty
                    ),
                    "isFull",
                    MethodDescriptor(SomeOtherType, BooleanType),
                    SomeOtherType,
                    "isThisFull",
                    MethodDescriptor.JustReturnsBoolean,
                    INVOKEVIRTUAL.opcode
                )

                it("and result in a proxy method that passes in the explicit this") {
                    val proxyMethod = proxy.findMethod("isFull").get
                    val instructions = proxyMethod.body.get.instructions
                    instructions(0) should be(ALOAD_1)
                    instructions(1) should be(INVOKEVIRTUAL(
                        SomeOtherType,
                        "isThisFull",
                        MethodDescriptor.JustReturnsBoolean
                    ))
                }
            }
        }

        describe("should be able to profixy $newInstance methods") {
            it("by picking an alternate name for the factory method") {
                val theType = ObjectType("ClassFileFactoryTest$newInstanceName")
                val proxy =
                    ClassFileFactory.Proxy(
                        TypeDeclaration(
                            ObjectType(theType.toJava + '$'+"Proxy"),
                            false,
                            Some(ObjectType.Object),
                            Set.empty
                        ),
                        "$newInstance",
                        new NoArgumentMethodDescriptor(theType),
                        theType,
                        "newInstance",
                        new NoArgumentMethodDescriptor(theType),
                        INVOKESTATIC.opcode
                    )
                val factoryMethod = checkAndReturnFactoryMethod(proxy)
                factoryMethod.name should be(ClassFileFactory.AlternativeFactoryMethodName)
            }
        }

        describe("should create a bridge method to the forwarding method if so desired") {
            it("the bridge method should stack & cast parameters and call the forwarding method") {
                val receiverType = ObjectType("ClassFileFactoryTest$BridgeCast")
                val proxyType = ObjectType(receiverType.simpleName + '$'+"Proxy")
                val methodDescriptor =
                    MethodDescriptor(IndexedSeq(ObjectType.String, DoubleType), IntegerType)
                val proxy =
                    ClassFileFactory.Proxy(
                        TypeDeclaration(proxyType, false, Some(ObjectType.Object), Set.empty),
                        "method",
                        methodDescriptor,
                        receiverType,
                        "method",
                        methodDescriptor,
                        INVOKESTATIC.opcode,
                        Some(MethodDescriptor(IndexedSeq(ObjectType.Object, DoubleType), IntegerType))
                    )
                val bridge = proxy.methods.find(m ⇒ ACC_BRIDGE.isSet(m.accessFlags))
                bridge should be('defined)
                bridge.get.body should be('defined)
                val body = bridge.get.body.get
                body.maxStack should be(4)
                body.maxLocals should be(5)
                body.instructions should be(
                    Array(
                        ALOAD_0,
                        ALOAD_1,
                        CHECKCAST(ObjectType.String),
                        null,
                        null,
                        DLOAD_2,
                        INVOKEVIRTUAL(proxyType, "method", methodDescriptor),
                        null,
                        null,
                        IRETURN
                    )
                )
            }
        }

        describe("should compute correct constructor stack and local values") {
            val definingType =
                TypeDeclaration(
                    ObjectType("SomeRandomType"),
                    false,
                    Some(ObjectType.Object),
                    Set.empty
                )

            def testConstructor(
                fieldTypes:     IndexedSeq[FieldType],
                expectedLocals: Int, expectedStack: Int
            ): Unit = {
                val fields = fieldTypes.zipWithIndex.map { p ⇒
                    val (ft, i) = p
                    Field(bi.ACC_PRIVATE.mask, "field"+i, ft, Seq.empty)
                }
                val constructor = ClassFileFactory.createConstructor(definingType, fields)
                constructor.body should be('defined)
                val code = constructor.body.get
                code.maxStack should be(expectedStack)
                code.maxLocals should be(expectedLocals)
            }

            it("for no fields") {
                testConstructor(IndexedSeq.empty, 1, 1)
            }

            it("for a single reference value field") {
                testConstructor(IndexedSeq(ObjectType.Object), 2, 2)
            }

            it("for primitive value fields") {
                testConstructor(IndexedSeq(IntegerType), 2, 2)
            }

            it("for wide primitive value fields") {
                testConstructor(IndexedSeq(DoubleType), 3, 3)
            }

            it("for multiple simple primitive and reference value fields") {
                testConstructor(IndexedSeq(ObjectType.Object, IntegerType, IntegerType), 4, 2)
            }

            it("for multiple wide primitive value fields") {
                testConstructor(IndexedSeq(DoubleType, LongType, DoubleType), 7, 3)
            }

            it("for everything together") {
                testConstructor(IndexedSeq(ObjectType.Object, LongType, IntegerType), 5, 3)
            }
        }

        describe("should create correct instructions for stacking parameters for the forwarding call") {

            it("should produce no instructions for empty descriptors") {
                val instructions = ClassFileFactory.parameterForwardingInstructions(
                    NoArgumentAndNoReturnValueMethodDescriptor,
                    NoArgumentAndNoReturnValueMethodDescriptor,
                    0,
                    Seq.empty,
                    ObjectType.Object
                )
                instructions should have size (0)
            }

            it("should forward all parameters for identical, non-empty descriptors") {
                var d = MethodDescriptor(IndexedSeq(IntegerType, ObjectType.String), VoidType)
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        ALOAD_1
                    )
                )
                d = MethodDescriptor(ArrayType.ArrayOfObjects, ObjectType.Object)
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ALOAD_0
                    )
                )
                d = MethodDescriptor((1 to 10).map(_ ⇒ ByteType).toIndexedSeq, VoidType)
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        ILOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD(4),
                        null,
                        ILOAD(5),
                        null,
                        ILOAD(6),
                        null,
                        ILOAD(7),
                        null,
                        ILOAD(8),
                        null,
                        ILOAD(9),
                        null
                    )
                )
                d =
                    MethodDescriptor(
                        IndexedSeq(DoubleType, ObjectType.String, ByteType,
                            LongType, DoubleType, FloatType),
                        VoidType
                    )
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        DLOAD_0,
                        ALOAD_2,
                        ILOAD_3,
                        LLOAD(4),
                        null,
                        DLOAD(6),
                        null,
                        FLOAD(8),
                        null
                    )
                )
            }
            it("should safely convert primitive values") {
                val d1 =
                    MethodDescriptor(
                        IndexedSeq(ByteType, CharType, ShortType,
                            IntegerType, FloatType, LongType),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        IndexedSeq(ShortType, ShortType, IntegerType,
                            LongType, DoubleType, DoubleType),
                        VoidType
                    )
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        ILOAD_1,
                        I2S,
                        ILOAD_2,
                        ILOAD_3,
                        I2L,
                        FLOAD(4),
                        null,
                        F2D,
                        LLOAD(5),
                        null,
                        L2D
                    )
                )
            }

            def valueOfDescriptor(baseType: BaseType): MethodDescriptor =
                MethodDescriptor(baseType, baseType.WrapperType)

            it("should create boxing instructions for primitive types") {
                val d1 =
                    MethodDescriptor(
                        IndexedSeq(ByteType, CharType, ShortType,
                            IntegerType, FloatType, LongType),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        IndexedSeq(ObjectType.Byte, ObjectType.Character,
                            ObjectType.Short, ObjectType.Integer, ObjectType.Float,
                            ObjectType.Long),
                        VoidType
                    )
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        INVOKESTATIC(ObjectType.Byte, "valueOf", valueOfDescriptor(ByteType)),
                        null,
                        null,
                        ILOAD_1,
                        INVOKESTATIC(ObjectType.Character, "valueOf", valueOfDescriptor(CharType)),
                        null,
                        null,
                        ILOAD_2,
                        INVOKESTATIC(ObjectType.Short, "valueOf", valueOfDescriptor(ShortType)),
                        null,
                        null,
                        ILOAD_3,
                        INVOKESTATIC(ObjectType.Integer, "valueOf", valueOfDescriptor(IntegerType)),
                        null,
                        null,
                        FLOAD(4),
                        null,
                        INVOKESTATIC(ObjectType.Float, "valueOf", valueOfDescriptor(FloatType)),
                        null,
                        null,
                        LLOAD(5),
                        null,
                        INVOKESTATIC(ObjectType.Long, "valueOf", valueOfDescriptor(LongType)),
                        null,
                        null
                    )
                )
            }
            it("should create unboxing instructions for wrapper types") {
                val d1 = MethodDescriptor(ObjectType.Integer, VoidType)
                val d2 = MethodDescriptor(IntegerType, VoidType)
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ALOAD_0,
                        INVOKEVIRTUAL(ObjectType.Integer, "intValue", MethodDescriptor.JustReturnsInteger),
                        null,
                        null
                    )
                )
            }
            it("should cast arbitrary reference types") {
                val d1 =
                    MethodDescriptor(
                        IndexedSeq(ObjectType.Object, ObjectType.Object),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        IndexedSeq(ObjectType.String, ArrayType.ArrayOfObjects),
                        VoidType
                    )
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(Array(
                    ALOAD_0,
                    CHECKCAST(ObjectType.String),
                    null,
                    null,
                    ALOAD_1,
                    CHECKCAST(ArrayType.ArrayOfObjects),
                    null,
                    null
                ))
            }

            it("should pack everything into an Object[] if necessary") {
                val d1 =
                    MethodDescriptor(
                        IndexedSeq(
                            IntegerType,
                            ObjectType.String,
                            ByteType,
                            BooleanType,
                            ObjectType.Integer,
                            LongType,
                            ShortType
                        ), VoidType
                    )
                val d2 = MethodDescriptor(ArrayType.ArrayOfObjects, ObjectType.Object)
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        BIPUSH(7),
                        null,
                        ANEWARRAY(ObjectType.Object),
                        null,
                        null,
                        DUP,
                        ICONST_0,
                        ILOAD_0,
                        INVOKESTATIC(ObjectType.Integer, "valueOf", valueOfDescriptor(IntegerType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        ICONST_1,
                        ALOAD_1,
                        AASTORE,
                        DUP,
                        ICONST_2,
                        ILOAD_2,
                        INVOKESTATIC(ObjectType.Byte, "valueOf", valueOfDescriptor(ByteType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        ICONST_3,
                        ILOAD_3,
                        INVOKESTATIC(ObjectType.Boolean, "valueOf", valueOfDescriptor(BooleanType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        ICONST_4,
                        ALOAD(4),
                        null,
                        AASTORE,
                        DUP,
                        ICONST_5,
                        LLOAD(5),
                        null,
                        INVOKESTATIC(ObjectType.Long, "valueOf", valueOfDescriptor(LongType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        BIPUSH(6),
                        null,
                        ILOAD(7),
                        null,
                        INVOKESTATIC(ObjectType.Short, "valueOf", valueOfDescriptor(ShortType)),
                        null,
                        null,
                        AASTORE
                    )
                )
            }
        }

        describe("should create correct return/conversion instructions") {

            it("should convert Object to base types by casting and unboxing") {
                BaseType.baseTypes foreach { t ⇒
                    val name = s"${t.toJava}Value"
                    val desc = new NoArgumentMethodDescriptor(t)
                    val conv = ClassFileFactory.returnAndConvertInstructions(t, ObjectType.Object)
                    conv should have size (7)
                    conv(0) should be(CHECKCAST(t.WrapperType))
                    conv(3) should be(INVOKEVIRTUAL(t.WrapperType, name, desc))
                    conv(6) should be(ReturnInstruction(t))
                }
            }

            it("should convert Object to any reference type by casting") {
                Seq(ObjectType.String, ArrayType(LongType), ObjectType.Integer).foreach(
                    t ⇒ ClassFileFactory.returnAndConvertInstructions(t, ObjectType.Object) should be(
                        Array(CHECKCAST(t.asReferenceType), null, null, ARETURN)
                    )
                )
            }

            it("should convert other types if possible, e.g. base types to wrappers") {
                ClassFileFactory.returnAndConvertInstructions(ObjectType.Byte, ByteType) should be(
                    Array(
                        INVOKESTATIC(ObjectType.Byte, "valueOf", MethodDescriptor(ByteType, ObjectType.Byte)),
                        null,
                        null,
                        ARETURN
                    )
                )
                ClassFileFactory.returnAndConvertInstructions(FloatType, LongType) should be(
                    Array(L2F, FRETURN)
                )
            }
        }
    }

    /* *************************************
     *
     * The require functions search the array of remainingInstructions for the next one
     * that loads a value of their respective required type onto the operand stack and
     * return the number of instructions "consumed" that way.
     *
     * For example, if the first Instruction of the array is an "ICONST_<X>",
     * requireInt would return 1.
     *
     * If no fitting instruction can be found, the function fails the test.
     *
     ************************************** */

    private def require(
        oneOf:                 Set[Opcode],
        remainingInstructions: Array[Instruction]
    ): Int = {
        val indexOfNextFittingInstruction =
            remainingInstructions.filter(_ != null).
                indexWhere(instruction ⇒ oneOf contains instruction.opcode)
        assert(
            indexOfNextFittingInstruction != -1,
            s"Could not find required instruction ${oneOf.mkString(",")}"
        )

        indexOfNextFittingInstruction + 1
    }

    private def requireInt(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                ILOAD_0.opcode,
                ILOAD_1.opcode,
                ILOAD_2.opcode,
                ILOAD_3.opcode,
                ILOAD.opcode
            ),
            remainingInstructions
        )

    private def requireLong(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                LLOAD_0.opcode,
                LLOAD_1.opcode,
                LLOAD_2.opcode,
                LLOAD_3.opcode,
                LLOAD.opcode
            ),
            remainingInstructions
        )

    private def requireFloat(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                FLOAD_0.opcode,
                FLOAD_1.opcode,
                FLOAD_2.opcode,
                FLOAD_3.opcode,
                FLOAD.opcode
            ),
            remainingInstructions
        )

    private def requireDouble(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                DLOAD_0.opcode,
                DLOAD_1.opcode,
                DLOAD_2.opcode,
                DLOAD_3.opcode,
                DLOAD.opcode
            ),
            remainingInstructions
        )

    private def requireReference(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                ALOAD_0.opcode,
                ALOAD_1.opcode,
                ALOAD_2.opcode,
                ALOAD_3.opcode,
                ALOAD.opcode,
                NEW.opcode
            ),
            remainingInstructions
        )

    /**
     * Iterates over the given list of tuples, generating a proxy class file for the
     * method specified by each tuple, and passes that proxy class file to the provided
     * test function.
     */
    private def testMethods(
        methods:    Iterable[(ObjectType, Method)],
        repository: ClassFileRepository
    )(
        test: (ClassFile, (ObjectType, Method)) ⇒ Unit
    ): Unit = {
        for {
            (calleeType, calleeMethod) ← methods
        } {
            testMethod(calleeType, calleeMethod, repository)(test)
        }
    }

    private def testMethod(
        calleeType: ObjectType, calleeMethod: Method,
        repository: ClassFileRepository
    )(
        test: (ClassFile, (ObjectType, Method)) ⇒ Unit
    ): Unit = {
        val calleeMethodName = calleeMethod.name
        val calleeMethodDescriptor = calleeMethod.descriptor
        val definingTypeName = calleeType.simpleName+"$"+calleeMethodName
        val definingType =
            TypeDeclaration(
                ObjectType(definingTypeName),
                false,
                Some(ObjectType.Object),
                Set()
            )
        val methodName = calleeMethodName+"$Forwarded"
        val methodDescriptor = calleeMethodDescriptor
        val invocationInstruction: Opcode =
            if (repository.classFile(calleeType).get.isInterfaceDeclaration) {
                INVOKEINTERFACE.opcode
            } else if (calleeMethod.isStatic) {
                INVOKESTATIC.opcode
            } else if (calleeMethod.isPrivate) {
                INVOKESPECIAL.opcode
            } else {
                INVOKEVIRTUAL.opcode
            }
        val classFile =
            ClassFileFactory.Proxy(
                definingType,
                methodName,
                methodDescriptor,
                calleeType,
                calleeMethodName,
                calleeMethodDescriptor,
                invocationInstruction
            )

        test(classFile, (calleeType, calleeMethod))
    }
}
