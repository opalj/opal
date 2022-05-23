/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntArraySet1
import org.opalj.collection.immutable.IntIntPair

import scala.collection.immutable.ArraySeq

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
        npairs:        ArraySeq[IntIntPair]
) extends CompoundConditionalBranchInstruction with LOOKUPSWITCHLike {

    final override def asLOOKUPSWITCH: this.type = this

    final override def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, modifiedByWide = false)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledLOOKUPSWITCH(
            InstructionLabel(currentPC + defaultOffset),
            npairs.map[(Int, InstructionLabel)] { e =>
                val IntIntPair(v, branchoffset) = e
                (v, InstructionLabel(currentPC + branchoffset))
            }
        )
    }

    override def tableSize: Int = npairs.size

    def jumpOffsets: Iterable[Int] = npairs.map(_._2)

    def caseValueOfJumpOffset(jumpOffset: Int): (List[Int], Boolean) = {
        (
            npairs.view.filter(_._2 == jumpOffset).map(_._1).toList,
            jumpOffset == defaultOffset
        )
    }

    override def caseValues: Iterator[Int] = npairs.iterator.filter(_._2 != defaultOffset).map(_._1)

    def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        val defaultTarget = currentPC + defaultOffset
        var pcs = List(defaultTarget)
        var seen: IntArraySet = new IntArraySet1(defaultTarget)
        npairs foreach { npair =>
            val offset = npair.value
            val nextTarget = currentPC + offset
            if (!seen.contains(nextTarget)) {
                seen += nextTarget
                pcs ::= nextTarget
            }
        }
        pcs
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val paddingOffset = (thisPC % 4) - (otherPC % 4)

        code.instructions(otherPC) match {

            case LOOKUPSWITCH(otherDefaultOffset, otherNpairs) =>
                (this.defaultOffset + paddingOffset == otherDefaultOffset) &&
                    this.npairs.size == otherNpairs.size && {
                        val tIt = this.npairs.iterator
                        val oIt = otherNpairs.iterator
                        var doesMatch = true
                        while (doesMatch && tIt.hasNext) {
                            val IntIntPair(tKey, tOffset) = tIt.next()
                            val IntIntPair(oKey, oOffset) = oIt.next()
                            doesMatch =
                                tKey == oKey && (tOffset + paddingOffset) == oOffset
                        }
                        doesMatch
                    }

            case _ => false
        }
    }

    override def toString(pc: Int): String = {
        "LOOKUPSWITCH("+
            npairs.iterator.
            map[String](p => s"${p._1}=${pc + p._2}${if (p._2 >= 0) "↓" else "↑"}").
            mkString(",") +
            s"; ifNoMatch=${(defaultOffset + pc)}${if (defaultOffset >= 0) "↓" else "↑"}"+
            ")"
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object LOOKUPSWITCH extends InstructionMetaInformation {

    final val opcode = 171

    /**
     * Creates [[LabeledLOOKUPSWITCH]] instructions with `Symbols` as the branch targets.
     *
     * @param branchTargets A list of tuples where the first value is the match value and
     *    the second value is the branch target.
     */
    def apply(
        defaultBranchTarget: InstructionLabel,
        branchTargets:       ArraySeq[(Int, InstructionLabel)]
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
        npairs:              ArraySeq[(Int, InstructionLabel)]
) extends LabeledInstruction with LOOKUPSWITCHLike {

    override def tableSize: Int = npairs.size

    def caseValues: Iterator[Int] = npairs.iterator.filter(_._2 != defaultBranchTarget).map(_._1)

    override def branchTargets: Iterator[InstructionLabel] = {
        npairs.iterator.map[InstructionLabel](_._2) ++ Iterator(defaultBranchTarget)
    }

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(currentPC: PC, pcs: Map[InstructionLabel, PC]): LOOKUPSWITCH = {
        LOOKUPSWITCH(
            asShortBranchoffset(pcs(defaultBranchTarget) - currentPC),
            npairs map { pair =>
                val (value, target) = pair
                IntIntPair(value, asShortBranchoffset(pcs(target) - currentPC))
            }
        )
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }

    override def toString(pc: Int): String = {
        npairs.iterator.
            map(p => s"${p._1}=${p._2}").
            mkString("LOOKUPSWITCH(", ",", s"; ifNoMatch=$defaultBranchTarget)")
    }
}
