/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 ≥ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPGELike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPGE.opcode

    final def mnemonic: String = "if_icmpge"

    final def operator: String = ">="

    final def condition: RelationalOperator = RelationalOperators.GE

}

case class IF_ICMPGE(branchoffset: Int) extends IFICMPInstruction[IF_ICMPGE] with IF_ICMPGELike {

    def copy(branchoffset: Int): IF_ICMPGE = new IF_ICMPGE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPLT = {
        IF_ICMPLT(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPGE(InstructionLabel(currentPC + branchoffset))
    }
}

object IF_ICMPGE extends InstructionMetaInformation {

    final val opcode = 162

    /**
     * Creates [[LabeledIF_ICMPGE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPGE = LabeledIF_ICMPGE(branchTarget)

}

case class LabeledIF_ICMPGE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPGELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPGE = {
        IF_ICMPGE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPLT = {
        LabeledIF_ICMPLT(newJumpTargetLabel)
    }
}
