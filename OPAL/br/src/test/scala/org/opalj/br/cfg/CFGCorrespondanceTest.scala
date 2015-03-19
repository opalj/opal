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
import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.opalj.collection.mutable.UShortSet

import scala.collection.immutable.HashSet

/**
 *
 * @author Erich Wittenbeck
 */
@RunWith(classOf[JUnitRunner])
class CFGCorrespondanceTest extends FunSpec with Matchers {

    val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)

    val testClass = testProject.classFile(ObjectType("controlflow/ExceptionCode")).get

    describe("Testing Correspondances on the PC-Level") {

        ignore("with only an if-clause in the finally-handler") {

            val testCFG = ControlFlowGraph(testClass.findMethod("tryFinally").get)

            var pcs = testCFG.correspondingPCsTo(44)

            pcs should be(UShortSet(7))

            pcs = testCFG.correspondingPCsTo(42)

            pcs should be(UShortSet.empty)

            pcs = testCFG.correspondingPCsTo(78)

            pcs should be(UShortSet.empty)

            pcs = testCFG.correspondingPCsTo(76)

            pcs should be(UShortSet(37))
        }

        ignore("also with loops") {

            val method = testClass.findMethod("loopExceptionWithCatchReturn").get
            // just to make sure the code has the expected shape..
            assert(method.body.get.instructions(85) ne null)

            val testCFG = ControlFlowGraph(method)

            var pcs = testCFG.correspondingPCsTo(85)

            pcs should be(UShortSet(26))

            pcs = testCFG.correspondingPCsTo(85)

            pcs should be(UShortSet(26))

            pcs = testCFG.correspondingPCsTo(12)

            pcs should be(UShortSet(71))

            pcs = testCFG.correspondingPCsTo(29)

            pcs should be(UShortSet.empty)

            pcs = testCFG.correspondingPCsTo(90)

            pcs should be(UShortSet.empty)

            pcs = testCFG.correspondingPCsTo(23)

            pcs should be(UShortSet(82))
        }

    }
}
