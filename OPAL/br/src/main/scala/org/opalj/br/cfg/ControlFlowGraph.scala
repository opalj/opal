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

import java.io.File
import java.io.PrintWriter
import java.io.IOException
import scala.io.StdIn
import scala.collection.SortedMap
import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.PC
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.analyses.Project
import org.opalj.br.{ ClassFile, Method, ObjectType }
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ControlTransferInstruction
import org.opalj.br.instructions.UnconditionalBranchInstruction
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
import org.opalj.br.instructions.UnconditionalBranch

/**
 * @author Erich Wittenbeck
 */
object ControlFlowGraph {

    private[cfg] def connectBlocks(predecessor: CFGBlock, successor: CFGBlock): Unit = {
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

        val catchBlocks: mutable.Map[ExceptionHandler, CatchBlock] = new mutable.AnyRefMap[ExceptionHandler, CatchBlock](code.exceptionHandlers.size)

        var exitPoints: mutable.Set[BasicBlock] = mutable.Set.empty[BasicBlock]

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

            catchBlocks += ((handler, catchBlock))
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
                            BlocksByPC(nextPC) = new BasicBlock(nextPC)

                    exitPoints = exitPoints + currentBlock

                }
            }

            val jvmExceptions = instructions(currentPC).jvmExceptions

            if (jvmExceptions.nonEmpty) {
                for (handler ← code.handlersFor(currentPC)) {
                    val catchType = handler.catchType.getOrElse(None)

                    if (catchType == None || jvmExceptions.contains(catchType)) {

                        BlocksByPC(nextPC) =
                            if (!isBeginningOfBlock(nextPC))
                                new BasicBlock(nextPC)
                            else
                                BlocksByPC(nextPC)

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

        new ControlFlowGraph(method, startBlock, exitBlock, BlocksByPC, catchBlocks)
    }
}

/**
 * @author Erich Wittenbeck
 */
case class ControlFlowGraph(
        method: Method,
        startBlock: StartBlock,
        endBlock: ExitBlock,
        blocksByPC: Array[BasicBlock],
        catchBlocks: scala.collection.Map[ExceptionHandler, CatchBlock]) {

    private var finallyToRegularBlock: Map[BasicBlock, BasicBlock] = Map.empty[BasicBlock, BasicBlock]

    private var regularToFinallyBlocks: Map[BasicBlock, Set[BasicBlock]] = Map.empty[BasicBlock, HashSet[BasicBlock]]

    def allBlocks: scala.collection.Set[CFGBlock] = startBlock.returnAllBlocks(new HashSet[CFGBlock])

    def toDot: String = {

        var bbIndex: Int = 0; var cbIndex: Int = 0;

        for (block ← allBlocks)
            block match {
                case bb: BasicBlock ⇒ { bb.ID = "bb"+bbIndex; bbIndex += 1 }
                case cb: CatchBlock ⇒ { cb.ID = "cb"+cbIndex; cbIndex += 1 }
                case sb: StartBlock ⇒ { sb.ID = "start" }
                case eb: ExitBlock  ⇒ { eb.ID = "exit" }
            }

        var dotString: String = "digraph{\ncompound=true;\n"

        for (block ← allBlocks) {
            dotString += block.toDot(method.body.get)
        }

        dotString = dotString+"}"

        dotString
    }

    def correspondingPCsTo(pc: PC): UShortSet = {

        var results = UShortSet.empty // holds all the correspondants of 'pc'
        val block = blocksByPC(pc) // the block for which the corresponding PCs are to be computed
        val correspondingBlocks = findCorrespondingBlocksForPC(pc) // the correspondants of 'block'
        val code = method.body.get // the code of which this CFG is based upon
        val index = block.indexOfPC(pc, code) // the index of the PC/Instruction that 'pc' has within block

        def instruction(pc: PC): Instruction = code.instructions(pc)

        for (correspondant ← correspondingBlocks) {
            val corrPCs = correspondant.programCounters(code) // the correspondant's list of PCs

            if (corrPCs.size == block.indexOfPC(block.endPC, code) + 1) { // check wether 'block' and 'correspondant' have same size
                if (instruction(pc).mnemonic == instruction(corrPCs(index)).mnemonic) { // check if the instructions that are indexed by 'index' in both blocks are the same type
                    results = results + corrPCs(index) // if yes to both: add the PC to the resulting UShortSet
                }
            }
        }

        results
    }

    private def findCorrespondingBlocksForPC(pc: PC): Set[BasicBlock] = {
        val bb = blocksByPC(pc)

        if (finallyToRegularBlock.isEmpty)
            findfinallyToRegularBlock

        if (finallyToRegularBlock contains bb) {
            HashSet(finallyToRegularBlock(bb))
        } else if (regularToFinallyBlocks contains bb) {
            regularToFinallyBlocks(bb)
        } else
            HashSet.empty
    }

    private def associateBasicBlocks(bbfin: BasicBlock, bbreg: BasicBlock): Unit = {
        finallyToRegularBlock = finallyToRegularBlock + ((bbfin, bbreg))

        if (regularToFinallyBlocks contains bbreg) {
            val newSet = regularToFinallyBlocks(bbreg) + (bbfin)
            regularToFinallyBlocks = regularToFinallyBlocks + ((bbreg, newSet))
        } else {
            regularToFinallyBlocks = regularToFinallyBlocks + ((bbreg, HashSet(bbfin)))
        }
    }

    private def findfinallyToRegularBlock(): Unit = {
        val code: Code = method.body.get

        for (handler ← code.exceptionHandlers if (handler.catchType.getOrElse(None) == None && !catchBlocks(handler).predecessors.isEmpty)) {

            val finallyCatchBlock: CatchBlock = catchBlocks(handler)

            // Find immediate successor of finallyCatchBlock

            var immediatePredecessor: BasicBlock = null

            for (predecessor ← finallyCatchBlock.predecessors.asInstanceOf[List[BasicBlock]]) {
                if (immediatePredecessor == null || immediatePredecessor.startPC < predecessor.startPC) {
                    immediatePredecessor = predecessor
                }
            }

            // The first BasicBlock in the regular execution path, which corresponds to the first Block
            // in the finally handler.
            var immediateCorrespondant: BasicBlock = immediatePredecessor.successors(0).asInstanceOf[BasicBlock]

            // In case the (regular) successor of the immediate Predecessor of the CatchBlock is control flow, instead of an actual correspondant,
            // choose the next Block in the execution path instead
            if (code.instructions(immediateCorrespondant.endPC).isInstanceOf[UnconditionalBranchInstruction])
                immediateCorrespondant = immediateCorrespondant.successors(0).asInstanceOf[BasicBlock]

            //contains all BasicBlocks, dominated by finallyCatchBlock
            var dominationSetFinally: Set[BasicBlock] = HashSet()

            //contains all BasicBlock, dominated by immediateCorrespondant
            var dominationSetRegular: Set[BasicBlock] = HashSet()

            //all BasicBlocks, that have already been processed. Prevents infinite looping
            var visited: Set[BasicBlock] = HashSet()

            // Workqueues for iterative, breadth-first (sub-)graph-traversal, one for Blocks in finally handler,
            // the other for Blocks in the regular path
            val workqueueFinally: mutable.Queue[BasicBlock] = mutable.Queue(finallyCatchBlock.successors(0).asInstanceOf[BasicBlock])

            val workqueueRegular: mutable.Queue[BasicBlock] = mutable.Queue(immediateCorrespondant)

            while (!workqueueFinally.isEmpty) {
                val bbfin = workqueueFinally.dequeue
                val bbreg = workqueueRegular.dequeue

                // Create corrispondance between the two BasicBlocks
                associateBasicBlocks(bbfin, bbreg)

                dominationSetFinally = dominationSetFinally + (bbfin)
                dominationSetRegular = dominationSetRegular + (bbreg)
                visited = visited + (bbfin, bbreg)

                // Find out, which Blocks will have to be put in the queues next

                val finsuccessors = bbfin.successors.filter { block ⇒ block.isInstanceOf[BasicBlock] }.asInstanceOf[List[BasicBlock]]
                val regsuccessors = bbreg.successors.filter { block ⇒ block.isInstanceOf[BasicBlock] }.asInstanceOf[List[BasicBlock]]

                // Iterate over both Lists in parallel

                var index: Int = 0
                while (index < finsuccessors.size) {
                    val finsucc = finsuccessors(index)
                    val regsucc = regsuccessors(index)

                    val isToBeProcessedFurther: Boolean = finsucc.predecessors.filter { block ⇒ !block.successors.contains(finsucc) }.forall { block ⇒
                        {
                            block match {
                                case bb: BasicBlock ⇒ {
                                    (dominationSetFinally contains bb)
                                }
                                case _ ⇒ { false }
                            }
                        }
                    }

                    if (isToBeProcessedFurther && !(visited contains finsucc)) {
                        workqueueFinally.enqueue(finsucc)
                        workqueueRegular.enqueue(regsucc)
                    }

                    index += 1
                }
            }
        }
    }
}
