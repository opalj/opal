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
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType

/**
 * We merely construct CFGs for various, self-made methods and check their blocks
 * for various properties.
 *
 * E.g.:
 *
 *  - Does each block have the correct amount of predecessors and successors?
 *
 *  - Does it have the correct amount of catchBlock-successors?
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BasicCFGJava8Test extends FunSpec with Matchers {

    val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)

    describe("cfgs with very simple control flow") {

        val testClass = testProject.classFile(ObjectType("controlflow/BoringCode")).get

        it("a cfg with no control flow statemts should consists of a single basic block") {

            val cfg = CFGFactory(testClass.findMethod("singleBlock").get)
            val bbs = cfg.allBBs

            bbs.size should be(1)
            cfg.startBlock.successors.size should be(1)
            cfg.normalReturnNode.predecessors.size should be(1)
            cfg.abnormalReturnNode.predecessors.size should be(0)
        }

        it("a cfg with some simple control flow statemts should consists of respective single basic blocks") {

            val cfg = CFGFactory(testClass.findMethod("conditionalOneReturn").get)
            val bbs = cfg.allBBs

            bbs.size should be(11)
            cfg.startBlock.successors.size should be(2)
            cfg.normalReturnNode.predecessors.size should be(1)
            cfg.abnormalReturnNode.predecessors.size should be(1)
        }

        it("a cfg for a method with multiple return statements should have corresponding basic blocks") {

            val cfg = CFGFactory(testClass.findMethod("conditionalTwoReturns").get)
            val bbs = cfg.allBBs

            bbs.size should be(6)
            cfg.startBlock.successors.size should be(2)
            cfg.normalReturnNode.predecessors.size should be(3)
            cfg.abnormalReturnNode.predecessors.size should be(1)
        }
    }
    //
    //    describe("Building Control Flow Graphs for some methods with loops") {
    //
    //        val testClass = testProject.classFile(ObjectType("controlflow/LoopCode")).get
    //
    //        it("Testing a simple Loop") {
    //
    //            val simpleLoopCFG = CFGFactory(testClass.findMethod("simpleLoop").get)
    //            val bbs = simpleLoopCFG.allBBs
    //
    //            bbs.size should be(5)
    //
    //            for (block ← bbs)
    //                block match {
    //                    case bb: BasicBlock ⇒ {
    //                        if (bb.startPC == 9) {
    //                            bb.successors(0) should be(bb.predecessors(0))
    //                        }
    //                    }
    //                    case _ ⇒ {}
    //                }
    //        }
    //
    //        it("Testing a method with two nested loops") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("nestedLoop").get)
    //            val bbs = cfg.allBBs
    //            bbs.size should be(8)
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 18 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(bb.predecessors(0))
    //                            bb.successors(0) should be(new BasicBlock(14))
    //                        }
    //                        case 32 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(14))
    //                            bb.successors(0) should be(new BasicBlock(5))
    //                        }
    //                        case 11 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(5))
    //                            bb.successors(0) should be(new BasicBlock(14))
    //                        }
    //                        case 38 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new ExitNode())
    //                        }
    //                        case 5 ⇒ {
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(2)
    //                        }
    //                        case _ ⇒ {}
    //                    }
    //                }
    //                case eb: ExitNode ⇒ eb.predecessors.size should be(1)
    //                case _            ⇒ {}
    //            }
    //        }
    //
    //        it("Testing a loop with a branch statement") {
    //
    //            val loopWithBranchCFG = CFGFactory(testClass.findMethod("loopWithBranch").get)
    //            val bbs = loopWithBranchCFG.allBBs
    //
    //            bbs.size should be(8)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.successors.size should be(2)
    //                        }
    //                        case 16 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(2)
    //                        }
    //                        case 9 | 39 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new ExitNode())
    //                        }
    //                        case 30 | 22 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(16))
    //                            bb.successors(0) should be(new BasicBlock(11))
    //                        }
    //                        case _ ⇒ {}
    //                    }
    //                }
    //                case eb: ExitNode ⇒ eb.predecessors.size should be(2)
    //                case _            ⇒ {}
    //            }
    //        }
    //
    //        it("Testing a non-terminating loop") {
    //
    //            val endlessLoopCFG = CFGFactory(testClass.findMethod("endlessLoop").get)
    //            val bbs = endlessLoopCFG.allBBs
    //
    //            bbs.size should be(2)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new BasicBlock(8))
    //                        }
    //                        case 8 ⇒ {
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new BasicBlock(8))
    //                        }
    //                        case _ ⇒ {}
    //                    }
    //                }
    //                case _ ⇒ {}
    //            }
    //        }
    //    }
    //
    //    describe("Methods with Switch-Statements") {
    //
    //        val testClass = testProject.classFile(ObjectType("controlflow/SwitchCode")).get
    //
    //        it("The degenerative case") {
    //
    //            val degenerateSwitchCFG = CFGFactory(testClass.findMethod("degenerateSwitch").get)
    //            val bbs = degenerateSwitchCFG.allBBs
    //
    //            bbs.size should be(4)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.successors.size should be(2)
    //                        }
    //                        case 20 | 22 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(0))
    //                            bb.successors(0) should be(new ExitNode())
    //                        }
    //                    }
    //                }
    //                case eb: ExitNode ⇒ {
    //                    eb.predecessors.size should be(2)
    //                }
    //                case _ ⇒ {}
    //            }
    //        }
    //
    //        it("Small distances between cases; No default case; No fall-through") {
    //
    //            val simpleSwitchCFG = CFGFactory(testClass.findMethod("simpleSwitchWithBreakNoDefault").get)
    //            val bbs = simpleSwitchCFG.allBBs
    //
    //            bbs.size should be(6)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.successors.size should be(4)
    //                        }
    //                        case 28 | 34 | 41 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(0))
    //                            bb.successors(0) should be(new BasicBlock(45))
    //                        }
    //                        case 45 ⇒ {
    //                            bb.predecessors.size should be(4)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new ExitNode())
    //                        }
    //                    }
    //                }
    //                case eb: ExitNode ⇒ {
    //                    eb.predecessors.size should be(1)
    //                    eb.predecessors(0) should be(new BasicBlock(45))
    //                }
    //                case _ ⇒ {}
    //            }
    //
    //        }
    //
    //        it("Great distances between cases; With default case; With fall-through") {
    //
    //            val disparateSwitchCFG = CFGFactory(testClass.findMethod("disparateSwitchWithoutBreakWithDefault").get)
    //            val bbs = disparateSwitchCFG.allBBs
    //
    //            bbs.size should be(7)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.successors.size should be(5)
    //                        }
    //                        case 44 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.successors(0) should be(new BasicBlock(47))
    //                        }
    //                        case 47 ⇒ {
    //                            bb.successors(0) should be(new BasicBlock(51))
    //
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(1) should be(new BasicBlock(0))
    //                        }
    //                        case 51 ⇒ {
    //                            bb.successors(0) should be(new BasicBlock(56))
    //
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(1) should be(new BasicBlock(0))
    //                        }
    //                        case 56 ⇒ {
    //                            bb.successors(0) should be(new BasicBlock(58))
    //
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(1) should be(new BasicBlock(0))
    //                        }
    //                        case 58 ⇒ {
    //                            bb.successors(0) should be(new ExitNode)
    //
    //                            bb.predecessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                            bb.predecessors(1) should be(new BasicBlock(0))
    //                        }
    //                    }
    //                }
    //                case eb: ExitNode ⇒ {
    //                    eb.predecessors.size should be(1)
    //                }
    //                case _ ⇒ {}
    //            }
    //
    //        }
    //
    //        it("With and Without Fallthrough") {
    //            val fallthroughCFG = CFGFactory(testClass.findMethod("withAndWithoutFallthrough").get)
    //            val bbs = fallthroughCFG.allBBs
    //
    //            bbs.size should be(9)
    //
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.successors.size should be(6)
    //                        }
    //                        case 36 | 54 | 48 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(0))
    //                        }
    //                        case 39 | 42 | 58 ⇒ {
    //                            bb.predecessors.size should be(2)
    //                            bb.predecessors(1) should be(new BasicBlock(0))
    //                        }
    //                        case 60 ⇒ {
    //                            bb.predecessors.size should be(3)
    //                        }
    //                    }
    //                }
    //                case eb: ExitNode ⇒ {
    //                    eb.predecessors.size should be(1)
    //                    eb.predecessors(0) should be(new BasicBlock(60))
    //                }
    //
    //            }
    //        }
    //    }
    //
    //    describe("Methods with Exception-Handling") {
    //
    //        val testClass = testProject.classFile(ObjectType("controlflow/ExceptionCode")).get
    //
    //        it("Code with a single try-catch-structure") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("simpleException").get)
    //            val bbs = cfg.allBBs
    //
    //            bbs.size should be(6)
    //
    //            var numberOfCatchNodes: Int = 0
    //
    //            bbs foreach {
    //                {
    //                    case cb: CatchNode ⇒ {
    //                        numberOfCatchNodes += 1
    //                        cb.predecessors.size should be(1)
    //                        cb.successors.size should be(1)
    //                        cb.predecessors(0) should be(new BasicBlock(0))
    //                        cb.successors(0) should be(new BasicBlock(29))
    //                    }
    //                    case bb: BasicBlock ⇒ {
    //                        bb.startPC match {
    //                            case 0 ⇒ {
    //                                bb.predecessors.size should be(0)
    //                                bb.successors.size should be(1)
    //                                bb.predecessors should be(Nil)
    //                                bb.successors(0) should be(new BasicBlock(25))
    //                            }
    //                            case 25 ⇒ {
    //                                bb.predecessors.size should be(1)
    //                                bb.successors.size should be(1)
    //                                bb.predecessors(0) should be(new BasicBlock(0))
    //                                bb.successors(0) should be(new BasicBlock(33))
    //                            }
    //                            case 29 | 33 ⇒ {
    //                                bb.predecessors.size should be(1)
    //                                bb.successors.size should be(1)
    //                                bb.successors(0) should be(new ExitNode())
    //                            }
    //                        }
    //                    }
    //                    case eb: ExitNode ⇒ {
    //                        eb.predecessors.size should be(2)
    //                    }
    //                }
    //            }
    //
    //            numberOfCatchNodes should be(1)
    //        }
    //
    //        it("Code with multiple try-catch-blocks and a finally-block") {
    //
    //            val multipleCatchCFG = CFGFactory(testClass.findMethod("multipleCatchAndFinally").get)
    //            val bbs = multipleCatchCFG.allBBs
    //            bbs.size should be(10)
    //            var numberOfCatchNodes: Int = 0
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.successors.size should be(1)
    //
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.predecessors.size should be(0)
    //                            bb.catchBlockSuccessors.size should be(2)
    //                        }
    //                        case 26 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.catchBlockSuccessors.size should be(1)
    //                        }
    //                        case 30 | 36 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0).isInstanceOf[CatchNode] should be(true)
    //                        }
    //                        case 27 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0) should be(new BasicBlock(26))
    //                        }
    //                        case 42 ⇒ {
    //
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0).isInstanceOf[CatchNode] should be(true)
    //                        }
    //                    }
    //                }
    //                case cb: CatchNode ⇒ numberOfCatchNodes += 1
    //                case eb: ExitNode  ⇒ eb.predecessors.size should be(4)
    //            }
    //
    //            numberOfCatchNodes should be(3)
    //
    //        }
    //
    //        it("Code with nested Exception-Handling") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("nestedExceptions").get)
    //            val bbs = cfg.allBBs
    //
    //            bbs.size should be(13)
    //
    //            var numberOfCatchNodes: Int = 0
    //
    //            bbs foreach {
    //                case cb: CatchNode ⇒ {
    //                    numberOfCatchNodes += 1
    //                    cb.predecessors.size should be(1)
    //                    if (cb.handlerPC == 70)
    //                        cb.predecessors(0).predecessors(0).isInstanceOf[CatchNode] should be(true)
    //                    if (cb.handlerPC == 52)
    //                        cb.successors(0).asInstanceOf[BasicBlock].catchBlockSuccessors(0).handlerPC should be(70)
    //                }
    //                case bb: BasicBlock ⇒ {
    //                    if (bb.startPC == 74)
    //                        bb.predecessors.size should be(2)
    //                    else if (bb.startPC == 0)
    //                        bb.predecessors.size should be(0)
    //                    else
    //                        bb.predecessors.size should be(1)
    //
    //                    bb.successors.size should be(1)
    //
    //                    bb.catchBlockSuccessors.size <= 1 should be(true)
    //                }
    //                case eb: ExitNode ⇒ eb.predecessors.size should be(3)
    //            }
    //
    //            numberOfCatchNodes should be(3)
    //        }
    //
    //        it("Catch-Block with Loop, and Finally-Block with return statement") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("loopExceptionWithFinallyReturn").get)
    //            val bbs = cfg.allBBs
    //
    //            bbs.size should be(13)
    //
    //            var numberOfCatchNodes: Int = 0
    //            bbs foreach {
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 35 ⇒ {
    //                            bb.predecessors.size should be(2)
    //                            for (pred ← bb.predecessors)
    //                                pred.isInstanceOf[CatchNode] should be(true)
    //                        }
    //                        case 13 ⇒ {
    //                            bb.predecessors.size should be(2)
    //                            for (pred ← bb.predecessors)
    //                                pred.isInstanceOf[BasicBlock] should be(true)
    //                            bb.successors.size should be(1)
    //                            bb.catchBlockSuccessors.size should be(1)
    //                            bb.catchBlockSuccessors(0).handlerPC should be(35)
    //                        }
    //                        case 0 ⇒ {
    //                            bb.catchBlockSuccessors.size should be(2)
    //                        }
    //                        case 20 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.successors.size should be(1)
    //                            bb.catchBlockSuccessors.size should be(1)
    //                            bb.catchBlockSuccessors(0).handlerPC should be(35)
    //                        }
    //                        case _ ⇒ {}
    //                    }
    //                }
    //                case cb: CatchNode ⇒ numberOfCatchNodes += 1
    //                case eb: ExitNode  ⇒ eb.predecessors.size should be(3)
    //            }
    //
    //            numberOfCatchNodes should be(4)
    //        }
    //
    //        it("Finally-Block with Loop, Catch-Block with return statement") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("loopExceptionWithCatchReturn").get)
    //            val bbs = cfg.allBBs
    //
    //            bbs.size should be(17)
    //
    //            var numberOfCatchNodes: Int = 0
    //
    //            bbs foreach {
    //                case cb: CatchNode ⇒ {
    //                    numberOfCatchNodes += 1
    //                    cb.predecessors.size should be(1)
    //                    cb.successors.size should be(1)
    //                }
    //                case bb: BasicBlock ⇒ {
    //                    bb.startPC match {
    //                        case 0 ⇒ {
    //                            bb.catchBlockSuccessors.size should be(2)
    //                            bb.successors.size should be(1)
    //                        }
    //                        case 63 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0).isInstanceOf[CatchNode] should be(true)
    //                        }
    //                        case 32 ⇒ {
    //                            bb.predecessors.size should be(1)
    //                            bb.predecessors(0).isInstanceOf[CatchNode] should be(true)
    //                        }
    //                        case _ ⇒ {}
    //                    }
    //                }
    //                case eb: ExitNode ⇒ {
    //                    eb.predecessors.size should be(3)
    //                }
    //            }
    //
    //            numberOfCatchNodes should be(2)
    //        }
    //
    //        it("Three-times nested Try-Catch-Finally-Structure") {
    //
    //            val cfg = CFGFactory(testClass.findMethod("highlyNestedFinally").get)
    //            val bbs = cfg.allBBs
    //
    //            bbs.size should be(22)
    //
    //            var numberOfCatchNodes: Int = 0
    //
    //            bbs foreach {
    //                case cb: CatchNode ⇒ {
    //                    numberOfCatchNodes += 1
    //                    cb.handlerPC match {
    //                        case 112 ⇒ cb.predecessors.size should be(1)
    //                        case 95  ⇒ cb.predecessors.size should be(1)
    //                        case 72  ⇒ cb.predecessors.size should be(1)
    //                        case 43  ⇒ cb.predecessors.size should be(1)
    //                        case 119 ⇒ cb.predecessors.size should be(1)
    //                        case 105 ⇒ cb.predecessors.size should be(1)
    //                        case 85  ⇒ cb.predecessors.size should be(1)
    //                        case 59  ⇒ cb.predecessors.size should be(1)
    //                    }
    //                }
    //                case bb: BasicBlock ⇒ {
    //                    if (bb.startPC == 0)
    //                        bb.predecessors.size should be(0)
    //                    else
    //                        bb.predecessors.size should be(1)
    //
    //                    bb.successors.size should be(1)
    //
    //                    bb.startPC match {
    //                        case 0  ⇒ bb.catchBlockSuccessors.size should be(2)
    //                        case 6  ⇒ bb.catchBlockSuccessors.size should be(2)
    //                        case 14 ⇒ bb.catchBlockSuccessors.size should be(2)
    //                        case 22 ⇒ bb.catchBlockSuccessors.size should be(2)
    //                        case _  ⇒ {}
    //                    }
    //                }
    //                case eb: ExitNode ⇒ eb.predecessors.size should be(9)
    //
    //            }
    //            numberOfCatchNodes should be(8)
    //        }
    //    }
}
