/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch always.
 *
 * @author Michael Eichberg
 */
trait GOTOLike extends GotoInstructionLike {

    final def opcode: Opcode = GOTO.opcode

    final def mnemonic: String = "goto"

    final def length: Int = 3

    final def stackSlotsChange: Int = 0
}

case class GOTO(branchoffset: Int) extends GotoInstruction with GOTOLike {

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledGOTO(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object GOTO extends InstructionMetaInformation {

    final val opcode = 167

    /**
     * Creates [[LabeledGOTO]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledGOTO = LabeledGOTO(branchTarget)

}

case class LabeledGOTO(
        branchTarget: InstructionLabel
) extends LabeledUnconditionalBranchInstruction with GOTOLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(
        currentPC: PC,
        pcs:       Map[InstructionLabel, PC]
    ): GotoInstruction = {
        GOTO(asShortBranchoffset(pcs(branchTarget) - currentPC))
    }

}
