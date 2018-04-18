/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.br.TestSupport.biProject

/**
 * @author Roberts Kolosovs
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveStackAndSynchronizationTest extends TACNaiveTest {

    val StackAndSynchronizeType = ObjectType("tactest/StackManipulationAndSynchronization")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val StackAndSynchronizeClassFile = project.classFile(StackAndSynchronizeType).get

    val PopMethod = StackAndSynchronizeClassFile.findMethod("pop").head
    val Pop2Case2Method = StackAndSynchronizeClassFile.findMethod("pop2case2").head
    val DupMethod = StackAndSynchronizeClassFile.findMethod("dup").head
    val MonitorEnterAndExitMethod = StackAndSynchronizeClassFile.findMethod("monitorEnterAndExit").head
    val InvokeStaticMethod = StackAndSynchronizeClassFile.findMethod("invokeStatic").head
    val InvokeInterfaceMethod = StackAndSynchronizeClassFile.findMethod("invokeInterface").head

    describe("the naive TAC of stack manipulation and synchronization instructions") {

        it("should correctly reflect pop") {
            val statements = TACNaive(PopMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

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
                        ObjectType("tactest/StackManipulationAndSynchronization"), false,
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
                "0: r_0 = this",
                "1: op_0 = r_0",
                "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnInt()",
                "3: ;",
                "4: return"
            ).mkString("\n"))
        }

        it("should correctly reflect pop2 mode 2") {
            val statements = TACNaive(method = Pop2Case2Method, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

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
                        false,
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
                "0: r_0 = this",
                "1: op_0 = r_0",
                "2: op_0 = op_0/*tactest.StackManipulationAndSynchronization*/.returnDouble()",
                "3: ;",
                "4: return"
            ).mkString("\n"))
        }

        it("should correctly reflect dup") {
            val statements = TACNaive(method = DupMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType.Object)),
                Nop(3),
                NonVirtualMethodCall(4, ObjectType.Object, false, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(0, ComputationalTypeReference), List()),
                Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Return(8)
            ))
            javaLikeCode.shouldEqual(Array(
                "0: r_0 = this",
                "1: op_0 = new Object",
                "2: ;",
                "3: op_0/*(non-virtual) java.lang.Object*/.<init>()",
                "4: r_1 = op_0",
                "5: return"
            ).mkString("\n"))
        }

        it("should correctly reflect monitorenter and -exit") {
            val statements = TACNaive(method = MonitorEnterAndExitMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                Nop(1),
                Assignment(2, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                MonitorEnter(3, SimpleVar(0, ComputationalTypeReference)),
                Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-1, ComputationalTypeReference)),
                VirtualMethodCall(5, ObjectType("tactest/StackManipulationAndSynchronization"), false, "pop", MethodDescriptor("()V"), SimpleVar(0, ComputationalTypeReference), List()),
                Assignment(8, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                MonitorExit(9, SimpleVar(0, ComputationalTypeReference)), Goto(10, 13),
                Assignment(13, SimpleVar(1, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                MonitorExit(14, SimpleVar(1, ComputationalTypeReference)),
                Throw(15, SimpleVar(0, ComputationalTypeReference)),
                Return(16)
            ))
            javaLikeCode.shouldEqual(Array(
                "0: r_0 = this",
                "1: op_0 = r_0",
                "2: ;",
                "3: r_1 = op_0",
                "4: monitorenter op_0",
                "5: op_0 = r_0",
                "6: op_0/*tactest.StackManipulationAndSynchronization*/.pop()",
                "7: op_0 = r_1",
                "8: monitorexit op_0",
                "9: goto 13",
                "10: op_1 = r_1",
                "11: monitorexit op_1",
                "12: throw op_0",
                "13: return"
            ).mkString("\n"))
        }

        it("should correctly reflect invokestatic") {
            val statements = TACNaive(method = InvokeStaticMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

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
                        ObjectType("tactest/StackManipulationAndSynchronization"), false,
                        "staticMethod",
                        MethodDescriptor(IndexedSeq[FieldType](IntegerType, IntegerType), IntegerType),
                        List(SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))
                    )
                ),
                Assignment(5, SimpleVar(-2, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Return(6)
            ))
            javaLikeCode.shouldEqual(Array(
                "0: r_0 = this",
                "1: op_0 = 1",
                "2: op_1 = 2",
                "3: op_0 = tactest.StackManipulationAndSynchronization.staticMethod(op_0, op_1)",
                "4: r_1 = op_0",
                "5: return"
            ).mkString("\n"))
        }

        it("should correctly reflect invokeinterface") {
            val statements = TACNaive(method = InvokeInterfaceMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), New(0, ObjectType("java/util/ArrayList"))),
                Nop(3),
                NonVirtualMethodCall(4, ObjectType("java/util/ArrayList"), false, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(0, ComputationalTypeReference), List()),
                Assignment(7, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Assignment(8, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(9, SimpleVar(1, ComputationalTypeReference), New(9, ObjectType.Object)),
                Nop(12),
                NonVirtualMethodCall(13, ObjectType.Object, false, "<init>", MethodDescriptor(IndexedSeq[FieldType](), VoidType), SimpleVar(1, ComputationalTypeReference), List()),
                Assignment(
                    16,
                    SimpleVar(0, ComputationalTypeInt),
                    VirtualFunctionCall(
                        16,
                        ObjectType("java/util/List"),
                        true,
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
                "0: r_0 = this",
                "1: op_0 = new ArrayList",
                "2: ;",
                "3: op_0/*(non-virtual) java.util.ArrayList*/.<init>()",
                "4: r_1 = op_0",
                "5: op_0 = r_1",
                "6: op_1 = new Object",
                "7: ;",
                "8: op_1/*(non-virtual) java.lang.Object*/.<init>()",
                "9: op_0 = op_0/*java.util.List*/.add(op_1)",
                "10: ;",
                "11: return"
            ).mkString("\n"))
        }
    }
}
