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

    //    val BoringCodeType = ObjectType("controlflow/BoringCode")
    val ArithmeticExpressionsType = ObjectType("tactest/ArithmeticExpressions")

    //    val testResources = locateTestResources("classfiles/cfgtest8.jar", "br")
    val testResources = locateTestResources("classfiles/tactest.jar", "ai")

    val project = Project(testResources)

    //    val BoringCodeClassFile = project.classFile(BoringCodeType).get
    val ArithmeticExpressionsClassFile = project.classFile(ArithmeticExpressionsType).get

    //    val SingleBlockMethod = BoringCodeClassFile.findMethod("singleBlock").get
    //    val ConditionalTwoReturnsMethod = BoringCodeClassFile.findMethod("conditionalTwoReturns").get
    val IntegerAddMethod = ArithmeticExpressionsClassFile.findMethod("integerAdd").get
    val IntegerAndMethod = ArithmeticExpressionsClassFile.findMethod("integerAnd").get
    val IntegerDivMethod = ArithmeticExpressionsClassFile.findMethod("integerDiv").get
    val IntegerIncMethod = ArithmeticExpressionsClassFile.findMethod("integerInc").get

    //    if (SingleBlockMethod.body.get.instructions.size == 0) fail()
    if (IntegerAddMethod.body.get.instructions.size == 0) fail()
    if (IntegerAndMethod.body.get.instructions.size == 0) fail()
    if (IntegerDivMethod.body.get.instructions.size == 0) fail()
    if (IntegerIncMethod.body.get.instructions.size == 0) fail()

    describe("The quadruples representation") {

        //        it("should correctly reflect mathematical operations (using no AI results)") {
        //            val statements = AsQuadruples(SingleBlockMethod, None)
        //            val javaLikeCode = ToJavaLike(statements)
        //
        //            assert(statements.nonEmpty)
        //            assert(javaLikeCode.length() > 0)
        //
        //            //            println(statements.mkString("\n"))
        //            //            println(javaLikeCode)
        //
        //            // TODO  test that everything is as expected...
        //        }

        it("should correctly reflect integer addition (using no AI results)") {
            val statements = AsQuadruples(IntegerAddMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)

            println(IntegerAddMethod.body.get.instructions.mkString("\n"))
            println(statements.mkString("\n"))
            println(javaLikeCode)
        }

        it("should correctly reflect integer logical and (using no AI results)") {
            val statements = AsQuadruples(IntegerAndMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)

            println(IntegerAndMethod.body.get.instructions.mkString("\n"))
            println(statements.mkString("\n"))
            println(javaLikeCode)
        }

        it("should correctly reflect integer division (using no AI results)") {
            val statements = AsQuadruples(IntegerDivMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)

            println(IntegerDivMethod.body.get.instructions.mkString("\n"))
            println(statements.mkString("\n"))
            println(javaLikeCode)
        }

        it("should correctly reflect integer incrementation by a constant (using no AI results)") {
            val statements = AsQuadruples(IntegerIncMethod, None)
            val javaLikeCode = ToJavaLike(statements)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length() > 0)

            println(IntegerIncMethod.body.get.instructions.mkString("\n"))
            println(statements.mkString("\n"))
            println(javaLikeCode)
        }

        //        it("should have correct absoulte jump targets (using no AI results)") {
        //            val statements = AsQuadruples(ConditionalTwoReturnsMethod, None)
        //            val javaLikeCode = ToJavaLike(statements)
        //
        //            assert(statements.nonEmpty)
        //            assert(javaLikeCode.length() > 0)
        //
        //            println(ConditionalTwoReturnsMethod.body.get.instructions.mkString("\n"))
        //            println(statements.mkString("\n"))
        //            println(javaLikeCode)
        //
        //            // TODO test that everything is as expected...
        //        }

        //        it("should correctly reflect mathematical operations (using AI results)") {
        //            val domain = new DefaultDomain(project, BoringCodeClassFile, SingleBlockMethod)
        //            val aiResult = BaseAI(BoringCodeClassFile, SingleBlockMethod, domain)
        //            val statements = AsQuadruples(SingleBlockMethod, Some(aiResult))
        //            val javaLikeCode = ToJavaLike(statements)
        //
        //            assert(statements.nonEmpty)
        //            assert(javaLikeCode.length() > 0)
        //
        //            println(statements.mkString("\n"))
        //            println(javaLikeCode)
        //
        //            //TODO  test that everything is as expected...
        //        }

        //        it("should have correct absoulte jump targets (using AI results)") {
        //            val domain = new DefaultDomain(project, BoringCodeClassFile, ConditionalTwoReturnsMethod)
        //            val aiResult = BaseAI(BoringCodeClassFile, ConditionalTwoReturnsMethod, domain)
        //
        //            val statements = AsQuadruples(ConditionalTwoReturnsMethod, Some(aiResult))
        //            val javaLikeCode = ToJavaLike(statements)
        //
        //            assert(statements.nonEmpty)
        //            assert(javaLikeCode.length() > 0)
        //
        //            println(ConditionalTwoReturnsMethod.body.get.instructions.mkString("\n"))
        //            println(statements.mkString("\n"))
        //            println(javaLikeCode)
        //
        //            // TODO test that everything is as expected...
        //        }
    }
}
