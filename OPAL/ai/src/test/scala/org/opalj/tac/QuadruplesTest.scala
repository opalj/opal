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
//import org.opalj.ai.domain.l1.DefaultDomain

/**
 * Tests the conversion of parsed methods to a quadruple representation
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class QuadruplesTest extends FunSpec with Matchers {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")
    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get
    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    val IntegerTestMethod = ArithmeticExpressionsClassFile.findMethod("integerTest").get
    val DoubleTestMethod = ArithmeticExpressionsClassFile.findMethod("doubleTest").get
    val IfTestMethod = ControlSequencesClassFile.findMethod("ifTest").get

    describe("The quadruples representation") {

        describe("of integer operations") {

            import BinaryArithmeticOperators._
            import UnaryArithmeticOperators._

            val IntegerAddMethod = ArithmeticExpressionsClassFile.findMethod("integerAdd").get
            val IntegerAndMethod = ArithmeticExpressionsClassFile.findMethod("integerAnd").get
            val IntegerDivMethod = ArithmeticExpressionsClassFile.findMethod("integerDiv").get
            val IntegerIncMethod = ArithmeticExpressionsClassFile.findMethod("integerInc").get
            val IntegerNegMethod = ArithmeticExpressionsClassFile.findMethod("integerNeg").get
            val IntegerMulMethod = ArithmeticExpressionsClassFile.findMethod("integerMul").get
            val IntegerOrMethod = ArithmeticExpressionsClassFile.findMethod("integerOr").get
            val IntegerRemMethod = ArithmeticExpressionsClassFile.findMethod("integerRem").get
            val IntegerShRMethod = ArithmeticExpressionsClassFile.findMethod("integerShR").get
            val IntegerShLMethod = ArithmeticExpressionsClassFile.findMethod("integerShL").get
            val IntegerSubMethod = ArithmeticExpressionsClassFile.findMethod("integerSub").get
            val IntegerAShMethod = ArithmeticExpressionsClassFile.findMethod("integerASh").get
            val IntegerXOrMethod = ArithmeticExpressionsClassFile.findMethod("integerXOr").get

            def binarySetupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: r_2 = p_2; \n"+
                    "    3: op_0 = r_1; \n"+
                    "    4: op_1 = r_2; \n"
            }

            def unarySetupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: op_0 = r_1; \n"
            }

            def returnJLC = "    6: return op_0; \n"

            def unaryReturnJLC = "    4: return op_0; \n"

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeInt))
            )

            it("should correctly reflect addition (using no AI results)") {
                val statements = AsQuadruples(IntegerAddMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 + op_1; \n"+returnJLC)
            }

            it("should correctly reflect logical and (using no AI results)") {
                val statements = AsQuadruples(IntegerAndMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 & op_1; \n"+returnJLC)
            }

            it("should correctly reflect division (using no AI results)") {
                val statements = AsQuadruples(IntegerDivMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 / op_1; \n"+returnJLC)
            }

            it("should correctly reflect incrementation by a constant (using no AI results)") {
                val statements = AsQuadruples(IntegerIncMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(-2, ComputationalTypeInt),
                        BinaryExpr(1, ComputationalTypeInt, Add, SimpleVar(-2, ComputationalTypeInt), IntConst(1, 1))),
                    ReturnValue(4, SimpleVar(0, ComputationalTypeInt))))
                javaLikeCode.shouldEqual(unarySetupJLC+"    3: r_1 = r_1 + 1; \n"+unaryReturnJLC)
            }

            it("should correctly reflect negation (using no AI results)") {
                val statements = AsQuadruples(IntegerNegMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(0, ComputationalTypeInt),
                        PrefixExpr(1, ComputationalTypeInt, Negate, SimpleVar(0, ComputationalTypeInt))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeInt))))
                javaLikeCode.shouldEqual(unarySetupJLC+"    3: op_0 = - op_0; \n"+unaryReturnJLC)
            }

            it("should correctly reflect multiplication (using no AI results)") {
                val statements = AsQuadruples(IntegerMulMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 * op_1; \n"+returnJLC)
            }

            it("should correctly reflect logical or (using no AI results)") {
                val statements = AsQuadruples(IntegerOrMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 | op_1; \n"+returnJLC)
            }

            it("should correctly reflect modulo (using no AI results)") {
                val statements = AsQuadruples(IntegerRemMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 % op_1; \n"+returnJLC)
            }

            it("should correctly reflect shift right (using no AI results)") {
                val statements = AsQuadruples(IntegerShRMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 >> op_1; \n"+returnJLC)
            }

            it("should correctly reflect shift left (using no AI results)") {
                val statements = AsQuadruples(IntegerShLMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 << op_1; \n"+returnJLC)
            }

            it("should correctly reflect subtraction (using no AI results)") {
                val statements = AsQuadruples(IntegerSubMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 - op_1; \n"+returnJLC)
            }

            it("should correctly reflect arithmetic shift right (using no AI results)") {
                val statements = AsQuadruples(IntegerAShMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 >>> op_1; \n"+returnJLC)
            }

            it("should correctly reflect logical xor (using no AI results)") {
                val statements = AsQuadruples(IntegerXOrMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 ^ op_1; \n"+returnJLC)
            }

            //TESTOUTPUT INT
            //            it("should just print a method for testing purposes") {
            //                val statements = AsQuadruples(IfTestMethod, None)
            //                val javaLikeCode = ToJavaLike(statements)
            //
            //                assert(statements.nonEmpty)
            //                assert(javaLikeCode.length() > 0)
            //
            //                println(IfTestMethod.body.get.instructions.mkString("\n"))
            //                println(statements.mkString("\n"))
            //                println(javaLikeCode)
            //            }
            //TESTOUTPUT INT
        }

        describe("of double operations") {
            import BinaryArithmeticOperators._
            import UnaryArithmeticOperators._

            val DoubleAddMethod = ArithmeticExpressionsClassFile.findMethod("doubleAdd").get
            val DoubleDivMethod = ArithmeticExpressionsClassFile.findMethod("doubleDiv").get
            val DoubleNegMethod = ArithmeticExpressionsClassFile.findMethod("doubleNeg").get
            val DoubleMulMethod = ArithmeticExpressionsClassFile.findMethod("doubleMul").get
            val DoubleRemMethod = ArithmeticExpressionsClassFile.findMethod("doubleRem").get
            val DoubleSubMethod = ArithmeticExpressionsClassFile.findMethod("doubleSub").get
            //            val DoubleCmpMethod = ArithmeticExpressionsClassFile.findMethod("doubleCmp").get

            def binarySetupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: r_3 = p_2; \n"+
                    "    3: op_0 = r_1; \n"+
                    "    4: op_2 = r_3; \n"
            }

            def returnJLC = "    6: return op_0; \n"

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                Assignment(-1, SimpleVar(-4, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                Assignment(1, SimpleVar(2, ComputationalTypeDouble), SimpleVar(-4, ComputationalTypeDouble)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeDouble))
            )

            it("should correctly reflect addition (using no AI results)") {
                val statements = AsQuadruples(DoubleAddMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Add, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 + op_2; \n"+returnJLC)
            }

            it("should correctly reflect division (using no AI results)") {
                val statements = AsQuadruples(DoubleDivMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Divide, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 / op_2; \n"+returnJLC)
            }

            it("should correctly reflect negation (using no AI results)") {
                val statements = AsQuadruples(DoubleNegMethod, None)
                val javaLikeCode = ToJavaLike(statements)
                //              TODO
                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                    Assignment(1, SimpleVar(0, ComputationalTypeDouble),
                        PrefixExpr(1, ComputationalTypeDouble, Negate, SimpleVar(0, ComputationalTypeDouble))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeDouble))))
                javaLikeCode.shouldEqual(
                    "    0: r_0 = this; \n"+
                        "    1: r_1 = p_1; \n"+
                        "    2: op_0 = r_1; \n"+
                        "    3: op_0 = - op_0; \n"+
                        "    4: return op_0; \n")
            }

            it("should correctly reflect multiplication (using no AI results)") {
                val statements = AsQuadruples(DoubleMulMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Multiply, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 * op_2; \n"+returnJLC)
            }

            it("should correctly reflect modulo (using no AI results)") {
                val statements = AsQuadruples(DoubleRemMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Modulo, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 % op_2; \n"+returnJLC)
            }

            it("should correctly reflect subtraction (using no AI results)") {
                val statements = AsQuadruples(DoubleSubMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Subtract, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 - op_2; \n"+returnJLC)
            }

            //            it("should correctly reflect comparison (using no AI results)") {
            //                val statements = AsQuadruples(DoubleCmpMethod, None)
            //                val javaLikeCode = ToJavaLike(statements)
            //                TODO
            //                assert(statements.nonEmpty)
            //                assert(javaLikeCode.length() > 0)
            //                statements.shouldEqual()
            //                javaLikeCode.shouldEqual()
            //            }

            //TESTOUTPUT DOUBLE
            //            it("should just print a method for testing purposes") {
            //                println(DoubleTestMethod.body.get.instructions.mkString("\n"))
            //
            //                val statements = AsQuadruples(DoubleTestMethod, None)
            //                assert(statements.nonEmpty)
            //                println(statements.mkString("\n"))
            //
            //                val javaLikeCode = ToJavaLike(statements)
            //                assert(javaLikeCode.length() > 0)
            //                println(javaLikeCode)
            //            }
            //TESTOUTPUT DOUBLE
        }

        describe("of float operations") {
            import BinaryArithmeticOperators._
            import UnaryArithmeticOperators._

            val FloatAddMethod = ArithmeticExpressionsClassFile.findMethod("floatAdd").get
            val FloatDivMethod = ArithmeticExpressionsClassFile.findMethod("floatDiv").get
            val FloatNegMethod = ArithmeticExpressionsClassFile.findMethod("floatNeg").get
            val FloatMulMethod = ArithmeticExpressionsClassFile.findMethod("floatMul").get
            val FloatRemMethod = ArithmeticExpressionsClassFile.findMethod("floatRem").get
            val FloatSubMethod = ArithmeticExpressionsClassFile.findMethod("floatSub").get
            //            val FloatCmpMethod = ArithmeticExpressionsClassFile.findMethod("floatCmp").get

            def binarySetupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: r_2 = p_2; \n"+
                    "    3: op_0 = r_1; \n"+
                    "    4: op_1 = r_2; \n"
            }

            def returnJLC = "    6: return op_0; \n"

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                Assignment(1, SimpleVar(1, ComputationalTypeFloat), SimpleVar(-3, ComputationalTypeFloat)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeFloat))
            )

            it("should correctly reflect addition (using no AI results)") {
                val statements = AsQuadruples(FloatAddMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Add, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 + op_1; \n"+returnJLC)
            }

            it("should correctly reflect division (using no AI results)") {
                val statements = AsQuadruples(FloatDivMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Divide, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 / op_1; \n"+returnJLC)
            }

            it("should correctly reflect negation (using no AI results)") {
                val statements = AsQuadruples(FloatNegMethod, None)
                val javaLikeCode = ToJavaLike(statements)
                //              TODO
                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                    Assignment(1, SimpleVar(0, ComputationalTypeFloat),
                        PrefixExpr(1, ComputationalTypeFloat, Negate, SimpleVar(0, ComputationalTypeFloat))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeFloat))))
                javaLikeCode.shouldEqual(
                    "    0: r_0 = this; \n"+
                        "    1: r_1 = p_1; \n"+
                        "    2: op_0 = r_1; \n"+
                        "    3: op_0 = - op_0; \n"+
                        "    4: return op_0; \n")
            }

            it("should correctly reflect multiplication (using no AI results)") {
                val statements = AsQuadruples(FloatMulMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Multiply, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 * op_1; \n"+returnJLC)
            }

            it("should correctly reflect modulo (using no AI results)") {
                val statements = AsQuadruples(FloatRemMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Modulo, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 % op_1; \n"+returnJLC)
            }

            it("should correctly reflect subtraction (using no AI results)") {
                val statements = AsQuadruples(FloatSubMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Subtract, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 - op_1; \n"+returnJLC)
            }

            //            it("should correctly reflect comparison (using no AI results)") {
            //                val statements = AsQuadruples(FloatCmpMethod, None)
            //                val javaLikeCode = ToJavaLike(statements)
            //                TODO
            //                assert(statements.nonEmpty)
            //                assert(javaLikeCode.length() > 0)
            //                statements.shouldEqual(binaryAST(
            //                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
            //                        BinaryExpr(2, ComputationalTypeLong, Add, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
            //                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 + op_2; \n"+returnJLC)
            //            }
        }

        describe("of long operations") {

            import BinaryArithmeticOperators._
            import UnaryArithmeticOperators._

            val LongAddMethod = ArithmeticExpressionsClassFile.findMethod("longAdd").get
            val LongAndMethod = ArithmeticExpressionsClassFile.findMethod("longAnd").get
            val LongDivMethod = ArithmeticExpressionsClassFile.findMethod("longDiv").get
            val LongNegMethod = ArithmeticExpressionsClassFile.findMethod("longNeg").get
            val LongMulMethod = ArithmeticExpressionsClassFile.findMethod("longMul").get
            val LongOrMethod = ArithmeticExpressionsClassFile.findMethod("longOr").get
            val LongRemMethod = ArithmeticExpressionsClassFile.findMethod("longRem").get
            val LongShRMethod = ArithmeticExpressionsClassFile.findMethod("longShR").get
            val LongShLMethod = ArithmeticExpressionsClassFile.findMethod("longShL").get
            val LongSubMethod = ArithmeticExpressionsClassFile.findMethod("longSub").get
            val LongAShMethod = ArithmeticExpressionsClassFile.findMethod("longASh").get
            val LongXOrMethod = ArithmeticExpressionsClassFile.findMethod("longXOr").get

            def binarySetupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: r_3 = p_2; \n"+
                    "    3: op_0 = r_1; \n"+
                    "    4: op_2 = r_3; \n"
            }

            def returnJLC = "    6: return op_0; \n"

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeLong), Param(ComputationalTypeLong, "p_1")),
                Assignment(-1, SimpleVar(-4, ComputationalTypeLong), Param(ComputationalTypeLong, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeLong), SimpleVar(-2, ComputationalTypeLong)),
                Assignment(1, SimpleVar(2, ComputationalTypeLong), SimpleVar(-4, ComputationalTypeLong)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeLong))
            )

            def binaryShiftAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeLong), Param(ComputationalTypeLong, "p_1")),
                Assignment(-1, SimpleVar(-4, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeLong), SimpleVar(-2, ComputationalTypeLong)),
                Assignment(1, SimpleVar(2, ComputationalTypeInt), SimpleVar(-4, ComputationalTypeInt)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeLong))
            )

            it("should correctly reflect addition (using no AI results)") {
                val statements = AsQuadruples(LongAddMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Add, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 + op_2; \n"+returnJLC)
            }

            it("should correctly reflect logical and (using no AI results)") {
                val statements = AsQuadruples(LongAndMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, And, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 & op_2; \n"+returnJLC)
            }

            it("should correctly reflect division (using no AI results)") {
                val statements = AsQuadruples(LongDivMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Divide, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 / op_2; \n"+returnJLC)
            }

            it("should correctly reflect negation (using no AI results)") {
                val statements = AsQuadruples(LongNegMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeLong), Param(ComputationalTypeLong, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeLong), SimpleVar(-2, ComputationalTypeLong)),
                    Assignment(1, SimpleVar(0, ComputationalTypeLong),
                        PrefixExpr(1, ComputationalTypeLong, Negate, SimpleVar(0, ComputationalTypeLong))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeLong))))
                javaLikeCode.shouldEqual(
                    "    0: r_0 = this; \n"+
                        "    1: r_1 = p_1; \n"+
                        "    2: op_0 = r_1; \n"+
                        "    3: op_0 = - op_0; \n"+
                        "    4: return op_0; \n")
            }

            it("should correctly reflect multiplication (using no AI results)") {
                val statements = AsQuadruples(LongMulMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Multiply, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 * op_2; \n"+returnJLC)
            }

            it("should correctly reflect logical or (using no AI results)") {
                val statements = AsQuadruples(LongOrMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Or, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 | op_2; \n"+returnJLC)
            }

            it("should correctly reflect modulo (using no AI results)") {
                val statements = AsQuadruples(LongRemMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Modulo, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 % op_2; \n"+returnJLC)
            }

            it("should correctly reflect shift right (using no AI results)") {
                val statements = AsQuadruples(LongShRMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryShiftAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, ShiftRight, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 >> op_2; \n"+returnJLC)
            }

            it("should correctly reflect shift left (using no AI results)") {
                val statements = AsQuadruples(LongShLMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryShiftAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, ShiftLeft, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 << op_2; \n"+returnJLC)
            }

            it("should correctly reflect subtraction (using no AI results)") {
                val statements = AsQuadruples(LongSubMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, Subtract, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 - op_2; \n"+returnJLC)
            }

            it("should correctly reflect arithmetic shift right (using no AI results)") {
                val statements = AsQuadruples(LongAShMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryShiftAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, UnsignedShiftRight, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 >>> op_2; \n"+returnJLC)
            }

            it("should correctly reflect logical xor (using no AI results)") {
                val statements = AsQuadruples(LongXOrMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeLong),
                        BinaryExpr(2, ComputationalTypeLong, XOr, SimpleVar(0, ComputationalTypeLong), SimpleVar(2, ComputationalTypeLong)))))
                javaLikeCode.shouldEqual(binarySetupJLC+"    5: op_0 = op_0 ^ op_2; \n"+returnJLC)
            }
        }

        describe("of integer if instructions") {

            import RelationalOperators._

            val ICMPNEMethod = ControlSequencesClassFile.findMethod("icmpne").get
            val ICMPEQMethod = ControlSequencesClassFile.findMethod("icmpeq").get
            val ICMPGEMethod = ControlSequencesClassFile.findMethod("icmpge").get
            val ICMPLTMethod = ControlSequencesClassFile.findMethod("icmplt").get
            val ICMPLEMethod = ControlSequencesClassFile.findMethod("icmple").get
            val ICMPGTMethod = ControlSequencesClassFile.findMethod("icmpgt").get

            def setupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: r_2 = p_2; \n"+
                    "    3: op_0 = r_1; \n"+
                    "    4: op_1 = r_2; \n"
            }

            def returnJLC = {
                "    6: op_0 = r_1; \n"+
                    "    7: return op_0; \n"+
                    "    8: op_0 = r_2; \n"+
                    "    9: return op_0; \n"
            }

            def resultAST(stmt: Stmt): Array[Stmt] = Array(
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

            it("should correctly reflect the not-equals case (using no AI results)") {
                val statements = AsQuadruples(ICMPNEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), NE, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 != op_1) goto 8; \n"+returnJLC)
            }

            it("should correctly reflect the equals case (using no AI results)") {
                val statements = AsQuadruples(ICMPEQMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), EQ, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 == op_1) goto 8; \n"+returnJLC)
            }

            it("should correctly reflect the greater-equals case (using no AI results)") {
                val statements = AsQuadruples(ICMPGEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GE, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 >= op_1) goto 8; \n"+returnJLC)
            }

            it("should correctly reflect the less-then case (using no AI results)") {
                val statements = AsQuadruples(ICMPLTMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LT, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 < op_1) goto 8; \n"+returnJLC)
            }

            it("should correctly reflect the less-equals case (using no AI results)") {
                val statements = AsQuadruples(ICMPLEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LE, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 <= op_1) goto 8; \n"+returnJLC)
            }

            it("should correctly reflect the greater-then case (using no AI results)") {
                val statements = AsQuadruples(ICMPGTMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GT, SimpleVar(1, ComputationalTypeInt), 8)))
                javaLikeCode.shouldEqual(setupJLC+"    5: if(op_0 > op_1) goto 8; \n"+returnJLC)
            }
        }

        describe("of compare to zero if instructions") {

            import RelationalOperators._

            val IfNEMethod = ControlSequencesClassFile.findMethod("ifne").get
            val IfEQMethod = ControlSequencesClassFile.findMethod("ifeq").get
            val IfGEMethod = ControlSequencesClassFile.findMethod("ifge").get
            val IfLTMethod = ControlSequencesClassFile.findMethod("iflt").get
            val IfLEMethod = ControlSequencesClassFile.findMethod("ifle").get
            val IfGTMethod = ControlSequencesClassFile.findMethod("ifgt").get

            def setupJLC = {
                "    0: r_0 = this; \n"+
                    "    1: r_1 = p_1; \n"+
                    "    2: op_0 = r_1; \n"
            }

            def returnJLC = {
                "    4: op_0 = r_1; \n"+
                    "    5: return op_0; \n"+
                    "    6: op_0 = 0; \n"+
                    "    7: return op_0; \n"
            }

            def resultAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                stmt,
                Assignment(4, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                ReturnValue(5, SimpleVar(0, ComputationalTypeInt)),
                Assignment(6, SimpleVar(0, ComputationalTypeInt), IntConst(6, 0)),
                ReturnValue(7, SimpleVar(0, ComputationalTypeInt))
            )

            it("should correctly reflect the not-equals case (using no AI results)") {
                val statements = AsQuadruples(IfNEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), NE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 != 0) goto 6; \n"+returnJLC)
            }

            it("should correctly reflect the equals case (using no AI results)") {
                val statements = AsQuadruples(IfEQMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), EQ, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 == 0) goto 6; \n"+returnJLC)
            }

            it("should correctly reflect the greater-equals case (using no AI results)") {
                val statements = AsQuadruples(IfGEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 >= 0) goto 6; \n"+returnJLC)
            }

            it("should correctly reflect the less-then case (using no AI results)") {
                val statements = AsQuadruples(IfLTMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LT, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 < 0) goto 6; \n"+returnJLC)
            }

            it("should correctly reflect the less-equals case (using no AI results)") {
                val statements = AsQuadruples(IfLEMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 <= 0) goto 6; \n"+returnJLC)
            }

            it("should correctly reflect the greater-then case (using no AI results)") {
                val statements = AsQuadruples(IfGTMethod, None)
                val javaLikeCode = ToJavaLike(statements)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length() > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GT, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(setupJLC+"    3: if(op_0 > 0) goto 6; \n"+returnJLC)
            }
        }
    }
}
