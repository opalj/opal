/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 > value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPGTLike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPGT.opcode

    final def mnemonic: String = "if_icmpgt"

    final def operator: String = ">"

    final def condition: RelationalOperator = RelationalOperators.GT

}

case class IF_ICMPGT(branchoffset: Int) extends IFICMPInstruction[IF_ICMPGT] with IF_ICMPGTLike {

    def copy(branchoffset: Int): IF_ICMPGT = new IF_ICMPGT(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPLE = {
        IF_ICMPLE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPGT(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPGT extends InstructionMetaInformation {

    final val opcode = 163

    /**
     * Creates [[LabeledIF_ICMPGT]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPGT = LabeledIF_ICMPGT(branchTarget)

}

case class LabeledIF_ICMPGT(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPGTLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPGT = {
        IF_ICMPGT(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPLE = {
        LabeledIF_ICMPLE(newJumpTargetLabel)
    }
}
