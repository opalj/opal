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
class CastTest extends FunSpec with Matchers {

    describe("The quadruples representation of cast instructions") {

        val CastInstructionsType = ObjectType("tactest/CastInstructions")

        val testResources = locateTestResources("classfiles/tactest.jar", "ai")

        val project = Project(testResources)

        val CastInstructionsClassFile = project.classFile(CastInstructionsType).get

        val TypecheckStringMethod = CastInstructionsClassFile.findMethod("typecheckString").get
        val TypecheckListMethod = CastInstructionsClassFile.findMethod("typecheckList").get
        val CheckcastMethod = CastInstructionsClassFile.findMethod("checkcast").get

        val D2FMethod = CastInstructionsClassFile.findMethod("d2f").get
        val D2LMethod = CastInstructionsClassFile.findMethod("d2l").get
        val D2IMethod = CastInstructionsClassFile.findMethod("d2i").get

        val F2DMethod = CastInstructionsClassFile.findMethod("f2d").get
        val F2IMethod = CastInstructionsClassFile.findMethod("f2i").get
        val F2LMethod = CastInstructionsClassFile.findMethod("f2l").get

        val L2DMethod = CastInstructionsClassFile.findMethod("l2d").get
        val L2IMethod = CastInstructionsClassFile.findMethod("l2i").get
        val L2FMethod = CastInstructionsClassFile.findMethod("l2f").get

        val I2DMethod = CastInstructionsClassFile.findMethod("i2d").get
        val I2LMethod = CastInstructionsClassFile.findMethod("i2l").get
        val I2FMethod = CastInstructionsClassFile.findMethod("i2f").get
        val I2SMethod = CastInstructionsClassFile.findMethod("i2s").get
        val I2BMethod = CastInstructionsClassFile.findMethod("i2b").get
        val I2CMethod = CastInstructionsClassFile.findMethod("i2c").get

        describe("using no AI results") {

            def longResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                strg,
                "4: r_3 = op_2;",
                "5: return;"
            )

            def shortResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                strg,
                "4: r_2 = op_1;",
                "5: return;"
            )

