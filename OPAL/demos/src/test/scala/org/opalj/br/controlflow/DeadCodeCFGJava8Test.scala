package org.opalj.br.controlflow

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.bi.reader.ClassFileReader
import org.opalj.br.ObjectType

@RunWith(classOf[JUnitRunner])
class DeadCodeCFGJava8Test extends FunSpec with Matchers {

	val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)
	val count = reader.ClassFile("C:/Users/User/Desktop/bup/classfiles/cfgtest8.jar", "BoringCode.class").head.methods.size
	
	
//	println("huhu "+ count)
	
    val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "demo")
    val testProject = Project(testFolder)

    describe("Building Control Flow Graphs for some trivial code") {

        val testClass = testProject.classFile(ObjectType("BoringCode")).get

        it("Testing a most simple method without any controll flow in it") {

            val singleBlockCFG = ControlFlowGraph(testClass.findMethod("singleBlock").get)
            val BlockList = singleBlockCFG.AllBlocks

            BlockList.size should be(3)
            singleBlockCFG.startBlock.successors.size should be(1)
            singleBlockCFG.endBlock.predecessors.size should be(1)

        }

        it("Testing a method with a simple branch statement and one return statement") {

            val conditionalOneReturnCFG = ControlFlowGraph(testClass.findMethod("conditionalOneReturn").get)
            val BlockList = conditionalOneReturnCFG.AllBlocks

            BlockList.size should be(12)
            conditionalOneReturnCFG.startBlock.successors.size should be(1)
            conditionalOneReturnCFG.endBlock.predecessors.size should be(1)
        }

        it("Testing a method with a simple branch statement and two return statements") {

            val conditionalTwoReturnsCFG = ControlFlowGraph(testClass.findMethod("conditionalTwoReturns").get)
            val BlockList = conditionalTwoReturnsCFG.AllBlocks

            BlockList.size should be(7)
            conditionalTwoReturnsCFG.startBlock.successors.size should be(1)
            conditionalTwoReturnsCFG.endBlock.predecessors.size should be(3)
        }
    }

    describe("Building Control Flow Graphs for some methods with loops") {

        val testClass = testProject.classFile(ObjectType("LoopCode")).get

        it("Testing a simple Loop") {

            val simpleLoopCFG = ControlFlowGraph(testClass.findMethod("simpleLoop").get)
            val BlockList = simpleLoopCFG.AllBlocks

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
            val BlockList = nestedLoopCFG.AllBlocks

            BlockList.size should be(9)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 18 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(bb.predecessors(0))
                                bb.successors(0) should be(BasicBlock(14))
                            }
                            case 32 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(BasicBlock(14))
                                bb.successors(0) should be(BasicBlock(5))
                            }
                            case 11 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(BasicBlock(5))
                                bb.successors(0) should be(BasicBlock(14))
                            }
                            case 38 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(ExitBlock())
                            }
                            case 5 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.successors.size should be(2)
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
            val BlockList = loopWithBranchCFG.AllBlocks

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
                                bb.successors(0) should be(ExitBlock())
                            }
                            case 30 | 22 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(BasicBlock(16))
                                //                bb.predecessors (0).asInstanceOf[BasicBlock].startPC should be(16)
                                bb.successors(0) should be(BasicBlock(11))
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
            // TODO Wirft noch exceptions

            val endlessLoopCFG = ControlFlowGraph(testClass.findMethod("endlessLoop").get)
            val BlockList = endlessLoopCFG.AllBlocks

            BlockList.size should be(3)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(BasicBlock(8))
                            }
                            case 8 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(BasicBlock(8))
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

        val testClass = testProject.classFile(ObjectType("SwitchCode")).get

        it("The degenerative case") {

            val degenerateSwitchCFG = ControlFlowGraph(testClass.findMethod("degenerateSwitch").get)
            val BlockList = degenerateSwitchCFG.AllBlocks

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
                                bb.predecessors(0) should be(BasicBlock(0))
                                bb.successors(0) should be(ExitBlock())
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
            val BlockList = simpleSwitchCFG.AllBlocks

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
                                bb.predecessors(0) should be(BasicBlock(0))
                                bb.successors(0) should be(BasicBlock(45))
                            }
                            case 45 ⇒ {
                                bb.predecessors.size should be(4)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(ExitBlock())
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                        eb.predecessors(0) should be(BasicBlock(45))
                    }
                    case _ ⇒ {}
                }
            }

        }

        it("Great distances between cases; With default case; With fall-through") {

            val disparateSwitchCFG = ControlFlowGraph(testClass.findMethod("disparateSwitchWithoutBreakWithDefault").get)
            val BlockList = disparateSwitchCFG.AllBlocks

            BlockList.size should be(8)

            for (block ← BlockList) {
                block match {
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(5)
                            }
//                            case 47 | 51 | 56 | 58 ⇒ {
//                                bb.predecessors.size should be(2)
//                                bb.successors.size should be(1)
//                                bb.predecessors(0) should be(BasicBlock(0))
//                            }
//                            case 44 ⇒ {
//                                bb.predecessors.size should be(1)
//                                bb.successors.size should be(1)
//                                bb.successors(0) should be(BasicBlock(47))
//                            }
//                            case 47 ⇒ {
//                                bb.successors(0) should be(BasicBlock(51))
//                            }
//                            case 51 ⇒ {
//                                bb.successors(0) should be(BasicBlock(56))
//                            }
//                            case 56 ⇒ {
//                                bb.successors(0) should be(BasicBlock(58))
//                            }
//                            case 58 ⇒ {
//                                bb.successors(0) should be(ExitBlock())
//                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                    }
                    case _ ⇒
                }
            }
        }

        it("With and Without Fallthrough") {
            val fallthroughCFG = ControlFlowGraph(testClass.findMethod("withAndWithoutFallthrough").get)
            val BlockList = fallthroughCFG.AllBlocks

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
                                bb.predecessors(0) should be(BasicBlock(0))
                            }
                            case 39 | 42 | 58 ⇒ {
                                bb.predecessors.size should be(2)
                                bb.predecessors(0) should be(BasicBlock(0))
                            }
                            case 60 ⇒ {
                                bb.predecessors.size should be(3)
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(1)
                        eb.predecessors(0) should be(BasicBlock(60))
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }
        }
    }

    describe("Methods with Exception-Handling") {

        val testClass = testProject.classFile(ObjectType("ExceptionCode")).get

        it("Code with a single try-catch-structure") {

            val simpleExceptionCFG = ControlFlowGraph(testClass.findMethod("simpleException").get)
            val BlockList = simpleExceptionCFG.AllBlocks

            BlockList.size should be(7)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        cb.successors.size should be(1)
                        cb.predecessors(0) should be(BasicBlock(0))
                        cb.successors(0) should be(BasicBlock(29))
                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(StartBlock())
                                bb.successors(0) should be(BasicBlock(25))
                            }
                            case 25 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.predecessors(0) should be(BasicBlock(0))
                                bb.successors(0) should be(BasicBlock(33))
                            }
                            case 29 | 33 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                                bb.successors(0) should be(ExitBlock())
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
            val BlockList = multipleCatchCFG.AllBlocks

            BlockList.size should be(11)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                    }
