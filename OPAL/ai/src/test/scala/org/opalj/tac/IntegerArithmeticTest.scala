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
class IntegerArithmeticTest extends FunSpec with Matchers {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

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

    describe("The quadruples representation of integer operations") {

        describe("using no AI results") {

            def binaryJLC(strg: String): Array[String] = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: r_2 = p_2;",
                    "3: op_0 = r_1;",
                    "4: op_1 = r_2;",
                    strg,
                    "6: return op_0;"
            )

            def unaryJLC(strg: String): Array[String] = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    strg,
                    "4: return op_0;"
            )

            def binaryAST(stmt: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
                stmt,
                ReturnValue(3, SimpleVar(0, ComputationalTypeInt))
            )

            it("should correctly reflect addition") {
                val statements = AsQuadruples(IntegerAddMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect logical and") {
                val statements = AsQuadruples(IntegerAndMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 & op_1;"))
            }

            it("should correctly reflect division") {
                val statements = AsQuadruples(IntegerDivMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect incrementation by a constant") {
                val statements = AsQuadruples(IntegerIncMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(-2, ComputationalTypeInt),
                        BinaryExpr(1, ComputationalTypeInt, Add, SimpleVar(-2, ComputationalTypeInt), IntConst(1, 1))),
                    ReturnValue(4, SimpleVar(0, ComputationalTypeInt))))
                javaLikeCode.shouldEqual(unaryJLC("3: r_1 = r_1 + 1;"))
            }

            it("should correctly reflect negation") {
                val statements = AsQuadruples(IntegerNegMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(0, ComputationalTypeInt),
                        PrefixExpr(1, ComputationalTypeInt, Negate, SimpleVar(0, ComputationalTypeInt))),
                    ReturnValue(2, SimpleVar(0, ComputationalTypeInt))))
                javaLikeCode.shouldEqual(unaryJLC("3: op_0 = - op_0;"))
            }

            it("should correctly reflect multiplication") {
                val statements = AsQuadruples(IntegerMulMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect logical or") {
                val statements = AsQuadruples(IntegerOrMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 | op_1;"))
            }

            it("should correctly reflect modulo") {
                val statements = AsQuadruples(IntegerRemMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect shift right") {
                val statements = AsQuadruples(IntegerShRMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >> op_1;"))
            }

            it("should correctly reflect shift left") {
                val statements = AsQuadruples(IntegerShLMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 << op_1;"))
            }

            it("should correctly reflect subtraction") {
                val statements = AsQuadruples(IntegerSubMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
            }

            it("should correctly reflect arithmetic shift right") {
                val statements = AsQuadruples(IntegerAShMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >>> op_1;"))
            }

            it("should correctly reflect logical xor") {
                val statements = AsQuadruples(IntegerXOrMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 ^ op_1;"))
            }
        }

        describe("using AI results") {

            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: r_2 = p_2;",
                    "3: op_0 = r_1;",
                    "4: op_1 = r_2;",
                    strg,
                    "6: return op_0 /*an int*/;"
            )

            def unaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    strg,
                    "4: return op_0 /*an int*/;"
            )

            def binaryAST(stmt1: Stmt, stmt2: Stmt): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
                stmt1,
                stmt2
            )

            it("should correctly reflect addition") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerAddMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerAddMethod, domain)
                val statements = AsQuadruples(IntegerAddMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect logical and") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerAndMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerAndMethod, domain)
                val statements = AsQuadruples(IntegerAndMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 & op_1;"))
            }

            it("should correctly reflect division") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerDivMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerDivMethod, domain)
                val statements = AsQuadruples(IntegerDivMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect incrementation by a constant") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerIncMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerIncMethod, domain)
                val statements = AsQuadruples(IntegerIncMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(-2, ComputationalTypeInt),
                        BinaryExpr(1, ComputationalTypeInt, Add, SimpleVar(-2, ComputationalTypeInt), IntConst(1, 1))),
                    ReturnValue(4, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(unaryJLC("3: r_1 = r_1 + 1;"))
            }

            it("should correctly reflect negation") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerNegMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerNegMethod, domain)
                val statements = AsQuadruples(IntegerNegMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(0, ComputationalTypeInt),
                        PrefixExpr(1, ComputationalTypeInt, Negate, SimpleVar(0, ComputationalTypeInt))),
                    ReturnValue(2, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(unaryJLC("3: op_0 = - op_0;"))
            }

            it("should correctly reflect multiplication") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerMulMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerMulMethod, domain)
                val statements = AsQuadruples(IntegerMulMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect logical or") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerOrMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerOrMethod, domain)
                val statements = AsQuadruples(IntegerOrMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 | op_1;"))
            }

            it("should correctly reflect modulo") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerRemMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerRemMethod, domain)
                val statements = AsQuadruples(IntegerRemMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect shift right") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerShRMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerShRMethod, domain)
                val statements = AsQuadruples(IntegerShRMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >> op_1;"))
            }

            it("should correctly reflect shift left") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerShLMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerShLMethod, domain)
                val statements = AsQuadruples(IntegerShLMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 << op_1;"))
            }

            it("should correctly reflect subtraction") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerSubMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerSubMethod, domain)
                val statements = AsQuadruples(IntegerSubMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
            }

            it("should correctly reflect arithmetic shift right") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerAShMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerAShMethod, domain)
                val statements = AsQuadruples(IntegerAShMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >>> op_1;"))
            }

            it("should correctly reflect logical xor") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerXOrMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerXOrMethod, domain)
                val statements = AsQuadruples(IntegerXOrMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 ^ op_1;"))
            }
        }
    }
}