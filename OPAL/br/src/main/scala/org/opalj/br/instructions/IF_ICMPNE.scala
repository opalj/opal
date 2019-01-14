/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 ≠ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPNELike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPNE.opcode

    final def mnemonic: String = "if_icmpne"

    final def operator: String = "!="

    final def condition: RelationalOperator = RelationalOperators.NE

}

case class IF_ICMPNE(branchoffset: Int) extends IFICMPInstruction[IF_ICMPNE] with IF_ICMPNELike {

    def copy(branchoffset: Int): IF_ICMPNE = new IF_ICMPNE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPEQ = {
        IF_ICMPEQ(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPNE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPNE extends InstructionMetaInformation {

    final val opcode = 160

    /**
     * Creates [[LabeledIF_ICMPNE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPNE = LabeledIF_ICMPNE(branchTarget)

}

case class LabeledIF_ICMPNE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPNELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPNE = {
        IF_ICMPNE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPEQ = {
        LabeledIF_ICMPEQ(newJumpTargetLabel)
    }
}
