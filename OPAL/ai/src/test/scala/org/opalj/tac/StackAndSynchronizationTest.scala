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
//import org.opalj.ai.domain.l1.DefaultDomain

/**
 * Tests the conversion of parsed methods to a quadruple representation
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class StackAndSynchronizationTest extends FunSpec with Matchers {

  val StackAndSynchronizeType = ObjectType("tactest/StackManipulationAndSynchronization")

  val testResources = locateTestResources("classfiles/tactest.jar", "ai")

  val project = Project(testResources)

  val StackAndSynchronizeClassFile = project.classFile(StackAndSynchronizeType).get

  val PopMethod = StackAndSynchronizeClassFile.findMethod("pop").get
  val Pop2Case2Method = StackAndSynchronizeClassFile.findMethod("pop2case2").get
  val DupMethod = StackAndSynchronizeClassFile.findMethod("dup").get
  val MonitorEnterAndExitMethod = StackAndSynchronizeClassFile.findMethod("monitorEnterAndExit").get
  val InvokeStaticMethod = StackAndSynchronizeClassFile.findMethod("invokeStatic").get
  val InvokeInterfaceMethod = StackAndSynchronizeClassFile.findMethod("invokeInterface").get

  describe("The quadruples representation of stack manipulation and synchronization instructions") {

    describe("using no AI results") {
      it("should correctly reflect pop") {
        println("---------- PopMethod -----------------")
        println(PopMethod.body.get.instructions.mkString("\n"))
      }

      it("should correctly reflect pop2 mode 2") {
        println("---------- Pop2Case2Method -----------------")
        println(Pop2Case2Method.body.get.instructions.mkString("\n"))
      }

      it("should correctly reflect dup") {
        println("---------- DupMethod -----------------")
        println(DupMethod.body.get.instructions.mkString("\n"))
      }

      it("should correctly reflect monitorenter and -exit") {
        println("---------- MonitorEnterAndExitMethod -----------------")
        println(MonitorEnterAndExitMethod.body.get.instructions.mkString("\n"))
      }
      
      it("should correctly reflect invokestatic") {
        println("---------- InvokeStaticMethod -----------------")
        println(InvokeStaticMethod.body.get.instructions.mkString("\n"))
      }
      
      it("should correctly reflect invokeinterface") {
        println("---------- InvokeInterfaceMethod -----------------")
        println(InvokeInterfaceMethod.body.get.instructions.mkString("\n"))
      }
    }
  }

}