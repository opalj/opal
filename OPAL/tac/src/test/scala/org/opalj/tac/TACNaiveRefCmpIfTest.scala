/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.br.TestSupport.biProject

/**
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveRefCmpIfTest extends TACNaiveTest {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val IfACMPEQMethod = ControlSequencesClassFile.findMethod("ifacmpeq").head
    val IfACMPNEMethod = ControlSequencesClassFile.findMethod("ifacmpne").head
    val IfNonNullMethod = ControlSequencesClassFile.findMethod("ifnonnull").head
    val IfNullMethod = ControlSequencesClassFile.findMethod("ifnull").head

    describe("the naive TAC of reference comparison if instructions") {

        def binaryResultAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
            Assignment(-1, SimpleVar(-3, ComputationalTypeReference), Param(ComputationalTypeReference, "p_2")),
            Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            Assignment(1, SimpleVar(1, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
            stmt,
            Assignment(5, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            ReturnValue(6, SimpleVar(0, ComputationalTypeReference)),
            Assignment(7, SimpleVar(0, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
            ReturnValue(8, SimpleVar(0, ComputationalTypeReference))
        )

        def unaryResultAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
            Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            stmt,
            Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            ReturnValue(5, SimpleVar(0, ComputationalTypeReference)),
            Assignment(6, SimpleVar(0, ComputationalTypeReference), NullExpr(6)),
            ReturnValue(7, SimpleVar(0, ComputationalTypeReference))
        )

        def binaryJLC(strg: String) = Array(
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

        def unaryJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            strg,
            "4: op_0 = r_1",
            "5: return op_0",
            "6: op_0 = null",
            "7: return op_0"
        ).mkString("\n")

        it("should correctly reflect the equals case") {
            val statements = TACNaive(method = IfACMPEQMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryResultAST(
                If(2, SimpleVar(0, ComputationalTypeReference), EQ, SimpleVar(1, ComputationalTypeReference), 8)
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 == op_1) goto 8"))
        }

        it("should correctly reflect the not-equals case") {
            val statements = TACNaive(method = IfACMPNEMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryResultAST(
                If(2, SimpleVar(0, ComputationalTypeReference), NE, SimpleVar(1, ComputationalTypeReference), 8)
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 != op_1) goto 8"))
        }

        it("should correctly reflect the non-null case") {
            val statements = TACNaive(method = IfNonNullMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(unaryResultAST(
                If(1, SimpleVar(0, ComputationalTypeReference), NE, NullExpr(-1), 6)
            ))
            javaLikeCode.shouldEqual(unaryJLC("3: if(op_0 != null) goto 6"))
        }

        it("should correctly reflect the is-null case") {
            val statements = TACNaive(method = IfNullMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(unaryResultAST(
                If(1, SimpleVar(0, ComputationalTypeReference), EQ, NullExpr(-1), 6)
            ))
            javaLikeCode.shouldEqual(unaryJLC("3: if(op_0 == null) goto 6"))
        }

    }
}
