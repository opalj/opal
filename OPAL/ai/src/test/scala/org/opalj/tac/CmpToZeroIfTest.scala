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
class CmpToZeroIfTest extends FunSpec with Matchers {

    val ControlSequencesType = ObjectType("tactest/ControlSequences")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ControlSequencesClassFile = project.classFile(ControlSequencesType).get

    import RelationalOperators._

    val IfNEMethod = ControlSequencesClassFile.findMethod("ifne").get
    val IfEQMethod = ControlSequencesClassFile.findMethod("ifeq").get
    val IfGEMethod = ControlSequencesClassFile.findMethod("ifge").get
    val IfLTMethod = ControlSequencesClassFile.findMethod("iflt").get
    val IfLEMethod = ControlSequencesClassFile.findMethod("ifle").get
    val IfGTMethod = ControlSequencesClassFile.findMethod("ifgt").get

    describe("The quadruples representation of compare to zero if instructions") {

        describe("using no AI results") {

            def resultJLC(strg: String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    strg,
                    "4: op_0 = r_1;",
                    "5: return op_0;",
                    "6: op_0 = 0;",
                    "7: return op_0;"
            )

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

            it("should correctly reflect the not-equals case") {
                val statements = AsQuadruples(IfNEMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), NE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 != 0) goto 6;"))
            }

            it("should correctly reflect the equals case") {
                val statements = AsQuadruples(IfEQMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), EQ, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 == 0) goto 6;"))
            }

            it("should correctly reflect the greater-equals case") {
                val statements = AsQuadruples(IfGEMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 >= 0) goto 6;"))
            }

            it("should correctly reflect the less-then case") {
                val statements = AsQuadruples(IfLTMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LT, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 < 0) goto 6;"))
            }

            it("should correctly reflect the less-equals case") {
                val statements = AsQuadruples(IfLEMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LE, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 <= 0) goto 6;"))
            }

            it("should correctly reflect the greater-then case") {
                val statements = AsQuadruples(IfGTMethod, None)
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GT, IntConst(-1, 0), 6)))
                javaLikeCode.shouldEqual(resultJLC("3: if(op_0 > 0) goto 6;"))
            }
        }

        describe("using AI results") {

            def resultJLC(strg1:String, strg2:String, strg3:String) = Array(
                "0: r_0 = this;",
                    "1: r_1 = p_1;",
                    "2: op_0 = r_1;",
                    strg1,
                    "4: op_0 = r_1;",
                    strg2,
                    "6: op_0 = 0;",
                    strg3
            )

            def resultAST(stmt: Stmt, expr1: Expr, expr2:Expr): Array[Stmt] = Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                stmt,
                Assignment(4, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
                ReturnValue(5, expr1),
                Assignment(6, SimpleVar(0, ComputationalTypeInt), IntConst(6, 0)),
                ReturnValue(7, expr2)
            )

            it("should correctly reflect the not-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfNEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfNEMethod, domain)
                val statements = AsQuadruples(IfNEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), NE, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, domain.IntegerConstant0),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 != 0) goto 6;",
                        "5: return op_0 /*int = 0*/;",
                        "7: return op_0 /*int = 0*/;"))
            }

            it("should correctly reflect the equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfEQMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfEQMethod, domain)
                val statements = AsQuadruples(IfEQMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), EQ, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, domain.AnIntegerValue.asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 == 0) goto 6;",
                        "5: return op_0 /*an int*/;",
                        "7: return op_0 /*int = 0*/;"))
            }

            it("should correctly reflect the greater-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfGEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfGEMethod, domain)
                val statements = AsQuadruples(IfGEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GE, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, 
                            domain.IntegerRange(Integer.MIN_VALUE, -1).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 >= 0) goto 6;",
                        "5: return op_0 /*int ∈ ["+Integer.MIN_VALUE+","+(-1)+"]*/;",
                        "7: return op_0 /*int = 0*/;"))
            }

            it("should correctly reflect the less-then case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfLTMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfLTMethod, domain)
                val statements = AsQuadruples(IfLTMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LT, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, 
                            domain.IntegerRange(0, Integer.MAX_VALUE).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 < 0) goto 6;",
                        "5: return op_0 /*int ∈ [0,"+Integer.MAX_VALUE+"]*/;",
                        "7: return op_0 /*int = 0*/;"))
            }

            it("should correctly reflect the less-equals case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfLEMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfLEMethod, domain)
                val statements = AsQuadruples(IfLEMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), LE, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, 
                            domain.IntegerRange(1, Integer.MAX_VALUE).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 <= 0) goto 6;",
                        "5: return op_0 /*int ∈ [1,"+Integer.MAX_VALUE+"]*/;",
                        "7: return op_0 /*int = 0*/;"))
            }

            it("should correctly reflect the greater-then case") {
                val domain = new DefaultDomain(project, ControlSequencesClassFile, IfGTMethod)
                val aiResult = BaseAI(ControlSequencesClassFile, IfGTMethod, domain)
                val statements = AsQuadruples(IfGTMethod, Some(aiResult))
                val javaLikeCode = ToJavaLike(statements,false)

                assert(statements.nonEmpty)
                assert(javaLikeCode.length > 0)
                statements.shouldEqual(resultAST(
                    If(1, SimpleVar(0, ComputationalTypeInt), GT, IntConst(-1, 0), 6),
                    DomainValueBasedVar(0, 
                            domain.IntegerRange(Integer.MIN_VALUE, 0).asInstanceOf[domain.DomainValue]),
                    DomainValueBasedVar(0, domain.IntegerConstant0)))
                javaLikeCode.shouldEqual(resultJLC(
                        "3: if(op_0 > 0) goto 6;",
                        "5: return op_0 /*int ∈ ["+Integer.MIN_VALUE+","+0+"]*/;",
                        "7: return op_0 /*int = 0*/;"))
            }
        }

    }
}