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
package org.opalj
package br
package cfg

import scala.collection.{Set ⇒ SomeSet}
import org.opalj.collection.mutable.UShortSet

/**
 * Represents the control flow graph of a method.
 *
 * To compute a `CFG` use the [[CFGFactory]].
 *
 * ==Thread-Safety==
 * This class is thread-safe; all data is effectively immutable.
 *
 * @param method The method for which the CFG was build.
 * @param normalReturnNode The unique exit node of the control flow graph if the
 * 		method returns normally.
 * @param abnormalReturnNode The unique exit node of the control flow graph if the
 * 		method returns abnormally (throws an exception).
 * @param basicBlocks An implicit map between a program counter and its associated
 * 		[[BasicBlock]].
 * @param catchNodes List of all catch nodes. (Usually, we have one [[CatchNode]] per
 * 		[[org.opalj.br.ExceptionHandler]].
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
case class CFG(
        code:                    Code,
        normalReturnNode:        ExitNode,
        abnormalReturnNode:      ExitNode,
        private val basicBlocks: Array[BasicBlock],
        catchNodes:              Seq[CatchNode]
) {

    final def startBlock: BasicBlock = basicBlocks(0)

    /**
     * Returns the basic block to which the instruction with the given `pc` belongs.
     *
     * @param pc A valid pc.
     */
    def bb(pc: PC): BasicBlock = basicBlocks(pc)

    /**
     * Returns the set of all reachable [[CFGNode]]s of the control flow graph.
     */
    lazy val reachableBBs: SomeSet[CFGNode] = basicBlocks(0).reachable(reflexive = true)

    /**
     * Returns the set of all [[BasicBlock]]s. (I.e., the exit and catch nodes are
     * not returned.)
     *
     * @note The returned set is recomputed every time this method is called.
     */
    def allBBs: Set[BasicBlock] = basicBlocks.filter(_ ne null).toSet

    /**
     * Iterates over all runtime successors of the instruction with the given pc.
     *
     * If the returned set is empty, then the instruction is either a return instruction or an
     * instruction that always causes an exception to be thrown that is not handled by
     * a handler of the respective method.
     *
     * @param pc A valid pc of an instruction of the code block from which this cfg was derived.
     */
    def successors(pc: PC): Set[PC] = {
        val bb = this.bb(pc)
        if (bb.endPC > pc) {
            // it must be - w.r.t. the code array - the next instruction
            Set(code.instructions(pc).indexOfNextInstruction(pc)(code))
        } else {
            // the set of successor can be (at the same time) a RegularBB or an ExitNode
            bb.successors.collect {
                case bb: BasicBlock ⇒ bb.startPC
                case cb: CatchNode  ⇒ cb.handlerPC
            }
        }
    }

}
