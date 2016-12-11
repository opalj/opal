/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import org.opalj.collection.mutable.UShortSet

/**
 * Access jump table by index and jump.
 *
 * @author Michael Eichberg
 */
trait TABLESWITCHLike extends CompoundConditionalBranchInstructionLike {
    protected def tableSize: Int

    def caseValues: Seq[Int]

    final def opcode: Opcode = TABLESWITCH.opcode

    final def mnemonic: String = "tableswitch"

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, false)
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        currentPC + 1 + (3 - (currentPC % 4)) + 12 + tableSize * 4
    }

}

case class TABLESWITCH(
        defaultOffset: Int,
        low:           Int,
        high:          Int,
        jumpOffsets:   IndexedSeq[Int]
) extends CompoundConditionalBranchInstruction with TABLESWITCHLike {

    override protected def tableSize = jumpOffsets.size

    def caseValueOfJumpOffset(jumpOffset: Int): (Seq[Int], Boolean) = {
        var caseValues = List.empty[Int]
        var i = jumpOffsets.length - 1
        while (i >= 0) {
            if (jumpOffsets(i) == jumpOffset)
                caseValues = high - i :: caseValues
            i -= 1
        }
        (caseValues, jumpOffset == defaultOffset)
    }

    override def caseValues: Seq[Int] =
        (low to high).filter(cv ⇒ jumpOffsets(cv - low) != defaultOffset)

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code: Code
    ): PCs = {
        var pcs = UShortSet(currentPC + defaultOffset)
        jumpOffsets foreach (offset ⇒ { pcs = (currentPC + offset) +≈: pcs })
        pcs
    }

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val paddingOffset = (thisPC % 4) - (otherPC % 4)

        code.instructions(otherPC) match {

            case TABLESWITCH(otherDefaultOffset, `low`, `high`, otherJumpOffsets) ⇒
                (this.defaultOffset + paddingOffset == otherDefaultOffset) && {
                    val tIt = this.jumpOffsets.iterator
                    val oIt = otherJumpOffsets.iterator
                    var doesMatch = true
                    while (doesMatch && tIt.hasNext) {
                        val tOffset = tIt.next()
                        val oOffset = oIt.next()
                        doesMatch = (tOffset + paddingOffset) == oOffset
                    }
                    doesMatch
                }

            case _ ⇒ false
        }
    }

    override def toString: String =
        "TABLESWITCH("+
            (low to high).zip(jumpOffsets).map(e ⇒ e._1+"⤼"+e._2).mkString(",")+
            ";default⤼"+defaultOffset+
            ")"

    override def toString(pc: Int): String =
        "TABLESWITCH("+
            (low to high).zip(jumpOffsets).map { keyOffset ⇒
                val (key, offset) = keyOffset
                key+"="+(pc + offset) + (if (offset >= 0) "↓" else "↑")
            }.mkString(", ")+
            "; ifNoMatch="+(defaultOffset + pc) + (if (defaultOffset >= 0) "↓" else "↑")+")"

}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object TABLESWITCH {

    final val opcode = 170

    /**
     * Creates [[LabeledTABLESWITCH]] instructions with a `Symbol` as the branch targets.
     *
     */
    def apply(
        defaultBranchTarget: Symbol,
        low:                 Int,
        high:                Int,
        branchTargets:       Symbol*
    ): LabeledTABLESWITCH = LabeledTABLESWITCH(
        defaultBranchTarget,
        low,
        high,
        branchTargets.toList
    )

}

case class LabeledTABLESWITCH(
        defaultBranchTarget: Symbol,
        low:                 Int,
        high:                Int,
        jumpTargets:         List[Symbol]
) extends LabeledInstruction with TABLESWITCHLike {
    override protected def tableSize = jumpTargets.size

    override def resolveJumpTargets(currentIndex: PC, branchoffsets: Map[Symbol, PC]): TABLESWITCH = {
        TABLESWITCH(
            branchoffsets(defaultBranchTarget) - currentIndex,
            low,
            high,
            jumpTargets.map(branchoffsets(_) - currentIndex).toIndexedSeq
        )
    }

    override def branchTargets: List[Symbol] = defaultBranchTarget :: jumpTargets.toList

    def caseValueOfJumpTarget(jumpTarget: Symbol): (Seq[Int], Boolean) = {
        var caseValues = List.empty[Int]
        var i = jumpTargets.length - 1
        while (i >= 0) {
            if (jumpTargets(i) == jumpTarget)
                caseValues = high - i :: caseValues
            i -= 1
        }
        (caseValues, jumpTarget == defaultBranchTarget)
    }

    override def caseValues: Seq[Int] =
        (low to high).filter(cv ⇒ jumpTargets(cv - low) != defaultBranchTarget)

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }

    override def toString(pc: Int): String =
        "TABLESWITCH("+
            (low to high).zip(jumpTargets).map { keyOffset ⇒
                val (key, target) = keyOffset
                key+"="+target
            }.mkString(", ")+
            "; ifNoMatch="+defaultBranchTarget+")"
}