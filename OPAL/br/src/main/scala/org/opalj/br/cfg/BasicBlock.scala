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

import org.opalj.br.PC
import org.opalj.br.Code

/**
 *
 * @author Erich Wittenbeck
 */

/**
 * Represents a basic block of bytecode in a control flow graph.
 * It stores the program counters implicitly by having a reference to the PC
 * at it's start- and end-point
 *
 * Parameters:
 *
 * @param startPC The initial starting PC of this BasicBlock
 *
 * ==Thread-Safety==
 * This class is thread-safe
 */
class BasicBlock(val startPC: PC) extends CFGBlock {

    private[cfg] var catchBlockSuccessors: List[CatchBlock] = Nil

    private[cfg] var endPC: PC = startPC

    private[cfg] def setEndPC(pc: PC): Unit = endPC = pc

    /**
     * Returns the program counters contained within this BasicBlock
     *
     * Parameters:
     *
     * @param code The code-object of the CFG
     */
    def programCounters(code: Code): Seq[PC] = {
        var res: List[PC] = Nil
        var pc: PC = startPC

        while (pc <= endPC) {
            res = res :+ pc // this is expensive (as said: return an iterator)
            pc = code.pcOfNextInstruction(pc)
        }

        res
    }

    /**
     * Returns the index of a given PC, if PCs were stored explicitly in an Array
     * or a similar structure
     *
     * Example:
     *
     * Presume this Block contains PCs 0,1,3,5 and 6.
     *
     * For input 3, it will return 2. For 5 it will return 3 etc.
     *
     * Parameters
     *
     * @param pc The program counter whose position is to be determined
     * @param code The code-object of the CFG
     */
    def positionOfPCwithinBlock(pc: PC, code: Code): Int = {
        var index = 0
        var currentPC = startPC

        while (currentPC < pc) {
            currentPC = code.pcOfNextInstruction(currentPC)
            index += 1
        }

        index
    }

    /**
     * Applies a given function to all Program Counters in the BasicBlock
     *
     * Parameters:
     *
     * @param f The function to be applied
     * @param code The code-object on which the CFG is based
     */
    def foreach[U](f: (PC) ⇒ U)(implicit code: Code): Unit = {
        var pc = this.startPC

        while (pc <= endPC) {
            f(pc)
            pc = code.instructions(pc).indexOfNextInstruction(pc, code)
        }
    }

    override def returnAllDescendants(visited: Set[CFGBlock]): Set[CFGBlock] = {

        var res = visited + this
        val worklist = (successors ++ catchBlockSuccessors)
        for (block ← worklist if !visited.contains(block))
            res = res ++ block.returnAllDescendants(res)
        res
    }

    override def id: Int = startPC

    override def toHRR: Option[String] = {
        Some("bb"+startPC)
    }

    def toDot(code: Code): String = {

        var blockLabel: String = this.toHRR.get+"\n"+"_____________________"+"\n"

        this.foreach { pc: PC ⇒
            {
                blockLabel = blockLabel + pc+": "+code.instructions(pc).toString(pc).replaceAll("\"", "")+"\n"
            }
        }(code)

        var res = this.toHRR.get+" [shape=box, label=\""+blockLabel+"\"];\n"

        val worklist = (successors ++ catchBlockSuccessors)

        for (succ ← worklist) {
            succ match {
                case cb: CatchBlock ⇒ {
                    res = res + this.toHRR.get+" -> "+succ.toHRR.get+"[color=red];\n"
                }
                case _ ⇒ {
                    res = res + this.toHRR.get+" -> "+succ.toHRR.get+";\n"
                }
            }
        }
        res
    }

    override def toString: String = {
        "bb@"+startPC+"to"+endPC
    }

}