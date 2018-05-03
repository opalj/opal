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
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveIntegerIfTest extends TACNaiveTest {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val ICMPNEMethod = ControlSequencesClassFile.findMethod("icmpne").head
    val ICMPEQMethod = ControlSequencesClassFile.findMethod("icmpeq").head
    val ICMPGEMethod = ControlSequencesClassFile.findMethod("icmpge").head
    val ICMPLTMethod = ControlSequencesClassFile.findMethod("icmplt").head
    val ICMPLEMethod = ControlSequencesClassFile.findMethod("icmple").head
    val ICMPGTMethod = ControlSequencesClassFile.findMethod("icmpgt").head

    describe("the naive TAC of integer if instructions") {

        def resultJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: r_2 = p_2",
            "3: op_0 = r_1",
            "4: op_1 = r_2",
            strg,
            "6: op_0 = r_1",
            "7: return op_0",
            "8: op_0 = r_2",
            "9: return op_0"
        ).mkString("\n")

        def resultAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
            Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
            Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
            Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
            stmt,
            Assignment(5, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
            ReturnValue(6, SimpleVar(0, ComputationalTypeInt)),
            Assignment(7, SimpleVar(0, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
            ReturnValue(8, SimpleVar(0, ComputationalTypeInt))
        )

        it("should correctly reflect the not-equals case") {
            val statements = TACNaive(method = ICMPNEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), NE, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 != op_1) goto 8"))
        }

        it("should correctly reflect the equals case") {
            val statements = TACNaive(method = ICMPEQMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), EQ, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 == op_1) goto 8"))
        }

        it("should correctly reflect the greater-equals case") {
            val statements = TACNaive(method = ICMPGEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), GE, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 >= op_1) goto 8"))
        }

        it("should correctly reflect the less-then case") {
            val statements = TACNaive(method = ICMPLTMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), LT, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 < op_1) goto 8"))
        }

        it("should correctly reflect the less-equals case") {
            val statements = TACNaive(method = ICMPLEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), LE, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 <= op_1) goto 8"))
        }

        it("should correctly reflect the greater-then case") {
            val statements = TACNaive(method = ICMPGTMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(2, SimpleVar(0, ComputationalTypeInt), GT, SimpleVar(1, ComputationalTypeInt), 8)
            ))
            javaLikeCode.shouldEqual(resultJLC("5: if(op_0 > op_1) goto 8"))
        }

    }
}
