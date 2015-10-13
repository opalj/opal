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

import scala.collection.{ Set ⇒ SomeSet }
import scala.collection.immutable.HashSet
import scala.collection.immutable.HashMap
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.instructions.JSRInstruction
import org.opalj.br.instructions.UnconditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.TABLESWITCH
import org.opalj.br.instructions.LOOKUPSWITCH
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.JSR
import org.opalj.br.instructions.JSR_W
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.GOTO
import org.opalj.br.instructions.GOTO_W
import org.opalj.br.PC
import org.opalj.br.analyses.ClassHierarchy
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.ObjectType

/**
 * A factory for computing control flow graphs for methods.
 *
 * @author Michael Eichberg
 */
object CFGFactory {

    /**
     * Constructs the control flow graph for a given method.
     *
     * @param method A method with a body (i.e., with some code.)
     */
    def apply(
        method: Method,
        classHierarchy: ClassHierarchy = Code.preDefinedClassHierarchy): CFG = {

        import classHierarchy.isSubtypeOf

        val code = method.body.get
        val instructions = code.instructions
        val codeSize = instructions.length

        val normalReturnNode = new ExitNode(normalReturn = true)
        val abnormalReturnNode = new ExitNode(normalReturn = false)

        // 1. basic initialization
        val bbs = new Array[BasicBlock](codeSize)

        var exceptionHandlers = Map.empty[ExceptionHandler, CatchNode]

        for (exceptionHandler ← code.exceptionHandlers) {
            val catchNode = new CatchNode(exceptionHandler)
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
        var previousPC = 0
        var subroutineReturnPCs = collection.immutable.Map.empty[PC, UShortSet]
        code.foreach { (pc, instruction) ⇒
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

            def connect(sourceBB: BasicBlock, targetBBStartPC: PC): BasicBlock = {
                // We ensure that the basic block associated with the PC `targetBBStartPC`
                // actually starts with the given PC.
                var targetBB = bbs(targetBBStartPC)
                if (targetBB eq null) {
                    targetBB = new BasicBlock(targetBBStartPC)
                    targetBB.setPredecessors(Set(sourceBB))
                    sourceBB.addSuccessor(targetBB)
                    bbs(targetBBStartPC) = targetBB
                } else if (targetBB.startPC < targetBBStartPC) {
                    // we have to split the basic block...
                    val newTargetBB = new BasicBlock(targetBBStartPC)
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
                    targetBB.successors.foreach { targetBBsuccessorBB ⇒
                        targetBBsuccessorBB.updatePredecessor(oldBB = targetBB, newBB = newTargetBB)
                    }
                    newTargetBB.setPredecessors(Set(sourceBB, targetBB))
                    targetBB.setSuccessors(Set(newTargetBB))
                    sourceBB.addSuccessor(newTargetBB)
                } else {
                    assert(
                        targetBB.startPC == targetBBStartPC,
                        s"targetBB's startPC ${targetBB.startPC} does not equal $pc")

                    sourceBB.addSuccessor(targetBB)
                    targetBB.addPredecessor(sourceBB)
                }
                targetBB
            }

            (instruction.opcode: @scala.annotation.switch) match {

                case RET.opcode ⇒
                    // we cannot determine the targets at the moment.
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    runningBB = null // <=> the next instruction gets a new bb
                case JSR.opcode | JSR_W.opcode ⇒
                    val jsrInstr = instruction.asInstanceOf[JSRInstruction]
                    val subroutinePC = pc + jsrInstr.branchoffset
                    val thisSubroutineReturnPCs = subroutineReturnPCs.getOrElse(subroutinePC, UShortSet.empty)
                    subroutineReturnPCs += (
                        subroutinePC ->
                        (jsrInstr.indexOfNextInstruction(pc) +≈: thisSubroutineReturnPCs)
                    )
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    /*val subroutineBB = */ connect(currentBB, subroutinePC)
                    runningBB = null // <=> the next instruction gets a new bb

                case ATHROW.opcode ⇒
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    // We typically don't know anything about the current exception;
                    // hence, we connect this bb with every exception handler in place.
                    var isHandled: Boolean = false
                    val catchNodeSuccessors =
                        code.exceptionHandlersFor(pc).map { eh ⇒
                            isHandled =
                                isHandled ||
                                    eh.catchType.isEmpty ||
                                    eh.catchType.get == ObjectType.Throwable
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

                case GOTO.opcode | GOTO_W.opcode ⇒
                    // GOTO WILL NEVER THROW AN EXCEPTION
                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    val GOTO = instruction.asInstanceOf[UnconditionalBranchInstruction]
                    val targetPC = pc + GOTO.branchoffset
                    connect(currentBB, targetPC)
                    runningBB = null

                case /*IFs:*/ 165 | 166 | 198 | 199 |
                    159 | 160 | 161 | 162 | 163 | 164 |
                    153 | 154 | 155 | 156 | 157 | 158 ⇒
                    val IF = instruction.asInstanceOf[SimpleConditionalBranchInstruction]

                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    // jump
                    val targetPC = pc + IF.branchoffset
                    connect(currentBB, targetPC)
                    // fall through case
                    runningBB = connect(currentBB, code.pcOfNextInstruction(pc))

                case TABLESWITCH.opcode | LOOKUPSWITCH.opcode ⇒
                    val SWITCH = instruction.asInstanceOf[CompoundConditionalBranchInstruction]

                    val currentBB = useRunningBB()
                    currentBB.endPC = pc
                    connect(currentBB, pc + SWITCH.defaultOffset)
                    SWITCH.jumpOffsets.foreach { offset ⇒ connect(currentBB, pc + offset) }
                    runningBB = null

                case /*xReturn:*/ 176 | 175 | 174 | 172 | 173 | 177 ⇒
                    val currentBB = useRunningBB
                    currentBB.endPC = pc
                    currentBB.addSuccessor(normalReturnNode)
                    normalReturnNode.addPredecessor(currentBB)
                    runningBB = null

                case _ /*ALL STANDARD INSTRUCTIONS THAT EITHER FALL THROUGH OR THROW A (JVM-BASED) EXCEPTION*/ ⇒

                    val currentBB = useRunningBB
                    val jvmExceptions = instruction.jvmExceptions
                    val isMethodInvoke = instruction.isInstanceOf[MethodInvocationInstruction]
                    if (jvmExceptions.nonEmpty || isMethodInvoke) {
                        def linkWithExceptionHandler(eh: ExceptionHandler): Unit = {
                            val catchNode = exceptionHandlers(eh)
                            currentBB.addSuccessor(catchNode)
                            catchNode.addPredecessor(currentBB)
                        }

                        val exceptionsToHandle: Iterable[ObjectType] =
                            if (isMethodInvoke) {
                                val caughtExceptions = code.handlersFor(pc).filter(_.catchType.isDefined).map(_.catchType.get).toList
                                //[DEBUG] println(s"$pc[caught]: "+caughtExceptions.mkString(","))
                                if (!caughtExceptions.exists(_ eq ObjectType.Throwable)) {
                                    // We add "Throwable" to make sure that - if any exception
                                    // occurs - we never miss an edge. Actually, we have
                                    // no idea which exceptions may be thrown.
                                    val allExceptions = caughtExceptions ++ Iterable(ObjectType.Throwable)
                                    //[DEBUG] println(s"$pc[all]: "+allExceptions.mkString(","))
                                    allExceptions
                                } else {
                                    caughtExceptions
                                }
                            } else {
                                jvmExceptions
                            }
                        //[DEBUG] println(s"$pc[handle]: "+exceptionsToHandle.mkString(","))
                        exceptionsToHandle.foreach { thrownException ⇒
                            val isHandled = code.handlersFor(pc).exists { eh ⇒
                                if (eh.catchType.isEmpty) {
                                    linkWithExceptionHandler(eh)
                                    //[DEBUG] println(s"[$pc] finally:"+jvmException+"   "+eh)
                                    true
                                } else {
                                    val isCaught = isSubtypeOf(thrownException, eh.catchType.get)
                                    //[DEBUG] println(s"[$pc] isCaught:"+isCaught + jvmException+"   "+eh)
                                    if (isCaught.isYes) {
                                        linkWithExceptionHandler(eh)
                                        true
                                    } else if (isCaught.isUnknown) {
                                        linkWithExceptionHandler(eh)
                                        false
                                    } else {
                                        false
                                    }
                                }
                            }
                            if (!isHandled) {
                                // also connect with exit
                                currentBB.addSuccessor(abnormalReturnNode)
                                abnormalReturnNode.addPredecessor(currentBB)
                            }
                        }

                        // this instruction may throw an exception; hence it ends this
                        // basic block
                        currentBB.endPC = pc
                        val nextPC = code.pcOfNextInstruction(pc)
                        runningBB = connect(currentBB, nextPC)
                    }
            }
            previousPC = pc
        }

        if (subroutineReturnPCs.nonEmpty) {
            for ((subroutinePC, returnAddresses) ← subroutineReturnPCs) {
                val returnBBs = returnAddresses.map(bbs(_)).toSet[CFGNode]
                val retBBs = bbs(subroutinePC).reachable(true).filter(bb ⇒ bb.successors.isEmpty && bb.isInstanceOf[BasicBlock])
                retBBs.foreach(_.setSuccessors(returnBBs))
                returnBBs.foreach { returnBB ⇒ returnBB.setPredecessors(retBBs.toSet) }
            }
        }

        CFG(method,
            normalReturnNode, abnormalReturnNode,
            bbs,
            exceptionHandlers.values.toList)
    }
}