//                    case fb: FinallyBlock ⇒ {
//                        numberOfFinallyBlocks += 1
//                        fb.predecessors.size should be(2)
//                        fb.successors(0) should be(BasicBlock(42))
//                    }
                    case bb: BasicBlock ⇒ {
                        bb.successors.size should be(1)

                        bb.startPC match {
                            case 0 | 26 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.catchBlockSuccessors.size should be(1)
//                                bb.finallyBlockSuccessors.size should be(1)
                            }
                            case 30 | 36 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                            case 27 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.predecessors(0) should be(BasicBlock(26))
                            }
                            case 42 ⇒ {

                                bb.predecessors.size should be(1)
                                bb.predecessors(0).isInstanceOf[CatchBlock] should be(true)
                            }
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(4)
                    }
                    case _ ⇒ {}
                }
            }

            numberOfCatchBlocks should be(2)

        }

        it("Code with nested Exception-Handling") {

            val nestedExceptionCFG = ControlFlowGraph(testClass.findMethod("nestedExceptions").get)
            val BlockList = nestedExceptionCFG.AllBlocks

            BlockList.size should be(14)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        if (cb.handlerPC == 70)
                            cb.predecessors(0).predecessors(0).isInstanceOf[CatchBlock] should be(true)
                        if (cb.handlerPC == 52)
                            cb.successors(0).asInstanceOf[BasicBlock].catchBlockSuccessors(0).handlerPC should be(70)
                    }
                    case bb: BasicBlock ⇒ {
                        if (bb.startPC == 74)
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
            val BlockList = catchWithLoopCFG.AllBlocks

            BlockList.size should be(14)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.successors(0) should be(BasicBlock(8))
                    }
