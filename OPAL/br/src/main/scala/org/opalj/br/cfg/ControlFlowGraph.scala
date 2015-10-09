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
package org.opalj.br.cfg

import scala.collection.mutable
import scala.collection.immutable.HashSet
import scala.collection.immutable.HashMap
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.PC
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ControlTransferInstruction
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.instructions.InvocationInstruction
import org.opalj.br.instructions.StoreLocalVariableInstruction
import org.opalj.br.instructions.LoadLocalVariableInstruction
import org.opalj.br.instructions.UnconditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.AStoreInstruction
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.JSR
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.GOTO
import org.opalj.br.instructions.GOTO_W

/**
 * @author Erich Wittenbeck
 */

/**
 * A factory for creating ControlFlowGraph-Objects
 *
 * ==Thread-Safety==
 * This object is thread-safe
 */
object ControlFlowGraph {

    /**
     * A utility method for connecting two CFGBlocks, thus adding edges to the graph
     */
    private[cfg] def connectBlocks(predecessor: CFGBlock, successor: CFGBlock): Unit = {

        predecessor match {

            case bb: BasicBlock ⇒

                successor match {
                    case cb: CatchBlock ⇒
                        bb.catchBlockSuccessors = cb :: bb.catchBlockSuccessors

                    case _ ⇒
                        predecessor.successors = successor :: predecessor.successors
                }
            case _ ⇒
                predecessor.successors = successor :: predecessor.successors
        }

        successor.predecessors = predecessor :: successor.predecessors

    }

