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

import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.io.writeAndOpen
import org.opalj.br.instructions.Instruction

/**
 * Helper methods to test the constructed CFGs.
 *
 * @author Michael Eichberg
 */
trait CFGTests extends FunSpec with Matchers {

    def cfgNodesCheck(
        m:              Method,
        code:           Code,
        cfg:            CFG[Instruction, Code],
        classHierarchy: ClassHierarchy
    ): Unit = {
        // validate that cfPCs returns the same information as the CFG
        val (cfJoins, cfForks, forkTargetPCs) = code.cfPCs(classHierarchy)
        val (allPredecessorPCs, exitPCs, cfJoinsAlt) = code.predecessorPCs(classHierarchy)

        assert(cfJoins == cfJoinsAlt)

        exitPCs foreach { pc ⇒
            assert(cfg.bb(pc).successors.forall(_.isExitNode))
        }

        cfJoins foreach { pc ⇒
            assert(
                cfg.bb(pc).startPC == pc,
                m.toJava(s"; the join PC $pc is not at the beginning of a BasicBlock node")
            )
        }
        cfForks foreach { pc ⇒
            assert(
                cfg.bb(pc).endPC == pc,
                m.toJava(s"; the fork PC $pc is not at the end of a BasicBlock node")
            )
            assert(
                forkTargetPCs(pc).nonEmpty
            )
        }

        cfg.allBBs foreach { bb ⇒
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

    def testCFGProperties(
        method:         Method,
        code:           Code,
        cfg:            CFG[Instruction, Code],
        classHierarchy: ClassHierarchy
    )(
        f: ⇒ Unit
    ): Unit = {
        try {
            cfgNodesCheck(method, code, cfg, classHierarchy)
            f
        } catch {
            case t: Throwable ⇒ writeAndOpen(cfg.toDot, method.name+"-CFG", ".gv"); throw t
        }
    }

}
