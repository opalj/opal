/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.cfg

import java.net.URL

import org.junit.runner.RunWith
import org.opalj.br.analyses.Project
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.TestSupport.biProject
import org.opalj.br.instructions.IF_ICMPNE
import org.opalj.br.instructions.IFNE
import org.opalj.br.Code
import org.opalj.br.instructions.IFEQ
import org.opalj.br.instructions.ILOAD
import org.scalatest.junit.JUnitRunner

/**
 * Computes the dominator tree of CFGs of a couple of methods and checks their sanity.
 *
 * @author Patrick Mell
 */
@RunWith(classOf[JUnitRunner])
class DominatorTreeTest extends AbstractCFGTest {

    /**
     * Takes an `index` and finds the next not-null instruction within code after `index`.
     * The index of that instruction is then returned. In case no instruction could be found, the
     * value of `index` is returned.
     */
    private def getNextNonNullInstr(index: Int, code: Code): Int = {
        var foundIndex = index
        var found = false
        for (i ← (index + 1).to(code.instructions.length)) {
            if (!found && code.instructions(i) != null) {
                foundIndex = i
                found = true
            }
        }
        foundIndex
    }

    describe("Sanity of dominator trees of control flow graphs") {

        val testProject: Project[URL] = biProject("controlflow.jar")
        val boringTestClassFile = testProject.classFile(ObjectType("controlflow/BoringCode")).get

        implicit val testClassHierarchy: ClassHierarchy = testProject.classHierarchy

        it("the dominator tree of a CFG with no control flow should be a tree where each "+
            "instruction is strictly dominator by its previous instruction (except for the root)") {
            val m = boringTestClassFile.findMethod("singleBlock").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            val domTree = cfg.dominatorTree

            printCFGOnFailure(m, code, cfg, Some(domTree)) {
                domTree.immediateDominators.zipWithIndex.foreach {
                    case (idom, index) ⇒
                        if (index == 0) {
                            idom should be(0)
                        } else {
                            idom should be(index - 1)
                        }
                }
            }
        }

        it("in a dominator tree of a CFG with control instructions, the first instruction within "+
            "that control structure should be dominated by the controlling instruction (like "+
            "an if)") {
            val m = boringTestClassFile.findMethod("conditionalTwoReturns").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            val domTree = cfg.dominatorTree

            printCFGOnFailure(m, code, cfg, Some(domTree)) {
                var index = 0
                code.foreachInstruction { next ⇒
                    next match {
                        case _: IFNE | _: IF_ICMPNE ⇒
                            val next = getNextNonNullInstr(index, code)
                            domTree.immediateDominators(next) should be(index)
                        case _ ⇒
                    }
                    index += 1
                }
            }
        }

        it("in a dominator tree of a CFG with an if-else right before the return, the return "+
            "should be dominated by the if check of the if-else") {
            val m = boringTestClassFile.findMethod("conditionalOneReturn").head
            val code = m.body.get
            val cfg = CFGFactory(code)
            val domTree = cfg.dominatorTree

            printCFGOnFailure(m, code, cfg, Some(domTree)) {
                val loadOfReturnOption = code.instructions.reverse.find(_.isInstanceOf[ILOAD])
                loadOfReturnOption should not be loadOfReturnOption.isEmpty

                val loadOfReturn = loadOfReturnOption.get
                val indexOfLoadOfReturn = code.instructions.indexOf(loadOfReturn)
                val ifOfLoadOfReturn = code.instructions.reverse.zipWithIndex.find {
                    case (instr, i) ⇒
                        i < indexOfLoadOfReturn && instr.isInstanceOf[IFEQ]
                }
                ifOfLoadOfReturn should not be ifOfLoadOfReturn.isEmpty

                val indexOfIf = code.instructions.indexOf(ifOfLoadOfReturn.get._1)
                domTree.immediateDominators(indexOfLoadOfReturn) should be(indexOfIf)
            }
        }

    }

}
