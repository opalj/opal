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
package org.opalj.br
package cfg

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.opalj.br.TestSupport
import scala.collection.JavaConverters._
import org.opalj.util.PerformanceEvaluation._
import org.opalj.util.Nanoseconds

/**
 * Just reads a lot of classfiles and builds CFGs for all methods with a body to
 * test if no exceptions occur.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BuildCFGsForJRE extends FunSpec with Matchers {

    describe("computing the cfg") {

        it("should be possible for all methods belonging to the current JDK") {
            val project = TestSupport.createJREProject

            val methodsCount = new java.util.concurrent.atomic.AtomicInteger(0)
            val errors = new java.util.concurrent.ConcurrentLinkedQueue[String]
            val executionTime = new java.util.concurrent.atomic.AtomicLong(0l)
            project.parForeachMethodWithBody(() ⇒ false) { m ⇒
                val (_, classFile, method) = m
                val code = method.body.get
                try {
                    val cfg = time {
                        CFGFactory(method)
                    } { t ⇒ executionTime.addAndGet(t.timeSpan) }

                    // check that each instruction is associated with a basic block
                    if (!code.programCounters.forall { cfg.bb(_) ne null }) {
                        fail("not all instructions are associated with a basic block")
                    }

                    // check the boundaries
                    var allStartPCs = Set.empty[Int]
                    var allEndPCs = Set.empty[Int]
                    val allBBs = cfg.allBBs
                    allBBs.foreach { bb ⇒
                        if (bb.startPC > bb.endPC)
                            fail(s"the startPC ${bb.startPC} is larger than the endPC ${bb.endPC}")
                        if (allStartPCs.contains(bb.startPC))
                            fail(s"the startPC ${bb.startPC} is used by multiple basic blocks (${allBBs.mkString(", ")}")
                        else
                            allStartPCs += bb.startPC

                        if (allEndPCs.contains(bb.endPC))
                            fail(s"the endPC ${bb.endPC} is used by multiple basic blocks")
                        else
                            allEndPCs += bb.endPC
                    }
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
                    methodsCount.incrementAndGet()
                } catch {
                    case t: Throwable ⇒
                        errors.add(method.toJava(classFile)+":"+t.getMessage)
                }
            }
            if (!errors.isEmpty())
                fail(s"analyzed ${methodsCount.get}/${project.methodsCount} methods; "+
                    s"failed for ${errors.size} methods: ${errors.asScala.mkString("\n")}")
            else
                info(s"analyzed ${methodsCount.get}(/${project.methodsCount}) methods in ${Nanoseconds(executionTime.get).toSeconds}")
        }
    }
}