            def typecheckResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                "3: op_1 = op_0 instanceof "+strg+";",
                "4: r_2 = op_1;",
                "5: return;"
            )

            def castResultAST(from: ComputationalType, to: BaseType): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, from), Param(from, "p_1")),
                Assignment(0, SimpleVar(0, from), SimpleVar(-2, from)),
                Assignment(1, SimpleVar(from.category.toInt, to.computationalType), PrimitiveTypecastExpr(1, to, SimpleVar(0, from))),
                Assignment(2, SimpleVar(-2 - from.category, to.computationalType), SimpleVar(from.category.toInt, to.computationalType)),
                Return(3)
            )

            def typecheckResultAST(refTp: ReferenceType): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), InstanceOf(1, SimpleVar(0, ComputationalTypeReference), refTp)),
                Assignment(4, SimpleVar(-3, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)),
                Return(5)
            )

            it("should correctly reflect the instanceof Object instruction") {
                val statements = AsQuadruples(method = TypecheckStringMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(typecheckResultAST(ObjectType.Object))
                javaLikeCode.shouldEqual(typecheckResultJLC(ObjectType.Object.toJava))
            }

            it("should correctly reflect the instanceof List instruction") {
                val statements = AsQuadruples(method = TypecheckListMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                val listTpe = ReferenceType.apply("java/util/List")
                statements.shouldEqual(typecheckResultAST(listTpe))
                javaLikeCode.shouldEqual(typecheckResultJLC(listTpe.toJava))
            }

            it("should correctly reflect the checkcast instruction") {
                val statements = AsQuadruples(method = CheckcastMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                val listType = ReferenceType("java/util/List")
                statements.shouldEqual(Array[Stmt](
                    Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                    Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                    Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                    Assignment(1, SimpleVar(0, ComputationalTypeReference), Checkcast(1, SimpleVar(0, ComputationalTypeReference), listType)),
                    Assignment(4, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                    Return(5)
                ))
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    s"3: op_0 = (${listType.toJava}) op_0;",
                    "4: r_2 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect the d2f instruction") {
                val statements = AsQuadruples(method = D2FMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, FloatType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (float) op_0;"))
            }

            it("should correctly reflect the d2i instruction") {
                val statements = AsQuadruples(method = D2IMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, IntegerType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (int) op_0;"))
            }

            it("should correctly reflect the d2l instruction") {
                val statements = AsQuadruples(method = D2LMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, LongType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (long) op_0;"))
            }

            it("should correctly reflect the f2d instruction") {
                val statements = AsQuadruples(method = F2DMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, DoubleType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (double) op_0;"))
            }

            it("should correctly reflect the f2l instruction") {
                val statements = AsQuadruples(method = F2LMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, LongType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (long) op_0;"))
            }

            it("should correctly reflect the f2i instruction") {
                val statements = AsQuadruples(method = F2IMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, IntegerType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (int) op_0;"))
            }

            it("should correctly reflect the l2d instruction") {
                val statements = AsQuadruples(method = L2DMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, DoubleType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (double) op_0;"))
            }

            it("should correctly reflect the l2f instruction") {
                val statements = AsQuadruples(method = L2FMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, FloatType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (float) op_0;"))
            }

            it("should correctly reflect the l2i instruction") {
                val statements = AsQuadruples(method = L2IMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, IntegerType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (int) op_0;"))
            }

            it("should correctly reflect the i2d instruction") {
                val statements = AsQuadruples(method = I2DMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, DoubleType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (double) op_0;"))
            }

            it("should correctly reflect the i2l instruction") {
                val statements = AsQuadruples(method = I2LMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, LongType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (long) op_0;"))
            }

            it("should correctly reflect the i2f instruction") {
                val statements = AsQuadruples(method = I2FMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, FloatType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (float) op_0;"))
            }

            it("should correctly reflect the i2c instruction") {
                val statements = AsQuadruples(method = I2CMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, CharType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (char) op_0;"))
            }

            it("should correctly reflect the i2b instruction") {
                val statements = AsQuadruples(method = I2BMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, ByteType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (byte) op_0;"))
            }

            it("should correctly reflect the i2s instruction") {
                val statements = AsQuadruples(method = I2SMethod, aiResult = None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, ShortType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (short) op_0;"))
            }
        }

        describe("using AI results") {

            def longResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                strg,
                "4: r_3 = op_2;",
                "5: return;"
            )

            def shortResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                strg,
                "4: r_2 = op_1;",
                "5: return;"
            )

            def typecheckResultJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                "3: op_1 = op_0 instanceof "+strg+";",
                "4: r_2 = op_1;",
                "5: return;"
            )

            def castResultAST(from: ComputationalType, to: BaseType): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, from), Param(from, "p_1")),
                Assignment(0, SimpleVar(0, from), SimpleVar(-2, from)),
                Assignment(1, SimpleVar(from.category.toInt, to.computationalType), PrimitiveTypecastExpr(1, to, SimpleVar(0, from))),
                Assignment(2, SimpleVar(-2 - from.category, to.computationalType), SimpleVar(from.category.toInt, to.computationalType)),
                Return(3)
            )

            def typecheckResultAST(refTp: ReferenceType): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), InstanceOf(1, SimpleVar(0, ComputationalTypeReference), refTp)),
                Assignment(4, SimpleVar(-3, ComputationalTypeInt), SimpleVar(1, ComputationalTypeInt)),
                Return(5)
            )

            it("should correctly reflect the instanceof Object instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, TypecheckStringMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, TypecheckStringMethod, domain)
                val statements = AsQuadruples(method = TypecheckStringMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(typecheckResultAST(ObjectType.Object))
                javaLikeCode.shouldEqual(typecheckResultJLC(ObjectType.Object.toJava))
            }

            it("should correctly reflect the instanceof List instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, TypecheckListMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, TypecheckListMethod, domain)
                val statements = AsQuadruples(method = TypecheckListMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                val listType = ReferenceType.apply("java/util/List")
                statements.shouldEqual(typecheckResultAST(listType))
                javaLikeCode.shouldEqual(typecheckResultJLC(listType.toJava))
            }

            it("should correctly reflect the checkcast instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, CheckcastMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, CheckcastMethod, domain)
                val statements = AsQuadruples(method = CheckcastMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                val expected =
                    Array[Stmt](
                        Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                        Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                        Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                        Assignment(1, SimpleVar(0, ComputationalTypeReference), Checkcast(1, SimpleVar(0, ComputationalTypeReference), ReferenceType("java/util/List"))),
                        Assignment(4, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                        Return(5)
                    )
                if (!(statements.toSeq == expected.toSeq)) {
                    fail(expected.
                        zip(statements).
                        map(ev ⇒ ev._1.toString()+" == "+ev._2.toString()+" = "+(ev._1 == ev._2)).
                        mkString("\n"))
                }
                javaLikeCode.shouldEqual(Array(
                    "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    "3: op_0 = (java.util.List) op_0;",
                    "4: r_2 = op_0;",
                    "5: return;"
                ))
            }

            it("should correctly reflect the d2f instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, D2FMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, D2FMethod, domain)
                val statements = AsQuadruples(method = D2FMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, FloatType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (float) op_0;"))
            }

            it("should correctly reflect the d2i instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, D2IMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, D2IMethod, domain)
                val statements = AsQuadruples(method = D2IMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, IntegerType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (int) op_0;"))
            }

            it("should correctly reflect the d2l instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, D2LMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, D2LMethod, domain)
                val statements = AsQuadruples(method = D2LMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeDouble, LongType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (long) op_0;"))
            }

            it("should correctly reflect the f2d instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, F2DMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, F2DMethod, domain)
                val statements = AsQuadruples(method = F2DMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, DoubleType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (double) op_0;"))
            }

            it("should correctly reflect the f2l instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, F2LMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, F2LMethod, domain)
                val statements = AsQuadruples(method = F2LMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, LongType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (long) op_0;"))
            }

            it("should correctly reflect the f2i instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, F2IMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, F2IMethod, domain)
                val statements = AsQuadruples(method = F2IMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeFloat, IntegerType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (int) op_0;"))
            }

            it("should correctly reflect the l2d instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, L2DMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, L2DMethod, domain)
                val statements = AsQuadruples(method = L2DMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, DoubleType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (double) op_0;"))
            }

            it("should correctly reflect the l2f instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, L2FMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, L2FMethod, domain)
                val statements = AsQuadruples(method = L2FMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, FloatType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (float) op_0;"))
            }

            it("should correctly reflect the l2i instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, L2IMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, L2IMethod, domain)
                val statements = AsQuadruples(method = L2IMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeLong, IntegerType))
                javaLikeCode.shouldEqual(longResultJLC("3: op_2 = (int) op_0;"))
            }

            it("should correctly reflect the i2d instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2DMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2DMethod, domain)
                val statements = AsQuadruples(method = I2DMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, DoubleType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (double) op_0;"))
            }

            it("should correctly reflect the i2l instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2LMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2LMethod, domain)
                val statements = AsQuadruples(method = I2LMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, LongType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (long) op_0;"))
            }

            it("should correctly reflect the i2f instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2FMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2FMethod, domain)
                val statements = AsQuadruples(method = I2FMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, FloatType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (float) op_0;"))
            }

            it("should correctly reflect the i2c instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2CMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2CMethod, domain)
                val statements = AsQuadruples(method = I2CMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, CharType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (char) op_0;"))
            }

            it("should correctly reflect the i2b instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2BMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2BMethod, domain)
                val statements = AsQuadruples(method = I2BMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, ByteType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (byte) op_0;"))
            }

            it("should correctly reflect the i2s instruction") {
                val domain = new DefaultDomain(project, CastInstructionsClassFile, I2SMethod)
                val aiResult = BaseAI(CastInstructionsClassFile, I2SMethod, domain)
                val statements = AsQuadruples(method = I2SMethod, aiResult = Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(castResultAST(ComputationalTypeInt, ShortType))
                javaLikeCode.shouldEqual(shortResultJLC("3: op_1 = (short) op_0;"))
            }
        }
    }
}
