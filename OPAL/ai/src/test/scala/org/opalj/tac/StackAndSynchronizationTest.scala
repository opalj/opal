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
package tac

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomain

/**
 * Tests the conversion of parsed methods to a quadruple representation
 *
 * @author Roberts Kolosovs
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class StackAndSynchronizationTest extends FunSpec with Matchers {

    val StackAndSynchronizeType = ObjectType("tactest/StackManipulationAndSynchronization")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val StackAndSynchronizeClassFile = project.classFile(StackAndSynchronizeType).get

    val PopMethod = StackAndSynchronizeClassFile.findMethod("pop").get
    val Pop2Case2Method = StackAndSynchronizeClassFile.findMethod("pop2case2").get
    val DupMethod = StackAndSynchronizeClassFile.findMethod("dup").get
    val MonitorEnterAndExitMethod = StackAndSynchronizeClassFile.findMethod("monitorEnterAndExit").get
    val InvokeStaticMethod = StackAndSynchronizeClassFile.findMethod("invokeStatic").get
    val InvokeInterfaceMethod = StackAndSynchronizeClassFile.findMethod("invokeInterface").get

    describe("The quadruples representation of stack manipulation and synchronization instructions") {

        describe("using no AI results") {
            it("should correctly reflect pop") {
                val statements = AsQuadruples(PopMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(
                        -1,
                        SimpleVar(-1, ComputationalTypeReference),
                        Param(ComputationalTypeReference, "this")
                    ),
                    Assignment(
                        0,
                        SimpleVar(0, ComputationalTypeReference),
                        SimpleVar(-1, ComputationalTypeReference)
                    ),
                    Assignment(
                        1,
                        SimpleVar(0, ComputationalTypeInt),
                        VirtualFunctionCall(
                            1,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "returnInt",
                            MethodDescriptor(IndexedSeq[FieldType](), IntegerType),
                            SimpleVar(0, ComputationalTypeReference),
                            List()
                        )
                    ),
                    Nop(4),
                    Return(5)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnInt();",
                    "3: ;",
                    "4: return;"
                ))
            }

            it("should correctly reflect pop2 mode 2") {
                val statements = AsQuadruples(method = Pop2Case2Method, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                    Assignment(
                        1,
                        SimpleVar(0, ComputationalTypeDouble),
                        VirtualFunctionCall(
                            1,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "returnDouble",
                            MethodDescriptor(IndexedSeq[FieldType](), DoubleType),
                            SimpleVar(0, ComputationalTypeReference),
                            List()
                        )
                    ),
                    Nop(4),
                    Return(5)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnDouble();",
                    "3: ;",
                    "4: return;"
                ))
            }

            it("should correctly reflect dup") {
                val statements = AsQuadruples(method = DupMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType.Object)),
                    Nop(3),
                    NonVirtualMethodCall(4, ObjectType.Object, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(0, ComputationalTypeReference), List()),
                    Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Return(8)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = new Object;",
                    "2: ;",
                    "3: op_0/* (Non-Virtual) java.lang.Object*/.<init>();",
                    "4: r_1 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect monitorenter and -exit") {
                val statements = AsQuadruples(method = MonitorEnterAndExitMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                    Nop(1),
                    Assignment(2, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    MonitorEnter(3, SimpleVar(0, ComputationalTypeReference)),
                    Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    MonitorExit(5, SimpleVar(0, ComputationalTypeReference)),
                    Goto(6, 13),
                    Assignment(9, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Assignment(10, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    MonitorExit(11, SimpleVar(0, ComputationalTypeReference)),
                    Assignment(12, SimpleVar(0, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
                    Throw(13, SimpleVar(0, ComputationalTypeReference)),
                    Return(14)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: ;",
                    "3: r_1 = op_0;",
                    "4: monitorenter op_0;",
                    "5: op_0 = r_1;",
                    "6: monitorexit op_0;",
                    "7: goto 13;",
                    "8: r_2 = op_0;",
                    "9: op_0 = r_1;",
                    "10: monitorexit op_0;",
                    "11: op_0 = r_2;",
                    "12: throw op_0;",
                    "13: return;"
                ))
            }

            it("should correctly reflect invokestatic") {
                val statements = AsQuadruples(method = InvokeStaticMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 1)),
                    Assignment(1, SimpleVar(1, ComputationalTypeInt), IntConst(1, 2)),
                    Assignment(
                        2,
                        SimpleVar(0, ComputationalTypeInt),
                        StaticFunctionCall(
                            2,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "staticMethod",
                            MethodDescriptor(IndexedSeq[FieldType](IntegerType, IntegerType), IntegerType),
                            List(SimpleVar(1, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt))
                        )
                    ),
                    Assignment(5, SimpleVar(-2, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                    Return(6)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = 1;",
                    "2: op_1 = 2;",
                    "3: op_0 = tactest.StackManipulationAndSynchronization.staticMethod(op_1, op_0);",
                    "4: r_1 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect invokeinterface") {
                val statements = AsQuadruples(method = InvokeInterfaceMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType("java/util/ArrayList"))),
                    Nop(3),
                    NonVirtualMethodCall(4, ObjectType("java/util/ArrayList"), "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(0, ComputationalTypeReference), List()),
                    Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Assignment(8, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    Assignment(9, SimpleVar(1, ComputationalTypeReference), New(9, ObjectType.Object)),
                    Nop(12),
                    NonVirtualMethodCall(13, ObjectType.Object, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(1, ComputationalTypeReference), List()),
                    Assignment(
                        16,
                        SimpleVar(0, ComputationalTypeInt),
                        VirtualFunctionCall(
                            16,
                            ObjectType("java/util/List"),
                            "add",
                            MethodDescriptor(IndexedSeq[FieldType](ObjectType.Object), BooleanType),
                            SimpleVar(0, ComputationalTypeReference),
                            List(SimpleVar(1, ComputationalTypeReference))
                        )
                    ),
                    Nop(21),
                    Return(22)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = new ArrayList;",
                    "2: ;",
                    "3: op_0/* (Non-Virtual) java.util.ArrayList*/.<init>();",
                    "4: r_1 = op_0;",
                    "5: op_0 = r_1;",
                    "6: op_1 = new Object;",
                    "7: ;",
                    "8: op_1/* (Non-Virtual) java.lang.Object*/.<init>();",
                    "9: op_0 = op_0/*java.util.List*/.add(op_1);",
                    "10: ;",
                    "11: return;"
                ))
            }
        }

        describe("using AI results") {
            it("should correctly reflect pop") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, PopMethod)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, PopMethod, domain)
                val statements = AsQuadruples(method = PopMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                    Assignment(
                        1,
                        SimpleVar(0, ComputationalTypeInt),
                        VirtualFunctionCall(
                            1,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "returnInt",
                            MethodDescriptor(IndexedSeq[FieldType](), IntegerType),
                            SimpleVar(0, ComputationalTypeReference),
                            List()
                        )
                    ),
                    Nop(4),
                    Return(5)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnInt();",
                    "3: ;",
                    "4: return;"
                ))
            }

            it("should correctly reflect pop2 mode 2") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, Pop2Case2Method)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, Pop2Case2Method, domain)
                val statements = AsQuadruples(method = Pop2Case2Method, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                    Assignment(
                        1,
                        SimpleVar(0, ComputationalTypeDouble),
                        VirtualFunctionCall(
                            1,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "returnDouble",
                            MethodDescriptor(IndexedSeq[FieldType](), DoubleType),
                            SimpleVar(0, ComputationalTypeReference),
                            List()
                        )
                    ),
                    Nop(4),
                    Return(5)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnDouble();",
                    "3: ;",
                    "4: return;"
                ))
            }

            it("should correctly reflect dup") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, DupMethod)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, DupMethod, domain)
                val statements = AsQuadruples(method = DupMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType.Object)),
                    Nop(3),
                    NonVirtualMethodCall(4, ObjectType.Object, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(0, ComputationalTypeReference), List()),
                    Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Return(8)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = new Object;",
                    "2: ;",
                    "3: op_0/* (Non-Virtual) java.lang.Object*/.<init>();",
                    "4: r_1 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect monitorenter and -exit") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, MonitorEnterAndExitMethod)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, MonitorEnterAndExitMethod, domain)
                val statements = AsQuadruples(method = MonitorEnterAndExitMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                    Nop(1),
                    Assignment(2, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    MonitorEnter(3, SimpleVar(0, ComputationalTypeReference)),
                    Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    MonitorExit(5, SimpleVar(0, ComputationalTypeReference)),
                    Goto(6, 13),
                    Assignment(9, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Assignment(10, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    MonitorExit(11, SimpleVar(0, ComputationalTypeReference)),
                    Assignment(12, SimpleVar(0, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
                    Throw(13, SimpleVar(0, ComputationalTypeReference)),
                    Return(14)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = r_0;",
                    "2: ;",
                    "3: r_1 = op_0;",
                    "4: monitorenter op_0;",
                    "5: op_0 = r_1;",
                    "6: monitorexit op_0;",
                    "7: goto 13;",
                    "8: r_2 = op_0;",
                    "9: op_0 = r_1;",
                    "10: monitorexit op_0;",
                    "11: op_0 = r_2;",
                    "12: throw op_0;",
                    "13: return;"
                ))
            }

            it("should correctly reflect invokestatic") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, InvokeStaticMethod)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, InvokeStaticMethod, domain)
                val statements = AsQuadruples(method = InvokeStaticMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 1)),
                    Assignment(1, SimpleVar(1, ComputationalTypeInt), IntConst(1, 2)),
                    Assignment(
                        2,
                        SimpleVar(0, ComputationalTypeInt),
                        StaticFunctionCall(
                            2,
                            ObjectType("tactest/StackManipulationAndSynchronization"),
                            "staticMethod",
                            MethodDescriptor(IndexedSeq[FieldType](IntegerType, IntegerType), IntegerType),
                            List(SimpleVar(1, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt))
                        )
                    ),
                    Assignment(5, SimpleVar(-2, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                    Return(6)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = 1;",
                    "2: op_1 = 2;",
                    "3: op_0 = tactest.StackManipulationAndSynchronization.staticMethod(op_1, op_0);",
                    "4: r_1 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect invokeinterface") {
                val domain = new DefaultDomain(project, StackAndSynchronizeClassFile, InvokeInterfaceMethod)
                val aiResult = BaseAI(StackAndSynchronizeClassFile, InvokeInterfaceMethod, domain)
                val statements = AsQuadruples(method = InvokeInterfaceMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType("java/util/ArrayList"))),
                    Nop(3),
                    NonVirtualMethodCall(
                        4,
                        ObjectType("java/util/ArrayList"),
                        "<init>",
                        MethodDescriptor(IndexedSeq[FieldType](), VoidType),
                        SimpleVar(0, ComputationalTypeReference),
                        List()
                    ),
                    Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Assignment(8, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    Assignment(9, SimpleVar(1, ComputationalTypeReference), New(9, ObjectType.Object)),
                    Nop(12),
                    NonVirtualMethodCall(
                        13,
                        ObjectType.Object,
                        "<init>",
                        MethodDescriptor(IndexedSeq[FieldType](), VoidType),
                        SimpleVar(1, ComputationalTypeReference),
                        List()
                    ),
                    Assignment(
                        16,
                        SimpleVar(0, ComputationalTypeInt),
                        VirtualFunctionCall(
                            16,
                            ObjectType("java/util/List"),
                            "add",
                            MethodDescriptor(IndexedSeq(ObjectType.Object), BooleanType),
                            SimpleVar(0, ComputationalTypeReference),
                            List(SimpleVar(1, ComputationalTypeReference))
                        )
                    ),
                    Nop(21),
                    Return(22)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: op_0 = new ArrayList;",
                    "2: ;",
                    "3: op_0/* (Non-Virtual) java.util.ArrayList*/.<init>();",
                    "4: r_1 = op_0;",
                    "5: op_0 = r_1;",
                    "6: op_1 = new Object;",
                    "7: ;",
                    "8: op_1/* (Non-Virtual) java.lang.Object*/.<init>();",
                    "9: op_0 = op_0/*java.util.List*/.add(op_1);",
                    "10: ;",
                    "11: return;"
                ))
            }
        }
    }

}
