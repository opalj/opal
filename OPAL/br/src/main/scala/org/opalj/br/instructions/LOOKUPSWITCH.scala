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
package instructions

/**
 * Access jump table by key match and jump.
 *
 * @param npairs A list of tuples where the first value is the match value and
 *    the second value is the jump offset.
 * @author Michael Eichberg
 */
case class LOOKUPSWITCH(
    defaultOffset: Int,
    npairs: IndexedSeq[(Int, Int)])
        extends CompoundConditionalBranchInstruction {

    final def opcode: Opcode = LOOKUPSWITCH.opcode

    final def mnemonic: String = "lookupswitch"

    def jumpOffsets = npairs.map(_._2)

    def caseValueOfJumpOffset(jumpOffset: Int): (Seq[Int], Boolean) = {
        (
            npairs.filter(_._2 == jumpOffset).map(_._1),
            jumpOffset == defaultOffset
        )
    }

    def caseValues: Seq[Int] = npairs.map(_._1)

    def indexOfNextInstruction(currentPC: Int, code: Code): Int =
        indexOfNextInstruction(currentPC, false)

    def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int =
        currentPC + 1 + (3 - (currentPC % 4)) + 8 + npairs.size * 8

    def nextInstructions(currentPC: PC, code: Code): PCs = {
        var pcs = collection.mutable.UShortSet(currentPC + defaultOffset)
        npairs foreach (npair ⇒ { val (_, offset) = npair; (currentPC + offset) +≈: pcs })
        pcs
    }

    override def toString(pc: Int): String = {
        "LOOKUPSWITCH("+
            npairs.map(p ⇒ p._1+"="+(pc + p._2) + (if (p._2 >= 0) "↓" else "↑")).mkString(",")+
            "; ifNoMatch="+(defaultOffset + pc) + (if (defaultOffset >= 0) "↓" else "↑")+")"
    }
}
object LOOKUPSWITCH {

    final val opcode = 171

}