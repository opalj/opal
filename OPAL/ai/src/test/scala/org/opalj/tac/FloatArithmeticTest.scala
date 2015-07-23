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
import org.opalj.ai.domain.l1.DefaultDomain

/**
 * Tests the conversion of parsed methods to a quadruple representation
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class FloatArithmeticTest extends FunSpec with Matchers {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    import BinaryArithmeticOperators._
    import UnaryArithmeticOperators._

    val FloatAddMethod = ArithmeticExpressionsClassFile.findMethod("floatAdd").get
    val FloatDivMethod = ArithmeticExpressionsClassFile.findMethod("floatDiv").get
    val FloatNegMethod = ArithmeticExpressionsClassFile.findMethod("floatNeg").get
    val FloatMulMethod = ArithmeticExpressionsClassFile.findMethod("floatMul").get
    val FloatRemMethod = ArithmeticExpressionsClassFile.findMethod("floatRem").get
    val FloatSubMethod = ArithmeticExpressionsClassFile.findMethod("floatSub").get
    //            val FloatCmpMethod = ArithmeticExpressionsClassFile.findMethod("floatCmp").get

    describe("The quadruples representation of float operations") {

        describe("using no AI results") {
            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: r_2 = p_2;",
                "3: op_0 = r_1;",
                "4: op_1 = r_2;",
                strg,
                "6: return op_0;"
            )

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                Assignment(1, SimpleVar(1, ComputationalTypeFloat), SimpleVar(-3, ComputationalTypeFloat)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeFloat))
            )

            it("should correctly reflect addition") {
                val statements = AsQuadruples(FloatAddMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Add, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect division") {
                val statements = AsQuadruples(FloatDivMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Divide, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect negation") {
                val statements = AsQuadruples(FloatNegMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                    Assignment(1, SimpleVar(0, ComputationalTypeFloat),
                        PrefixExpr(1, ComputationalTypeFloat, Negate, SimpleVar(0, ComputationalTypeFloat))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeFloat))))
                javaLikeCode.shouldEqual(
                    Array("0: r_0 = this;",
                        "1: r_1 = p_1;",
                        "2: op_0 = r_1;",
                        "3: op_0 = - op_0;",
                        "4: return op_0;"))
            }

            it("should correctly reflect multiplication") {
                val statements = AsQuadruples(FloatMulMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Multiply, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect modulo") {
                val statements = AsQuadruples(FloatRemMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Modulo, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect subtraction") {
                val statements = AsQuadruples(FloatSubMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Subtract, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
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

        describe("using AI results") {
            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: r_2 = p_2;",
                "3: op_0 = r_1;",
                "4: op_1 = r_2;",
                strg,
                "6: return op_0 /*AFloatValue*/;"
            )

            def binaryAST(stmt1: Stmt, stmt2: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                Assignment(1, SimpleVar(1, ComputationalTypeFloat), SimpleVar(-3, ComputationalTypeFloat)),
                stmt1,
                stmt2
            )

            it("should correctly reflect addition") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatAddMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatAddMethod, domain)
                val statements = AsQuadruples(FloatAddMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Add, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect division") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatDivMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatDivMethod, domain)
                val statements = AsQuadruples(FloatDivMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Divide, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect negation") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatNegMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatNegMethod, domain)
                val statements = AsQuadruples(FloatNegMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeFloat), Param(ComputationalTypeFloat, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeFloat), SimpleVar(-2, ComputationalTypeFloat)),
                    Assignment(1, SimpleVar(0, ComputationalTypeFloat),
                        PrefixExpr(1, ComputationalTypeFloat, Negate, SimpleVar(0, ComputationalTypeFloat))),
                    ReturnValue(2, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(
                    Array("0: r_0 = this;",
                        "1: r_1 = p_1;",
                        "2: op_0 = r_1;",
                        "3: op_0 = - op_0;",
                        "4: return op_0 /*AFloatValue*/;"))
            }

            it("should correctly reflect multiplication") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatMulMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatMulMethod, domain)
                val statements = AsQuadruples(FloatMulMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Multiply, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect modulo") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatRemMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatRemMethod, domain)
                val statements = AsQuadruples(FloatRemMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Modulo, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect subtraction") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, FloatSubMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, FloatSubMethod, domain)
                val statements = AsQuadruples(FloatSubMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeFloat),
                        BinaryExpr(2, ComputationalTypeFloat, Subtract, SimpleVar(0, ComputationalTypeFloat), SimpleVar(1, ComputationalTypeFloat))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AFloatValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
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
    }
}