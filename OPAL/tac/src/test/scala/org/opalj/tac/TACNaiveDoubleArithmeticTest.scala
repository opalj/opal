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
class TACNaiveDoubleArithmeticTest extends TACNaiveTest {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    import BinaryArithmeticOperators._
    import UnaryArithmeticOperators._

    val DoubleAddMethod = ArithmeticExpressionsClassFile.findMethod("doubleAdd").head
    val DoubleDivMethod = ArithmeticExpressionsClassFile.findMethod("doubleDiv").head
    val DoubleNegMethod = ArithmeticExpressionsClassFile.findMethod("doubleNeg").head
    val DoubleMulMethod = ArithmeticExpressionsClassFile.findMethod("doubleMul").head
    val DoubleRemMethod = ArithmeticExpressionsClassFile.findMethod("doubleRem").head
    val DoubleSubMethod = ArithmeticExpressionsClassFile.findMethod("doubleSub").head
    //            val DoubleCmpMethod = ArithmeticExpressionsClassFile.findMethod("doubleCmp").get

    describe("the naive TAC of double operations") {

        def binaryJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: r_3 = p_2",
            "3: op_0 = r_1",
            "4: op_2 = r_3",
            strg,
            "6: return op_0"
        ).mkString("\n")

        def binaryAST(stmt: Stmt[IdBasedVar]) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
            Assignment(-1, SimpleVar(-4, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_2")),
            Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
            Assignment(1, SimpleVar(2, ComputationalTypeDouble), SimpleVar(-4, ComputationalTypeDouble)),
            stmt,
            ReturnValue(3, SimpleVar(0, ComputationalTypeDouble))
        )

        it("should correctly reflect addition") {
            val statements = TACNaive(method = DoubleAddMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                    BinaryExpr(2, ComputationalTypeDouble, Add, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_2"))
        }

        it("should correctly reflect division") {
            val statements = TACNaive(method = DoubleDivMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                    BinaryExpr(2, ComputationalTypeDouble, Divide, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_2"))
        }

        it("should correctly reflect negation") {
            val statements = TACNaive(method = DoubleNegMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                Assignment(1, SimpleVar(0, ComputationalTypeDouble),
                    PrefixExpr(1, ComputationalTypeDouble, Negate, SimpleVar(0, ComputationalTypeDouble))),
                ReturnValue(2, SimpleVar(0, ComputationalTypeDouble))
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
            val statements = TACNaive(method = DoubleMulMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                    BinaryExpr(2, ComputationalTypeDouble, Multiply, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_2"))
        }

        it("should correctly reflect modulo") {
            val statements = TACNaive(method = DoubleRemMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                    BinaryExpr(2, ComputationalTypeDouble, Modulo, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_2"))
        }

        it("should correctly reflect subtraction") {
            val statements = TACNaive(method = DoubleSubMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(binaryAST(
                Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                    BinaryExpr(2, ComputationalTypeDouble, Subtract, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))
            ))
            javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_2"))
        }
    }
}
