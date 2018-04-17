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
class TACNaiveSwitchTest extends TACNaiveTest {

    val SwitchStatementsType = ObjectType("tactest/SwitchStatements")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val SwitchStatementsClassFile = project.classFile(SwitchStatementsType).get

    val TableSwitchMethod = SwitchStatementsClassFile.findMethod("tableSwitch").head
    val LookupSwitchMethod = SwitchStatementsClassFile.findMethod("lookupSwitch").head

    describe("the naive TAC of switch instructions") {
        it("should correctly reflect tableswitch case") {
            val statements = TACNaive(method = TableSwitchMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, true)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)

            val expected = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Switch(1, 10, SimpleVar(0, ComputationalTypeInt), IndexedSeq((1, 4), (2, 6), (3, 8))),
                Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 1)),
                ReturnValue(29, SimpleVar(0, ComputationalTypeInt)),
                Assignment(30, SimpleVar(0, ComputationalTypeInt), IntConst(30, 2)),
                ReturnValue(31, SimpleVar(0, ComputationalTypeInt)),
                Assignment(32, SimpleVar(0, ComputationalTypeInt), IntConst(32, 3)),
                ReturnValue(33, SimpleVar(0, ComputationalTypeInt)),
                Assignment(34, SimpleVar(0, ComputationalTypeInt), IntConst(34, 0)),
                ReturnValue(35, SimpleVar(0, ComputationalTypeInt))
            )
            compareStatements(expected, statements)

            javaLikeCode.shouldEqual(Array(
                "0:/*pc=-1:*/ r_0 = this",
                "1:/*pc=-1:*/ r_1 = p_1",
                "2:/*pc=0:*/ op_0 = r_1",
                "3:/*pc=1:*/ switch(op_0){\n    1: goto 4;\n    2: goto 6;\n    3: goto 8;\n    default: goto 10\n}",
                "4:/*pc=28:*/ op_0 = 1",
                "5:/*pc=29:*/ return op_0",
                "6:/*pc=30:*/ op_0 = 2",
                "7:/*pc=31:*/ return op_0",
                "8:/*pc=32:*/ op_0 = 3",
                "9:/*pc=33:*/ return op_0",
                "10:/*pc=34:*/ op_0 = 0",
                "11:/*pc=35:*/ return op_0"
            ).mkString("\n"))
        }

        it("should correctly reflect lookupswitch case") {
            val statements = TACNaive(method = LookupSwitchMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, true)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Switch(1, 8, SimpleVar(0, ComputationalTypeInt), IndexedSeq((1, 4), (10, 6))),
                Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 10)),
                ReturnValue(30, SimpleVar(0, ComputationalTypeInt)),
                Assignment(31, SimpleVar(0, ComputationalTypeInt), IntConst(31, 200)),
                ReturnValue(34, SimpleVar(0, ComputationalTypeInt)),
                Assignment(35, SimpleVar(0, ComputationalTypeInt), IntConst(35, 0)),
                ReturnValue(36, SimpleVar(0, ComputationalTypeInt))
            ))
            javaLikeCode.shouldEqual(Array(
                "0:/*pc=-1:*/ r_0 = this",
                "1:/*pc=-1:*/ r_1 = p_1",
                "2:/*pc=0:*/ op_0 = r_1",
                "3:/*pc=1:*/ switch(op_0){\n    1: goto 4;\n    10: goto 6;\n    default: goto 8\n}",
                "4:/*pc=28:*/ op_0 = 10",
                "5:/*pc=30:*/ return op_0",
                "6:/*pc=31:*/ op_0 = 200",
                "7:/*pc=34:*/ return op_0",
                "8:/*pc=35:*/ op_0 = 0",
                "9:/*pc=36:*/ return op_0"
            ).mkString("\n"))
        }

    }
}
