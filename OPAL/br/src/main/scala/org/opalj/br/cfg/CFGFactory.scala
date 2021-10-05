/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cfg

import scala.annotation.switch

import scala.collection.immutable.IntMap
import scala.collection.immutable.HashMap

import org.opalj.collection.immutable.IntArraySet
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.JSRInstruction
import org.opalj.br.instructions.UnconditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.TABLESWITCH
import org.opalj.br.instructions.LOOKUPSWITCH
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.JSR
import org.opalj.br.instructions.JSR_W
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.GOTO
import org.opalj.br.instructions.GOTO_W
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEDYNAMIC

/**
 * A factory for computing control flow graphs for methods.
 *
 * @author Michael Eichberg
 */
object CFGFactory {

    def apply(method: Method, classHierarchy: ClassHierarchy): Option[CFG[Instruction, Code]] = {
        method.body.map(code => apply(code, classHierarchy))
    }

    /**
     * Constructs the control flow graph for a given method.
     *
     * The constructed [[CFG]] basically consists of the code's basic blocks. Additionally,
     * two artifical exit nodes are added.
     * One artificial exit node is added to facilitate the navigation to all normal
     * return instructions. A second artificial node is added that enables the navigation
     * to all instructions that led to an abnormal return. Exception handlers are
     * directly added to the graph using [[CatchNode]]s. Each exception handler is
     * associated with exactly one [[CatchNode]] and all instructions that may throw
     * a corresponding exception will have the respective [[CatchNode]] as a successor.
     *
     * @note  The algorithm supports all Java bytecode instructions - in particular JSR/RET.
     *
     * @note  The code is parsed linearly and the graph is therefore constructed implicitly.
     *        Hence, it is possible that the graph contains nodes that cannot be reached from
     *        the start node.
     *
     * @param code A method's body (i.e., the code.)
     * @param classHierarchy The class hierarchy that will be used to determine
     *      if a certain exception is potentially handled by an exception handler.
     */
    def apply(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): CFG[Instruction, Code] = {

        /*
         * The basic idea of the algorithm is to create the cfg using a single sweep over
         * the instructions and while doing so to determine the basic block boundaries. Here,
         * the idea is that the current basic block is extended to also capture the current
         * instruction unless the previous instruction ended the basic block.
         */

        import classHierarchy.isASubtypeOf

        val instructions = code.instructions
        val codeSize = instructions.length

        val normalReturnNode = new ExitNode(normalReturn = true)
        val abnormalReturnNode = new ExitNode(normalReturn = false)

        // 1. basic initialization
        val bbs = new Array[BasicBlock](codeSize)
        // BBs is a sparse array; only those fields are used that are related to an instruction

        var exceptionHandlers = HashMap.empty[ExceptionHandler, CatchNode]
        for ((exceptionHandler, index) <- code.exceptionHandlers.iterator.zipWithIndex) {
            val catchNode = new CatchNode(exceptionHandler, index)
            exceptionHandlers += (exceptionHandler -> catchNode)
            val handlerPC = exceptionHandler.handlerPC
            var handlerBB = bbs(handlerPC)
            if (handlerBB eq null) {
                handlerBB = new BasicBlock(handlerPC)
                handlerBB.setPredecessors(Set(catchNode))
                bbs(handlerPC) = handlerBB
            } else {
                handlerBB.addPredecessor(catchNode)
            }
            catchNode.setSuccessors(Set(handlerBB))
        }

        // 2. iterate over the code to determine basic block boundaries
        var runningBB: BasicBlock = null
        var previousPC: PC = 0
        var subroutineReturnPCs = IntMap.empty[IntArraySet] // PC => IntArraySet
        code.iterate { (pc, instruction) =>
            if (runningBB eq null) {
                runningBB = bbs(pc)
                if (runningBB eq null)
                    runningBB = new BasicBlock(pc)
            }

            def useRunningBB(): BasicBlock = {
                var currentBB = bbs(pc)
                if (currentBB eq null) {
                    currentBB = runningBB
                    bbs(pc) = currentBB
                } else {
                    // We have hit the beginning of a new basic block;
                    // i.e., this instruction starts a new block.

                    // Let's check if we have to close the previous basic block...
                    if (runningBB ne currentBB) {
                        runningBB.endPC = previousPC
                        runningBB.addSuccessor(currentBB)
                        currentBB.addPredecessor(runningBB)
                        runningBB = currentBB
                    }
                }

                currentBB
            }

            /*
             * Returns the pair consisting of the basic block associated with the current
             * instruction and the BasicBlock create/associated with targetBBStartPC.
             */
            def connect(
                theSourceBB:     BasicBlock,
                targetBBStartPC: PC
            ): ( /*newSourceBB*/ BasicBlock, /*targetBB*/ BasicBlock) = {
                // We ensure that the basic block associated with the PC `targetBBStartPC`
                // actually starts with the given PC.
                val targetBB = bbs(targetBBStartPC)
                if (targetBB eq null) {
                    val newTargetBB = new BasicBlock(targetBBStartPC)
                    newTargetBB.setPredecessors(Set(theSourceBB))
                    theSourceBB.addSuccessor(newTargetBB)
                    bbs(targetBBStartPC) = newTargetBB
                    (theSourceBB, newTargetBB)
                } else if (targetBB.startPC < targetBBStartPC) {
                    // we have to split the basic block...
                    val newTargetBB = new BasicBlock(targetBBStartPC)
                    // if a block has to split itself (due to a back link...)
                    val sourceBB = if (theSourceBB eq targetBB) newTargetBB else theSourceBB
                    newTargetBB.endPC = targetBB.endPC
                    bbs(targetBBStartPC) = newTargetBB
                    // update the bbs associated with the following instruction
                    var nextPC = targetBBStartPC + 1
                    while (nextPC < codeSize) {
                        val nextBB = bbs(nextPC)
                        if (nextBB eq null) {
                            nextPC += 1
                        } else if (nextBB eq targetBB) {
                            bbs(nextPC) = newTargetBB
                            nextPC += 1
                        } else {
                            // we have hit another bb => we're done.
                            nextPC = codeSize
                        }
                    }
                    targetBB.endPC = code.pcOfPreviousInstruction(targetBBStartPC)
                    newTargetBB.setSuccessors(targetBB.successors)
                    targetBB.successors.foreach { targetBBsuccessorBB =>
                        targetBBsuccessorBB.updatePredecessor(oldBB = targetBB, newBB = newTargetBB)
                    }
                    newTargetBB.setPredecessors(Set(sourceBB, targetBB))
                    targetBB.setSuccessors(Set(newTargetBB))
                    sourceBB.addSuccessor(newTargetBB)
                    (sourceBB, newTargetBB)
                } else {
                    assert(
                        targetBB.startPC == targetBBStartPC,
                        s"targetBB's startPC ${targetBB.startPC} does not equal $pc"
                    )

                    theSourceBB.addSuccessor(targetBB)
                    targetBB.addPredecessor(theSourceBB)
                    (theSourceBB, targetBB) // <= return
                }
            }

            def linkWithExceptionHandler(currentBB: BasicBlock, eh: ExceptionHandler): Unit = {
                val catchNode = exceptionHandlers(eh)
                currentBB.addSuccessor(catchNode)
                catchNode.addPredecessor(currentBB)
            }

            def handleExceptions(currentBB: BasicBlock, jvmExceptions: List[ObjectType]): Unit = {
                var areHandled = true
                jvmExceptions.foreach { thrownException =>
                    areHandled &= code.handlersFor(pc).exists { eh =>
                        val catchType = eh.catchType
                        if (catchType.isEmpty) {
                            linkWithExceptionHandler(currentBB, eh)
                            true // also aborts the evaluation
                        } else {
                            val isCaught = isASubtypeOf(thrownException, catchType.get)
                            if (isCaught.isYes) {
                                linkWithExceptionHandler(currentBB, eh)
                                true // also aborts the evaluation
                            } else if (isCaught.isUnknown) {
                                linkWithExceptionHandler(currentBB, eh)
                                false
                            } else {
                                false
                            }
                        }
                    }
                }
                if (!areHandled) {
                    // also connect with exit unless all exceptions are handled
                    currentBB.addSuccessor(abnormalReturnNode)
                    abnormalReturnNode.addPredecessor(currentBB)
                }
            }

            (instruction.opcode: @switch) match {

                case RET.opcode =>
                    // We cannot determine the target instructions at the moment;
                    // we first need to be able to connect the ret instruction with
                    // its jsr instructions.
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    runningBB = null // <=> the next instruction gets a new bb
                case JSR.opcode | JSR_W.opcode =>
                    val jsrInstr = instruction.asInstanceOf[JSRInstruction]
                    val subroutinePC = pc + jsrInstr.branchoffset
                    val thisSubroutineReturnPCs =
                        subroutineReturnPCs.getOrElse(subroutinePC, IntArraySet.empty)
                    subroutineReturnPCs += (
                        subroutinePC ->
                        (thisSubroutineReturnPCs + jsrInstr.indexOfNextInstruction(pc))
                    )
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    /*val subroutineBB = */ connect(currentBB, subroutinePC)
                    runningBB = null // <=> the next instruction gets a new bb

                case ATHROW.opcode =>
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    // We typically don't know anything about the current exception;
                    // hence, we connect this bb with every exception handler in place.
                    var isHandled: Boolean = false
                    val catchNodeSuccessors =
                        code.handlersFor(pc).map { eh =>
                            val catchType = eh.catchType
                            isHandled = isHandled ||
                                catchType.isEmpty || catchType.get == ObjectType.Throwable
                            val catchNode = exceptionHandlers(eh)
                            catchNode.addPredecessor(currentBB)
                            catchNode
                        }.toSet[CFGNode]
                    currentBB.setSuccessors(catchNodeSuccessors)
                    if (!isHandled) {
                        currentBB.addSuccessor(abnormalReturnNode)
                        abnormalReturnNode.addPredecessor(currentBB)
                    }
                    runningBB = null

                case GOTO.opcode | GOTO_W.opcode =>
                    // GOTO WILL NEVER THROW AN EXCEPTION
                    instruction match {
                        case GOTO(3) | GOTO_W(4) =>
                            // THE GOTO INSTRUCTION IS EFFECTIVELY USELESS (A NOP) AS IT IS JUST
                            // A JUMP TO THE NEXT INSTRUCTION; HENCE, WE DO NOT HAVE TO END THE
                            // CURRENT BLOCK.
                            useRunningBB()
                        case _ =>
                            val currentBB = useRunningBB()
                            currentBB.endPC = pc
                            val GOTO = instruction.asInstanceOf[UnconditionalBranchInstruction]
                            connect(currentBB, pc + GOTO.branchoffset)
                            runningBB = null
                    }

                case /*IFs:*/ 165 | 166 | 198 | 199 |
                    159 | 160 | 161 | 162 | 163 | 164 |
                    153 | 154 | 155 | 156 | 157 | 158 =>
                    val IF = instruction.asSimpleConditionalBranchInstruction
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    // jump
                    val (selfBB, _ /*targetBB*/ ) = connect(currentBB, pc + IF.branchoffset)
                    val newCurrentBB = if (selfBB ne currentBB) selfBB else currentBB
                    // fall through case
                    runningBB = connect(newCurrentBB, code.pcOfNextInstruction(pc))._1

                case TABLESWITCH.opcode | LOOKUPSWITCH.opcode =>
                    val SWITCH = instruction.asInstanceOf[CompoundConditionalBranchInstruction]
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    val (selfBB, _ /*targetBB*/ ) = connect(currentBB, pc + SWITCH.defaultOffset)
                    val newCurrentBB = if (selfBB ne currentBB) selfBB else currentBB
                    SWITCH.jumpOffsets.foreach { offset => connect(newCurrentBB, pc + offset) }
                    runningBB = null

                case /*xReturn:*/ 176 | 175 | 174 | 172 | 173 | 177 =>
                    val currentBB = useRunningBB()
                    handleExceptions(currentBB, instruction.jvmExceptions)
                    currentBB.endPC = pc
                    currentBB.addSuccessor(normalReturnNode)
                    normalReturnNode.addPredecessor(currentBB)
                    runningBB = null

                case _ /* INSTRUCTIONS THAT EITHER FALL THROUGH OR THROW A (JVM-BASED) EXCEPTION*/ =>
                    assert(instruction.nextInstructions(pc, regularSuccessorsOnly = true).size == 1)

                    val currentBB = useRunningBB()

                    def endBasicBlock(): Unit = {
                        // this instruction may throw an exception; hence it ends this
                        // basic block
                        currentBB.endPC = pc
                        val nextPC = code.pcOfNextInstruction(pc)
                        runningBB = connect(currentBB, nextPC)._2
                    }

                    instruction.opcode match {
                        case INVOKEDYNAMIC.opcode |
                            INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                            INVOKESTATIC.opcode | INVOKESPECIAL.opcode =>
                            // just treat every exception handler as a potential target
                            val isHandled = code.handlersFor(pc).exists { eh =>
                                if (eh.catchType.isEmpty) {
                                    linkWithExceptionHandler(currentBB, eh)
                                    true // also aborts the evaluation
                                } else {
                                    linkWithExceptionHandler(currentBB, eh)
                                    false
                                }
                            }
                            if (!isHandled) {
                                // also connect with exit unless we found a finally handler
                                currentBB.addSuccessor(abnormalReturnNode)
                                abnormalReturnNode.addPredecessor(currentBB)
                            }
                            endBasicBlock()

                        case _ =>
                            val jvmExceptions = instruction.jvmExceptions
                            handleExceptions(currentBB, jvmExceptions)
                            if (jvmExceptions.nonEmpty) {
                                endBasicBlock()
                            }
                    }
            }
            previousPC = pc
        }

        // Analyze the control flow graphs of all subroutines to connect the ret
        // instructions with their correct target addresses.
        if (subroutineReturnPCs.nonEmpty) {
            subroutineReturnPCs.foreach(subroutine => bbs(subroutine._1).setIsStartOfSubroutine())
            for ((subroutinePC, returnToAddresses) <- subroutineReturnPCs) {
                val returnBBs = returnToAddresses.transform[CFGNode, Set[CFGNode]](
                    ra => bbs(ra),
                    Set.newBuilder[CFGNode]
                )
                val subroutineBB = bbs(subroutinePC)
                val subroutineBBs: List[BasicBlock] = subroutineBB.subroutineFrontier(code, bbs)
                val retBBs = subroutineBBs.toSet[CFGNode]
                retBBs.foreach(_.setSuccessors(returnBBs))
                returnBBs.foreach(_.addPredecessors(retBBs))
            }
        }

        val effectiveExceptionHandlers = exceptionHandlers.values filter { catchNode =>
            catchNode.predecessors.nonEmpty || {
                catchNode.successors foreach { successor => successor.removePredecessor(catchNode) }
                false
            }
        }

        CFG(
            code,
            normalReturnNode, abnormalReturnNode, effectiveExceptionHandlers.toList,
            bbs
        )
    }
}
