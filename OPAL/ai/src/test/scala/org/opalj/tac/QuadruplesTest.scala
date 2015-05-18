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
class QuadruplesTest extends FunSpec with Matchers {

    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

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

    if (IntegerAddMethod.body.get.instructions.size == 0) fail()
    if (IntegerAndMethod.body.get.instructions.size == 0) fail()
    if (IntegerDivMethod.body.get.instructions.size == 0) fail()
    if (IntegerIncMethod.body.get.instructions.size == 0) fail()
    if (IntegerNegMethod.body.get.instructions.size == 0) fail()
    if (IntegerMulMethod.body.get.instructions.size == 0) fail()
    if (IntegerOrMethod.body.get.instructions.size == 0) fail()
    if (IntegerRemMethod.body.get.instructions.size == 0) fail()
    if (IntegerShRMethod.body.get.instructions.size == 0) fail()
    if (IntegerShLMethod.body.get.instructions.size == 0) fail()
    if (IntegerSubMethod.body.get.instructions.size == 0) fail()
    if (IntegerAShMethod.body.get.instructions.size == 0) fail()
    if (IntegerXOrMethod.body.get.instructions.size == 0) fail()

    val IntegerTestMethod = ArithmeticExpressionsClassFile.findMethod("integerTest").get

    describe("The quadruples representation") {

        it("should correctly reflect integer addition (using no AI results)") {
            val statements = AsQuadruples(IntegerAddMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
            statements.shouldEqual(ASTMaker.intAddRes)
            javaLikeCode.shouldEqual(JLCMaker.intAddRes)
        }

        it("should correctly reflect integer logical and (using no AI results)") {
            val statements = AsQuadruples(IntegerAndMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer division (using no AI results)") {
            val statements = AsQuadruples(IntegerDivMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer incrementation by a constant (using no AI results)") {
            val statements = AsQuadruples(IntegerIncMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer negation (using no AI results)") {
            val statements = AsQuadruples(IntegerNegMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer multiplication (using no AI results)") {
            val statements = AsQuadruples(IntegerMulMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer Or operation (using no AI results)") {
            val statements = AsQuadruples(IntegerOrMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer modulo operation (using no AI results)") {
            val statements = AsQuadruples(IntegerRemMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer shift right (using no AI results)") {
            val statements = AsQuadruples(IntegerShRMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer shift left (using no AI results)") {
            val statements = AsQuadruples(IntegerShLMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer subtraction (using no AI results)") {
            val statements = AsQuadruples(IntegerSubMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer arithmetic shift right (using no AI results)") {
            val statements = AsQuadruples(IntegerAShMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        it("should correctly reflect integer XOr operation (using no AI results)") {
            val statements = AsQuadruples(IntegerXOrMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)
        }

        //TESTOUTPUT
        it("should just print me some stuff for testing purposes") {
            val statements = AsQuadruples(IntegerTestMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)

            println(IntegerTestMethod.body.get.instructions.mkString("\n"))
            println(statements.mkString("\n"))
            println(javaLikeCode)
        }
        //TESTOUTPUT
    }
}
