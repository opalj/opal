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
//import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
//import org.opalj.bi.reader.ClassFileReader
import org.opalj.br.ObjectType

/**
 *
 * @author Erich Wittenbeck
 */

/**
 * The same as BasicCFGJava8Test, but for class-files compiled with Eclipse Luna's
 * internal compiler
 */
@RunWith(classOf[JUnitRunner])
class BasicCFGLunaTest extends FunSpec with Matchers {

    val testJAR = "classfiles/cfgtestLuna.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)

    describe("Building Control Flow Graphs for some trivial code") {

        val testClass = testProject.classFile(ObjectType("controlflow/BoringCode")).get

        it("should create a valid CFG for a most simple method without any controll flow in it") {

            val singleBlockCFG = ControlFlowGraph(testClass.findMethod("singleBlock").get)
            val BlockList = singleBlockCFG.allBlocks

            BlockList.size should be(3)
            singleBlockCFG.startBlock.successors.size should be(1)
            singleBlockCFG.endBlock.predecessors.size should be(1)

        }

        it("Testing a method with a simple branch statement and one return statement") {

            val conditionalOneReturnCFG = ControlFlowGraph(testClass.findMethod("conditionalOneReturn").get)
            val BlockList = conditionalOneReturnCFG.allBlocks

            BlockList.size should be(12)
            conditionalOneReturnCFG.startBlock.successors.size should be(1)
            conditionalOneReturnCFG.endBlock.predecessors.size should be(1)
        }

        it("Testing a method with a simple branch statement and two return statements") {

            val conditionalTwoReturnsCFG = ControlFlowGraph(testClass.findMethod("conditionalTwoReturns").get)
            val BlockList = conditionalTwoReturnsCFG.allBlocks

            BlockList.size should be(7)
            conditionalTwoReturnsCFG.startBlock.successors.size should be(1)
            conditionalTwoReturnsCFG.endBlock.predecessors.size should be(3)
        }
    }

