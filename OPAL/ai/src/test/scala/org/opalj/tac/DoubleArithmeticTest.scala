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
class DoubleArithmeticTest extends FunSpec with Matchers {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    import BinaryArithmeticOperators._
    import UnaryArithmeticOperators._

    val DoubleAddMethod = ArithmeticExpressionsClassFile.findMethod("doubleAdd").get
    val DoubleDivMethod = ArithmeticExpressionsClassFile.findMethod("doubleDiv").get
    val DoubleNegMethod = ArithmeticExpressionsClassFile.findMethod("doubleNeg").get
    val DoubleMulMethod = ArithmeticExpressionsClassFile.findMethod("doubleMul").get
    val DoubleRemMethod = ArithmeticExpressionsClassFile.findMethod("doubleRem").get
    val DoubleSubMethod = ArithmeticExpressionsClassFile.findMethod("doubleSub").get
    //            val DoubleCmpMethod = ArithmeticExpressionsClassFile.findMethod("doubleCmp").get

    describe("The quadruples representation of double operations") {

        describe("using no AI results") {
            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: r_3 = p_2;",
                    "3: op_0 = r_1;",
                    "4: op_2 = r_3;",
                    strg,
                    "6: return op_0;"
            )

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                Assignment(-1, SimpleVar(-4, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                Assignment(1, SimpleVar(2, ComputationalTypeDouble), SimpleVar(-4, ComputationalTypeDouble)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeDouble))
            )

            it("should correctly reflect addition") {
                val statements = AsQuadruples(DoubleAddMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Add, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_2;"))
            }

            it("should correctly reflect division") {
                val statements = AsQuadruples(DoubleDivMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Divide, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_2;"))
            }

            it("should correctly reflect negation") {
                val statements = AsQuadruples(DoubleNegMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                    Assignment(1, SimpleVar(0, ComputationalTypeDouble),
                        PrefixExpr(1, ComputationalTypeDouble, Negate, SimpleVar(0, ComputationalTypeDouble))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeDouble))))
                javaLikeCode.shouldEqual(
                    Array("0: r_0 = this;",
                        "1: r_1 = p_1;",
                        "2: op_0 = r_1;",
                        "3: op_0 = - op_0;",
                        "4: return op_0;"))
            }

            it("should correctly reflect multiplication") {
                val statements = AsQuadruples(DoubleMulMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Multiply, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_2;"))
            }

            it("should correctly reflect modulo") {
                val statements = AsQuadruples(DoubleRemMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Modulo, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_2;"))
            }

            it("should correctly reflect subtraction") {
                val statements = AsQuadruples(DoubleSubMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Subtract, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_2;"))
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
        }

        describe("using AI results") {
            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: r_3 = p_2;",
                    "3: op_0 = r_1;",
                    "4: op_2 = r_3;",
                    strg,
                    "6: return op_0 /*a double*/;"
            )

            def binaryAST(stmt1: Stmt, stmt2: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                Assignment(-1, SimpleVar(-4, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                Assignment(1, SimpleVar(2, ComputationalTypeDouble), SimpleVar(-4, ComputationalTypeDouble)),
                stmt1,
                stmt2
            )

            it("should correctly reflect addition") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleAddMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleAddMethod, domain)
                val statements = AsQuadruples(DoubleAddMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Add, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_2;"))
            }

            it("should correctly reflect division") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleDivMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleDivMethod, domain)
                val statements = AsQuadruples(DoubleDivMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Divide, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_2;"))
            }

            it("should correctly reflect negation") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleNegMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleNegMethod, domain)
                val statements = AsQuadruples(DoubleNegMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeDouble), Param(ComputationalTypeDouble, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeDouble), SimpleVar(-2, ComputationalTypeDouble)),
                    Assignment(1, SimpleVar(0, ComputationalTypeDouble),
                        PrefixExpr(1, ComputationalTypeDouble, Negate, SimpleVar(0, ComputationalTypeDouble))),
                    ReturnValue(2, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(
                    Array("0: r_0 = this;",
                        "1: r_1 = p_1;",
                        "2: op_0 = r_1;",
                        "3: op_0 = - op_0;",
                        "4: return op_0 /*a double*/;"))
            }

            it("should correctly reflect multiplication") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleMulMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleMulMethod, domain)
                val statements = AsQuadruples(DoubleMulMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Multiply, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_2;"))
            }

            it("should correctly reflect modulo") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleRemMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleRemMethod, domain)
                val statements = AsQuadruples(DoubleRemMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Modulo, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_2;"))
            }

            it("should correctly reflect subtraction") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, DoubleSubMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, DoubleSubMethod, domain)
                val statements = AsQuadruples(DoubleSubMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeDouble),
                        BinaryExpr(2, ComputationalTypeDouble, Subtract, SimpleVar(0, ComputationalTypeDouble), SimpleVar(2, ComputationalTypeDouble))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.ADoubleValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_2;"))
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
        }
    }
}