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
class SwitchTest extends FunSpec with Matchers {

  val SwitchStatementsType = ObjectType("tactest/SwitchStatements")

  val testResources = locateTestResources("classfiles/tactest.jar", "ai")

  val project = Project(testResources)

  val SwitchStatementsClassFile = project.classFile(SwitchStatementsType).get

  val TableSwitchMethod = SwitchStatementsClassFile.findMethod("tableSwitch").get
  val LookupSwitchMethod = SwitchStatementsClassFile.findMethod("lookupSwitch").get

  describe("The quadruples representation of switch instructions") {
    describe("using no AI results") {
      it("should correctly reflect tableswitch case") {
        val statements = AsQuadruples(TableSwitchMethod, None)
        val javaLikeCode = ToJavaLike(statements, false)

        assert(statements.nonEmpty)
        assert(javaLikeCode.length > 0)
        statements.shouldEqual(Array(
          Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
          Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
          Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
          Switch(1, 10, SimpleVar(0, ComputationalTypeInt), IndexedSeq((0, 4), (1, 6), (2, 8))),
          Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 1)),
          ReturnValue(29, SimpleVar(0, ComputationalTypeInt)),
          Assignment(30, SimpleVar(0, ComputationalTypeInt), IntConst(30, 2)),
          ReturnValue(31, SimpleVar(0, ComputationalTypeInt)),
          Assignment(32, SimpleVar(0, ComputationalTypeInt), IntConst(32, 3)),
          ReturnValue(33, SimpleVar(0, ComputationalTypeInt)),
          Assignment(34, SimpleVar(0, ComputationalTypeInt), IntConst(34, 0)),
          ReturnValue(35, SimpleVar(0, ComputationalTypeInt))))
        javaLikeCode.shouldEqual(Array(
          "0: r_0 = this;",
          "1: r_1 = p_1;",
          "2: op_0 = r_1;",
          "3: switch(op_0){\n    0: goto 4;\n    1: goto 6;\n    2: goto 8;\n    default: goto 10;\n}",
          "4: op_0 = 1;",
          "5: return op_0;",
          "6: op_0 = 2;",
          "7: return op_0;",
          "8: op_0 = 3;",
          "9: return op_0;",
          "10: op_0 = 0;",
          "11: return op_0;"))
      }

      it("should correctly reflect lookupswitch case") {
        val statements = AsQuadruples(LookupSwitchMethod, None)
        val javaLikeCode = ToJavaLike(statements, false)

        assert(statements.nonEmpty)
        assert(javaLikeCode.length > 0)
        statements.shouldEqual(Array(
          Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
          Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
          Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
          Switch(1, 8, SimpleVar(0, ComputationalTypeInt), IndexedSeq((1, 4), (10, 6))),
          Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 10)),
          ReturnValue(30, SimpleVar(0, ComputationalTypeInt)),
          Assignment(31, SimpleVar(0, ComputationalTypeInt), IntConst(31, 200)),
          ReturnValue(34, SimpleVar(0, ComputationalTypeInt)),
          Assignment(35, SimpleVar(0, ComputationalTypeInt), IntConst(35, 0)),
          ReturnValue(36, SimpleVar(0, ComputationalTypeInt))))
        javaLikeCode.shouldEqual(Array(
          "0: r_0 = this;",
          "1: r_1 = p_1;",
          "2: op_0 = r_1;",
          "3: switch(op_0){\n    1: goto 4;\n    10: goto 6;\n    default: goto 8;\n}",
          "4: op_0 = 10;",
          "5: return op_0;",
          "6: op_0 = 200;",
          "7: return op_0;",
          "8: op_0 = 0;",
          "9: return op_0;"))
      }
    }

    describe("using AI results") {
      it("should correctly reflect tableswitch case") {
        val domain = new DefaultDomain(project, SwitchStatementsClassFile, TableSwitchMethod)
        val aiResult = BaseAI(SwitchStatementsClassFile, TableSwitchMethod, domain)
        val statements = AsQuadruples(TableSwitchMethod, Some(aiResult))
        val javaLikeCode = ToJavaLike(statements, false)

        assert(statements.nonEmpty)
        assert(javaLikeCode.length > 0)
        statements.shouldEqual(Array(
          Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
          Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
          Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
          Switch(1, 10, SimpleVar(0, ComputationalTypeInt), IndexedSeq((0, 4), (1, 6), (2, 8))),
          Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 1)),
          ReturnValue(29, DomainValueBasedVar(0, domain.IntegerRange(1)/*int=1*/)),
          Assignment(30, SimpleVar(0, ComputationalTypeInt), IntConst(30, 2)),
          ReturnValue(31, DomainValueBasedVar(0, domain.IntegerRange(2)/*int=2*/)),
          Assignment(32, SimpleVar(0, ComputationalTypeInt), IntConst(32, 3)),
          ReturnValue(33, DomainValueBasedVar(0, domain.IntegerRange(3)/*int=3*/)),
          Assignment(34, SimpleVar(0, ComputationalTypeInt), IntConst(34, 0)),
          ReturnValue(35, DomainValueBasedVar(0, domain.IntegerRange(0)/*int=0*/))))
        javaLikeCode.shouldEqual(Array(
          "0: r_0 = this;",
          "1: r_1 = p_1;",
          "2: op_0 = r_1;",
          "3: switch(op_0){\n    0: goto 4;\n    1: goto 6;\n    2: goto 8;\n    default: goto 10;\n}",
          "4: op_0 = 1;",
          "5: return op_0 /*int = 1*/;",
          "6: op_0 = 2;",
          "7: return op_0 /*int = 2*/;",
          "8: op_0 = 3;",
          "9: return op_0 /*int = 3*/;",
          "10: op_0 = 0;",
          "11: return op_0 /*int = 0*/;"))
      }

      it("should correctly reflect lookupswitch case") {
        val domain = new DefaultDomain(project, SwitchStatementsClassFile, LookupSwitchMethod)
        val aiResult = BaseAI(SwitchStatementsClassFile, LookupSwitchMethod, domain)
        val statements = AsQuadruples(LookupSwitchMethod, Some(aiResult))
        val javaLikeCode = ToJavaLike(statements, false)

        assert(statements.nonEmpty)
        assert(javaLikeCode.length > 0)
        statements.shouldEqual(Array(
          Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
          Assignment(-1, SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")),
          Assignment(0, SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)),
          Switch(1, 8, SimpleVar(0, ComputationalTypeInt), IndexedSeq((1, 4), (10, 6))),
          Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 10)),
          ReturnValue(30, DomainValueBasedVar(0, domain.IntegerRange(10)/*int=10*/)),
          Assignment(31, SimpleVar(0, ComputationalTypeInt), IntConst(31, 200)),
          ReturnValue(34, DomainValueBasedVar(0, domain.IntegerRange(200)/*int=200*/)),
          Assignment(35, SimpleVar(0, ComputationalTypeInt), IntConst(35, 0)),
          ReturnValue(36, DomainValueBasedVar(0, domain.IntegerRange(0)/*int=0*/))))
        javaLikeCode.shouldEqual(Array(
          "0: r_0 = this;",
          "1: r_1 = p_1;",
          "2: op_0 = r_1;",
          "3: switch(op_0){\n    1: goto 4;\n    10: goto 6;\n    default: goto 8;\n}",
          "4: op_0 = 10;",
          "5: return op_0 /*int = 10*/;",
          "6: op_0 = 200;",
          "7: return op_0 /*int = 200*/;",
          "8: op_0 = 0;",
          "9: return op_0 /*int = 0*/;"))
      }
    }
  }
}