/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.br
package cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import java.net.URL

import org.opalj.br.TestSupport.biProject
import org.opalj.br.analyses.Project

/**
 * Computes the CFGs for various methods and checks their block structure. For example:
 *  - Does each block have the correct amount of predecessors and successors?
 *  - Does it have the correct amount of catchBlock-successors?
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class CFGTest extends AbstractCFGTest {

    describe("Properties of CFGs") {

        val testProject: Project[URL] = biProject("controlflow.jar")
        val testClassFile = testProject.classFile(ObjectType("controlflow/BoringCode")).get

        implicit val testClassHierarchy = testProject.classHierarchy

        it("the cfg of a method with no control flow statements should have one BasicBlock node") {
            val m = testClassFile.findMethod("singleBlock").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            printCFGOnFailure(m, code, cfg) {
                cfg.allBBs.size should be(1)
                cfg.startBlock.successors.size should be(2)
                cfg.normalReturnNode.predecessors.size should be(1)
                cfg.abnormalReturnNode.predecessors.size should be(1)
            }
        }

        it("the cfg of a method with one `if` should have basic blocks for both branches") {
            val m = testClassFile.findMethod("conditionalOneReturn").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            printCFGOnFailure(m, code, cfg) {
                cfg.allBBs.size should be(11)
                cfg.startBlock.successors.size should be(2)
                cfg.normalReturnNode.predecessors.size should be(1)
                cfg.abnormalReturnNode.predecessors.size should be(2)
            }
        }

        it("a cfg with multiple return statements should have corresponding basic blocks") {
            val m = testClassFile.findMethod("conditionalTwoReturns").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            printCFGOnFailure(m, code, cfg) {
                cfg.allBBs.size should be(6)
                cfg.startBlock.successors.size should be(2)
                cfg.normalReturnNode.predecessors.size should be(3)
                cfg.abnormalReturnNode.predecessors.size should be(4)
            }
        }
    }
}
