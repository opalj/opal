/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.opalj.collection.immutable.UIDSet
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.TestSupport.biProject
import org.opalj.br.analyses.Project
import org.opalj.log.GlobalLogContext
import com.typesafe.config.Config
import org.opalj.br.reader.InvokedynamicRewriting
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import scala.collection.immutable.ArraySeq

/**
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ClassFileFactoryTest extends AnyFunSpec with Matchers {

    /**
     * ********************************************************************************************
     * TEST FIXTURE
     * ********************************************************************************************
     */
    val testProject = biProject("proxy.jar")
    assert(testProject.projectClassFilesCount > 0)

    val lambdasProject = {
        val jarFile = locateTestResources("lambdas-1.8-g-parameters-genericsignature.jar", "bi")
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = InvokedynamicRewriting.InvokedynamicRewritingConfigKey
        val logRewritingsConfigKey = InvokedynamicRewriting.LambdaExpressionsLogRewritingsConfigKey
        val config = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.TRUE))

        Project(jarFile, GlobalLogContext, config)
    }

    val StaticMethods = ObjectType("proxy/StaticMethods")
    val InstanceMethods = ObjectType("proxy/InstanceMethods")
    val Constructors = ObjectType("proxy/Constructors")
    val PrivateInstanceMethods = ObjectType("proxy/PrivateInstanceMethods")
    val InterfaceMethods = ObjectType("proxy/InterfaceMethods")

    /**
     * ********************************************************************************************
     * HELPER METHODS
     * ********************************************************************************************
     */

    private def getMethods(
        classType:  ObjectType,
        repository: ClassFileRepository
    ): Iterable[(ObjectType, Method)] = {
        repository.classFile(classType) match {
            case Some(cf) => cf.methods.map { (classType, _) }
            case None     => fail(s"${classType.toJava} cannot be found")
        }
    }

    private def collectTheMethodOf(classFile: ClassFile)(filter: Method => Boolean): Method = {
        val methods = classFile.methods.filter(filter)
        methods should have size (1)
        val method = methods.head
        method.body shouldBe defined
        method
    }

    private def collectTheConstructor(classFile: ClassFile): Method = {
        collectTheMethodOf(classFile)(_.isConstructor)
    }

    private def collectTheFactoryMethod(classFile: ClassFile): Method = {
        collectTheMethodOf(classFile) { m =>
            m.isStatic && m.isPublic && (m.name == "$newInstance" || m.name == "$createInstance")
        }
    }

    private def collectTheForwardingMethod(classFile: ClassFile): Method = {
        collectTheMethodOf(classFile) { m =>
            !(m.isConstructor || m.name == "$newInstance" || m.name == "$createInstance")
        }
    }

    /**
     * ********************************************************************************************
     * TESTS
     * ********************************************************************************************
     */

    describe("ClassFileFactory") {

        describe("should be able to proxify methods") {
            val instanceMethods = getMethods(InstanceMethods, testProject)
            instanceMethods should not be (Symbol("Empty"))
            val constructors = getMethods(Constructors, testProject)
            constructors should not be (Symbol("Empty"))
            val privateInstanceMethods = getMethods(PrivateInstanceMethods, testProject)
            privateInstanceMethods should not be (Symbol("Empty"))
            val interfaceMethods = getMethods(InterfaceMethods, testProject)
            interfaceMethods should not be (Symbol("Empty"))
            val staticMethods = getMethods(StaticMethods, testProject)
            staticMethods should not be (Symbol("Empty"))
            val methods: Iterable[(ObjectType, Method)] = instanceMethods ++
                constructors ++ privateInstanceMethods ++ interfaceMethods ++
                staticMethods

            it("with one instance field for instance methods, none for static") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    if (calleeTypeAndMethod._2.isStatic) {
                        classFile.fields should have size (0)
                    } else {
                        classFile.fields should have size (1)
                        val field = classFile.fields(0)
                        bi.ACC_FINAL.isSet(field.accessFlags) should be(true)
                        field.fieldType should be(calleeTypeAndMethod._1)
                    }
                }
            }

            it("and a constructor that sets that instance field (if present)") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    if (!calleeTypeAndMethod._2.isStatic) {
                        val constructor = collectTheConstructor(classFile)
                        bi.ACC_PUBLIC.isSet(constructor.accessFlags) should be(true)
                        constructor.parameterTypes should have size (1)
                        constructor.parameterTypes(0) should be(calleeTypeAndMethod._1)

                        constructor.body shouldBe defined
                        val body = constructor.body.get
                        val instructions = body.instructions
                        body.maxLocals should be(2)
                        body.maxStack should be(2)
                        instructions should be(Array(
                            ALOAD_0,
                            INVOKESPECIAL(
                                ObjectType.Object, false,
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

            it("and one static factory method that calls the constructor") {
                testMethods(methods, testProject) { (classFile, _) =>
                    val factoryMethod = collectTheFactoryMethod(classFile)
                    val constructor = collectTheConstructor(classFile)
                    val parameterTypes = constructor.parameterTypes
                    factoryMethod.descriptor should be(MethodDescriptor(parameterTypes, classFile.thisType))
                    val maxLocals = factoryMethod.parameterTypes.iterator.map(_.computationalType.operandSize).sum
                    val maxStack = maxLocals + 2 // new + dup makes two extra on the stack
                    var currentVariableIndex = 0
                    val loadParametersInstructions: Array[Instruction] =
                        factoryMethod.parameterTypes.flatMap[Instruction] { t =>
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
                                INVOKESPECIAL(classFile.thisType, false, "<init>", constructor.descriptor),
                                null,
                                null,
                                ARETURN
                            )
                    )
                }
            }

            it("and one forwarding method") {
                testMethods(methods, testProject) { (classFile, _) =>
                    val method = collectTheForwardingMethod(classFile)
                    bi.ACC_PUBLIC.isSet(method.accessFlags) should be(true)
                }
            }

            it("that calls the callee method with the appropriate invokeX instruction") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    val method = collectTheForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    if (calleeMethod.isStatic) {
                        body.instructions should contain(INVOKESTATIC(
                            calleeType, false,
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
                            calleeType, false,
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
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    val method = collectTheForwardingMethod(classFile)
                    var currentInstruction = 0
                    val body = method.body.get
                    val instructions = body.instructions

                    val parameters =
                        if (calleeTypeAndMethod._2.isStatic) {
                            method.parameterTypes
                        } else {
                            calleeTypeAndMethod._1 +: method.parameterTypes
                        }
                    parameters.foreach { requiredParameter =>
                        val remainingInstructions =
                            instructions.slice(currentInstruction, instructions.size)
                        val consumedInstructions =
                            requiredParameter match {
                                case IntegerType      => requireIntLoad(remainingInstructions)
                                case ShortType        => requireIntLoad(remainingInstructions)
                                case ByteType         => requireIntLoad(remainingInstructions)
                                case CharType         => requireIntLoad(remainingInstructions)
                                case BooleanType      => requireIntLoad(remainingInstructions)
                                case FloatType        => requireFloatLoad(remainingInstructions)
                                case DoubleType       => requireDoubleLoad(remainingInstructions)
                                case LongType         => requireLongLoad(remainingInstructions)
                                case _: ReferenceType => requireReferenceLoadOrCreation(remainingInstructions)
                            }
                        currentInstruction += consumedInstructions
                    }
                }
            }

            it("and computes correct maxLocals/maxStack values") {
                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    val method = collectTheForwardingMethod(classFile)
                    val body = method.body.get
                    val operandsSize: Int =
                        (if (calleeTypeAndMethod._2.isStatic) 0 else 1 /* for `this`*/ ) +
                            method.parameterTypes.map(_.computationalType.operandSize).sum

                    val returnSize: Int = method.returnType.operandSize
                    val stackSize = math.max(operandsSize, returnSize)
                    body.maxStack should be >= (stackSize)
                    body.maxLocals should be(1 + operandsSize + returnSize)
                }
            }

            val methodWithManyParametersAndNoReturnValue = testProject.
                classFile(InstanceMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").head

            val staticMethodWithManyParametersAndNoReturnValue = testProject.
                classFile(StaticMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").head

            it("and produces correctly indexed load instructions") {
                testMethod(
                    InstanceMethods,
                    methodWithManyParametersAndNoReturnValue,
                    testProject
                ) { (classFile, calleeTypeAndMethod) =>
                    val calleeField = classFile.fields.head
                    val method = collectTheForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    val instructions = body.instructions
                    body.maxStack should be >= (13)
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
                ) { (classFile, calleeTypeAndMethod) =>
                    val method = collectTheForwardingMethod(classFile)
                    val (calleeType, calleeMethod) = calleeTypeAndMethod
                    val body = method.body.get
                    val instructions = body.instructions
                    body.maxStack should be >= (12)
                    body.maxLocals should be(13)
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
                            calleeType, false,
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
                    ).head
                val proxy =
                    ClassFileFactory.Proxy(
                        testProject.classFile(StaticMethods).get.thisType,
                        testProject.classFile(StaticMethods).get.isInterfaceDeclaration,
                        TypeDeclaration(
                            ObjectType("StaticMethods$ProxyWithFiveArguments"),
                            false,
                            Some(ObjectType.Object),
                            UIDSet.empty
                        ),
                        methodWithFiveDoubleParameters.name,
                        methodWithFiveDoubleParameters.descriptor,
                        StaticMethods, false,
                        InvokeStaticMethodHandle(
                            StaticMethods,
                            false,
                            methodWithFiveDoubleParameters.name,
                            methodWithFiveDoubleParameters.descriptor
                        ),
                        INVOKESTATIC.opcode,
                        MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                        ArraySeq.empty // <= not tested...
                    )

                val method = collectTheForwardingMethod(proxy)
                val body = method.body.get
                val instructions = body.instructions
                body.maxStack should be >= (10)
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
                        StaticMethods, false,
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
                    (theType, method) <- methods
                } {
                    val (invocationInstruction, methodHandle: MethodCallMethodHandle) =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            (
                                INVOKEINTERFACE.opcode,
                                InvokeInterfaceMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else if (method.isStatic) {
                            (
                                INVOKESTATIC.opcode,
                                InvokeStaticMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else if (method.isPrivate) {
                            (
                                INVOKESPECIAL.opcode,
                                InvokeSpecialMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else {
                            (
                                INVOKEVIRTUAL.opcode,
                                InvokeVirtualMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            testProject.classFile(StaticMethods).get.thisType,
                            testProject.classFile(StaticMethods).get.isInterfaceDeclaration,
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                UIDSet.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            false,
                            methodHandle,
                            invocationInstruction,
                            MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                            ArraySeq.empty // <= not tested...
                        )

                    val constructor = collectTheConstructor(proxy)
                    if (method.isStatic) {
                        constructor.parameterTypes should be(ArraySeq(IntegerType))
                    } else {
                        constructor.parameterTypes should be(ArraySeq(theType, IntegerType))
                    }

                    val factory = collectTheFactoryMethod(proxy)
                    if (method.isStatic) {
                        factory.parameterTypes should be(ArraySeq(IntegerType))
                    } else {
                        factory.parameterTypes should be(ArraySeq(theType, IntegerType))
                    }

                    val forwarder = collectTheForwardingMethod(proxy)
                    forwarder.parameterTypes should be(method.parameterTypes)
                }
            }

            it("and correctly forwards those static parameters") {
                for {
                    (theType, method) <- methods
                } {
                    val (invocationInstruction: Opcode, methodHandle: MethodCallMethodHandle) =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            (
                                INVOKEINTERFACE.opcode,
                                InvokeInterfaceMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else if (method.isStatic) {
                            (
                                INVOKESTATIC.opcode,
                                InvokeStaticMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else if (method.isPrivate) {
                            (
                                INVOKESPECIAL.opcode,
                                InvokeSpecialMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        } else {
                            (
                                INVOKEVIRTUAL.opcode,
                                InvokeVirtualMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        IntegerType +: method.parameterTypes,
                                        method.returnType
                                    )
                                )
                            )
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            testProject.classFile(StaticMethods).get.thisType,
                            testProject.classFile(StaticMethods).get.isInterfaceDeclaration,
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                UIDSet.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            false,
                            methodHandle,
                            invocationInstruction,
                            MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                            ArraySeq.empty // <= not tested...
                        )
                    val forwarderMethod = collectTheForwardingMethod(proxy)
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

                testMethods(methods, testProject) { (classFile, calleeTypeAndMethod) =>
                    val method = collectTheForwardingMethod(classFile)
                    val body = method.body.get
                    val instructions = body.instructions
                    instructions.last should be(method.returnType match {
                        case VoidType         => RETURN
                        case IntegerType      => IRETURN
                        case ShortType        => IRETURN
                        case ByteType         => IRETURN
                        case CharType         => IRETURN
                        case BooleanType      => IRETURN
                        case LongType         => LRETURN
                        case FloatType        => FRETURN
                        case DoubleType       => DRETURN
                        case _: ReferenceType => ARETURN
                        case CTIntType        => throw new UnknownError("unexpected type")
                    })
                }
                for {
                    (theType, method) <- methods if method.returnType != VoidType
                } {
                    val (invocationInstruction: Opcode, methodHandle: MethodCallMethodHandle) =
                        if (testProject.classFile(theType).get.isInterfaceDeclaration) {
                            (
                                INVOKEINTERFACE.opcode,
                                InvokeInterfaceMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        method.parameterTypes,
                                        ObjectType.Object
                                    )
                                )
                            )
                        } else if (method.isStatic) {
                            (
                                INVOKESTATIC.opcode,
                                InvokeStaticMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        method.parameterTypes,
                                        ObjectType.Object
                                    )
                                )
                            )
                        } else if (method.isPrivate) {
                            (
                                INVOKESPECIAL.opcode,
                                InvokeSpecialMethodHandle(
                                    theType,
                                    false,
                                    method.name,
                                    MethodDescriptor(
                                        method.parameterTypes,
                                        ObjectType.Object
                                    )
                                )
                            )
                        } else {
                            (
                                INVOKEVIRTUAL.opcode,
                                InvokeVirtualMethodHandle(
                                    theType,
                                    method.name,
                                    MethodDescriptor(
                                        method.parameterTypes,
                                        ObjectType.Object
                                    )
                                )
                            )
                        }
                    val proxy =
                        ClassFileFactory.Proxy(
                            testProject.classFile(StaticMethods).get.thisType,
                            testProject.classFile(StaticMethods).get.isInterfaceDeclaration,
                            TypeDeclaration(
                                ObjectType(s"TestProxy$$${theType}$$${method.name}"),
                                false,
                                Some(ObjectType.Object),
                                UIDSet.empty
                            ),
                            "theProxy",
                            method.descriptor,
                            theType,
                            false,
                            methodHandle,
                            invocationInstruction,
                            MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                            ArraySeq.empty // <= not tested...
                        )
                    val forwarderMethod = collectTheForwardingMethod(proxy)
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
                val lambdas = lambdasProject.allProjectClassFiles.find(_.fqn == "lambdas/Lambdas").get
                it("they are not constructor references") {
                    for {
                        MethodWithBody(body) <- lambdas.methods
                        invokedynamic <- body.instructions.collect { case i: INVOKEDYNAMIC => i }
                    } {
                        val targetMethodHandle = invokedynamic.bootstrapMethod.
                            arguments(1).asInstanceOf[MethodCallMethodHandle]
                        val proxyInterfaceMethodDescriptor = invokedynamic.bootstrapMethod.
                            arguments(2).asInstanceOf[MethodDescriptor]
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
                        MethodWithBody(body) <- lambdas.methods
                        invokedynamic <- body.instructions.collect { case i: INVOKEDYNAMIC => i }
                    } {
                        val targetMethodHandle = invokedynamic.bootstrapMethod.
                            arguments(1).asInstanceOf[MethodCallMethodHandle]
                        assert(!ClassFileFactory.isNewInvokeSpecial(
                            targetMethodHandle.opcodeOfUnderlyingInstruction,
                            targetMethodHandle.name
                        ))
                    }
                }
            }

            val MethodReferences = lambdasProject.allProjectClassFiles.find(_.fqn == "lambdas/methodreferences/MethodReferences").get

            describe("references to constructors") {
                it("should be correctly identified") {
                    val newValueMethod = MethodReferences.findMethod("newValue").head
                    val body = newValueMethod.body.get
                    val indy =
                        body.instructionIterator.collectFirst({
                            case i: INVOKEDYNAMIC => i
                        }: PartialFunction[Instruction, INVOKEDYNAMIC]) match {
                            case Some(i) => i
                            case None    => fail(s"no invokedynamic instruction:\n$body")
                        }
                    val targetMethod = indy.bootstrapMethod.arguments(1).
                        asInstanceOf[MethodCallMethodHandle]
                    val opcode = targetMethod.opcodeOfUnderlyingInstruction
                    val methodName = targetMethod.name
                    assert(ClassFileFactory.isNewInvokeSpecial(opcode, methodName))
                }

                val SomeType = ObjectType("SomeType")
                val proxy = ClassFileFactory.Proxy(
                    MethodReferences.thisType,
                    MethodReferences.isInterfaceDeclaration,
                    TypeDeclaration(
                        ObjectType("MethodRefConstructorProxy"),
                        false,
                        Some(ObjectType.Object),
                        UIDSet.empty
                    ),
                    "get",
                    MethodDescriptor(ObjectType.String, SomeType),
                    SomeType, false,
                    InvokeSpecialMethodHandle(
                        SomeType,
                        false,
                        "<init>",
                        MethodDescriptor(ObjectType.String, SomeType)
                    ),
                    INVOKESPECIAL.opcode,
                    MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                    ArraySeq.empty // <= not tested...
                )

                val proxyMethod = proxy.findMethod("get").head

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
                    ).head
                    val invokedynamic = filterOutEmptyValuesMethod.body.get.instructions.
                        find(_.isInstanceOf[INVOKEDYNAMIC]).get.asInstanceOf[INVOKEDYNAMIC]
                    val targetMethodHandle = invokedynamic.bootstrapMethod.
                        arguments(1).asInstanceOf[MethodCallMethodHandle]
                    val proxyInterfaceMethodDescriptor = invokedynamic.bootstrapMethod.
                        arguments(2).asInstanceOf[MethodDescriptor]
                    assert(ClassFileFactory.isVirtualMethodReference(
                        targetMethodHandle.opcodeOfUnderlyingInstruction,
                        targetMethodHandle.receiverType.asObjectType,
                        targetMethodHandle.methodDescriptor,
                        proxyInterfaceMethodDescriptor
                    ))
                }

                val SomeOtherType = ObjectType("SomeOtherType")
                val proxy = ClassFileFactory.Proxy(
                    MethodReferences.thisType,
                    MethodReferences.isInterfaceDeclaration,
                    TypeDeclaration(
                        ObjectType("InstanceMethodRefProxy"),
                        false,
                        Some(ObjectType.Object),
                        UIDSet.empty
                    ),
                    "isFull",
                    MethodDescriptor(SomeOtherType, BooleanType),
                    SomeOtherType, false,
                    InvokeVirtualMethodHandle(
                        SomeOtherType,
                        "isThisFull",
                        MethodDescriptor.JustReturnsBoolean
                    ),
                    INVOKEVIRTUAL.opcode,
                    MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                    ArraySeq.empty // <= not tested...
                )

                it("and result in a proxy method that passes in the explicit this") {
                    val proxyMethod = proxy.findMethod("isFull").head
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
                val lambdas = lambdasProject.allProjectClassFiles.find(_.fqn == "lambdas/Lambdas").get
                val theType = ObjectType("ClassFileFactoryTest$newInstanceName")
                val proxy =
                    ClassFileFactory.Proxy(
                        lambdas.thisType,
                        lambdas.isInterfaceDeclaration,
                        TypeDeclaration(
                            ObjectType(theType.toJava + '$'+"Proxy"),
                            false,
                            Some(ObjectType.Object),
                            UIDSet.empty
                        ),
                        "$newInstance",
                        new NoArgumentMethodDescriptor(theType),
                        theType, false,
                        InvokeStaticMethodHandle(
                            theType,
                            false,
                            "newInstance",
                            new NoArgumentMethodDescriptor(theType)
                        ),
                        INVOKESTATIC.opcode,
                        MethodDescriptor.NoArgsAndReturnVoid, // not tested...
                        ArraySeq.empty // <= not tested...
                    )
                val factoryMethod = collectTheFactoryMethod(proxy)
                factoryMethod.name should be(ClassFileFactory.AlternativeFactoryMethodName)
            }
        }

        describe("should create a bridge method to the forwarding method if so desired") {

            it("the bridge method should stack & cast parameters and call the forwarding method") {
                val lambdas = lambdasProject.allProjectClassFiles.find(_.fqn == "lambdas/Lambdas").get
                val receiverType = ObjectType("ClassFileFactoryTest$BridgeCast")
                val proxyType = ObjectType(receiverType.simpleName + '$'+"Proxy")
                val methodDescriptor =
                    MethodDescriptor(ArraySeq(ObjectType.String, DoubleType), IntegerType)
                val proxy =
                    ClassFileFactory.Proxy(
                        lambdas.thisType,
                        lambdas.isInterfaceDeclaration,
                        TypeDeclaration(proxyType, false, Some(ObjectType.Object), UIDSet.empty),
                        "method",
                        methodDescriptor,
                        receiverType, false,
                        InvokeVirtualMethodHandle(
                            receiverType,
                            "method",
                            methodDescriptor
                        ),
                        INVOKESTATIC.opcode,
                        MethodDescriptor.NoArgsAndReturnVoid, // <= not tested...
                        ArraySeq(MethodDescriptor(ArraySeq(ObjectType.Object, DoubleType), IntegerType))
                    )
                val bridge = proxy.methods.find(m => ACC_BRIDGE.isSet(m.accessFlags))
                bridge shouldBe defined
                bridge.get.body shouldBe defined
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

            it("should be able to add multiple bridge methods") {
                val lambdas = lambdasProject.allProjectClassFiles.find(_.fqn == "lambdas/Lambdas").get
                val receiverType = ObjectType("ClassFileFactoryTest$BridgeCast")
                val proxyType = ObjectType(receiverType.simpleName + '$'+"Proxy")
                val methodDescriptor =
                    MethodDescriptor(ArraySeq(ObjectType.String, DoubleType), IntegerType)
                val proxy =
                    ClassFileFactory.Proxy(
                        lambdas.thisType,
                        lambdas.isInterfaceDeclaration,
                        TypeDeclaration(proxyType, false, Some(ObjectType.Object), UIDSet.empty),
                        "method",
                        methodDescriptor,
                        receiverType, false,
                        InvokeVirtualMethodHandle(
                            receiverType,
                            "method",
                            methodDescriptor
                        ),
                        INVOKESTATIC.opcode,
                        MethodDescriptor.NoArgsAndReturnVoid, // <= Not relevant...
                        ArraySeq(
                            MethodDescriptor(ArraySeq(ObjectType.Object, DoubleType), IntegerType),
                            MethodDescriptor(ArraySeq(ObjectType.Serializable, DoubleType), IntegerType)
                        )
                    )
                val bridges = proxy.methods.filter(m => ACC_BRIDGE.isSet(m.accessFlags))
                bridges.length should be(2)
                bridges.foreach { bridge =>
                    bridge.body shouldBe defined
                    val body = bridge.body.get
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
        }

        describe("should compute correct constructor stack and local values") {
            val definingType =
                TypeDeclaration(
                    ObjectType("SomeRandomType"),
                    false,
                    Some(ObjectType.Object),
                    UIDSet.empty
                )

            def testConstructor(
                fieldTypes:     ArraySeq[FieldType],
                expectedLocals: Int, expectedStack: Int
            ): Unit = {
                val fields = fieldTypes.zipWithIndex.map { p =>
                    val (ft, i) = p
                    Field(bi.ACC_PRIVATE.mask, "field"+i, ft, NoAttributes)
                }
                val constructor = ClassFileFactory.createConstructor(definingType, fields)
                constructor.body shouldBe defined
                val code = constructor.body.get
                code.maxStack should be(expectedStack)
                code.maxLocals should be(expectedLocals)
            }

            it("for no fields") {
                testConstructor(ArraySeq.empty, 1, 1)
            }

            it("for a single reference value field") {
                testConstructor(ArraySeq(ObjectType.Object), 2, 2)
            }

            it("for primitive value fields") {
                testConstructor(ArraySeq(IntegerType), 2, 2)
            }

            it("for wide primitive value fields") {
                testConstructor(ArraySeq(DoubleType), 3, 3)
            }

            it("for multiple simple primitive and reference value fields") {
                testConstructor(ArraySeq(ObjectType.Object, IntegerType, IntegerType), 4, 2)
            }

            it("for multiple wide primitive value fields") {
                testConstructor(ArraySeq(DoubleType, LongType, DoubleType), 7, 3)
            }

            it("for everything together") {
                testConstructor(ArraySeq(ObjectType.Object, LongType, IntegerType), 5, 3)
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
                var d = MethodDescriptor(ArraySeq(IntegerType, ObjectType.String), VoidType)
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        ALOAD_1
                    )
                )
                d = MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                ClassFileFactory.parameterForwardingInstructions(
                    d, d, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ALOAD_0
                    )
                )
                d = MethodDescriptor(ArraySeq.fill[FieldType](10)(ByteType), VoidType)
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
                        ArraySeq(DoubleType, ObjectType.String, ByteType,
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
                        ArraySeq(ByteType, CharType, ShortType, IntegerType, FloatType, LongType),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        ArraySeq(ShortType, ShortType, IntegerType,
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

            def valueOfDescriptor(baseType: BaseType): MethodDescriptor = {
                MethodDescriptor(baseType, baseType.WrapperType)
            }

            it("should create boxing instructions for primitive types") {
                val d1 =
                    MethodDescriptor(
                        ArraySeq(ByteType, CharType, ShortType, IntegerType, FloatType, LongType),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        ArraySeq(ObjectType.Byte, ObjectType.Character,
                            ObjectType.Short, ObjectType.Integer, ObjectType.Float, ObjectType.Long),
                        VoidType
                    )
                ClassFileFactory.parameterForwardingInstructions(
                    d1, d2, 0, Seq.empty, ObjectType.Object
                ) should be(
                    Array(
                        ILOAD_0,
                        INVOKESTATIC(ObjectType.Byte, false, "valueOf", valueOfDescriptor(ByteType)),
                        null,
                        null,
                        ILOAD_1,
                        INVOKESTATIC(ObjectType.Character, false, "valueOf", valueOfDescriptor(CharType)),
                        null,
                        null,
                        ILOAD_2,
                        INVOKESTATIC(ObjectType.Short, false, "valueOf", valueOfDescriptor(ShortType)),
                        null,
                        null,
                        ILOAD_3,
                        INVOKESTATIC(ObjectType.Integer, false, "valueOf", valueOfDescriptor(IntegerType)),
                        null,
                        null,
                        FLOAD(4),
                        null,
                        INVOKESTATIC(ObjectType.Float, false, "valueOf", valueOfDescriptor(FloatType)),
                        null,
                        null,
                        LLOAD(5),
                        null,
                        INVOKESTATIC(ObjectType.Long, false, "valueOf", valueOfDescriptor(LongType)),
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
                        ArraySeq(ObjectType.Object, ObjectType.Object),
                        VoidType
                    )
                val d2 =
                    MethodDescriptor(
                        ArraySeq(ObjectType.String, ArrayType.ArrayOfObject),
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
                    CHECKCAST(ArrayType.ArrayOfObject),
                    null,
                    null
                ))
            }

            it("should pack everything into an Object[] if necessary") {
                val d1 =
                    MethodDescriptor(
                        ArraySeq(
                            IntegerType,
                            ObjectType.String,
                            ByteType,
                            BooleanType,
                            ObjectType.Integer,
                            LongType,
                            ShortType
                        ), VoidType
                    )
                val d2 = MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
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
                        INVOKESTATIC(ObjectType.Integer, false, "valueOf", valueOfDescriptor(IntegerType)),
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
                        INVOKESTATIC(ObjectType.Byte, false, "valueOf", valueOfDescriptor(ByteType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        ICONST_3,
                        ILOAD_3,
                        INVOKESTATIC(ObjectType.Boolean, false, "valueOf", valueOfDescriptor(BooleanType)),
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
                        INVOKESTATIC(ObjectType.Long, false, "valueOf", valueOfDescriptor(LongType)),
                        null,
                        null,
                        AASTORE,
                        DUP,
                        BIPUSH(6),
                        null,
                        ILOAD(7),
                        null,
                        INVOKESTATIC(ObjectType.Short, false, "valueOf", valueOfDescriptor(ShortType)),
                        null,
                        null,
                        AASTORE
                    )
                )
            }
        }

        describe("should create correct return/conversion instructions") {

            it("should convert Object to base types by casting and unboxing") {
                BaseType.baseTypes foreach { t =>
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
                    t => ClassFileFactory.returnAndConvertInstructions(t, ObjectType.Object) should be(
                        Array(CHECKCAST(t.asReferenceType), null, null, ARETURN)
                    )
                )
            }

            it("should convert other types if possible, e.g. base types to wrappers") {
                import ObjectType.Byte
                ClassFileFactory.returnAndConvertInstructions(Byte, ByteType) should be(
                    Array(
                        INVOKESTATIC(Byte, false, "valueOf", MethodDescriptor(ByteType, Byte)),
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

        describe("should handle serializable lambdas correctly") {
            val IntersectionTypes = lambdasProject.allProjectClassFiles.find(
                _.fqn == "lambdas/methodreferences/IntersectionTypes"
            ).get
            val deserializedLambdaMethodDescriptor =
                MethodDescriptor(
                    ArraySeq(ObjectType.SerializedLambda),
                    ObjectType.Object
                )
            val implMethod = InvokeSpecialMethodHandle(
                IntersectionTypes.thisType,
                false,
                "lambda$1",
                MethodDescriptor(ArraySeq(
                    ObjectType.Float,
                    ObjectType.String,
                    ObjectType.Integer
                ), ObjectType.String)
            )
            val samMethodType = MethodDescriptor(ObjectType.Object, ObjectType.Object)
            val instantiatedMethodType = MethodDescriptor(ObjectType.String, ObjectType.Object)
            val proxy = ClassFileFactory.Proxy(
                IntersectionTypes.thisType,
                IntersectionTypes.isInterfaceDeclaration,
                TypeDeclaration(
                    ObjectType("SerializableProxy"),
                    false,
                    Some(ObjectType.Object),
                    UIDSet(ObjectType("java/util/Function"), ObjectType.Serializable)
                ),
                "apply",
                instantiatedMethodType,
                IntersectionTypes.thisType, false,
                implMethod,
                INVOKESPECIAL.opcode,
                samMethodType,
                ArraySeq.empty[MethodDescriptor]
            )

            it("should add writeReplace and $deserializeLambda$ methods") {
                proxy.findMethod("writeReplace", MethodDescriptor.JustReturnsObject) shouldBe defined
                proxy.findMethod("$deserializeLambda$", deserializedLambdaMethodDescriptor) shouldBe defined
            }

            it("should forward $deserializeLambda$ to the caller") {
                val dl = proxy.findMethod("$deserializeLambda$", deserializedLambdaMethodDescriptor).get

                dl.body shouldBe defined
                dl.body.get.instructions should be(Array(
                    ALOAD_0,
                    INVOKESTATIC(
                        IntersectionTypes.thisType,
                        IntersectionTypes.isInterfaceDeclaration,
                        "$deserializeLambda$",
                        deserializedLambdaMethodDescriptor
                    ), null, null,
                    ARETURN
                ))
            }

            it("should create a SerializedLambda object inside writeReplace") {
                val wr = proxy.findMethod("writeReplace", MethodDescriptor.JustReturnsObject).get

                wr.body shouldBe defined
                wr.body.get.instructions should be(Array(
                    NEW(ObjectType.SerializedLambda), null, null,
                    DUP,
                    LoadClass(ObjectType("SerializableProxy")), null,
                    LDC(ConstantString(ObjectType("java/util/Function").fqn)), null,
                    LDC(ConstantString("apply")), null,
                    LDC(ConstantString(samMethodType.toJVMDescriptor)), null,
                    LDC(ConstantInteger(implMethod.referenceKind.referenceKind)), null,
                    LDC(ConstantString(implMethod.receiverType.asObjectType.fqn)), null,
                    LDC(ConstantString(implMethod.name)), null,
                    LDC(ConstantString(implMethod.methodDescriptor.toJVMDescriptor)), null,
                    LDC(ConstantString(instantiatedMethodType.toJVMDescriptor)), null,
                    // Add the caputeredArgs
                    BIPUSH(2), null,
                    ANEWARRAY(ObjectType.Object), null, null,
                    DUP,
                    BIPUSH(0), null,
                    ALOAD_0,
                    GETFIELD(ObjectType("SerializableProxy"), "staticParameter0", ObjectType.Float), null, null,
                    AASTORE,
                    DUP,
                    BIPUSH(1), null,
                    ALOAD_0,
                    GETFIELD(ObjectType("SerializableProxy"), "staticParameter1", ObjectType.String), null, null,
                    AASTORE,
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
                ))
            }
        }
    }

    /**
     * Searches the array of `remainingInstructions` for the next one
     * that is searched for.
     *
     * For example, if the first Instruction of the array is an `ICONST_1`,
     * `require(Set(ICONST_1.opcode),...)` would return 1.
     *
     * If no instruction can be found, an `AssertionError` will be thrown.
     */
    private def require(
        oneOf:                 Set[Opcode],
        remainingInstructions: Array[Instruction]
    ): Int = {
        val instructions = remainingInstructions.iterator.filter(_ != null)
        val indexOfFirstMatchingInstruction = instructions.indexWhere(oneOf contains _.opcode)
        assert(
            indexOfFirstMatchingInstruction != -1,
            s"Could not find required instruction ${oneOf.mkString(",")}"
        )

        indexOfFirstMatchingInstruction + 1
    }

    private def requireIntLoad(remainingInstructions: Array[Instruction]): Int = {
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
    }

    private def requireLongLoad(remainingInstructions: Array[Instruction]): Int = {
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
    }

    private def requireFloatLoad(remainingInstructions: Array[Instruction]): Int = {
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
    }

    private def requireDoubleLoad(remainingInstructions: Array[Instruction]): Int = {
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
    }

    private def requireReferenceLoadOrCreation(remainingInstructions: Array[Instruction]): Int = {
        val required = Set(
            ALOAD_0.opcode,
            ALOAD_1.opcode,
            ALOAD_2.opcode,
            ALOAD_3.opcode,
            ALOAD.opcode,
            NEW.opcode
        )
        require(required, remainingInstructions)
    }

    /**
     * Iterates over the given list of methods to generate a proxy class file for the
     * method specified by each tuple, and passes that proxy class file to the provided
     * test function.
     */
    private def testMethods(
        methods:    Iterable[(ObjectType, Method)],
        repository: ClassFileRepository
    )(
        test: (ClassFile, (ObjectType, Method)) => Unit
    ): Unit = {
        for (
            (calleeType, calleeMethod) <- methods
        ) {
            testMethod(calleeType, calleeMethod, repository)(test)
        }
    }

    private def testMethod(
        calleeType: ObjectType, calleeMethod: Method,
        repository: ClassFileRepository
    )(
        test: (ClassFile, (ObjectType, Method)) => Unit
    ): Unit = {
        val calleeMethodName = calleeMethod.name
        val calleeMethodDescriptor = calleeMethod.descriptor
        val definingTypeName = calleeType.simpleName+"$"+calleeMethodName
        val definingType =
            TypeDeclaration(
                ObjectType(definingTypeName),
                false,
                Some(ObjectType.Object),
                UIDSet.empty
            )
        val methodName = calleeMethodName+"$Forwarded"
        val methodDescriptor = calleeMethodDescriptor
        val calleeIsInterface = repository.classFile(calleeType).get.isInterfaceDeclaration
        val (invocationInstruction: Opcode, methodHandle: MethodCallMethodHandle) =
            if (calleeIsInterface) {
                (
                    INVOKEINTERFACE.opcode,
                    InvokeInterfaceMethodHandle(
                        calleeType,
                        calleeMethodName,
                        calleeMethodDescriptor
                    )
                )
            } else if (calleeMethod.isStatic) {
                (
                    INVOKESTATIC.opcode,
                    InvokeStaticMethodHandle(
                        calleeType,
                        calleeIsInterface,
                        calleeMethodName,
                        calleeMethodDescriptor
                    )
                )
            } else if (calleeMethod.isPrivate) {
                (
                    INVOKESPECIAL.opcode,
                    InvokeSpecialMethodHandle(
                        calleeType,
                        calleeIsInterface,
                        calleeMethodName,
                        calleeMethodDescriptor
                    )
                )
            } else {
                (
                    INVOKEVIRTUAL.opcode,
                    InvokeVirtualMethodHandle(
                        calleeType,
                        calleeMethodName,
                        calleeMethodDescriptor
                    )
                )
            }
        val classFile =
            ClassFileFactory.Proxy(
                repository.classFile(calleeType).get.thisType,
                calleeIsInterface,
                definingType,
                methodName, methodDescriptor,
                calleeType, calleeIsInterface,
                methodHandle, invocationInstruction,
                MethodDescriptor.NoArgsAndReturnVoid, // <= not tested...
                ArraySeq.empty[MethodDescriptor] // <= not tested...
            )

        test(classFile, (calleeType, calleeMethod))
    }
}
