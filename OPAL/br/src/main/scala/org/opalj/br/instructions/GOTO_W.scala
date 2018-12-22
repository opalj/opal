/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch always.
 *
 * @author Michael Eichberg
 */
trait GOTO_WLike extends GotoInstructionLike {

    final def opcode: Opcode = GOTO_W.opcode

    final def mnemonic: String = "goto_w"

    final def length: Int = 5

    final def stackSlotsChange: Int = 0
}

case class GOTO_W(branchoffset: Int) extends GotoInstruction with GOTO_WLike {

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledGOTO_W(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object GOTO_W extends InstructionMetaInformation {

    final val opcode = 200

    /**
     * Creates [[LabeledGOTO_W]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledGOTO_W = LabeledGOTO_W(branchTarget)

}

case class LabeledGOTO_W(
        branchTarget: InstructionLabel
) extends LabeledUnconditionalBranchInstruction with GOTO_WLike {

    override def resolveJumpTargets(currentPC: PC, pcs: Map[InstructionLabel, PC]): GOTO_W = {
        GOTO_W(pcs(branchTarget) - currentPC)
    }

}
