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
class RefCmpIfTest extends FunSpec with Matchers {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val IfACMPEQMethod = ControlSequencesClassFile.findMethod("ifacmpeq").get
    val IfACMPNEMethod = ControlSequencesClassFile.findMethod("ifacmpne").get
    val IfNonNullMethod = ControlSequencesClassFile.findMethod("ifnonnull").get
    val IfNullMethod = ControlSequencesClassFile.findMethod("ifnull").get

    describe("The quadruples representation of reference comparison if instructions") {
        describe("using no AI results") {

            def binaryResultAST(stmt: Stmt): Array[Stmt] = Array(
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

            def unaryResultAST(stmt: Stmt): Array[Stmt] = Array(
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
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: r_2 = p_2;",
                "3: op_0 = r_1;",
                "4: op_1 = r_2;",
                strg,
                "6: op_0 = r_1;",
                "7: return op_0;",
                "8: op_0 = r_2;",
                "9: return op_0;"
            )

            def unaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                strg,
                "4: op_0 = r_1;",
                "5: return op_0;",
                "6: op_0 = null;",
                "7: return op_0;"
            )

            it("should correctly reflect the equals case") {
                val statements = AsQuadruples(IfACMPEQMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryResultAST(
                    If(2, SimpleVar(0, ComputationalTypeReference), EQ, SimpleVar(1, ComputationalTypeReference), 8)))
                javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 == op_1) goto 8;"))
            }

            it("should correctly reflect the not-equals case") {
                val statements = AsQuadruples(IfACMPNEMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryResultAST(
                    If(2, SimpleVar(0, ComputationalTypeReference), NE, SimpleVar(1, ComputationalTypeReference), 8)))
                javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 != op_1) goto 8;"))
            }

            it("should correctly reflect the non-null case") {
                val statements = AsQuadruples(IfNonNullMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(unaryResultAST(
                    If(1, SimpleVar(0, ComputationalTypeReference), NE, NullExpr(-1), 6)))
                javaLikeCode.shouldEqual(unaryJLC("3: if(op_0 != null) goto 6;"))
            }

            it("should correctly reflect the is-null case") {
                val statements = AsQuadruples(IfNullMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(unaryResultAST(
                    If(1, SimpleVar(0, ComputationalTypeReference), EQ, NullExpr(-1), 6)))
                javaLikeCode.shouldEqual(unaryJLC("3: if(op_0 == null) goto 6;"))
            }
        }

        describe("using AI results") {

            def binaryResultAST(stmt: Stmt, expr1: Expr, expr2: Expr): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeReference), Param(ComputationalTypeReference, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(1, SimpleVar(1, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
                stmt,
                Assignment(5, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                ReturnValue(6, expr1),
                Assignment(7, SimpleVar(0, ComputationalTypeReference), SimpleVar(-3, ComputationalTypeReference)),
                ReturnValue(8, expr2)
            )

            def unaryResultAST(stmt: Stmt, expr1: Expr, expr2: Expr): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                stmt,
                Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                ReturnValue(5, expr1),
                Assignment(6, SimpleVar(0, ComputationalTypeReference), NullExpr(6)),
                ReturnValue(7, expr2)
            )

            def binaryJLC(strg: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: r_2 = p_2;",
                "3: op_0 = r_1;",
                "4: op_1 = r_2;",
                strg,
                "6: op_0 = r_1;",
                "7: return op_0 /*{_ <: java.lang.Object, null}[@-2;t=102]*/;",
                "8: op_0 = r_2;",
                "9: return op_0 /*{_ <: java.lang.Object, null}[@-3;t=103]*/;"
            )

            def unaryJLC(cmp: String, ret1: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: op_0 = r_1;",
                cmp,
                "4: op_0 = r_1;",
                ret1,
                "6: op_0 = null;",
                "7: return op_0 /*null[@6;t=103]*/;"
            )

            it("should correctly reflect the equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfACMPEQMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfACMPEQMethod, domain)
                val statements = AsQuadruples(IfACMPEQMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryResultAST(
                    If(2, SimpleVar(0, ComputationalTypeReference), EQ, SimpleVar(1, ComputationalTypeReference), 8),
                    DomainValueBasedVar(0, domain.ReferenceValue(-2, ObjectType.Object).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.ReferenceValue(-3, ObjectType.Object).asInstanceOf[domain.DomainValue])))
                javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 == op_1) goto 8;"))
            }

            it("should correctly reflect the not-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfACMPNEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfACMPNEMethod, domain)
                val statements = AsQuadruples(IfACMPNEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(binaryResultAST(
                    If(2, SimpleVar(0, ComputationalTypeReference), NE, SimpleVar(1, ComputationalTypeReference), 8),
                    DomainValueBasedVar(0, domain.ReferenceValue(-2, ObjectType.Object).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.ReferenceValue(-3, ObjectType.Object).asInstanceOf[domain.DomainValue])))
                javaLikeCode.shouldEqual(binaryJLC("5: if(op_0 != op_1) goto 8;"))
            }

            it("should correctly reflect the non-null case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfNonNullMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfNonNullMethod, domain)
                val statements = AsQuadruples(IfNonNullMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(unaryResultAST(
                    If(1, SimpleVar(0, ComputationalTypeReference), NE, NullExpr(-1), 6),
                    DomainValueBasedVar(0, domain.NullValue(-2).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.NullValue(6).asInstanceOf[domain.DomainValue])))
                javaLikeCode.shouldEqual(unaryJLC(
                    "3: if(op_0 != null) goto 6;",
                    "5: return op_0 /*null[@-2;t=102]*/;"))
            }

            it("should correctly reflect the is-null case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfNullMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfNullMethod, domain)
                val statements = AsQuadruples(IfNullMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(unaryResultAST(
                    If(1, SimpleVar(0, ComputationalTypeReference), EQ, NullExpr(-1), 6),
                    DomainValueBasedVar(0, domain.NonNullObjectValue(-2, ObjectType.Object).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.NullValue(6).asInstanceOf[domain.DomainValue])))
                javaLikeCode.shouldEqual(unaryJLC(
                    "3: if(op_0 == null) goto 6;",
                    "5: return op_0 /*_ <: java.lang.Object[@-2;t=102]*/;"))
            }
        }
    }
}