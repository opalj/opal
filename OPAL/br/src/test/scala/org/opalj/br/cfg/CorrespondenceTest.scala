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
package org.opalj.br.cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.opalj.UShort
import org.opalj.collection.mutable.UShortSet

/**
 * @author Erich Wittenbeck
 *
 * We construct CFGs for a variety of methods and analyze the correspondences
 * within them.
 *
 * We then check the correspondence-sets for specific program counters and if they
 * contain the correct elements.
 */
@RunWith(classOf[JUnitRunner])
class CorrespondenceTest extends FunSpec with Matchers {

    /*
     * A utility method used when testing UShortSets,
     * which contain more than 2 elements.
     */
    def UShortSetToSet(uset: UShortSet): Set[UShort] = {
        var result: Set[UShort] = Set.empty

        uset.foreach { ushort ⇒ result = result + (ushort) }

        result
    }

    val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)

    val testClass = testProject.classFile(ObjectType("controlflow/FinallyCode")).get

    val altJAR = "classfiles/various.jar"
    val altFolder = TestSupport.locateTestResources(altJAR, "br")
    val altProject = Project(altFolder)

    describe("Testing self-made methods that exemplify important (edge) cases") {

        it("with only an if-clause in the finally-handler") {

            val testCFG = ControlFlowGraph(testClass.findMethod("tryFinally").get)

            def pcs = testCFG.correspondingPCsTo

            pcs(44) should be(UShortSet(7))

            pcs(42) should be(UShortSet.empty)

            pcs(78) should be(UShortSet.empty)

            pcs(76) should be(UShortSet(37))
        }

        it("Classic use of resources with try-finally, pre Java SE 7") {

            val method = testClass.findMethod("tryWithResourcesOldSchool").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(29) should be(UShortSet(41))

            pcs(41) should be(UShortSet(29))

        }

        it("a simple try-finally-construct with a loop in the finally-statement") {

            val method = testClass.findMethod("loopInFinally").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(85) should be(UShortSet(26, 57))

            pcs(12) should be(UShortSet(43, 71))

            pcs(29) should be(UShortSet.empty)

            pcs(90) should be(UShortSet.empty)

            pcs(23) should be(UShortSet(54, 82))
        }

        it("With the finally-duplicate dominated by a catch-block") {

            val method = testClass.findMethod("duplicateInCatchblock").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(18) should be(UShortSet(7, 25))

            pcs(7) should be(UShortSet(18, 25))

            pcs(25) should be(UShortSet(7, 18))
        }

        it("Superceeding a return in try-block with return in finally-block") {

            val method = testClass.findMethod("tryFinallyBranch").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(12) should be(UShortSet(16, 22))

            pcs(16) should be(UShortSet(12, 22))

            pcs(22) should be(UShortSet(12, 16))
        }

        it("Testing try-with-resources") {
            val method = testClass.findMethod("tryWithResources").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(99) should be(UShortSet.empty)

            pcs(87) should be(UShortSet(44))

            pcs(44) should be(UShortSet(87))

            pcs(85) should be(UShortSet(42))

            pcs(42) should be(UShortSet(85))

            pcs(57) should be(UShortSet.empty)
        }

        it("a try-finally-structure nested into a finally-statement") {
            val method = testClass.findMethod("nestedFinally").get

            val testCFG = ControlFlowGraph(method)

            def pcs = testCFG.correspondingPCsTo

            pcs(6) should be(UShortSet(35))

            pcs(35) should be(UShortSet(6))

            pcs(11).size should be(3)

            val testSet = UShortSetToSet(pcs(11))

            testSet should contain(21)

            testSet should contain(40)

            testSet should contain(50)
        }

        it("a method with four-times nested try-catch-finally-structure") {

            val testCFG = ControlFlowGraph(testClass.findMethod("highlyNestedFinally").get)

            def pcs = testCFG.correspondingPCsTo

            pcs(41).size should be(8)

            val testSet = UShortSetToSet(pcs(41))

            testSet should contain(121)

            testSet should contain(117)

            testSet should contain(110)

            testSet should contain(103)

            testSet should contain(93)

            testSet should contain(83)

            testSet should contain(70)

            testSet should contain(57)

        }

    }

    describe("Testing with classes from the JDK, which exemplified bugs that were not detected with the above test-cases") {

        val altClass1 = altProject.classFile(ObjectType("com/jrockit/mc/common/xml/XmlToolkit")).get

        it("a trivial try-finally-construct") {

            val xmlCFG = ControlFlowGraph(altClass1.findMethod("loadDocumentFromFile").get)

            def pcs = xmlCFG.correspondingPCsTo()

            pcs(24) should be(UShortSet(35))
        }

        it("a finally-handlers endPC and handlerPC being equal") {

            val xmlCFG = ControlFlowGraph(altClass1.findMethod("storeDocumentToFile").get)

            def pcs = xmlCFG.correspondingPCsTo()

            pcs(39) should be(UShortSet(29))
        }

        val altClass2 = altProject.classFile(ObjectType("org/apache/jasper/servlet/JspCServletContext")).get

        it("ensuring that the correspondences are transitive") {

            val altCFG = ControlFlowGraph(altClass2.findMethod("getResource").get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(66) should be(UShortSet(86, 104))
        }

        val altClass3 = altProject.classFile(ObjectType("com/jrockit/mc/ui/fields/FieldToolkit")).get

        it("a finally-code-duplicate being dominated by a catch-block") {

            var altCFG: ControlFlowGraph = null

            // There is another method called 'initializeFields' without exception-handling
            for {
                method ← altClass3.methods
                if (method.name == "initializeFields"
                    && method.body.get.exceptionHandlers.nonEmpty)
            } { altCFG = ControlFlowGraph(method) }

            val pcs = altCFG.correspondingPCsTo()

            pcs(161) should be(UShortSet(179, 141))
        }

        val altClass4 = altProject.classFile(ObjectType("org/apache/lucene/index/IndexWriter")).get

        it("a method with finally-duplicates which are only reachable from conditional jumps within a finally-handlers handled range") {

            val altCFG = ControlFlowGraph(altClass4.findMethod("rollbackInternal").get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(312) should be(UShortSet(194, 256))
        }

        val altClass5 = altProject.classFile(ObjectType("com/sun/el/parser/ELParser")).get

        it("a method which caused our correspondence-analysis to associate program counters with themselves.") {

            val altCFG = ControlFlowGraph(altClass5.findMethod("Compare").get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(938) should be(UShortSet.empty)
        }

        val altClass6 = altProject.classFile(ObjectType("org/apache/tools/ant/taskdefs/optional/junit/JUnitTask")).get

        ignore("a method that was compiled using JSR/Ret. Building the CFG will, thus, fail.") {

            val method = altClass6.findMethod("logVmExit")

            println(method.nonEmpty)

            val altCFG = ControlFlowGraph(method.get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(0) should be(UShortSet.empty)

        }

        /*
     * The following tests will fail, due to a known, but yet to be fixed bug.
     * 
     * We assumed, that for every finally-handler, all conditional jumps from inside
     * the handler's range to outside said range, will be another duplicate of the
     * finally-code.
     * 
     * This is NOT always the case.
     * 
     * I propose some sort of pre-processing, where supposed duplicates, that were
     * identified using above strategy, are first to be checked for actually having
     * the same logical structure as a 'proper' duplicate. Only then we will also
     * consider them for building the correspondence-mapping.
     */

        val altClass7 = altProject.classFile(ObjectType("org/eclipse/jdt/internal/compiler/parser/AbstractCommentParser")).get

        ignore("will fail due to a known bug") {

            val method = altClass7.findMethod("parseParam")

            val altCFG = ControlFlowGraph(method.get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(952) should be(UShortSet.empty)

        }

        val altClass8 = altProject.classFile(ObjectType("com/oracle/tools/packager/linux/LinuxRpmBundler")).get

        ignore("Will fail due to known bug") {

            val method = altClass8.findMethod("testTool")

            val altCFG = ControlFlowGraph(method.get)

            def pcs = altCFG.correspondingPCsTo()

            pcs(339).size should be(3)
        }

    }

}