    describe("Building Control Flow Graphs for some methods with loops") {

        val testClass = testProject.classFile(ObjectType("controlflow/LoopCode")).get

        it("Testing a simple Loop") {

            val simpleLoopCFG = ControlFlowGraph(testClass.findMethod("simpleLoop").get)
            val BlockList = simpleLoopCFG.allBlocks

            BlockList.size should be(6)

            for (block ← BlockList)
                block match {
                    case bb: BasicBlock ⇒ {
                        if (bb.startPC == 9) {
                            bb.successors(0) should be(bb.predecessors(0))
                        }
                    }
                    case _ ⇒ {}
                }
        }

        it("Testing a method with two nested loops") {

            val nestedLoopCFG = ControlFlowGraph(testClass.findMethod("nestedLoop").get)
            val BlockList = nestedLoopCFG.allBlocks

            BlockList.size should be(9)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 32 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.successors.size should be(2)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }
                            case 38 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new ExitBlock())
                            }
                            case 14 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(bb.predecessors(0))
                            }
                            case 8 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new BasicBlock(25))
                            }
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new BasicBlock(32))
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case _ ⇒ {}
                }
            }
        }

        it("Testing a loop with a branch statement") {

            val loopWithBranchCFG = ControlFlowGraph(testClass.findMethod("loopWithBranch").get)
            val BlockList = loopWithBranchCFG.allBlocks

            BlockList.size should be(9)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 | 16 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(2)
                            }
                            case 9 | 39 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new ExitBlock())
                            }
                            case 30 | 22 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(16))
                                bb.successors(0) should be(new BasicBlock(11))
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(2)
                    }
                    case _ ⇒ {}
                }
            }
        }

        it("Testing a non-terminating loop") {

            val endlessLoopCFG = ControlFlowGraph(testClass.findMethod("endlessLoop").get)
            val BlockList = endlessLoopCFG.allBlocks

            BlockList.size should be(3)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new BasicBlock(8))
                            }
                            case 8 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new BasicBlock(8))
                            }
                            case _ ⇒ {}
                        }
                    }
                    case _ ⇒ {}
                }
            }
        }
    }

    describe("Methods with Switch-Statements") {

        val testClass = testProject.classFile(ObjectType("controlflow/SwitchCode")).get

        it("The degenerative case") {

            val degenerateSwitchCFG = ControlFlowGraph(testClass.findMethod("degenerateSwitch").get)
            val BlockList = degenerateSwitchCFG.allBlocks

            BlockList.size should be(5)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(2)
                            }
                            case 20 | 22 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(0))
                                bb.successors(0) should be(new ExitBlock())
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(2)
                    }
                    case _ ⇒ {}
                }
            }
        }

        it("Small distances between cases; No default case; No fall-through") {

            val simpleSwitchCFG = ControlFlowGraph(testClass.findMethod("simpleSwitchWithBreakNoDefault").get)
            val BlockList = simpleSwitchCFG.allBlocks

            BlockList.size should be(7)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(4)
                            }
                            case 28 | 34 | 41 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(0))
                                bb.successors(0) should be(new BasicBlock(45))
                            }
                            case 45 ⇒ {
                                bb.predecessors.size should be(4)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new ExitBlock())
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                        eb.predecessors(0) should be(new BasicBlock(45))
                    }
                    case _ ⇒ {}
                }
            }

        }

        it("Great distances between cases; With default case; With fall-through") {

            val disparateSwitchCFG = ControlFlowGraph(testClass.findMethod("disparateSwitchWithoutBreakWithDefault").get)
            val BlockList = disparateSwitchCFG.allBlocks

            BlockList.size should be(8)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(5)
                            }
                            case 44 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new BasicBlock(47))
                            }
                            case 47 ⇒ {
                                bb.successors(0) should be(new BasicBlock(51))

                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }
                            case 51 ⇒ {
                                bb.successors(0) should be(new BasicBlock(54))

                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }
                            case 54 ⇒ {
                                bb.successors(0) should be(new BasicBlock(56))

                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }
                            case 56 ⇒ {
                                bb.successors(0) should be(new ExitBlock)

                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }

                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case _ ⇒ {}
                }
            }
        }

        it("With and Without Fallthrough") {
            val fallthroughCFG = ControlFlowGraph(testClass.findMethod("withAndWithoutFallthrough").get)
            val BlockList = fallthroughCFG.allBlocks

            BlockList.size should be(10)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.successors.size should be(6)
                            }
                            case 36 | 54 | 48 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(0))
                            }
                            case 39 | 42 | 58 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.predecessors(1) should be(new BasicBlock(0))
                            }
                            case 60 ⇒ {
                                bb.predecessors.size should be(3)
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                        eb.predecessors(0) should be(new BasicBlock(60))
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }
        }
    }

    describe("Methods with Exception-Handling") {

        val testClass = testProject.classFile(ObjectType("controlflow/ExceptionCode")).get

        it("Code with a single try-catch-structure") {

            val simpleExceptionCFG = ControlFlowGraph(testClass.findMethod("simpleException").get)
            val BlockList = simpleExceptionCFG.allBlocks

            BlockList.size should be(7)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        cb.successors.size should be(1)
                        cb.predecessors(0) should be(new BasicBlock(0))
                        cb.successors(0) should be(new BasicBlock(29))
                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(new StartBlock())
                                bb.successors(0) should be(new BasicBlock(25))
                            }
                            case 25 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(0))
                                bb.successors(0) should be(new BasicBlock(33))
                            }
                            case 29 | 33 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new ExitBlock())
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(2)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(1)
        }

        it("Code with multiple try-catch-blocks and a finally-block") {

            val multipleCatchCFG = ControlFlowGraph(testClass.findMethod("multipleCatchAndFinally").get)
            multipleCatchCFG.toDot
            val BlockList = multipleCatchCFG.allBlocks

            BlockList.size should be(12)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                    }
                    case bb: BasicBlock ⇒ {
                        bb.successors.size should be(1)

                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.catchBlockSuccessors.size should be(2)
                            }
                            case 31 | 38 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                            case 27 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0) should be(new BasicBlock(26))
                            }
                            case 45 ⇒ {
                                bb.successors.size should be(1)
                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                            case 46 ⇒ {
                                bb.predecessors.size should be(4)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(new ExitBlock)
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case _ ⇒ {}
                }
            }

            numberOfCatchBlocks should be(3)

        }

        it("Code with nested Exception-Handling") {

            val nestedExceptionCFG = ControlFlowGraph(testClass.findMethod("nestedExceptions").get)
            val BlockList = nestedExceptionCFG.allBlocks

            BlockList.size should be(13)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        if (cb.handlerPC == 63)
                            cb.predecessors(0).predecessors(0).isInstanceOf[CatchBlock] should be(true)
                        if (cb.handlerPC == 45)
                            cb.successors(0).asInstanceOf[BasicBlock].catchBlockSuccessors(0).handlerPC should be(63)
                    }
                    case bb: BasicBlock ⇒ {
                        if (bb.startPC == 67)
                            bb.predecessors.size should be(2)
                        else
                            bb.predecessors.size should be(1)

                        bb.successors.size should be(1)

                        bb.catchBlockSuccessors.size <= 1 should be(true)
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(3)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(3)
        }

        it("Catch-Block with Loop, and Finally-Block with return statement") {

            val catchWithLoopCFG = ControlFlowGraph(testClass.findMethod("loopExceptionWithFinallyReturn").get)
            val BlockList = catchWithLoopCFG.allBlocks

            BlockList.size should be(14)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        if (cb.handlerPC == 37)
                            cb.predecessors.size should be(3)
                        else
                            cb.predecessors.size should be(1)
                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 5 | 17 | 34 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(2)
        }

        it("Finally-Block with Loop, Catch-Block with return statement") {

            val finallyWithLoopCFG = ControlFlowGraph(testClass.findMethod("loopExceptionWithCatchReturn").get)
            val BlockList = finallyWithLoopCFG.allBlocks

            BlockList.size should be(18)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        cb.successors.size should be(1)
                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.catchBlockSuccessors.size should be(2)
                                bb.successors.size should be(1)
                            }
                            case 63 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                            case 32 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(3)
                    }
                    case sb: StartBlock ⇒ {}
                }
            }

            numberOfCatchBlocks should be(2)
        }

        it("Three-times nested Try-Catch-Finally-Structure") {

            val highlyNestedCFG = ControlFlowGraph(testClass.findMethod("highlyNestedFinally").get)
            val BlockList = highlyNestedCFG.allBlocks

            BlockList.size should be(24)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1

                        cb.handlerPC match {
                            case 35 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 43 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 47 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 55 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 67 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 71 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 79 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                            case 59 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                        }
                    }
                    case bb: BasicBlock ⇒ {
                        bb.successors.size should be(1)

                        bb.startPC match {
                            case 0 | 6 | 14 | 22 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.catchBlockSuccessors.size should be(2)
                            }
                            case 80 ⇒ {
                                bb.predecessors.size should be(9)
                                bb.successors(0) should be(new ExitBlock)
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(8)
        }
    }
}
