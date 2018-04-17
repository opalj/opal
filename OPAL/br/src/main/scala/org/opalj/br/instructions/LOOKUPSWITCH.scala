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

import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntArraySet1
import org.opalj.collection.immutable.Chain

/**
 * Access jump table by key match and jump.
 *
 * @author Malte Limmeroth
 */
trait LOOKUPSWITCHLike extends CompoundConditionalBranchInstructionLike {

    /**
     * The number of different case values. (Several case values may share the same target and
     * in particular may also point to the default target.)
     */
    def tableSize: Int

    final def opcode: Opcode = LOOKUPSWITCH.opcode

    final def mnemonic: String = "lookupswitch"

    def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, false)
    }

    def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        currentPC + 1 + (3 - (currentPC % 4)) + 8 + tableSize * 8
    }
}

/**
 * Access jump table by key match and jump.
 *
 * @param   npairs A list of tuples where the first value is the match/case value and
 *          the second value is the jump offset.
 *
 * @author  Michael Eichberg
 */
case class LOOKUPSWITCH(
        defaultOffset: Int,
        npairs:        IndexedSeq[(Int, Int)]
) extends CompoundConditionalBranchInstruction with LOOKUPSWITCHLike {

    final override def asLOOKUPSWITCH: this.type = this

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledLOOKUPSWITCH(
            InstructionLabel(currentPC + defaultOffset),
            npairs.map { e ⇒
                val (v, branchoffset) = e
                (v, InstructionLabel(currentPC + branchoffset))
            }
        )
    }

    override def tableSize: Int = npairs.size

    def jumpOffsets: Iterable[Int] = npairs.view.map(_._2)

    def caseValueOfJumpOffset(jumpOffset: Int): (Chain[Int], Boolean) = {
        (
            npairs.view.filter(_._2 == jumpOffset).map(_._1)(Chain.GenericSpecializedCBF),
            jumpOffset == defaultOffset
        )
    }

    override def caseValues: Iterable[Int] = npairs.view.filter(_._2 != defaultOffset).map(_._1)

    def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        val defaultTarget = currentPC + defaultOffset
        var pcs = Chain.singleton(defaultTarget)
        var seen: IntArraySet = new IntArraySet1(defaultTarget)
        npairs foreach { npair ⇒
            val (_, offset) = npair
            val nextTarget = (currentPC + offset)
            if (!seen.contains(nextTarget)) {
                seen += nextTarget
                pcs :&:= nextTarget
            }
        }
        pcs
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val paddingOffset = (thisPC % 4) - (otherPC % 4)

        code.instructions(otherPC) match {

            case LOOKUPSWITCH(otherDefaultOffset, otherNpairs) ⇒
                (this.defaultOffset + paddingOffset == otherDefaultOffset) &&
                    this.npairs.size == otherNpairs.size && {
                        val tIt = this.npairs.iterator
                        val oIt = otherNpairs.iterator
                        var doesMatch = true
                        while (doesMatch && tIt.hasNext) {
                            val (tKey, tOffset) = tIt.next()
                            val (oKey, oOffset) = oIt.next()
                            doesMatch =
                                tKey == oKey && (tOffset + paddingOffset) == oOffset
                        }
                        doesMatch
                    }

            case _ ⇒ false
        }
    }

    override def toString(pc: Int): String = {
        "LOOKUPSWITCH("+
            npairs.map(p ⇒ p._1+"="+(pc + p._2) + (if (p._2 >= 0) "↓" else "↑")).mkString(",")+
            "; ifNoMatch="+(defaultOffset + pc) + (if (defaultOffset >= 0) "↓" else "↑")+")"
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object LOOKUPSWITCH {

    final val opcode = 171

    /**
     * Creates [[LabeledLOOKUPSWITCH]] instructions with `Symbols` as the branch targets.
     *
     * @param branchTargets A list of tuples where the first value is the match value and
     *    the second value is the branch target.
     */
    def apply(
        defaultBranchTarget: InstructionLabel,
        branchTargets:       IndexedSeq[(Int, InstructionLabel)]
    ): LabeledLOOKUPSWITCH = LabeledLOOKUPSWITCH(defaultBranchTarget, branchTargets)

}

/**
 *
 * Represents a [[LOOKUPSWITCH]] instruction with unresolved jump targets represented as `Symbols`.
 *
 * @author Malte Limmeroth
 *
 */
case class LabeledLOOKUPSWITCH(
        defaultBranchTarget: InstructionLabel,
        npairs:              IndexedSeq[(Int, InstructionLabel)]
) extends LabeledInstruction with LOOKUPSWITCHLike {

    override def tableSize = npairs.size

    def caseValues: Iterable[Int] = npairs.view.filter(_._2 != defaultBranchTarget).map(_._1)

    override def branchTargets: Seq[InstructionLabel] = npairs.map(_._2)

    def caseValueOfJumpTarget(jumpTarget: InstructionLabel): (Chain[Int], Boolean) = {
        (
            npairs.filter(_._2 == jumpTarget).map(_._1)(Chain.GenericSpecializedCBF),
            jumpTarget == defaultBranchTarget
        )
    }

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(currentPC: PC, pcs: Map[InstructionLabel, PC]): LOOKUPSWITCH = {
        LOOKUPSWITCH(
            asShortBranchoffset(pcs(defaultBranchTarget) - currentPC),
            npairs.map { pair ⇒
                val (value, target) = pair
                (value, asShortBranchoffset(pcs(target) - currentPC))
            }.toIndexedSeq
        )
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }

    override def toString(pc: Int): String = {
        npairs.map(p ⇒ p._1+"="+p._2).
            mkString("LOOKUPSWITCH(", ",", s"; ifNoMatch=$defaultBranchTarget)")
    }
}
