/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br
package cfg

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

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
