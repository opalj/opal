package org.opalj.br.controlflow

import java.io.File
import java.io.PrintWriter
import java.io.IOException
import scala.io.StdIn
import scala.collection.SortedMap
import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet
import scala.collection.immutable.TreeMap
import org.opalj.br.PC
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.analyses.Project
import org.opalj.br.{ ClassFile, Method, ObjectType }
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ControlTransferInstruction
import org.opalj.br.instructions.GOTO
import org.opalj.br.instructions.GOTO_W
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.LOOKUPSWITCH
import org.opalj.br.instructions.TABLESWITCH
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.instructions.JSR
import org.opalj.br.instructions.RET
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler

object ControlFlowGraph {

    def connectBlocks(predecessor: CFGBlock, successor: CFGBlock): Unit = {
        predecessor.addSucc(successor)
        successor.addPred(predecessor)
    }

    def apply(method: Method): ControlFlowGraph = {

        //        println
        //        println
        //		  println("CFG for "+method.name)
        //        println

        val code: Code = method.body.get

        val instructions: Array[Instruction] = code.instructions

        val codeLength: Int = instructions.length

        val previousPCs: Array[PC] = new Array[PC](codeLength + 1)

        val BlocksByPC = new Array[BasicBlock](codeLength + 1)

        var catchBlocks: Map[ExceptionHandler, CatchBlock] = new HashMap[ExceptionHandler, CatchBlock]

        var exitPoints: Set[BasicBlock] = new HashSet[BasicBlock]

        val startBlock = new StartBlock

        var currentBlock: BasicBlock = new BasicBlock(0)

        connectBlocks(startBlock, currentBlock)

        BlocksByPC(0) = currentBlock

        var currentPC: PC = instructions(0).indexOfNextInstruction(0, code)

        var nextPC: PC = 0;

        def isControlTransfer(pc: PC): Boolean = instructions(pc).isInstanceOf[ControlTransferInstruction]

        def isBeginningOfBlock(pc: PC): Boolean = (pc < codeLength) &&
            (if (BlocksByPC(pc) == null) false else (BlocksByPC(pc).startPC == pc))

        def isReturnFromMethod(pc: PC): Boolean = instructions(pc).isInstanceOf[ReturnInstruction] || instructions(pc) == ATHROW

        for (handler ← code.exceptionHandlers) {
            val handlerPC: PC = handler.handlerPC

            BlocksByPC(handlerPC) = if (!isBeginningOfBlock(handlerPC)) new BasicBlock(handlerPC) else BlocksByPC(handlerPC)

            val catchBlock: CatchBlock = new CatchBlock(handler)

            catchBlocks = catchBlocks + ((handler, catchBlock))
        }

        while (0 < currentPC && currentPC < codeLength) {

            if (instructions(currentPC).isInstanceOf[JSR] ||
                instructions(currentPC).isInstanceOf[RET])
                throw new BytecodeProcessingFailedException("JSR/RET not yet processed")

            val controlFlow: Boolean = isControlTransfer(currentPC)
            val beginningOfBlock: Boolean = isBeginningOfBlock(currentPC)
            val returnFromMethod: Boolean = isReturnFromMethod(currentPC)

            nextPC = code.pcOfNextInstruction(currentPC)
            previousPCs(nextPC) = currentPC

            if (!(controlFlow || beginningOfBlock || returnFromMethod)) {
                currentBlock.addPC(currentPC)

                BlocksByPC(currentPC) = currentBlock
            } else {

                if (beginningOfBlock) {

                    val nextBlock = BlocksByPC(currentPC)

                    if (!(isControlTransfer(currentBlock.endPC) || isReturnFromMethod(currentBlock.endPC))) { // in Hilfmethode packen
                        connectBlocks(currentBlock, nextBlock)
                    }

                    currentBlock = nextBlock
                }
                if (controlFlow) {

                    if (!beginningOfBlock) {
                        currentBlock.addPC(currentPC)
                    }

                    BlocksByPC(currentPC) = currentBlock

                    (instructions(currentPC).opcode: @scala.annotation.switch) match {

                        case GOTO.opcode | GOTO_W.opcode ⇒ {
                            if (!isBeginningOfBlock(nextPC) && (nextPC < BlocksByPC.length)) { // Bugquelle? Was ist nextPC?
                                val nextBlock = new BasicBlock((nextPC))
                                BlocksByPC(nextPC) = nextBlock
                            }
                        }
                        case _ ⇒ {}
                    }

                    for (pc ← instructions(currentPC).nextInstructions(currentPC, code)) {

                        if (pc < currentPC) {

                            if (!isBeginningOfBlock(pc)) {

                                val blockToBeSplit: BasicBlock = BlocksByPC(pc)

                                BlocksByPC(pc) = blockToBeSplit.split(currentBlock, pc, previousPCs(pc))

                                BlocksByPC(pc).foreach(instructionPC ⇒ { BlocksByPC(instructionPC) = BlocksByPC(pc) })(code)

                                if (currentBlock == (blockToBeSplit)) {
                                    currentBlock = BlocksByPC(pc)
                                }

                                connectBlocks(currentBlock, BlocksByPC(pc))
                            } else {
                                connectBlocks(currentBlock, BlocksByPC(pc))
                            }
                        } else {

                            if (isBeginningOfBlock(pc)) {
                                connectBlocks(currentBlock, BlocksByPC(pc))
                            } else {
                                BlocksByPC(pc) = new BasicBlock(pc)
                                connectBlocks(currentBlock, BlocksByPC(pc))
                            }
                        }
                    }
                }
                if (returnFromMethod) {

                    if (!beginningOfBlock) {
                        currentBlock.addPC(currentPC)
                        BlocksByPC(currentPC) = currentBlock
                    }

                    if (nextPC < codeLength && nextPC >= 0)
                        if (!isBeginningOfBlock(nextPC))
                            BlocksByPC(nextPC) = BasicBlock(nextPC)

                    exitPoints = exitPoints + currentBlock

                }
            }

            val runtimeExceptions = instructions(currentPC).runtimeExceptions

            if (runtimeExceptions.nonEmpty) {
                for (handler ← code.handlersFor(currentPC)) {
                    val catchType = handler.catchType.getOrElse(None)

                    if (catchType == None || runtimeExceptions.contains(catchType)) {
                        //						BlocksByPC(nextPC) = BasicBlock(nextPC) 

                        BlocksByPC(nextPC) = if (!isBeginningOfBlock(nextPC)) BasicBlock(nextPC) else BlocksByPC(nextPC)

                        val catchBlock = catchBlocks(handler)
                        val handlerBlock = BlocksByPC(handler.handlerPC)

                        connectBlocks(currentBlock, catchBlock)

                        if (catchBlock.predecessors.tail.isEmpty)
                            connectBlocks(catchBlock, handlerBlock)
                    }
                }
            }

            currentPC = nextPC

        }

        val exitBlock = new ExitBlock

        for (block ← exitPoints) {
            connectBlocks(block, exitBlock)
        }

        new ControlFlowGraph(method, startBlock, exitBlock)
    }
}

