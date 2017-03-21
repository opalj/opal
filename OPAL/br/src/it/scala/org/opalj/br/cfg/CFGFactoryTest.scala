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
package org.opalj.br
package cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import java.io.File
import java.io.FilenameFilter

import scala.collection.JavaConverters._

import org.opalj.util.PerformanceEvaluation._
import org.opalj.util.Nanoseconds

import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.MethodInfo

import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithLambdaExpressionsSupportAndCaching

/**
 * Just reads a lot of classfiles and builds CFGs for all methods with a body to
 * test if no exceptions occur and that the different methods return comparable results.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class CFGFactoryTest extends CFGTests {

    def analyzeProject(name: String, project: SomeProject): Unit = {
        time { doAnalyzeProject(name, project) } { t ⇒ info("the analysis took "+t.toSeconds) }
    }

    def doAnalyzeProject(name: String, project: SomeProject): Unit = {
        val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val errors = new java.util.concurrent.ConcurrentLinkedQueue[String]
        val executionTime = new java.util.concurrent.atomic.AtomicLong(0l)

        implicit val classHierarchy = project.classHierarchy

        project.parForeachMethodWithBody() { m ⇒
            val MethodInfo(_, classFile, method) = m
            implicit val code = method.body.get
            try {
                val cfg = time { CFGFactory(code) } { t ⇒ executionTime.addAndGet(t.timeSpan) }

                // check that each instruction is associated with a basic block
                if (!code.programCounters.forall { cfg.bb(_) ne null }) {
                    fail("not all instructions are associated with a basic block")
                }

                // check that allBBs returns the same bbs as a manual iterator
                assert(cfg.allBBs.toSet == code.programCounters.map(cfg.bb(_)).filter(_ ne null).toSet)

                // check the boundaries
                var allStartPCs = Set.empty[Int]
                var allEndPCs = Set.empty[Int]
                cfg.allBBs.foreach { bb ⇒
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
                cfgNodesCheck(method.toJava(classFile), code, cfg, classHierarchy)

                // check the wiring
                cfg.allBBs.foreach { bb ⇒
                    bb.successors.foreach { successorBB ⇒
                        if (!successorBB.predecessors.contains(bb))
                            fail(s"the successor $successorBB does not reference its predecessor $bb")
                    }

                    bb.predecessors.foreach { predecessorBB ⇒
                        if (!predecessorBB.successors.contains(bb))
                            fail(s"the predecessor $predecessorBB does not reference its successor $bb")
                    }
                }
                cfg.reachableBBs should not be (empty)

                // check the correspondence of "instruction.nextInstruction" and the information
                // contained in the cfg
                code.iterate { (pc, instruction) ⇒
                    val nextInstructions = instruction.nextInstructions(pc).toSet
                    if (nextInstructions.isEmpty) {
                        if (!cfg.bb(pc).successors.forall { succBB ⇒ succBB.isCatchNode || succBB.isExitNode })
                            fail(
                                s"the successor nodes of a return instruction $pc:($instruction)"+
                                    s"have to be catch nodes, but we found: ${cfg.bb(pc).successors}"
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
                code.iterate { (pc, instruction) ⇒
                    {
                        val cfgSuccessors = cfg.successors(pc)
                        var cfgForeachSuccessors = Set.empty[PC]
                        var cfgForeachSuccessorCount = 0
                        cfg.foreachSuccessor(pc) { cfgForeachSuccessor ⇒
                            cfgForeachSuccessors += cfgForeachSuccessor
                            cfgForeachSuccessorCount += 1
                        }
                        assert(cfgSuccessors == cfgForeachSuccessors)
                        assert(cfgSuccessors.size == cfgForeachSuccessorCount)
                    }

                    {
                        val cfgPredecessors = cfg.predecessors(pc)
                        var cfgForeachPredecessors = Set.empty[PC]
                        var cfgForeachPredecessorCount = 0
                        cfg.foreachPredecessor(pc) { cfgForeachPredecessor ⇒
                            cfgForeachPredecessors += cfgForeachPredecessor
                            cfgForeachPredecessorCount += 1
                        }
                        assert(cfgPredecessors == cfgForeachPredecessors)
                        assert(cfgPredecessors.size == cfgForeachPredecessorCount)
                    }
                }

                methodsCount.incrementAndGet()
            } catch {
                case t: Throwable ⇒
                    val instructions = code.instructions.size
                    val message = s"instructions=$instructions; "+t.getMessage
                    errors.add(method.toJava(classFile, message))
            }
        }
        if (!errors.isEmpty())
            fail(
                s"analyzed ${methodsCount.get}/${project.methodsCount} methods; "+
                    errors.asScala.toList.sorted.mkString(s"failed for ${errors.size} methods\n", "\n", "")
            )
        else
            info(
                s"analyzed ${methodsCount.get}(/${project.methodsCount}) methods "+
                    s"in ∑ ${Nanoseconds(executionTime.get).toSeconds}"
            )
    }

    //
    // Configuration of the tested projects
    //

    describe("computing the cfg") {

        val cache = new BytecodeInstructionsCache
        val reader = new Java9FrameworkWithLambdaExpressionsSupportAndCaching(cache)
        import reader.AllClassFiles

        it(s"should be possible for all methods of the JDK ($JRELibraryFolder)") {
            val project = TestSupport.createJREProject
            analyzeProject("JDK", project)
        }

        val classFilesFolder = locateTestResources("classfiles", "bi")
        val filter = new FilenameFilter() {
            def accept(dir: File, name: String) = { name.endsWith(".jar") }
        }
        val jars = classFilesFolder.listFiles(filter)
        jars.foreach { jar ⇒
            it(s"should be possible for all methods of $jar") {
                val project = Project(AllClassFiles(Seq(jar)), Traversable.empty, true)
                analyzeProject(jar.getName, project)
            }
        }
    }
}
