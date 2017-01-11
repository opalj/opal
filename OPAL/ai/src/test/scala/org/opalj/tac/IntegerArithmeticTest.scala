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

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.br.TestSupport.biProject
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
                val statements = AsQuadruples(method = IntegerAddMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect logical and") {
                val statements = AsQuadruples(method = IntegerAndMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 & op_1;"))
            }

            it("should correctly reflect division") {
                val statements = AsQuadruples(method = IntegerDivMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect incrementation by a constant") {
                val statements = AsQuadruples(method = IntegerIncMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

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
                javaLikeCode.shouldEqual(unaryJLC("3: r_1 = r_1 + 1;"))
            }

            it("should correctly reflect negation") {
                val statements = AsQuadruples(method = IntegerNegMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

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
                javaLikeCode.shouldEqual(unaryJLC("3: op_0 = - op_0;"))
            }

            it("should correctly reflect multiplication") {
                val statements = AsQuadruples(method = IntegerMulMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect logical or") {
                val statements = AsQuadruples(method = IntegerOrMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 | op_1;"))
            }

            it("should correctly reflect modulo") {
                val statements = AsQuadruples(method = IntegerRemMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect shift right") {
                val statements = AsQuadruples(method = IntegerShRMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >> op_1;"))
            }

            it("should correctly reflect shift left") {
                val statements = AsQuadruples(method = IntegerShLMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 << op_1;"))
            }

            it("should correctly reflect subtraction") {
                val statements = AsQuadruples(method = IntegerSubMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
            }

            it("should correctly reflect arithmetic shift right") {
                val statements = AsQuadruples(method = IntegerAShMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >>> op_1;"))
            }

            it("should correctly reflect logical xor") {
                val statements = AsQuadruples(method = IntegerXOrMethod, aiResult = None)._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)))
                ))
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
                val statements = AsQuadruples(method = IntegerAddMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Add, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 + op_1;"))
            }

            it("should correctly reflect logical and") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerAndMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerAndMethod, domain)
                val statements = AsQuadruples(method = IntegerAndMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, And, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 & op_1;"))
            }

            it("should correctly reflect division") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerDivMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerDivMethod, domain)
                val statements = AsQuadruples(method = IntegerDivMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Divide, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 / op_1;"))
            }

            it("should correctly reflect incrementation by a constant") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerIncMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerIncMethod, domain)
                val statements = AsQuadruples(method = IntegerIncMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(-2, ComputationalTypeInt),
                        BinaryExpr(1, ComputationalTypeInt, Add, SimpleVar(-2, ComputationalTypeInt), IntConst(1, 1))),
                    ReturnValue(4, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(unaryJLC("3: r_1 = r_1 + 1;"))
            }

            it("should correctly reflect negation") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerNegMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerNegMethod, domain)
                val statements = AsQuadruples(method = IntegerNegMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(Array(
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                    Assignment(1, SimpleVar(0, ComputationalTypeInt),
                        PrefixExpr(1, ComputationalTypeInt, Negate, SimpleVar(0, ComputationalTypeInt))),
                    ReturnValue(2, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(unaryJLC("3: op_0 = - op_0;"))
            }

            it("should correctly reflect multiplication") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerMulMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerMulMethod, domain)
                val statements = AsQuadruples(method = IntegerMulMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Multiply, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 * op_1;"))
            }

            it("should correctly reflect logical or") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerOrMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerOrMethod, domain)
                val statements = AsQuadruples(method = IntegerOrMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Or, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 | op_1;"))
            }

            it("should correctly reflect modulo") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerRemMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerRemMethod, domain)
                val statements = AsQuadruples(method = IntegerRemMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Modulo, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 % op_1;"))
            }

            it("should correctly reflect shift right") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerShRMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerShRMethod, domain)
                val statements = AsQuadruples(method = IntegerShRMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >> op_1;"))
            }

            it("should correctly reflect shift left") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerShLMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerShLMethod, domain)
                val statements = AsQuadruples(method = IntegerShLMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, ShiftLeft, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 << op_1;"))
            }

            it("should correctly reflect subtraction") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerSubMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerSubMethod, domain)
                val statements = AsQuadruples(method = IntegerSubMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, Subtract, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 - op_1;"))
            }

            it("should correctly reflect arithmetic shift right") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerAShMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerAShMethod, domain)
                val statements = AsQuadruples(method = IntegerAShMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, UnsignedShiftRight, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 >>> op_1;"))
            }

            it("should correctly reflect logical xor") {
                val domain = new DefaultDomain(project, ArithmeticExpressionsClassFile, IntegerXOrMethod)
                val aiResult = BaseAI(ArithmeticExpressionsClassFile, IntegerXOrMethod, domain)
                val statements = AsQuadruples(method = IntegerXOrMethod, aiResult = Some(aiResult))._1
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryAST(
                    Assignment(2, SimpleVar(0, ComputationalTypeInt),
                        BinaryExpr(2, ComputationalTypeInt, XOr, SimpleVar(0, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt))),
                    ReturnValue(3, DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]))
                ))
                javaLikeCode.shouldEqual(binaryJLC("5: op_0 = op_0 ^ op_1;"))
            }
        }
    }
}
