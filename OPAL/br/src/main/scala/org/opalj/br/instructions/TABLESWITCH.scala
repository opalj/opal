/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntArraySet1

/**
 * Access jump table by index and jump.
 *
 * @author Michael Eichberg
 */
trait TABLESWITCHLike extends CompoundConditionalBranchInstructionLike {

    /** The smallest '''case''' value. `high` &geq; `low` */
    def low: Int

    /** The largest '''case''' value. `high` &geq; `low` */
    def high: Int

    final override def opcode: Opcode = TABLESWITCH.opcode

    final override def mnemonic: String = "tableswitch"

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, false)
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        currentPC + 1 + (3 - (currentPC % 4)) + 12 + (high - low + 1) * 4
    }

}

case class TABLESWITCH(
        defaultOffset: Int,
        low:           Int,
        high:          Int,
        jumpOffsets:   IndexedSeq[Int]
) extends CompoundConditionalBranchInstruction with TABLESWITCHLike {

    final override def asTABLESWITCH: this.type = this

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledTABLESWITCH(
            InstructionLabel(currentPC + defaultOffset),
            low,
            high,
            jumpOffsets.map { branchoffset ⇒ InstructionLabel(currentPC + branchoffset) }
        )
    }

    def caseValueOfJumpOffset(jumpOffset: Int): (Chain[Int], Boolean) = {
        var caseValues = Chain.empty[Int]
        var i = jumpOffsets.length - 1
        while (i >= 0) {
            if (jumpOffsets(i) == jumpOffset)
                caseValues = high - i :&: caseValues
            i -= 1
        }
        (caseValues, jumpOffset == defaultOffset)
    }

    override def caseValues: Iterable[Int] = {
        (low to high).view.filter(cv ⇒ jumpOffsets(cv - low) != defaultOffset)
    }

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        val defaultTarget = currentPC + defaultOffset
        var pcs = Chain.singleton(defaultTarget)
        var seen: IntArraySet = IntArraySet1(defaultTarget)
        jumpOffsets foreach { offset ⇒
            val newPC = (currentPC + offset)
            if (!seen.contains(newPC)) {
                seen += newPC
                pcs :&:= newPC
            }
        }
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

    override def toString: String = {
        s"TABLESWITCH($low -> $high; "+
            (low to high).zip(jumpOffsets).map(e ⇒ e._1+"⤼"+e._2).mkString(",")+
            ";default⤼"+defaultOffset+
            ")"
    }

    override def toString(pc: PC): String = {
        s"TABLESWITCH($low -> $high; "+
            (low to high).zip(jumpOffsets).map { keyOffset ⇒
                val (key, offset) = keyOffset
                key+"="+(pc + offset) + (if (offset >= 0) "↓" else "↑")
            }.mkString(", ")+
            "; ifNoMatch="+(defaultOffset + pc) + (if (defaultOffset >= 0) "↓" else "↑")+")"
    }

}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object TABLESWITCH {

    final val opcode = 170

    /**
     * Creates [[LabeledTABLESWITCH]] instructions with `Symbols` as the branch targets.
     *
     * @param branchTargets The first target is chosen when the branch value has the value `low`.
     *                      The second target is chosen if the value is `low+1` etc.
     *
     */
    def apply(
        defaultBranchTarget: InstructionLabel,
        low:                 Int,
        high:                Int,
        branchTargets:       IndexedSeq[InstructionLabel]
    ): LabeledTABLESWITCH = {
        require(
            branchTargets.size == high - low + 1,
            s"there have to be high-low+1 (${high - low + 1}) targets"
        )
        LabeledTABLESWITCH(defaultBranchTarget, low, high, branchTargets)
    }
}

/**
 * Represents a [[TABLESWITCH]] instruction with unresolved jump targets represented as `Symbols`.
 *
 * @author Malte Limmeroth
 */
case class LabeledTABLESWITCH(
        defaultBranchTarget: InstructionLabel,
        low:                 Int,
        high:                Int,
        jumpTargets:         IndexedSeq[InstructionLabel]
) extends LabeledInstruction with TABLESWITCHLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(currentPC: PC, pcs: Map[InstructionLabel, PC]): TABLESWITCH = {
        TABLESWITCH(
            asShortBranchoffset(pcs(defaultBranchTarget) - currentPC),
            low,
            high,
            jumpTargets.map(target ⇒ asShortBranchoffset(pcs(target) - currentPC))
        )
    }

    override def branchTargets: Seq[InstructionLabel] = defaultBranchTarget +: jumpTargets

    def caseValueOfJumpTarget(jumpTarget: InstructionLabel): (Chain[Int], Boolean) = {
        var caseValues = Chain.empty[Int]
        var i = jumpTargets.length - 1
        while (i >= 0) {
            if (jumpTargets(i) == jumpTarget)
                caseValues = high - i :&: caseValues
            i -= 1
        }
        (caseValues, jumpTarget == defaultBranchTarget)
    }

    override def caseValues: Iterable[Int] = {
        (low to high).view.filter(cv ⇒ jumpTargets(cv - low) != defaultBranchTarget)
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }

    override def toString(pc: Int): String = {
        (low to high).zip(jumpTargets).map { keyOffset ⇒
            val (key, target) = keyOffset
            key+"="+target
        }.mkString("TABLESWITCH(", ", ", "; ifNoMatch="+defaultBranchTarget+")")
    }
}
