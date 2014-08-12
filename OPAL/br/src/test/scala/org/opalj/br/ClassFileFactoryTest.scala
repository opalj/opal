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

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.bi.TestSupport.locateTestResources
import analyses.Project
import instructions._

@RunWith(classOf[JUnitRunner])
class ClassFileFactoryTest extends FunSpec with Matchers {
    val testProject = Project(locateTestResources("classfiles/proxy.jar", "br"))

    val StaticMethods = ObjectType("proxy/StaticMethods")
    val InstanceMethods = ObjectType("proxy/InstanceMethods")
    val Constructors = ObjectType("proxy/Constructors")
    val PrivateInstanceMethods = ObjectType("proxy/PrivateInstanceMethods")
    val InterfaceMethods = ObjectType("proxy/InterfaceMethods")

    private def getMethods(
        theClass: ObjectType,
        repository: ClassFileRepository): Iterable[(ObjectType, Method)] =
        repository.classFile(theClass).map { cf ⇒
            cf.methods.map((theClass, _))
        }.getOrElse(Iterable.empty)

    describe("a Proxy ClassFile") {
        describe("should proxify instance methods") {
            val instanceMethods = getMethods(InstanceMethods, testProject)
            instanceMethods should not be ('empty)
            val constructors = getMethods(Constructors, testProject)
            constructors should not be ('empty)
            val privateInstanceMethods = getMethods(PrivateInstanceMethods, testProject)
            privateInstanceMethods should not be ('empty)
            val interfaceMethods = getMethods(InterfaceMethods, testProject)
            interfaceMethods should not be ('empty)
            val methods: Iterable[(ObjectType, Method)] = instanceMethods ++
                constructors ++ privateInstanceMethods ++ interfaceMethods

            it("with one instance field") {
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    classFile.fields should have size (1)
                    val field = classFile.fields(0)
                    bi.ACC_FINAL.unapply(field.accessFlags) should be(true)
                    hasFlag(field.accessFlags, bi.ACC_FINAL.mask) should be(true)
                    hasFlag(field.accessFlags, bi.ACC_PRIVATE.mask) should be(true)
                    field.fieldType should be(tuple._1)
                }
            }

            it("and a constructor that sets that instance field") {
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    val constructor = classFile.methods.find(_.isConstructor)
                    constructor should be('defined)
                    for (cons ← constructor) {
                        hasFlag(cons.accessFlags, bi.ACC_PUBLIC.mask) should be(true)
                        cons.parameterTypes should have size (1)
                        cons.parameterTypes(0) should be(tuple._1)

                        cons.body should be('defined)
                        for (body ← cons.body; instructions = body.instructions) {
                            body.maxLocals should be(2)
                            body.maxStack should be(2)
                            instructions should be(Array(
                                ALOAD_0,
                                INVOKESPECIAL(ObjectType.Object,
                                    "<init>",
                                    NoArgumentAndNoReturnValueMethodDescriptor),
                                ALOAD_0,
                                ALOAD_1,
                                PUTFIELD(classFile.thisType,
                                    classFile.fields(0).name,
                                    classFile.fields(0).fieldType),
                                RETURN))
                        }
                    }
                }
            }

            it("and one forwarding method") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    classFile.methods.filterNot(_.isConstructor) should have size (1)
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    hasFlag(method.accessFlags, bi.ACC_PUBLIC.mask) should be(true)
                }
            }

            it("that calls the callee method with invokevirtual, invokespecial if private, or invokeinterface if the target type is an interface") {
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    val (calleeType, calleeMethod) = tuple
                    for (body ← method.body) {
                        if (testProject.classFile(tuple._1).get.isInterfaceDeclaration) {
                            body.instructions should contain(INVOKEINTERFACE(
                                calleeType,
                                calleeMethod.name,
                                calleeMethod.descriptor
                            ))
                        } else if (tuple._2.isPrivate) {
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
            }

            it("and passes all parameters correctly [barring reference type check]") {
                testMethods(methods, testProject) { (classFile, triple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    var currentInstruction = 0
                    for {
                        body ← method.body
                        instructions = body.instructions
                        invoke = instructions.find(_.isInstanceOf[INVOKESTATIC])
                        indexOfInvoke = instructions.indexOf(invoke)
                        instructionsPrecedingInvoke = instructions.slice(0, indexOfInvoke)
                        requiredParameter ← method.parameterTypes
                    } {
                        val remainingInstructions = instructions.slice(currentInstruction, instructions.size)
                        val consumedInstructions = requiredParameter match {
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
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    for (body ← method.body) {
                        val operandsSize: Int = 1 + // for `this` 
                            method.parameterTypes.map(_.computationalType.operandSize).sum

                        val returnSize: Int =
                            if (method.returnType != VoidType) {
                                method.returnType.computationalType.operandSize
                            } else {
                                0
                            }
                        val stackSize = math.max(operandsSize, returnSize)
                        body.maxStack should be(stackSize)
                        body.maxLocals should be(1 + operandsSize + returnSize)
                    }
                }
            }

            val methodWithManyParametersAndNoReturnValue = testProject.
                classFile(InstanceMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").get

            it("and produces correctly indexed load instructions") {
                testMethod(InstanceMethods,
                    methodWithManyParametersAndNoReturnValue,
                    testProject) { (classFile, tuple) ⇒
                        val calleeField = classFile.fields.head
                        val method = classFile.methods.filterNot(_.isConstructor).head
                        method.body should be('defined)
                        val (calleeType, calleeMethod) = tuple
                        for (body ← method.body; instructions = body.instructions) {
                            body.maxStack should be(13)
                            body.maxLocals should be(14)
                            instructions should be(Array(
                                ALOAD_0,
                                GETFIELD(classFile.thisType,
                                    calleeField.name,
                                    calleeField.fieldType),
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
                                INVOKEVIRTUAL(calleeType,
                                    calleeMethod.name,
                                    calleeMethod.descriptor),
                                null,
                                null,
                                RETURN
                            ))
                        }
                    }
            }
        }

        describe("should proxify static methods") {
            val methods = getMethods(StaticMethods, testProject).filter(_._2.isStatic)
            methods should not be ('empty)

            it("with no instance field") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    classFile.fields should be('empty)
                }
            }

            it("and an empty default constructor") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    val constructor = classFile.constructors.next
                    hasFlag(constructor.accessFlags, bi.ACC_PUBLIC.mask) should be(true)
                    constructor.body should be('defined)
                    for (body ← constructor.body) {
                        body.maxLocals should be(1)
                        body.maxStack should be(1)
                        body.instructions should be(Array(
                            instructions.ALOAD_0,
                            instructions.INVOKESPECIAL(classFile.superclassType.getOrElse(ObjectType.Object),
                                "<init>",
                                NoArgumentAndNoReturnValueMethodDescriptor),
                            instructions.RETURN
                        ))
                    }
                }
            }

            it("and one forwarding method") {
                testMethods(methods, testProject) { (classFile, _) ⇒
                    classFile.methods.filterNot(_.isConstructor) should have size (1)
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    hasFlag(method.accessFlags, bi.ACC_PUBLIC.mask) should be(true)
                }
            }

            it("that calls the callee method with invokestatic") {
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    val (calleeType, calleeMethod) = tuple
                    for (body ← method.body) {
                        body.instructions should contain(INVOKESTATIC(
                            calleeType,
                            calleeMethod.name,
                            calleeMethod.descriptor
                        ))
                    }
                }
            }

            it("and passes all parameters correctly [barring reference type check]") {
                // TODO rename: "triple" doesn't fit at all!
                testMethods(methods, testProject) { (classFile, triple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    var currentInstruction = 0
                    for {
                        body ← method.body
                        instructions = body.instructions
                        invoke = instructions.find(_.isInstanceOf[INVOKESTATIC])
                        indexOfInvoke = instructions.indexOf(invoke)
                        instructionsPrecedingInvoke = instructions.slice(0, indexOfInvoke)
                        requiredParameter ← method.parameterTypes
                    } {
                        val remainingInstructions = instructions.slice(currentInstruction, instructions.size)
                        val consumedInstructions = requiredParameter match {
                            case BooleanType | ByteType | CharType | ShortType | IntegerType ⇒
                                requireInt(remainingInstructions)
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
                testMethods(methods, testProject) { (classFile, tuple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    for (body ← method.body) {
                        val operandsSize: Int = method.parameterTypes.map(_.computationalType.operandSize).sum
                        val returnSize: Int =
                            if (method.returnType != VoidType) {
                                method.returnType.computationalType.operandSize
                            } else {
                                0
                            }
                        val stackSize = math.max(operandsSize, returnSize)
                        body.maxStack should be(stackSize)
                        body.maxLocals should be(1 + operandsSize + returnSize)
                    }
                }
            }

            val methodWithManyParametersAndNoReturnValue = testProject.
                classFile(StaticMethods).get.
                findMethod("methodWithManyParametersAndNoReturnValue").get

            it("and produces correctly indexed load instructions") {
                testMethod(StaticMethods,
                    methodWithManyParametersAndNoReturnValue,
                    testProject) { (classFile, tuple) ⇒
                        val method = classFile.methods.filterNot(_.isConstructor).head
                        method.body should be('defined)
                        val (calleeType, calleeMethod) = tuple
                        for (body ← method.body; instructions = body.instructions) {
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
                                INVOKESTATIC(calleeType,
                                    calleeMethod.name,
                                    calleeMethod.descriptor),
                                null,
                                null,
                                RETURN
                            ))
                        }
                    }
            }

            it("and handles multiple parameters of the same type correctly") {
                val methodWithFiveDoubleParameters = testProject.
                    classFile(StaticMethods).get.findMethod(
                        "doubleDoubleDoubleDoubleDoubleAndNoReturnValue").get
                val proxy = ClassFileFactory.Proxy(
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
                    true,
                    false,
                    false)

                val method = proxy.methods.filterNot(_.isConstructor).head
                method.body should be('defined)
                for (body ← method.body; instructions = body.instructions) {
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
                            methodWithFiveDoubleParameters.descriptor),
                        null,
                        null,
                        RETURN
                    ))
                }
            }

            it("and returns correctly") {
                testMethods(methods, testProject) { (classFile, triple) ⇒
                    val method = classFile.methods.filterNot(_.isConstructor).head
                    method.body should be('defined)
                    method.body foreach { body ⇒
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
                }
            }

        }
    }

    describe("the ClassFileFactory") {
        describe("should compute correct constructor stack and local values") {
            val definingType = TypeDeclaration(
                ObjectType("SomeRandomType"),
                false,
                Some(ObjectType.Object),
                Set.empty)

            def testConstructor(fieldTypes: IndexedSeq[FieldType],
                                expectedLocals: Int, expectedStack: Int) {
                val fields = fieldTypes.zipWithIndex.map { p ⇒
                    val (ft, i) = p
                    Field(bi.ACC_PRIVATE.mask, "field"+i, ft, Seq.empty)
                }
                val constructor = ClassFileFactory.createConstructor(definingType, fields)
                constructor.body should be('defined)
                for (code ← constructor.body) {
                    code.maxStack should be(expectedStack)
                    code.maxLocals should be(expectedLocals)
                }
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
    }

    // checks if the given flag is among the given combination of flags
    private def hasFlag(flags: Int, flag: Int): Boolean =
        (flags & flag) == flag

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
        oneOf: Set[Opcode],
        remainingInstructions: Array[Instruction]): Int = {
        val indexOfNextFittingInstruction =
            remainingInstructions.filter(_ != null).
                indexWhere(instruction ⇒ oneOf contains instruction.opcode)
        indexOfNextFittingInstruction should not be (-1)

        indexOfNextFittingInstruction + 1
    }

    private def requireInt(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                ILOAD_0.opcode,
                ILOAD_1.opcode,
                ILOAD_2.opcode,
                ILOAD_3.opcode,
                ILOAD.opcode,
                ICONST_0.opcode // TODO Why does this make sense?
            ),
            remainingInstructions)

    private def requireLong(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                LLOAD_0.opcode,
                LLOAD_1.opcode,
                LLOAD_2.opcode,
                LLOAD_3.opcode,
                LLOAD.opcode,
                LCONST_0.opcode // TODO Why does this make sense?
            ),
            remainingInstructions)

    private def requireFloat(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                FLOAD_0.opcode,
                FLOAD_1.opcode,
                FLOAD_2.opcode,
                FLOAD_3.opcode,
                FLOAD.opcode,
                FCONST_0.opcode // TODO Why does this make sense?
            ),
            remainingInstructions)

    private def requireDouble(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                DLOAD_0.opcode,
                DLOAD_1.opcode,
                DLOAD_2.opcode,
                DLOAD_3.opcode,
                DLOAD.opcode,
                DCONST_0.opcode // TODO Why does this make sense?
            ),
            remainingInstructions)

    private def requireReference(remainingInstructions: Array[Instruction]): Int =
        require(
            Set(
                ALOAD_0.opcode,
                ALOAD_1.opcode,
                ALOAD_2.opcode,
                ALOAD_3.opcode,
                ALOAD.opcode,
                ACONST_NULL.opcode // TODO Why does this make sense?
            ),
            remainingInstructions)

    /**
     * Iterates over the given list of triples, generating a proxy class file for the
     * method specified by each triple, and passes that proxy class file to the provided
     * test function.
     */
    def testMethods(
        methods: Iterable[(ObjectType, Method)],
        repository: ClassFileRepository)(
            test: (ClassFile, (ObjectType, Method)) ⇒ Unit): Unit = {
        for {
            (calleeType, calleeMethod) ← methods
        } {
            testMethod(calleeType, calleeMethod, repository)(test)
        }
    }

    def testMethod(
        calleeType: ObjectType, calleeMethod: Method,
        repository: ClassFileRepository)(
            test: (ClassFile, (ObjectType, Method)) ⇒ Unit) {
        val calleeMethodName = calleeMethod.name
        val calleeMethodDescriptor = calleeMethod.descriptor
        val definingTypeName = calleeType.simpleName+"$"+calleeMethodName
        val definingType = TypeDeclaration(
            ObjectType(definingTypeName),
            false,
            Some(ObjectType.Object),
            Set())
        val methodName = calleeMethodName+"$Forwarded"
        val methodDescriptor = calleeMethodDescriptor
        val classFile = ClassFileFactory.Proxy(
            definingType,
            methodName,
            methodDescriptor,
            calleeType,
            calleeMethodName,
            calleeMethodDescriptor,
            calleeMethod.isStatic,
            calleeMethod.isPrivate,
            repository.classFile(calleeType).get.isInterfaceDeclaration
        )

        test(classFile, (calleeType, calleeMethod))
    }
}