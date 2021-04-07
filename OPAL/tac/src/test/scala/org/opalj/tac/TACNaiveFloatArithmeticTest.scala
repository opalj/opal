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
class TACNaiveFloatArithmeticTest extends TACNaiveTest {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    import BinaryArithmeticOperators._
    import RelationalOperators._
    import UnaryArithmeticOperators._

    val FloatAddMethod = ArithmeticExpressionsClassFile.findMethod("floatAdd").head
    val FloatDivMethod = ArithmeticExpressionsClassFile.findMethod("floatDiv").head
    val FloatNegMethod = ArithmeticExpressionsClassFile.findMethod("floatNeg").head
    val FloatMulMethod = ArithmeticExpressionsClassFile.findMethod("floatMul").head
    val FloatRemMethod = ArithmeticExpressionsClassFile.findMethod("floatRem").head
    val FloatSubMethod = ArithmeticExpressionsClassFile.findMethod("floatSub").head
    val FloatCmpMethod = ArithmeticExpressionsClassFile.findMethod("floatCmp").head

    describe("the naive TAC of float operations") {

        def binaryJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: r_2 = p_2",
            "3: op_0 = r_1",
            "4: op_1 = r_2",
            strg,
            "6: return op_0"
        ).mkString("\n")

        def binaryAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
            Assignment(-1, SimpleVar(-3, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_2")),
            Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
            Assignment(1, SimpleVar(1, ComputationalTypeFloat), SimpleVar(-3, ComputationalTypeFloat)),
            stmt,
            ReturnValue(3, SimpleVar(0, ComputationalTypeFloat))
        )

        it("should correctly reflect addition") {
            val statements = TACNaive(method = FloatAddMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                    BinaryExpr(2, ComputationalTypeFloat, Add, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1"))
        }

        it("should correctly reflect division") {
            val statements = TACNaive(method = FloatDivMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                    BinaryExpr(2, ComputationalTypeFloat, Divide, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1"))
        }

        it("should correctly reflect negation") {
            val statements = TACNaive(method = FloatNegMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                Assignment(1, SimpleVar(0, ComputationalTypeFloat),
                    PrefixExpr(1, ComputationalTypeFloat, Negate, SimpleVar(0, ComputationalTypeFloat))),
                ReturnValue(2, SimpleVar(0, ComputationalTypeFloat))
            ))
            javaLikeCode.shouldEqual(
                Array(
                    "0: r_0 = this",
                    "1: r_1 = p_1",
                    "2: op_0 = r_1",
                    "3: op_0 = - op_0",
                    "4: return op_0"
                ).mkString("\n")
            )
        }

        it("should correctly reflect multiplication") {
            val statements = TACNaive(method = FloatMulMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                    BinaryExpr(2, ComputationalTypeFloat, Multiply, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1"))
        }

        it("should correctly reflect modulo") {
            val statements = TACNaive(method = FloatRemMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                    BinaryExpr(2, ComputationalTypeFloat, Modulo, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1"))
        }

        it("should correctly reflect subtraction") {
            val statements = TACNaive(method = FloatSubMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                    BinaryExpr(2, ComputationalTypeFloat, Subtract, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1"))
        }

        it("should correctly reflect comparison") {
            val statements = TACNaive(method = FloatCmpMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                Assignment(1, SimpleVar(1, ComputationalTypeFloat), SimpleVar(-3, ComputationalTypeFloat)),
                Assignment(2, SimpleVar(0, ComputationalTypeInt), Compare(2, SimpleVar(0, ComputationalTypeFloat), CMPG, SimpleVar(1, ComputationalTypeFloat))),
                If(3, SimpleVar(0, ComputationalTypeInt), GE, IntConst(-3, 0), 9),
                Assignment(6, SimpleVar(0, ComputationalTypeInt), IntConst(6, 1)),
                ReturnValue(7, SimpleVar(0, ComputationalTypeInt)),
                Assignment(8, SimpleVar(0, ComputationalTypeInt), IntConst(8, 0)),
                ReturnValue(9, SimpleVar(0, ComputationalTypeInt))
            ))
            javaLikeCode.shouldEqual(Array(
                "0: r_0 = this",
                "1: r_1 = p_1",
                "2: r_2 = p_2",
                "3: op_0 = r_1",
                "4: op_1 = r_2",
                "5: op_0 = op_0 cmpg op_1",
                "6: if(op_0 >= 0) goto 9",
                "7: op_0 = 1",
                "8: return op_0",
                "9: op_0 = 0",
                "10: return op_0"
            ).mkString("\n"))
        }
    }
}
