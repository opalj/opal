/* BSD 2-Clause License:
 * Copyright (c) 2009 - 201&
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
package br
package cfg

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

import org.opalj.concurrent.ConcurrentExceptions
import org.opalj.util.PerformanceEvaluation._
import org.opalj.util.Nanoseconds
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project

/**
 * Just reads a lot of class files and computes CFGs related information for all methods
 * with a body. Basically, tests if no exceptions occur and that the different methods
 * return comparable results.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
class CFGsSmokeTest extends AbstractCFGTest {

    def doAnalyzeProject(project: SomeProject): Unit = {

        implicit val classHierarchy: ClassHierarchy = project.classHierarchy

        val methodsWithBodyCount = project.allMethodsWithBody.size
        val methodsCount = new AtomicInteger(0)
        val executionTime = new AtomicLong(0L)

        project.parForeachMethodWithBody() { mi =>
            val method = mi.method
            implicit val code: Code = method.body.get

            val cfg = time { CFGFactory(code) } { t => executionTime.addAndGet(t.timeSpan) }

            // check that each instruction is associated with a basic block
            code.programCounters foreach { pc =>
                if (cfg.bb(pc) == null) {
                    fail(method.toJava(s"instruction $pc is not associated with a basic block"))
                }
            }

            // check that allBBs returns the same bbs as a manual iterator
            val manuallyCollectsBBs = code.programCounters.map[BasicBlock](cfg.bb).toSet
            assert(cfg.allBBs.toSet == manuallyCollectsBBs)

            // check the boundaries
            var allStartPCs = Set.empty[Int]
            var allEndPCs = Set.empty[Int]
            cfg.allBBs.foreach { bb =>
                if (bb.startPC > bb.endPC)
                    fail(s"the startPC ${bb.startPC} is larger than the endPC ${bb.endPC}")
                if (allStartPCs.contains(bb.startPC))
                    fail(
                        s"the startPC ${bb.startPC} is used by multiple basic blocks "+
                            s" (${cfg.allBBs.mkString(", ")}"
                    )
                else
                    allStartPCs += bb.startPC

                if (allEndPCs.contains(bb.endPC))
                    fail(s"the endPC ${bb.endPC} is used by multiple basic blocks")
                else
                    allEndPCs += bb.endPC
            }
            cfgNodesCheck(method, code, cfg)

            // check the wiring
            cfg.allBBs.foreach { bb =>
                bb.successors.foreach { successorBB =>
                    if (!successorBB.predecessors.contains(bb))
                        fail(s"the bb $successorBB does not reference its predecessor $bb")
                }

                bb.predecessors.foreach { predecessorBB =>
                    if (!predecessorBB.successors.contains(bb))
                        fail(s"the bb $predecessorBB does not reference its successor $bb")
                }
            }
            cfg.reachableBBs should not be (empty)

            // check the correspondence of "instruction.nextInstruction" and the information
            // contained in the cfg
            code.iterate { (pc, instruction) =>
                val nextInstructions = IntTrieSet(instruction.nextInstructions(pc))
                if (nextInstructions.isEmpty) {
                    val successors = cfg.bb(pc).successors
                    if (successors.exists(succBB => succBB.isBasicBlock))
                        fail(
                            s"the successor nodes of a return instruction $pc:($instruction)"+
                                s"have to be catch|exit nodes; found: $successors"
                        )
                } else {
                    val cfgSuccessors = cfg.successors(pc)
                    if (nextInstructions != cfgSuccessors) {
                        fail(s"the instruction ($instruction) with pc $pc has the following "+
                            s"instruction successors:\n\t$nextInstructions and\n"+
                            s"the following cfg successors:\n\t$cfgSuccessors\n"+
                            s"the nodes are:\n\t${cfg.bb(pc)} =>\n\t\t"+
                            cfg.bb(pc).successors.mkString("\n\t\t"))
                    }
                }
            }

            // check that cfg.successors and cfg.foreachSuccessor return the same sets
            code iterate { (pc, instruction) =>
                {
                    val cfgSuccessors = cfg.successors(pc)
                    var cfgForeachSuccessors = IntTrieSet.empty
                    var cfgForeachSuccessorCount = 0
                    cfg.foreachSuccessor(pc) { cfgForeachSuccessor =>
                        cfgForeachSuccessors += cfgForeachSuccessor
                        cfgForeachSuccessorCount += 1
                    }
                    assert(cfgSuccessors == cfgForeachSuccessors)
                    assert(cfgSuccessors.size == cfgForeachSuccessorCount)
                }

                {
                    val cfgPredecessors = cfg.predecessors(pc)
                    var cfgForeachPredecessors = IntTrieSet.empty
                    var cfgForeachPredecessorCount = 0
                    cfg.foreachPredecessor(pc) { cfgForeachPredecessor =>
                        cfgForeachPredecessors += cfgForeachPredecessor
                        cfgForeachPredecessorCount += 1
                    }
                    assert(cfgPredecessors == cfgForeachPredecessors)
                    assert(cfgPredecessors.size == cfgForeachPredecessorCount)
                }
            }

            methodsCount.incrementAndGet()
        }
        info(
            s"analyzed ${methodsCount.get}/$methodsWithBodyCount methods "+
                s"in ∑ ${Nanoseconds(executionTime.get).toSeconds}"
        )
    }

    def analyzeProject(project: SomeProject): Unit = {
        time {
            try { doAnalyzeProject(project) } catch {
                case ce: ConcurrentExceptions => ce.printStackTrace(Console.err)
            }
        } { t => info("the analysis took "+t.toSeconds) }
    }

    //
    // Configuration of the tested projects ...
    //

    describe("using a method's code") {

        it(s"it should be possible to compute the CFG for all methods of the JDK ($JRELibraryFolder)") {
            analyzeProject(TestSupport.createJREProject())
        }

        allBITestJARs() foreach { jarFile =>
            it(s"it should be possible to compute the CFG for all methods of ${jarFile.getName}") {
                analyzeProject(Project(jarFile))
            }
        }
    }
}
