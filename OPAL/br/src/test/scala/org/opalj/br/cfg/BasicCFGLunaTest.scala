/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.br.cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType

/**
 * The same as BasicCFGJava8Test, but for class-files compiled with Eclipse Luna's
 * internal compiler
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BasicCFGLunaTest extends FunSpec with Matchers {

    val testJAR = "classfiles/cfgtestLuna.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)

    describe("cfgs with very simple control flow") {

        val testClass = testProject.classFile(ObjectType("controlflow/BoringCode")).get

        it("a cfg with no control flow statemts should consists of a single basic block") {

            val cfg = CFGFactory(testClass.findMethod("singleBlock").get)
            val bbs = cfg.allBBs

            bbs.size should be(1)
            cfg.startBlock.successors.size should be(1)
            cfg.normalReturnNode.predecessors.size should be(1)
            cfg.abnormalReturnNode.predecessors.size should be(0)
        }

        it("a cfg with some simple control flow statemts should consists of respective single basic blocks") {

            val cfg = CFGFactory(testClass.findMethod("conditionalOneReturn").get)
            val bbs = cfg.allBBs

            bbs.size should be(11)
            cfg.startBlock.successors.size should be(2)
            cfg.normalReturnNode.predecessors.size should be(1)
            cfg.abnormalReturnNode.predecessors.size should be(1)
        }

        it("a cfg for a method with multiple return statements should have corresponding basic blocks") {

            val cfg = CFGFactory(testClass.findMethod("conditionalTwoReturns").get)
            val bbs = cfg.allBBs

            bbs.size should be(6)
            cfg.startBlock.successors.size should be(2)
            cfg.normalReturnNode.predecessors.size should be(3)
            cfg.abnormalReturnNode.predecessors.size should be(1)
        }
    }
}
