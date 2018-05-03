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
class TACNaiveConstantsTest extends TACNaiveTest {

    val ConstantsType = ObjectType("tactest/Constants")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ConstantsClassFile = project.classFile(ConstantsType).get

    val IntConstsMethod = ConstantsClassFile.findMethod("intConsts").head
    val LongConstsMethod = ConstantsClassFile.findMethod("longConsts").head
    val FloatConstsMethod = ConstantsClassFile.findMethod("floatConsts").head
    val DoubleConstsMethod = ConstantsClassFile.findMethod("doubleConsts").head
    val NullRefConstMethod = ConstantsClassFile.findMethod("nullReferenceConst").head
    val LoadConstsInstrMethod = ConstantsClassFile.findMethod("loadConstants").head

    describe("the naive TAC of instructions loading constants") {

        it("should correctly reflect the integer constants") {
            val statements = TACNaive(method = IntConstsMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 0)),
                Assignment(1, SimpleVar(-2, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(2, SimpleVar(0, ComputationalTypeInt), IntConst(2, 1)),
                Assignment(3, SimpleVar(-3, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(4, SimpleVar(0, ComputationalTypeInt), IntConst(4, 2)),
                Assignment(5, SimpleVar(-4, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(6, SimpleVar(0, ComputationalTypeInt), IntConst(6, 3)),
                Assignment(7, SimpleVar(-5, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(9, SimpleVar(0, ComputationalTypeInt), IntConst(9, 4)),
                Assignment(10, SimpleVar(-6, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(12, SimpleVar(0, ComputationalTypeInt), IntConst(12, 5)),
                Assignment(13, SimpleVar(-7, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(15, SimpleVar(0, ComputationalTypeInt), IntConst(15, -1)),
                Assignment(16, SimpleVar(-8, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Return(18)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = 0",
                    "2: r_1 = op_0",
                    "3: op_0 = 1",
                    "4: r_2 = op_0",
                    "5: op_0 = 2",
                    "6: r_3 = op_0",
                    "7: op_0 = 3",
                    "8: r_4 = op_0",
                    "9: op_0 = 4",
                    "10: r_5 = op_0",
                    "11: op_0 = 5",
                    "12: r_6 = op_0",
                    "13: op_0 = -1",
                    "14: r_7 = op_0",
                    "15: return"
                ).mkString("\n")
            )
        }

        it("should correctly reflect the long constants") {
            val statements = TACNaive(method = LongConstsMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeLong), LongConst(0, 0L)),
                Assignment(1, SimpleVar(-2, ComputationalTypeLong), SimpleVar(0, ComputationalTypeLong)),
                Assignment(2, SimpleVar(0, ComputationalTypeLong), LongConst(2, 1L)),
                Assignment(3, SimpleVar(-4, ComputationalTypeLong), SimpleVar(0, ComputationalTypeLong)),
                Return(4)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = 0l",
                    "2: r_1 = op_0",
                    "3: op_0 = 1l",
                    "4: r_3 = op_0",
                    "5: return"
                ).mkString("\n")
            )
        }

        it("should correctly reflect the float constants") {
            val statements = TACNaive(method = FloatConstsMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), FloatConst(0, 0.0f)),
                Assignment(1, SimpleVar(-2, ComputationalTypeFloat), SimpleVar(0, ComputationalTypeFloat)),
                Assignment(2, SimpleVar(0, ComputationalTypeFloat), FloatConst(2, 1.0f)),
                Assignment(3, SimpleVar(-3, ComputationalTypeFloat), SimpleVar(0, ComputationalTypeFloat)),
                Assignment(4, SimpleVar(0, ComputationalTypeFloat), FloatConst(4, 2.0f)),
                Assignment(5, SimpleVar(-4, ComputationalTypeFloat), SimpleVar(0, ComputationalTypeFloat)),
                Return(6)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = 0.0f",
                    "2: r_1 = op_0",
                    "3: op_0 = 1.0f",
                    "4: r_2 = op_0",
                    "5: op_0 = 2.0f",
                    "6: r_3 = op_0",
                    "7: return"
                ).mkString("\n")
            )
        }

        it("should correctly reflect the double constants") {
            val statements = TACNaive(method = DoubleConstsMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeDouble), DoubleConst(0, 0.0d)),
                Assignment(1, SimpleVar(-2, ComputationalTypeDouble), SimpleVar(0, ComputationalTypeDouble)),
                Assignment(2, SimpleVar(0, ComputationalTypeDouble), DoubleConst(2, 1.0d)),
                Assignment(3, SimpleVar(-4, ComputationalTypeDouble), SimpleVar(0, ComputationalTypeDouble)),
                Return(4)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = 0.0d",
                    "2: r_1 = op_0",
                    "3: op_0 = 1.0d",
                    "4: r_3 = op_0",
                    "5: return"
                ).mkString("\n")
            )
        }

        it("should correctly reflect the null reference constants") {
            val statements = TACNaive(method = NullRefConstMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), NullExpr(0)),
                Assignment(1, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Return(2)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = null",
                    "2: r_1 = op_0",
                    "3: return"
                ).mkString("\n")
            )
        }

        it("should correctly reflect the other constant loading instructions") {
            val statements = TACNaive(method = LoadConstsInstrMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 10)),
                Assignment(2, SimpleVar(-2, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Assignment(3, SimpleVar(0, ComputationalTypeFloat), FloatConst(3, 10.0f)),
                Assignment(5, SimpleVar(-3, ComputationalTypeFloat), SimpleVar(0, ComputationalTypeFloat)),
                Assignment(6, SimpleVar(0, ComputationalTypeLong), LongConst(6, 10L)),
                Assignment(9, SimpleVar(-4, ComputationalTypeLong), SimpleVar(0, ComputationalTypeLong)),
                Assignment(10, SimpleVar(0, ComputationalTypeDouble), DoubleConst(10, 10.0d)),
                Assignment(13, SimpleVar(-6, ComputationalTypeDouble), SimpleVar(0, ComputationalTypeDouble)),
                Return(15)
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: op_0 = 10",
                    "2: r_1 = op_0",
                    "3: op_0 = 10.0f",
                    "4: r_2 = op_0",
                    "5: op_0 = 10l",
                    "6: r_3 = op_0",
                    "7: op_0 = 10.0d",
                    "8: r_5 = op_0",
                    "9: return"
                ).mkString("\n")
            )
        }
    }
}
