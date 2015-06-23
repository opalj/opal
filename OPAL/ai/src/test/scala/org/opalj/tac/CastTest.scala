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

        val TypecheckMethod = CastInstructionsClassFile.findMethod("typecheck").get

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

        describe("using no AI results") {

          it("should correctly reflect the instanceof instruction") {
            println(TypecheckMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the d2f instruction") {
            println(D2FMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the d2i instruction") {
            println(D2IMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the d2l instruction") {
            println(D2LMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the f2d instruction") {
            println(F2DMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the f2l instruction") {
            println(F2LMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the f2i instruction") {
            println(F2IMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the l2d instruction") {
            println(L2DMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the l2f instruction") {
            println(L2FMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the l2i instruction") {
            println(L2IMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the i2d instruction") {
            println(I2DMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the i2l instruction") {
            println(I2LMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
          
          it("should correctly reflect the i2f instruction") {
            println(I2FMethod.body.get.instructions.mkString("\n"))
            println("---------------------------")
          }
        }

        describe("using AI results") {

        }
    }
}