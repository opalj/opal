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

import scala.collection.immutable.HashSet
import scala.collection.immutable.TreeSet
import org.opalj.br.PC
import org.opalj.br.Code

/**
 * @author Erich Wittenbeck
 */
class BasicBlock(val startPC: PC) extends CFGBlock {

    private[cfg] var catchBlockSuccessors: List[CatchBlock] = Nil

    private[cfg] var endPC: PC = startPC

    private[cfg] def addPC(pc: PC): Unit = endPC = pc // TODO Rename: setEndPC

    private[cfg] override def addSucc(block: CFGBlock): Unit = block match {
        case cb: CatchBlock ⇒ { catchBlockSuccessors = catchBlockSuccessors :+ cb }
        case _              ⇒ super.addSucc(block)
    }

    def programCounters(code: Code): Seq[PC] = { // TODO Return an iterator (this give the caller more opportunities!)
        var res: List[PC] = Nil
        var pc: PC = startPC

        while (pc <= endPC) {
            res = res :+ pc // this is expensive (as said: return an iterator)
            pc = code.pcOfNextInstruction(pc)
        }

        res
    }

    // TODO add documentation
    def indexOfPC(pc: PC, code: Code): Int = {
        var res = 0 // TODO rename: index
        var currentPC = startPC

        while (currentPC < pc) {
            currentPC = code.pcOfNextInstruction(currentPC)
            res += 1
        }

        res
    }

    private[cfg] def split(
        block: BasicBlock,
        newBlockStartPC: PC,
        oldBlockEndPC: PC): BasicBlock = {

        val newBlock = new BasicBlock(newBlockStartPC)
        newBlock.endPC = endPC
        newBlock.successors = successors
        newBlock.catchBlockSuccessors = catchBlockSuccessors
        newBlock.addPred(this)

        for (successor ← newBlock.successors) {
            val oldBlock = this
            successor.predecessors =
                successor.predecessors.map {
                    case `oldBlock` ⇒ newBlock
                    case aBlock     ⇒ aBlock
                }
        }

        successors = List(newBlock)
        endPC = oldBlockEndPC

        newBlock
    }

    def foreach[U](f: (PC) ⇒ U)(implicit code: Code): Unit = {
        var pc = this.startPC

        while (pc <= endPC) {
            f(pc)
            pc = code.instructions(pc).indexOfNextInstruction(pc, code)
        }
    }

    override def returnAllBlocks(visited: Set[CFGBlock]): Set[CFGBlock] = {

        var res = visited + this
        val worklist = (successors ++ catchBlockSuccessors)
        for (block ← worklist if !visited.contains(block))
            res = res ++ block.returnAllBlocks(res)
        res
    }

    override def equals(any: Any): Boolean = {
        any match {
            case that: BasicBlock ⇒ this.startPC == that.startPC // TODO This is questionable (how about the id field!)
            case _                ⇒ false
        }
    }

    override def hashCode(): Int = startPC * 171; // TODO This is questionable (how about the id field!)

    def toDot(code: Code): String = {

        var blockLabel: String = ID+"\n"+"_____________________"+"\n"

        if (startPC == endPC) { // Sonderfall : 1 Instruktion
            blockLabel = blockLabel + startPC+":\t"+code.instructions(startPC).toString(startPC).replaceAll("\"", "")+"\n"
        } else {
            val padding: String = if (code.instructions(startPC).indexOfNextInstruction(startPC, code) == endPC) { "" } else { "\t***\n" } // Sonderfall: 2 Instructions

            blockLabel = blockLabel + startPC+":\t"+code.instructions(startPC).toString(startPC).replaceAll("\"", "")+"\n"+
                padding + endPC+":\t"+code.instructions(endPC).toString(endPC).replaceAll("\"", "")+"\n"
        }

        var res = ID+" [shape=box, label=\""+blockLabel+"\"];\n"

        val worklist = (successors ++ catchBlockSuccessors)

        for (succ ← worklist) {
            succ match {
                case cb: CatchBlock ⇒ {
                    res = res + ID+" -> "+succ.ID+"[color=red];\n"
                }
                case _ ⇒ {
                    res = res + ID+" -> "+succ.ID+";\n"
                }
            }
        }
        res
    }

    override def toString: String = {
        "bb@"+startPC+"to"+endPC
    }
}