case class ControlFlowGraph(
        method: Method,
        startBlock: StartBlock,
        endBlock: ExitBlock) {

    def AllBlocks: HashSet[CFGBlock] = startBlock.returnAllBlocks(new HashSet[CFGBlock])

    def traverseWithFunction[T](f: CFGBlock ⇒ T): Unit = {
        traverseWithFunctionAndBlock(f)(startBlock, new HashSet[CFGBlock])
    }

    private def traverseWithFunctionAndBlock[T](f: CFGBlock ⇒ T)(block: CFGBlock, visited: HashSet[CFGBlock]): Unit = {

        val newVisitedSet: HashSet[CFGBlock] = visited + block
        val successors = block match { case bb: BasicBlock ⇒ bb.successors ++ bb.catchBlockSuccessors; case _ ⇒ block.successors }

        f(block)

        for (successor ← successors if (!visited.contains(successor))) {
            traverseWithFunctionAndBlock[T](f)(successor, newVisitedSet)
        }
    }

    def toDot: String = {

        var bbIndex: Int = 0; var cbIndex: Int = 0;

        for (block ← AllBlocks)
            block match {
                case bb: BasicBlock ⇒ { bb.ID = "bb"+bbIndex; bbIndex += 1 }
                case cb: CatchBlock ⇒ { cb.ID = "cb"+cbIndex; cbIndex += 1 }
                case sb: StartBlock ⇒ { sb.ID = "start" }
                case eb: ExitBlock  ⇒ { eb.ID = "exit" }
            }

        var dotString: String = "digraph{\ncompound=true;\n"

        for (block ← AllBlocks) {
            dotString += block.toDot(method.body.get)
        }

        dotString = dotString+"}"

        dotString
    }
}