    /**
     * Constructs a ControlFlowGraph-object from a Method-object
     *
     * @param method: The Method-object the graph is going to be based on.
     */
    def apply(method: Method): ControlFlowGraph = {

        val code: Code = method.body.get

        val instructions: Array[Instruction] = code.instructions

        val codeLength: Int = instructions.length

        // This array maps program counters to the BasicBlocks they are contained within
        val BlocksByPC = new Array[BasicBlock](codeLength + 1)

        val catchBlocks: mutable.Map[ExceptionHandler, CatchBlock] =
            new mutable.AnyRefMap[ExceptionHandler, CatchBlock](code.exceptionHandlers.size)

        // Will hold all BasicBlocks ending with a return- or athrow-instruction at
        // their end
        var endBlocks: mutable.Set[BasicBlock] = mutable.Set.empty[BasicBlock]

        val startBlock = new StartBlock

        val exitBlock = new ExitBlock

        var currentBlock: BasicBlock = new BasicBlock(0)

        connectBlocks(startBlock, currentBlock)

        BlocksByPC(0) = currentBlock

        var nextPC: PC = 0;

        // Some utility-methods

        def previousPCof(pc: PC) = code.pcOfPreviousInstruction(pc)

        def isControlTransfer(pc: PC): Boolean =
            instructions(pc).isInstanceOf[ControlTransferInstruction]

        def isBeginningOfBlock(pc: PC): Boolean = (pc < codeLength) &&
            (if (BlocksByPC(pc) == null) false else (BlocksByPC(pc).startPC == pc))

        def isReturnFromMethod(pc: PC): Boolean =
            instructions(pc).isInstanceOf[ReturnInstruction] || instructions(pc) == ATHROW

        /*
         * Whenever there are loops in the bytecode, there is a chance,
         * that the jump-target of the looping instruction, e.g. a goto, is not the
         * entry-point of a previously finished BasicBlock, but right in it's middle.
         * 
         * To keep our notion of BasicBlocks consistent (only one entry-point),
         * we need to 'split' the Block containing the target PC.
         *  
         * To do this, we set the endPC of the block we want to split to the PC,
         * that directly precedes the jump-target. We then create a new BasicBlock,
         * which has the jump-target as it's startPC. It's endPC will be the former
         * endPC of the block that we split.
         * 
         * We then reset the edges in the graph accordingly.
         * 
         * The new BasicBlock is then 'emited', i.e. returned, for further processing
         * by the main-algorithm.
         * 
         * --Parameters--
         * 
         * blockToBeSplit: The BasicBlock we want to split up into two.
         * 
         * incomingEdge: The BasicBlock, which has the jump-instruction at it's end,
         * that caused the split
         * 
         * incomingEdgePC: The program counter at which blockToBeSplit is to be split up.
         */
        def split(
            blockToBeSplit: BasicBlock,
            incomingEdge: BasicBlock,
            incomingEdgePC: PC): BasicBlock = {

            val newBlock = new BasicBlock(incomingEdgePC)
            newBlock.endPC = blockToBeSplit.endPC
            newBlock.successors = blockToBeSplit.successors
            newBlock.catchBlockSuccessors = blockToBeSplit.catchBlockSuccessors
            newBlock.predecessors = List(blockToBeSplit)

            for (successor ← newBlock.successors) {
                val oldBlock = blockToBeSplit
                successor.predecessors =
                    successor.predecessors.map {
                        case `oldBlock` ⇒ newBlock
                        case aBlock     ⇒ aBlock
                    }
            }

            blockToBeSplit.successors = List(newBlock)
            blockToBeSplit.endPC = previousPCof(incomingEdgePC)

            blockToBeSplit.catchBlockSuccessors = Nil

            newBlock
        }

        /*
         * Initialize all CatchBlocks and mark the handlingPCs as entry-points of BasicBlocks
         */
        for (handler ← code.exceptionHandlers) {
            val handlerPC: PC = handler.handlerPC

            BlocksByPC(handlerPC) =
                if (!isBeginningOfBlock(handlerPC)) new BasicBlock(handlerPC)
                else BlocksByPC(handlerPC)

            val catchBlock: CatchBlock = new CatchBlock(handler)

            catchBlocks += ((handler, catchBlock))
        }

        /*
         * The main loop.
         * Iterate over all instructions one after another and add them to a BasicBlock,
         * by setting it's endPC
         */
        for (currentPC ← code.programCounters.drop(1)) {

            val currentInstruction = instructions(currentPC)

            // This method is only defined for bytecode generated by javac 1.4.2 and above
            if (currentInstruction.isInstanceOf[JSR] ||
                currentInstruction.isInstanceOf[RET])
                throw new BytecodeProcessingFailedException("JSR/RET not yet supported")

            // Various properties of currentPC used in the future
            val isExitPointOfCurrentBlock: Boolean = isControlTransfer(currentPC)
            val isEntryPointOfNewBlock: Boolean = isBeginningOfBlock(currentPC)
            val isExitPointOfMethod: Boolean = isReturnFromMethod(currentPC)

            nextPC = code.pcOfNextInstruction(currentPC)

            // The trivial case
            if (!isExitPointOfCurrentBlock && !isEntryPointOfNewBlock && !isExitPointOfMethod) {

                currentBlock.setEndPC(currentPC)

                BlocksByPC(currentPC) = currentBlock
            } else {

                /*
                * If we have encountered the entry point of a new BasicBlock, we will
                * connect the currentBlock to it...
                */
                if (isEntryPointOfNewBlock) {

                    val nextBlock = BlocksByPC(currentPC)

                    /*
                    * ...unless the previous instruction was a Return-
                    * or a Control Transfer-Instruction, in which case the proper connection of Blocks
                    * has been already taken care of in the previous iteration of the main loop
                    */
                    if (!isControlTransfer(previousPCof(currentPC))
                        && !isReturnFromMethod(previousPCof(currentPC))) {

                        connectBlocks(currentBlock, nextBlock)
                    }

                    currentBlock = nextBlock
                }

                if (isExitPointOfCurrentBlock) {

                    if (!isEntryPointOfNewBlock) {
                        currentBlock.setEndPC(currentPC)
                    }

                    BlocksByPC(currentPC) = currentBlock

                    /*
                    * If the current instruction is an Unconditional Jump and
                    * the following pc (nextPC) has not yet been marked as the start
                    * of a new BasicBlock, we will do so now
                    */
                    (currentInstruction.opcode: @scala.annotation.switch) match {

                        case GOTO.opcode | GOTO_W.opcode ⇒
                            if (!isBeginningOfBlock(nextPC) && (nextPC < BlocksByPC.length)) {
                                val nextBlock = new BasicBlock((nextPC))
                                BlocksByPC(nextPC) = nextBlock
                            }
                        case _ ⇒ {}
                    }

                    /*
                     * Now we will work through all the possible jump targets of our current instruction
                     */
                    for (jumpTarget ← currentInstruction.nextInstructions(currentPC, code)) {

                        /*
                         * First we check through jump targets located at previous
                         * program counters.
                         */
                        if (jumpTarget < currentPC) {

                            /*
                             * The jump target is in the middle of a BasicBlock.
                             * Said block needs to be split up.
                             */
                            if (!isBeginningOfBlock(jumpTarget)) {

                                val blockToBeSplit: BasicBlock = BlocksByPC(jumpTarget)

                                BlocksByPC(jumpTarget) =
                                    split(blockToBeSplit, currentBlock, jumpTarget)

                                // associate PCs with the new BasicBlock
                                BlocksByPC(jumpTarget).foreach(instructionPC ⇒ {
                                    BlocksByPC(instructionPC) = BlocksByPC(jumpTarget)
                                })(code)

                                // If currentBlock itself was split, set it to be the
                                // new BasicBlock resulting from the split
                                if (currentBlock == (blockToBeSplit)) {
                                    currentBlock = BlocksByPC(jumpTarget)
                                }

                                connectBlocks(currentBlock, BlocksByPC(jumpTarget))
                            } else {
                                connectBlocks(currentBlock, BlocksByPC(jumpTarget))
                            }
                        } else {

                            /*
                            * Otherwise, we connect to the block associated with the target pc, unless there isn't one yet,
                            * in which case we will initialize it now.
                            */
                            if (isBeginningOfBlock(jumpTarget)) {
                                connectBlocks(currentBlock, BlocksByPC(jumpTarget))
                            } else {
                                BlocksByPC(jumpTarget) = new BasicBlock(jumpTarget)
                                connectBlocks(currentBlock, BlocksByPC(jumpTarget))
                            }
                        }
                    }
                }

                /*
                * If we have encountered a Return Instruction, it will be added to
                * the current block, which will then be added to the Set of the
                * program's exit points.
                */
                if (isExitPointOfMethod) {

                    if (!isEntryPointOfNewBlock) {
                        currentBlock.setEndPC(currentPC)
                        BlocksByPC(currentPC) = currentBlock
                    }

                    if (nextPC < codeLength && nextPC >= 0)
                        if (!isBeginningOfBlock(nextPC))
                            BlocksByPC(nextPC) = new BasicBlock(nextPC)

                    endBlocks = endBlocks + currentBlock

                }
            }

            /*
            * Finally, we check whether the current Instruction throws any exceptions
            * and whether or not it is associated with an appropriate exception- or
            * finally-handler.
            * If so, or if it is an Invocation of another Method, the current block
            * will connected to the CatchBlock associated with
            * the handler.
            */

            // Exceptions potentially thrown by the current instruction
            val jvmExceptions = currentInstruction.jvmExceptions

            /*
             * Utility-Method
             * 
             * If the instruction at currentPC throws an exception, which is to be
             * handled by the given handler, we consider it to be an exit point of
             * the currentBlock. I.e. we mark nextPC as the entry-point of another
             * BasicBlock and connect currentBlock with it as well as
             * the catchBlock associated with the handler. The catchBlock is then
             * connected with the BasicBlock starting at it's handlerPC
             */
            def addExceptionalControlFlowEdgesToGraph(handler: ExceptionHandler): Unit = {

                BlocksByPC(nextPC) =
                    if (!isBeginningOfBlock(nextPC))
                        new BasicBlock(nextPC)
                    else
                        BlocksByPC(nextPC)

                val catchBlock = catchBlocks(handler)
                val handlerBlock = BlocksByPC(handler.handlerPC)

                connectBlocks(currentBlock, catchBlock)

                // Prevents adding the same edge multiple times
                if (catchBlock.predecessors.tail.isEmpty)
                    connectBlocks(catchBlock, handlerBlock)
            }

            if (jvmExceptions.nonEmpty) {

                val isInvocationOfMethod: Boolean = currentInstruction.isInstanceOf[InvocationInstruction]

                for (handler ← code.exceptionHandlersFor(currentPC)) {

                    val catchType = handler.catchType.get

                    if (jvmExceptions.contains(catchType) || isInvocationOfMethod) {

                        addExceptionalControlFlowEdgesToGraph(handler)
                    }
                }

                val throwsUnhandledExceptions: Boolean =
                    currentBlock.catchBlockSuccessors.size < jvmExceptions.size

                if (throwsUnhandledExceptions || isInvocationOfMethod) {

                    // Repeat the above for finally-handlers
                    for {
                        handler ← code.handlersFor(currentPC)
                        if (handler.catchType.isEmpty)
                    } {
                        addExceptionalControlFlowEdgesToGraph(handler)
                    }
                }
            }
        }

        /*
        * Connect all BasicBlocks from the exit point -set to
        * the ExitBlock
        */
        for (block ← endBlocks) {
            connectBlocks(block, exitBlock)
        }

        new ControlFlowGraph(method, startBlock, exitBlock, BlocksByPC, catchBlocks)
    }

}

