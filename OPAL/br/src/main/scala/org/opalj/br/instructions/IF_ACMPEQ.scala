/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if reference comparison succeeds; succeeds if and only if value1 == value2.
 *
 * @author Michael Eichberg
 */
trait IF_ACMPEQLike extends IFACMPInstructionLike {

    final def opcode: Opcode = IF_ACMPEQ.opcode

    final def mnemonic: String = "if_acmpeq"

    final def operator: String = "=="

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IF_ACMPEQ(branchoffset: Int) extends IFACMPInstruction[IF_ACMPEQ] with IF_ACMPEQLike {

    def copy(branchoffset: Int): IF_ACMPEQ = new IF_ACMPEQ(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ACMPNE = {
        IF_ACMPNE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ACMPEQ(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ACMPEQ extends InstructionMetaInformation {

    final val opcode = 165

    /**
     * Creates[[LabeledIF_ACMPEQ]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ACMPEQ = LabeledIF_ACMPEQ(branchTarget)

}

case class LabeledIF_ACMPEQ(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ACMPEQLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ACMPEQ = {
        IF_ACMPEQ(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ACMPNE = {
        LabeledIF_ACMPNE(newJumpTargetLabel)
    }
}
