/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if reference comparison succeeds; succeeds if and only if value1 ≠ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ACMPNELike extends IFACMPInstructionLike {

    final def opcode: Opcode = IF_ACMPNE.opcode

    final def mnemonic: String = "if_acmpne"

    final def operator: String = "!="

    final def condition: RelationalOperator = RelationalOperators.NE

}

case class IF_ACMPNE(branchoffset: Int) extends IFACMPInstruction[IF_ACMPNE] with IF_ACMPNELike {

    def copy(branchoffset: Int): IF_ACMPNE = new IF_ACMPNE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ACMPEQ = {
        IF_ACMPEQ(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ACMPNE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ACMPNE extends InstructionMetaInformation {

    final val opcode = 166

    /**
     * Creates [[LabeledIF_ACMPNE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ACMPNE = LabeledIF_ACMPNE(branchTarget)

}

case class LabeledIF_ACMPNE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ACMPNELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ACMPNE = {
        IF_ACMPNE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ACMPEQ = {
        LabeledIF_ACMPEQ(newJumpTargetLabel)
    }
}