//                    case fb: FinallyBlock ⇒ {
//                        numberOfFinallyBlocks += 1
//                        fb.successors(0) should be(BasicBlock(35))
//                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 35 ⇒ {
                                bb.predecessors.size should be(2)
                                for (pred ← bb.predecessors)
                                    pred.isInstanceOf[CatchBlock] should be(true)
                            }
                            case 13 ⇒ {
                                bb.predecessors.size should be(2)
                                for (pred ← bb.predecessors)
                                    pred.isInstanceOf[BasicBlock] should be(true)
                            }
//                            case 0 ⇒ {
//                                bb.catchBlockSuccessors.size should be(1)
//                            }
                            case 0 | 5 | 8 | 20 | 25 | 33 ⇒ {
                                bb.predecessors.size should be(1)
                                bb.successors.size should be(1)
                            }
//                            case 0 | 8 | 20 ⇒ {
//                                bb.finallyBlockSuccessors.size should be(1)
//                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(3)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(1)
        }

        it("Finally-Block with Loop, Catch-Block with return statement") {

            val finallyWithLoopCFG = ControlFlowGraph(testClass.findMethod("loopExceptionWithCatchReturn").get)
            val BlockList = finallyWithLoopCFG.AllBlocks

            BlockList.size should be(18)

            var numberOfCatchBlocks: Int = 0

            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1
                        cb.predecessors.size should be(1)
                        cb.successors.size should be(1)
                    }
//                    case fb: FinallyBlock ⇒ {
//                        numberOfFinallyBlocks += 1
//                        fb.predecessors.size should be(1)
//                        fb.successors.size should be(1)
//                    }
                    case bb: BasicBlock ⇒ {
                        bb.startPC match {
                            case 0 ⇒ {
                                bb.catchBlockSuccessors.size should be(1)
//                                bb.finallyBlockSuccessors.size should be(1)
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
                        eb.predecessors.size should be(2)
                    }
                    case sb: StartBlock ⇒ {}
                }
            }

            numberOfCatchBlocks should be(1)
        }

        it("Three-times nested Try-Catch-Finally-Structure") {

            val highlyNestedCFG = ControlFlowGraph(testClass.findMethod("highlyNestedFinally").get)
            val BlockList = highlyNestedCFG.AllBlocks

            BlockList.size should be(23)

            var numberOfCatchBlocks: Int = 0
            
            for (block ← BlockList) {
                block match {
                    case cb: CatchBlock ⇒ {
                        numberOfCatchBlocks += 1

                        cb.handlerPC match {
                            case 112 ⇒ {
                                cb.predecessors.size should be(4)
                            }
                            case 95 ⇒ {
                                cb.predecessors.size should be(3)
                            }
                            case 72 ⇒ {
                                cb.predecessors.size should be(2)
                            }
                            case 43 ⇒ {
                                cb.predecessors.size should be(1)
                            }
                        }
                    }
//                    case fb: FinallyBlock ⇒ {
//                        numberOfFinallyBlocks += 1
//
//                        fb.handlerPC match {
//                            case 119 ⇒ {
//                                fb.predecessors.size should be(4)
//                            }
//                            case 105 ⇒ {
//                                fb.predecessors.size should be(3)
//                            }
//                            case 85 ⇒ {
//                                fb.predecessors.size should be(2)
//                            }
//                            case 59 ⇒ {
//                                fb.predecessors.size should be(1)
//                            }
//                        }
//                    }
                    case bb: BasicBlock ⇒ {
                        bb.predecessors.size should be(1)
                        bb.successors.size should be(1)

                        bb.startPC match {
                            case 0 ⇒ {
//                                bb.finallyBlockSuccessors.size should be(1)
                                bb.catchBlockSuccessors.size should be(1)
                            }
                            case 6 ⇒ {
//                                bb.finallyBlockSuccessors.size should be(2)
                                bb.catchBlockSuccessors.size should be(2)
                            }
                            case 14 ⇒ {
//                                bb.finallyBlockSuccessors.size should be(3)
                                bb.catchBlockSuccessors.size should be(3)
                            }
                            case 22 ⇒ {
//                                bb.finallyBlockSuccessors.size should be(4)
                                bb.catchBlockSuccessors.size should be(4)
                            }
                            case _ ⇒ {}
                        }
                    }
                    case eb: ExitBlock ⇒ {
                        eb.predecessors.size should be(9)
                    }
                    case sb: StartBlock ⇒ {

                    }
                }
            }

            numberOfCatchBlocks should be(4)
        }
    }
}