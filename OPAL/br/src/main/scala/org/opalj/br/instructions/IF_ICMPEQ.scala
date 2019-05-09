/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 = value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPEQLike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPEQ.opcode

    final def mnemonic: String = "if_icmpeq"

    final def operator: String = "=="

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IF_ICMPEQ(branchoffset: Int) extends IFICMPInstruction[IF_ICMPEQ] with IF_ICMPEQLike {

    def copy(branchoffset: Int): IF_ICMPEQ = new IF_ICMPEQ(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPNE = {
        IF_ICMPNE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPEQ(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPEQ extends InstructionMetaInformation {

    final val opcode = 159

    /**
     * Creates [[LabeledIF_ICMPEQ]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPEQ = LabeledIF_ICMPEQ(branchTarget)

}

case class LabeledIF_ICMPEQ(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPEQLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPEQ = {
        IF_ICMPEQ(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPNE = {
        LabeledIF_ICMPNE(newJumpTargetLabel)
    }
}