/**
 * Represents a control flow graph in it's entirety.
 *
 * Also provides methods for working with and analyzing the CFG
 *
 * ==Thread-Safety==
 *
 * This class is technically not thread-safe, due to the blocksByPC-array.
 * However, this array is not supposed to be altered by the user.
 *
 * ==Parameters==
 *
 * @param method The method based on which the CFG was build.
 * @param startBlock The entry-point of the graph
 * @param endBlock The exit-point of the graph
 * @param blocksByPC An array which maps program counters to the BasicBlocks they are
 *  contained within
 * @param catchBlocks Maps exception-handlers to their catchBlocks
 *
 */
case class ControlFlowGraph(
        method: Method,
        startBlock: StartBlock,
        endBlock: ExitBlock,
        blocksByPC: Array[BasicBlock],
        catchBlocks: scala.collection.Map[ExceptionHandler, CatchBlock]) {

    /**
     * Recursively traverses the entire graph in a depth-first fashion from the startBlock
     * onwards, and returns an immutable set of all reachable CFGBlocks of the CFG
     */
    def allBlocks: scala.collection.Set[CFGBlock] = startBlock.returnAllDescendants(new HashSet[CFGBlock])

    /**
     * Returns a representation of the graph in the DOT-format
     */
    def toDot: String = {

        var dotString: String = "digraph{\ncompound=true;\n"

        for (block ← allBlocks) {
            dotString += block.toDot(method.body.get)
        }

        dotString = dotString+"}"

        dotString
    }

    /**
     * Determines and returns the set of CFGBlocks that are dominated by a given node.
     *
     * Example:
     * Given a graph with blocks A, B, C, D, E and F, with the edges:
     *
     * A->B;
     * A->F;
     * B->C;
     * B->D;
     * C->E;
     * D->E;
     * E->F;
     *
     * In this Scenario, blocksDominatedBy(B), will yield {B,C,D,E}.
     * The method called for B and C will only contain B and C themselves, respectively.
     *
     * @param dominator The CFGBlock for which the domination-set is to be computed.
     */
    def blocksDominatedBy(dominator: CFGBlock): Set[CFGBlock] = {

        var result = dominator.returnAllDescendants(new HashSet[CFGBlock])

        var hasChanged: Boolean = true

        /*
         * In each Iteration:
         * 
         * Remove all blocks who have a predecessor, that is not contained in results.
         * Also remove all of their immediate successors.
         * 
         * Excempt from removal is dominator itself.
         */
        while (hasChanged) {

            hasChanged = false

            for (block ← result if (block ne dominator)) {
                if (block.predecessors.exists { pred ⇒ !result.contains(pred) }) {
                    result = result - (block)
                    for (succ ← block.successors if (succ ne dominator)) {
                        result = result - (succ)
                    }

                    hasChanged = true
                }
            }

        }
        result
    }

    /**
     * Returns a function-object, which, in turn, returns for every given PC a UShortSet,
     * which contains all of it's corresponding PCs.
     *
     * Two PCs correspond to each other,
     * if they were compiled from the exact same finally-statement,
     * are at the exact same position relative to the finally-codes entry-point
     * and of course, point to the same Instruction or the same kind of Instructions.
     *
     * Finally-Statements are statements in Java, which can be put after a try-statement
     * and zero or more catch-statements and are guaranteed to be executed eventually.
     *
     * Thus, the Java Compiler has to duplicate and inline the code of the
     * finally-statement at all possible exit-points of the preceeding
     * try- and catch-statements.
     */
    def correspondingPCsTo(): PC ⇒ UShortSet = {

        val code: Code = method.body.get

        // The resulting mapping from program counters to the sets of their correspondents
        val correspondencies: mutable.Map[PC, UShortSet] = mutable.Map.empty[PC, UShortSet]

        def instruction(pc: PC): Instruction = code.instructions(pc)

        def nextPCof(pc: PC): PC = code.pcOfNextInstruction(pc)

        /* Mapping for indicies of local variables. Needed for matching Instructions
        * in different locations in the bytecode.
        */
        var correspondingVarIndicies: Array[PC] = Array.fill[Int](code.maxLocals)(-1)

        // Will carry the domination-sets of various blocks.
        var dominationSetOf: Map[CFGBlock, scala.collection.Set[CFGBlock]] =
            Map[CFGBlock, scala.collection.Set[CFGBlock]]()

        /*
         * Compute the dominations-sets of all catch-handlers.
         */
        for (handler ← method.body.get.exceptionHandlers) {
            val dominator = blocksByPC(handler.handlerPC)
            if (!dominationSetOf.contains(dominator)) {
                dominationSetOf = dominationSetOf + ((dominator, blocksDominatedBy(dominator)))
            }
        }

        /*
         * Updates the correspondences-mapping
         * 
         * The mapping must be transitive, so if two corresponding
         * instructions/program counters have been found, the mapping will also
         * be updated for all PCs in their correspondence-sets.
         */
        def associatePCWithOtherPC(mainPC: PC, otherPC: PC): Unit = {

            var newSet = UShortSet(otherPC)

            if (correspondencies contains mainPC) {
                newSet = newSet ++ correspondencies(mainPC)
            }

            if (correspondencies contains otherPC) {
                newSet = newSet ++ correspondencies(otherPC).filter { pc ⇒ pc != mainPC }
            }

            for (pc ← newSet) {
                if (correspondencies contains pc) {
                    correspondencies += ((pc, correspondencies(pc) + (mainPC)))
                }
            }

            correspondencies += ((mainPC, newSet))
        }

        /*
         * Checks whether two instructions correspond to each other or not.
         * 
         * Two Instruction-Objects occuring at corresponding spots in two duplicates
         * of the finally-code can be considered correspondants of each other, if
         *  - They are one and the same, or
         *  - They have the same op-code, or
         *  - They are both either store- or load-instructions and there is a mapping
         *    between the local variables they access, which is consisten throughout
         *    both of the finally-code-duplicates.
         */
        def correspondingInstructions(insta: Instruction, instb: Instruction): Boolean = {
            if (insta eq instb)
                true
            else if (insta.opcode == instb.opcode)
                true
            else if (insta.isInstanceOf[StoreLocalVariableInstruction]
                && instb.isInstanceOf[StoreLocalVariableInstruction]) {

                val storea = insta.asInstanceOf[StoreLocalVariableInstruction]
                val storeb = instb.asInstanceOf[StoreLocalVariableInstruction]

                val lvIndexA = storea.lvIndex
                val lvIndexB = storeb.lvIndex

                if (correspondingVarIndicies(lvIndexA) == -1
                    && correspondingVarIndicies(lvIndexB) == -1) {

                    correspondingVarIndicies(lvIndexA) = lvIndexB
                    correspondingVarIndicies(lvIndexB) = lvIndexA

                    true

                } else
                    (correspondingVarIndicies(lvIndexA) == lvIndexB
                        && correspondingVarIndicies(lvIndexB) == lvIndexA)

            } else if (insta.isInstanceOf[LoadLocalVariableInstruction]
                && instb.isInstanceOf[LoadLocalVariableInstruction]) {

                val loada = insta.asInstanceOf[LoadLocalVariableInstruction]
                val loadb = instb.asInstanceOf[LoadLocalVariableInstruction]

                val lvIndexA = loada.lvIndex
                val lvIndexB = loadb.lvIndex

                if (correspondingVarIndicies(lvIndexA) == -1
                    && correspondingVarIndicies(lvIndexB) == -1) {

                    correspondingVarIndicies(lvIndexA) = lvIndexB
                    correspondingVarIndicies(lvIndexB) = lvIndexA

                    true

                } else
                    (correspondingVarIndicies(lvIndexA) == lvIndexB
                        && correspondingVarIndicies(lvIndexB) == lvIndexA)

            } else
                false
        }

        /*
         * This will iterate through a section of bytecode, specified by startPC and endPC,
         * and return a List of all jump-targets of all control transfer instructions,
         * that come after endPC.
         */
        def getTargetPointsOfSection(startPC: PC, endPC: PC): List[PC] = {

            var result: List[PC] = Nil
            var pc = startPC

            while (pc <= endPC) {

                instruction(pc) match {

                    case ubi: UnconditionalBranchInstruction ⇒
                        if (pc + ubi.branchoffset > endPC)
                            result = result :+ (pc + ubi.branchoffset)

                    case scbi: SimpleConditionalBranchInstruction ⇒
                        if (pc + scbi.branchoffset > endPC)
                            result = result :+ (pc + scbi.branchoffset)

                    case _ ⇒ {}
                }

                pc = nextPCof(pc)
            }

            result
        }

        /*
         * This map takes the handlerPC of a finally-handler as key and returns the set
         * of all starting PCs of duplicates of the finally-code as value
         * 
         * It will be gradually build by the loop below
         */
        var finallyCodeStartPCs: HashMap[PC, UShortSet] = new HashMap[PC, UShortSet]

        // Used to update finallyCodeStartPCs
        def addFinallyCodeStartPC(handlerPC: PC, pc: PC): Unit = {

            val uss: UShortSet = finallyCodeStartPCs(handlerPC) + ((pc))

            finallyCodeStartPCs = finallyCodeStartPCs + ((handlerPC, uss))
        }

        // We iterate over all exception handlers with catch-type 'None' (finally-handlers).
        // We will mark all occurences of finally-code, by computing their starting pc and then putting them into
        // the finallyCodeStartPCs-Set
        for {
            handler ← code.exceptionHandlers
            if handler.catchType.isEmpty
        } {
            val startPC = handler.startPC
            val endPC = handler.endPC
            val handlerPC = handler.handlerPC

            val firstPCToBeAdded: PC =
                ((if (instruction(handlerPC).isInstanceOf[AStoreInstruction]) nextPCof(handlerPC)
                else handlerPC))

            // Naturally, we add the handlerPC first, or rather: it's direct, non-AStore - successor in the code-array
            if (!finallyCodeStartPCs.contains(handlerPC))
                finallyCodeStartPCs = finallyCodeStartPCs + ((handlerPC, UShortSet(firstPCToBeAdded)))

            /*
            * We also mark all jump-targets within the handled range
            */
            for (pc ← getTargetPointsOfSection(startPC, endPC))
                addFinallyCodeStartPC(handlerPC, pc)

            /*
            * If the finally-code starts directly after the handled range of instructions,
            * the last Instruction of the range must be either a GOTO or GOTO_W to the 
            * 'regular' finally-code. To find said GOTO(_W) statement we thus have to
            * subtract 3 or 5 from the handlerPC.
            * 
            * The jump target is consequentely marked.
            */
            if (endPC == handlerPC) {

                val previousPC: PC = code.pcOfPreviousInstruction(endPC)

                instruction(previousPC) match {
                    case ubi: UnconditionalBranchInstruction ⇒
                        addFinallyCodeStartPC(handlerPC, (previousPC + ubi.branchoffset))
                    case _ ⇒ {}
                }
            }

            /*
            * Otherwise, we just mark the endPC of our Handler.
            * If it is dominated by a catchBlock, i.e. only reachable when an exception
            * is thrown, we check the handler of the catchBlock for it's endPC.
            * 
            * It will be a duplicate of the finally-code or a jump to such a duplicate.
            */
            if (endPC < handlerPC) {
                addFinallyCodeStartPC(handlerPC, endPC)

                for {
                    pair @ (dominator, dominationSet) ← dominationSetOf
                    if (dominator.predecessors.nonEmpty)
                } {

                    if (dominationSet.contains(blocksByPC(endPC))) {

                        val dominatingCatchBlock: CatchBlock =
                            dominator.predecessors(0).asInstanceOf[CatchBlock]

                        if (dominatingCatchBlock.handler.catchType.nonEmpty) {

                            val otherBlock =
                                blocksByPC(dominatingCatchBlock.endPC).asInstanceOf[BasicBlock]

                            var pcToBeAdded = otherBlock.endPC

                            instruction(otherBlock.endPC) match {
                                case ubi: UnconditionalBranchInstruction ⇒ {
                                    pcToBeAdded = otherBlock.endPC + ubi.branchoffset
                                }
                                case _ ⇒ {}
                            }

                            addFinallyCodeStartPC(handlerPC, pcToBeAdded)
                        }
                    }
                }
            }
        }

        /*
         * Utility-method
         * 
         * Converts a UShortSet into an Array
         */
        def UShortSetToArray(uset: UShortSet): Array[PC] = {
            val result: Array[PC] = new Array[PC](uset.size)
            var index: Int = 0

            for (pc ← uset) {
                result(index) = pc
                index += 1
            }

            result
        }

        /*
         * For every set from finallyCodeStartPCs, we iterate over
         * each *unique* pair of program counters, and start matching the
         * sub-CFGs from there.
         */
        for {
            fcsp ← finallyCodeStartPCs.values
            fcspArray = UShortSetToArray(fcsp)
            fcspMaxIndex = fcspArray.length - 1
            leftPCIndex ← 0 to fcspMaxIndex - 1
            rightPCIndex ← leftPCIndex + 1 to fcspMaxIndex
        } {
            /*
            * We will call one of the finally-code-subgraphs the 'left' and
            * the other the 'right' side.
            * We take their initial pcs from the array
            */
            var leftPC = fcspArray(leftPCIndex)
            var rightPC = fcspArray(rightPCIndex)

            /*
            * Consequently, we mark the first blocks of either side/subgraph
            */
            val initialLeftBlock: BasicBlock = blocksByPC(leftPC)
            val initialRightBlock: BasicBlock = blocksByPC(rightPC)

            //will contain all BasicBlocks, dominated by initialLeftBlock
            val dominationSetLeft: Set[CFGBlock] = blocksDominatedBy(initialLeftBlock)

            //will contain all BasicBlocks, dominated by initialRightBlock
            val dominationSetRight: Set[CFGBlock] = blocksDominatedBy(initialRightBlock)

            //all BasicBlocks, that have already been processed. 
            //Prevents infinite looping
            var visited: Set[BasicBlock] = HashSet()

            // Workqueues for iterative, breadth-first (sub-)graph-traversal, 
            //one for Blocks on the left, the other for Blocks the right side
            val workqueueLeft: mutable.Queue[BasicBlock] = mutable.Queue(initialLeftBlock)

            val workqueueRight: mutable.Queue[BasicBlock] = mutable.Queue(initialRightBlock)

            /*
            * Loops over both queues in parallel
            */
            while (!workqueueLeft.isEmpty) {

                val bbleft = workqueueLeft.dequeue()
                val bbright = workqueueRight.dequeue()

                /*
                 * The end of try-, catch- or finally-blocks in Java-source-code are NOT
                 * necessarily congruent with the end- and start-points of BasicBlocks
                 * on the bytecode-level.
                 * 
                 * Thus, if we start our matching-process with the initial BasicBlocks,
                 * we take leftPC and rightPC directly from the fcspArray.
                 * 
                * If we are not at the start of the pairwise comparison of
                * the subgraphs/sides anymore, we will start from each of 
                * the blocks respective startPCs
                */
                if (bbleft ne initialLeftBlock) {
                    leftPC = bbleft.startPC
                    rightPC = bbright.startPC
                }

                /*
                * Pairwise comparison and checking of correspondance of all the Instructions
                * - represented by their pcs - in the right and left block.
                * Builds the correspondance-map, as long as the Instructions also
                * correspond.
                */
                while (leftPC <= bbleft.endPC && rightPC <= bbright.endPC &&
                    correspondingInstructions(instruction(leftPC), instruction(rightPC))) {

                    associatePCWithOtherPC(leftPC, rightPC)
                    associatePCWithOtherPC(rightPC, leftPC)

                    leftPC = nextPCof(leftPC)
                    rightPC = nextPCof(rightPC)
                }

                visited = visited + (bbleft, bbright)

                /*
                * The successors of the left and right block, that have to potentially be 
                * compared in later loop iterations
                * 
                * The successors which are catchBlocks need to be mapped to the
                * BasicBlocks they are connected to.
                */
                val leftsuccessors = (bbleft.successors.filter { bb ⇒ bb.isInstanceOf[BasicBlock] }
                    ++ bbleft.catchBlockSuccessors.map { cb: CatchBlock ⇒ cb.successors(0) }).asInstanceOf[List[BasicBlock]]

                val rightsuccessors = (bbright.successors.filter { bb ⇒ bb.isInstanceOf[BasicBlock] }
                    ++ bbright.catchBlockSuccessors.map { cb: CatchBlock ⇒ cb.successors(0) }).asInstanceOf[List[BasicBlock]]

                /*
                * We iterate over both successor-lists in parallel, in order to see,
                * which have to be processed further, i.e. enqueued in the workqueues
                * 
                * If both lists are of different size, we know that we have reached the
                * end of the respective subgraphs
                * 
                */
                if (leftsuccessors.size == rightsuccessors.size) {

                    var index: Int = 0

                    while (index < leftsuccessors.size) {

                        val leftsucc = leftsuccessors(index)
                        val rightsucc = rightsuccessors(index)

                        /*
                        * leftsucc and rightsucc are only to be enqueued for further processing,
                        * if they are dominated by the initial left and right blocks,
                        * respectively (ergo: belong to the finally code),
                        * and have not already been visited/processed.
                        */
                        val isToBeProcessedFurther: Boolean =
                            leftsucc.predecessors.filter { block ⇒ !block.successors.contains(leftsucc) }.forall { block ⇒
                                {
                                    block match {
                                        case bb: BasicBlock ⇒
                                            (dominationSetLeft contains bb)
                                        case _ ⇒ false
                                    }
                                }
                            } && rightsucc.predecessors.filter { block ⇒ !block.successors.contains(rightsucc) }.forall { block ⇒
                                {
                                    block match {
                                        case bb: BasicBlock ⇒
                                            (dominationSetRight contains bb)
                                        case _ ⇒ false
                                    }
                                }
                            } && (leftsucc ne rightsucc) && !(visited contains leftsucc) && !(visited contains rightsucc)

                        if (isToBeProcessedFurther) {
                            workqueueLeft.enqueue(leftsucc)
                            workqueueRight.enqueue(rightsucc)
                        }

                        index += 1
                    }
                }
            }

            /*
            * Before the next two subgraphs are compared, the correspondences of
            * the local variable indicies have to be reset.
            */
            correspondingVarIndicies = Array.fill[Int](code.maxLocals)(-1)

        }

        // The function-object that will be returned to the caller        
        def resultingFunction(pc: PC): UShortSet =
            if (correspondencies contains pc) correspondencies(pc) else UShortSet.empty

        resultingFunction

    }

}