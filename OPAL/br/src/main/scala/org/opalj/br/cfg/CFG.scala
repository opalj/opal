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

//import scala.collection.mutable
import scala.collection.{ Set ⇒ SomeSet }
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.Method
import org.opalj.br.PC
//import org.opalj.br.Code
//import org.opalj.br.ExceptionHandler
//import org.opalj.bytecode.BytecodeProcessingFailedException
//import org.opalj.br.instructions.Instruction
//import org.opalj.br.instructions.ControlTransferInstruction
//import org.opalj.br.instructions.ReturnInstruction
//import org.opalj.br.instructions.InvocationInstruction
//import org.opalj.br.instructions.StoreLocalVariableInstruction
//import org.opalj.br.instructions.LoadLocalVariableInstruction
//import org.opalj.br.instructions.UnconditionalBranchInstruction
//import org.opalj.br.instructions.SimpleConditionalBranchInstruction
//import org.opalj.br.instructions.AStoreInstruction
//import org.opalj.br.instructions.ATHROW
//import org.opalj.br.instructions.JSR
//import org.opalj.br.instructions.RET
//import org.opalj.br.instructions.GOTO
//import org.opalj.br.instructions.GOTO_W

/**
 * Represents a control flow graph.
 *
 * ==Thread-Safety==
 * This class is technically not thread-safe, due to the basicBlocks-array.
 * However, this array is not supposed to be altered by the user.
 *
 * @param method The method for which the CFG was build.
 * @param normalReturnNode The unique exit node of the control flow graph if the m
 * 		method returns normally.
 * @param abnormalReturnNode The unique exit node of the control flow graph if the m
 * 		method returns abnormally (throws an exception).
 * @param basicBlocks An implicit map between a program counter and its associated
 * 		[[BasicBlock]].
 * @param catchNodes Maps exception-handlers to their catchNodes
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
case class CFG(
        method: Method,
        normalReturnNode: ExitNode,
        abnormalReturnNode: ExitNode,
        private val basicBlocks: Array[BasicBlock],
        catchNodes: Seq[CatchNode]) {

    final def startBlock: BasicBlock = basicBlocks(0)

    /**
     * Returns the set of all reachable [[CFGNode]]s of the control flow graph.
     */
    lazy val reachableBBs: SomeSet[CFGNode] = basicBlocks(0).reachable(reflexive = true)

    def allBBs: Set[BasicBlock] = basicBlocks.filter(_ ne null).toSet

    def bb(pc: PC): BasicBlock = basicBlocks(pc)

    /**
     * Determines and returns the set of CFGNodes that are dominated by a given node.
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
     * @param dominator The CFGNode for which the domination-set is to be computed.
     */
    def blocksDominatedBy(dominator: CFGNode): SomeSet[CFGNode] = {

        var result = dominator.reachable(reflexive = true)

        var hasChanged: Boolean = true

        /*
         * In each Iteration:
         *
         * Remove all blocks who have a predecessor, that is not contained in results.
         * Also remove all of their immediate successors.
         *
         * Exempt from removal is the dominator itself.
         */
        while (hasChanged) {

            hasChanged = false

            for {
                block ← result
                if (block ne dominator)
                if (block.predecessors.exists { pred ⇒ !result.contains(pred) })
            } {
                result = result - (block)
                for (succ ← block.successors if (succ ne dominator)) {
                    result = result - (succ)
                }

                hasChanged = true
            }
        }
        result
    }
}
