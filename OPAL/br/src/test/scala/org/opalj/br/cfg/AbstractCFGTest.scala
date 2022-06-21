/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br
package cfg

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

import org.opalj.io.writeAndOpen
import org.opalj.br.instructions.Instruction

/**
 * Helper methods to test the CFG related methods.
 *
 * @author Michael Eichberg
 */
abstract class AbstractCFGTest extends AnyFunSpec with Matchers with BeforeAndAfterAll {

    private[this] val oldCFGValidateSetting = CFG.Validate

    override def beforeAll(): Unit = {
        CFG.updateValidate(true)
    }

    override def afterAll(): Unit = {
        CFG.updateValidate(oldCFGValidateSetting)
    }

    /**
     * Tests the correspondence of the information made available using a CFG
     * and `Code.cfPCs`.
     */
    def cfgNodesCheck(
        m:    Method,
        code: Code,
        cfg:  CFG[Instruction, Code]
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Unit = {
        // validate that cfPCs returns the same information as the CFG
        val (cfJoins, cfForks, forkTargetPCs) = code.cfPCs
        val (allPredecessorPCs, exitPCs, cfJoinsAlt) = code.predecessorPCs

        assert(cfJoins == cfJoinsAlt)

        exitPCs foreach { pc =>
            assert(cfg.bb(pc).successors.forall(_.isExitNode))
        }

        cfJoins foreach { pc =>
            assert(
                cfg.bb(pc).startPC == pc,
                m.toJava(s"; the join PC $pc is not at the beginning of a BasicBlock node")
            )
        }
        cfForks foreach { pc =>
            assert(
                cfg.bb(pc).endPC == pc,
                m.toJava(s"; the fork PC $pc is not at the end of a BasicBlock node")
            )
            assert(
                forkTargetPCs(pc).nonEmpty
            )
        }

        cfg.allBBs foreach { bb =>
            if (bb.startPC != 0 || bb.predecessors.nonEmpty) {
                if (bb.predecessors.size > 1) {
                    assert(
                        cfJoins.contains(bb.startPC),
                        m.toJava(s"; a basic block's start PC (${bb.startPC} predecessors ${bb.predecessors}) is not a join PC")
                    )
                    allPredecessorPCs(bb.startPC).hasMultipleElements
                }
            }

            if (bb.successors.count(!_.isExitNode) > 1) {
                assert(
                    cfForks.contains(bb.endPC),
                    m.toJava(s"; a basic block's end PC(${bb.endPC}}) is not a fork PC")
                )
            }

        }

        assert((code.cfJoins -- cfJoins).isEmpty)
    }

    /** If the execution of `f` results in an exception the CFG is printed. */
    def printCFGOnFailure(
        method: Method,
        code:   Code,
        cfg:    CFG[Instruction, Code]
    )(
        f: => Unit
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Unit = {
        try {
            cfgNodesCheck(method, code, cfg)
            f
        } catch {
            case t: Throwable =>
                writeAndOpen(cfg.toDot, method.name+"-CFG", ".gv")
                throw t
        }
    }

}
