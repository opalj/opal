/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

/**
 * Access jump table by index and jump.
 *
 * @author Michael Eichberg
 */
case class TABLESWITCH(
    defaultOffset: Int,
    low: Int,
    high: Int,
    jumpOffsets: IndexedSeq[Int])
        extends CompoundConditionalBranchInstruction {

    def opcode: Int = 170

    def mnemonic: String = "tableswitch"

    def indexOfNextInstruction(currentPC: Int, code: Code): Int = {
        currentPC + 1 + (3 - (currentPC % 4)) + 12 + jumpOffsets.size * 4
    }

    override def toString =
        "TABLESWITCH\n"+
            (low to high).zip(jumpOffsets).map(keyOffset ⇒ {
                val (key, offset) = keyOffset
                "\t"+key+" ⇒ "+offset
            }).mkString("\n")

    override def toString(pc: Int): String =
        "TABLESWITCH("+
            (low to high).zip(jumpOffsets).map(keyOffset ⇒ {
                val (key, offset) = keyOffset
                key+"="+(pc + offset) + (if (offset >= 0) "↓" else "↑")
            }).mkString(",")+
            "; ifNoMatch="+(defaultOffset + pc) + (if (defaultOffset >= 0) "↓" else "↑")+")"

}
