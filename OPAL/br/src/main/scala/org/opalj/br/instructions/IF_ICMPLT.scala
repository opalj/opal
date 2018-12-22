/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 < value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPLTLike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPLT.opcode

    final def mnemonic: String = "if_icmplt"

    final def operator: String = "<"

    final def condition: RelationalOperator = RelationalOperators.LT

}

case class IF_ICMPLT(branchoffset: Int) extends IFICMPInstruction[IF_ICMPLT] with IF_ICMPLTLike {

    def copy(branchoffset: Int): IF_ICMPLT = new IF_ICMPLT(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPGE = {
        IF_ICMPGE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPLT(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPLT extends InstructionMetaInformation {

    final val opcode = 161

    /**
     * Creates [[LabeledIF_ICMPLT]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPLT = LabeledIF_ICMPLT(branchTarget)

}

case class LabeledIF_ICMPLT(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPLTLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPLT = {
        IF_ICMPLT(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPGE = {
        LabeledIF_ICMPGE(newJumpTargetLabel)
    }
}
