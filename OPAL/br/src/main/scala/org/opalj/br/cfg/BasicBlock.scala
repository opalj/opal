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
import org.opalj.br.PC
import org.opalj.br.Code

/**
 * Represents a basic block of a method's control flow graph (CFG). The basic block
 * is identified by referring to the first and last instruction belonging to the
 * basic block.
 *
 * @param startPC The pc of the first instruction belonging to the `BasicBlock`.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
class BasicBlock(val startPC: PC) extends CFGNode {

    private[this] var _endPC: PC = 0 // will be initialized at construction time
    private[cfg] def endPC_=(pc: PC): Unit = {
        assert(pc >= startPC, s"the endPc $pc is smaller than the startPC $startPC")
        _endPC = pc
    }
    /**
     * The pc of the last instruction belonging to this basic block.
     */
    def endPC: PC = _endPC

    /**
     * Returns the index of an instruction – identified by its program counter (pc) –
     * in a basic block.
     *
     * ==Example==
     * Given a basic block which has five instructions which have the following
     * program counters: {0,1,3,5,6}. In this case the index of the instruction with
     * program counter 3 will be 2 and in case of the instruction with pc 6 the index
     * will be 4.
     *
     * @param pc The program counter of the instruction for which the index is needed.
     * 	`pc` has to satisfy: `startPC <= pc <= endPC`.
     * @param code The code to which this basic block belongs.
     */
    def index(pc: PC)(implicit code: Code): Int = {
        assert(pc >= startPC && pc <= endPC)

        var index = 0
        var currentPC = startPC
        while (currentPC < pc) {
            currentPC = code.pcOfNextInstruction(currentPC)
            index += 1
        }
        index
    }

    /**
     * Calls the function `f` for all instructions - indentified by their respective
     * pcs - of a basic block.
     *
     * @param f The function that will be called.
     * @param code The [[org.opalj.br.Code]]` object to which this `BasicBlock` implicitly
     * 		belongs.
     */
    def foreach[U](f: PC ⇒ U)(implicit code: Code): Unit = {
        val instructions = code.instructions

        var pc = this.startPC
        val endPC = this.endPC
        while (pc <= endPC) {
            f(pc)
            pc = instructions(pc).indexOfNextInstruction(pc, code)
        }
    }

    //
    // FOR DEBUGING/VISUALIZATION PURPOSES
    //

    override def toString: String = s"BasicBlock(startPC=$startPC, endPC=$endPC)"

    override def nodeId: Long = startPC.toLong

    override def toHRR: Option[String] = Some(s"[$startPC,$endPC]")

    override def visualProperties: Map[String, String] = {
        var visualProperties = Map("shape" -> "box", "labelloc" -> "l")

        if (startPC == 0) {
            visualProperties += "fillcolor" -> "green"
            visualProperties += "style" -> "filled"
        }

        if (!hasSuccessors) { // in this case something is very broken (internally)...
            visualProperties += "shape" -> "octagon"
            visualProperties += "fillcolor" -> "gray"
            visualProperties += "style" -> "filled"
        }

        visualProperties
    }
}
