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
class TACNaiveIntegerArithmeticTest extends TACNaiveTest {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    import BinaryArithmeticOperators._
    import UnaryArithmeticOperators._

    val IntegerAddMethod = ArithmeticExpressionsClassFile.findMethod("integerAdd").head
    val IntegerAndMethod = ArithmeticExpressionsClassFile.findMethod("integerAnd").head
    val IntegerDivMethod = ArithmeticExpressionsClassFile.findMethod("integerDiv").head
    val IntegerIncMethod = ArithmeticExpressionsClassFile.findMethod("integerInc").head
    val IntegerNegMethod = ArithmeticExpressionsClassFile.findMethod("integerNeg").head
    val IntegerMulMethod = ArithmeticExpressionsClassFile.findMethod("integerMul").head
    val IntegerOrMethod = ArithmeticExpressionsClassFile.findMethod("integerOr").head
    val IntegerRemMethod = ArithmeticExpressionsClassFile.findMethod("integerRem").head
    val IntegerShRMethod = ArithmeticExpressionsClassFile.findMethod("integerShR").head
    val IntegerShLMethod = ArithmeticExpressionsClassFile.findMethod("integerShL").head
    val IntegerSubMethod = ArithmeticExpressionsClassFile.findMethod("integerSub").head
    val IntegerAShMethod = ArithmeticExpressionsClassFile.findMethod("integerASh").head
    val IntegerXOrMethod = ArithmeticExpressionsClassFile.findMethod("integerXOr").head

    describe("the naive TAC of integer operations") {

        def binaryJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: r_2 = p_2",
            "3: op_0 = r_1",
            "4: op_1 = r_2",
            strg,
            "6: return op_0"
        ).mkString("\n")

        def unaryJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            strg,
            "4: return op_0"
        ).mkString("\n")

        def binaryAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
            Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
            Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
            Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
            stmt,
            ReturnValue(3, SimpleVar(0, ComputationalTypeInt))
        )

        it("should correctly reflect addition") {
            val statements = TACNaive(method = IntegerAddMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1"))
        }

        it("should correctly reflect logical and") {
            val statements = TACNaive(method = IntegerAndMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 & op_1"))
        }

        it("should correctly reflect division") {
            val statements = TACNaive(method = IntegerDivMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1"))
        }

        it("should correctly reflect incrementation by a constant") {
            val statements = TACNaive(method = IntegerIncMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(-2, ComputationalTypeInt),
                    BinaryExpr(1, ComputationalTypeInt, Add, SimpleVar(-2, ComputationalTypeInt), IntConst(1, 1))),
                ReturnValue(4, SimpleVar(0, ComputationalTypeInt))
            ))
            javaLikeCode.shouldEqual(unaryJLC("3: r_1 = r_1 + 1"))
        }

        it("should correctly reflect negation") {
            val statements = TACNaive(method = IntegerNegMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(0, ComputationalTypeInt),
                    PrefixExpr(1, ComputationalTypeInt, Negate, SimpleVar(0, ComputationalTypeInt))),
                ReturnValue(2, SimpleVar(0, ComputationalTypeInt))
            ))
            javaLikeCode.shouldEqual(unaryJLC("3: op_0 = - op_0"))
        }

        it("should correctly reflect multiplication") {
            val statements = TACNaive(method = IntegerMulMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1"))
        }

        it("should correctly reflect logical or") {
            val statements = TACNaive(method = IntegerOrMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 | op_1"))
        }

        it("should correctly reflect modulo") {
            val statements = TACNaive(method = IntegerRemMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1"))
        }

        it("should correctly reflect shift right") {
            val statements = TACNaive(method = IntegerShRMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >> op_1"))
        }

        it("should correctly reflect shift left") {
            val statements = TACNaive(method = IntegerShLMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 << op_1"))
        }

        it("should correctly reflect subtraction") {
            val statements = TACNaive(method = IntegerSubMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1"))
        }

        it("should correctly reflect arithmetic shift right") {
            val statements = TACNaive(method = IntegerAShMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >>> op_1"))
        }

        it("should correctly reflect logical xor") {
            val statements = TACNaive(method = IntegerXOrMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeInt),
                    BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 ^ op_1"))
        }

    }
}
