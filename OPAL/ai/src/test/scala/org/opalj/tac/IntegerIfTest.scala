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
class IntegerIfTest extends FunSpec with Matchers {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val ICMPNEMethod = ControlSequencesClassFile.findMethod("icmpne").get
    val ICMPEQMethod = ControlSequencesClassFile.findMethod("icmpeq").get
    val ICMPGEMethod = ControlSequencesClassFile.findMethod("icmpge").get
    val ICMPLTMethod = ControlSequencesClassFile.findMethod("icmplt").get
    val ICMPLEMethod = ControlSequencesClassFile.findMethod("icmple").get
    val ICMPGTMethod = ControlSequencesClassFile.findMethod("icmpgt").get

    describe("The quadruples representation of integer if instructions") {
        describe("using no AI results") {

            def resultJLC(strg: String) = Array(
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

            it("should correctly reflect the not-equals case") {
                val statements = AsQuadruples(ICMPNEMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), NE, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 != op_1) goto 8;"))
            }

            it("should correctly reflect the equals case") {
                val statements = AsQuadruples(ICMPEQMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), EQ, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 == op_1) goto 8;"))
            }

            it("should correctly reflect the greater-equals case") {
                val statements = AsQuadruples(ICMPGEMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GE, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 >= op_1) goto 8;"))
            }

            it("should correctly reflect the less-then case") {
                val statements = AsQuadruples(ICMPLTMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LT, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 < op_1) goto 8;"))
            }

            it("should correctly reflect the less-equals case") {
                val statements = AsQuadruples(ICMPLEMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LE, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 <= op_1) goto 8;"))
            }

            it("should correctly reflect the greater-then case") {
                val statements = AsQuadruples(ICMPGTMethod, None)
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GT, SimpleVar(1, ComputationalTypeInt), 8)
                ))
                javaLikeCode.shouldEqual(resultJLC("5: if(op_0 > op_1) goto 8;"))
            }
        }

        describe("using AI results") {

            def resultJLC(strg1: String, strg2: String, strg3: String) = Array(
                "0: r_0 = this;",
                "1: r_1 = p_1;",
                "2: r_2 = p_2;",
                "3: op_0 = r_1;",
                "4: op_1 = r_2;",
                strg1,
                "6: op_0 = r_1;",
                strg2,
                "8: op_0 = r_2;",
                strg3
            )

            def resultAST(stmt: Stmt, expr1: Expr, expr2: Expr): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(-1, SimpleVar(-3, ComputationalTypeInt), Param(ComputationalTypeInt, "p_2")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
                stmt,
                Assignment(5, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                ReturnValue(6, expr1),
                Assignment(7, SimpleVar(0, ComputationalTypeInt), SimpleVar(-3, ComputationalTypeInt)),
                ReturnValue(8, expr2)
            )

            it("should correctly reflect the not-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPNEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPNEMethod, domain)
                val statements = AsQuadruples(ICMPNEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), NE, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue])
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 != op_1) goto 8;",
                    "7: return op_0 /*an int*/;",
                    "9: return op_0 /*an int*/;"
                ))
            }

            it("should correctly reflect the equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPEQMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPEQMethod, domain)
                val statements = AsQuadruples(ICMPEQMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), EQ, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue])
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 == op_1) goto 8;",
                    "7: return op_0 /*an int*/;",
                    "9: return op_0 /*an int*/;"
                ))
            }

            it("should correctly reflect the greater-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPGEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPGEMethod, domain)
                val statements = AsQuadruples(ICMPGEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GE, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(
                        0,
                        domain.IntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE - 1).asInstanceOf[domain.DomainValue]
                    ),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue])
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 >= op_1) goto 8;",
                    "7: return op_0 /*int ∈ ["+Integer.MIN_VALUE+","+(Integer.MAX_VALUE - 1)+"]*/;",
                    "9: return op_0 /*an int*/;"
                ))
            }

            it("should correctly reflect the less-then case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPLTMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPLTMethod, domain)
                val statements = AsQuadruples(ICMPLTMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LT, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(
                        0,
                        domain.IntegerRange(Integer.MIN_VALUE + 1, Integer.MAX_VALUE).asInstanceOf[domain.DomainValue]
                    )
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 < op_1) goto 8;",
                    "7: return op_0 /*an int*/;",
                    "9: return op_0 /*int ∈ ["+(Integer.MIN_VALUE + 1)+","+Integer.MAX_VALUE+"]*/;"
                ))
            }

            it("should correctly reflect the less-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPLEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPLEMethod, domain)
                val statements = AsQuadruples(ICMPLEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), LE, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(
                        0,
                        domain.IntegerRange(Integer.MIN_VALUE + 1, Integer.MAX_VALUE).asInstanceOf[domain.DomainValue]
                    ),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue])
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 <= op_1) goto 8;",
                    "7: return op_0 /*int ∈ ["+(Integer.MIN_VALUE + 1)+","+Integer.MAX_VALUE+"]*/;",
                    "9: return op_0 /*an int*/;"
                ))
            }

            it("should correctly reflect the greater-then case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, ICMPGTMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, ICMPGTMethod, domain)
                val statements = AsQuadruples(ICMPGTMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements, false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(2, SimpleVar(0, ComputationalTypeInt), GT, SimpleVar(1, ComputationalTypeInt), 8),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(
                        0,
                        domain.IntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE - 1).asInstanceOf[domain.DomainValue]
                    )
                ))
                javaLikeCode.shouldEqual(resultJLC(
                    "5: if(op_0 > op_1) goto 8;",
                    "7: return op_0 /*an int*/;",
                    "9: return op_0 /*int ∈ ["+Integer.MIN_VALUE+","+(Integer.MAX_VALUE - 1)+"]*/;"
                ))
            }
        }
    }
}