/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.br.TestSupport.biProject

/**
 * @author Roberts Kolosovs
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveCmpToZeroIfTest extends TACNaiveTest {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val IfNEMethod = ControlSequencesClassFile.findMethod("ifne").head
    val IfEQMethod = ControlSequencesClassFile.findMethod("ifeq").head
    val IfGEMethod = ControlSequencesClassFile.findMethod("ifge").head
    val IfLTMethod = ControlSequencesClassFile.findMethod("iflt").head
    val IfLEMethod = ControlSequencesClassFile.findMethod("ifle").head
    val IfGTMethod = ControlSequencesClassFile.findMethod("ifgt").head

    describe("the naive TAC of compare to zero if instructions") {

        def resultJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            strg,
            "4: op_0 = r_1",
            "5: return op_0",
            "6: op_0 = 0",
            "7: return op_0"
        ).mkString("\n")

        def resultAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
            Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
            stmt,
            Assignment(4, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
            ReturnValue(5, SimpleVar(0, ComputationalTypeInt)),
            Assignment(6, SimpleVar(0, ComputationalTypeInt), IntConst(6, 0)),
            ReturnValue(7, SimpleVar(0, ComputationalTypeInt))
        )

        it("should correctly reflect the not-equals case") {
            val statements = TACNaive(method = IfNEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), NE, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 != 0) goto 6"))
        }

        it("should correctly reflect the equals case") {
            val statements = TACNaive(method = IfEQMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), EQ, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 == 0) goto 6"))
        }

        it("should correctly reflect the greater-equals case") {
            val statements = TACNaive(method = IfGEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), GE, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 >= 0) goto 6"))
        }

        it("should correctly reflect the less-then case") {
            val statements = TACNaive(method = IfLTMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), LT, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 < 0) goto 6"))
        }

        it("should correctly reflect the less-equals case") {
            val statements = TACNaive(method = IfLEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), LE, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 <= 0) goto 6"))
        }

        it("should correctly reflect the greater-then case") {
            val statements = TACNaive(method = IfGTMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(resultAST(
                If(1, SimpleVar(0, ComputationalTypeInt), GT, IntConst(-1, 0), 6)
            ))
            javaLikeCode.shouldEqual(resultJLC("3: if(op_0 > 0) goto 6"))
        }
    }
}
