/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison succeeds; succeeds if and only if value1 ≤ value2.
 *
 * @author Michael Eichberg
 */
trait IF_ICMPLELike extends IFICMPInstructionLike {

    final def opcode: Opcode = IF_ICMPLE.opcode

    final def mnemonic: String = "if_icmple"

    final def operator: String = "<="

    final def condition: RelationalOperator = RelationalOperators.LE

}

case class IF_ICMPLE(branchoffset: Int) extends IFICMPInstruction[IF_ICMPLE] with IF_ICMPLELike {

    def copy(branchoffset: Int): IF_ICMPLE = new IF_ICMPLE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IF_ICMPGT = {
        IF_ICMPGT(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIF_ICMPLE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IF_ICMPLE extends InstructionMetaInformation {

    final val opcode = 164

    /**
     * Creates [[LabeledIF_ICMPLE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIF_ICMPLE = LabeledIF_ICMPLE(branchTarget)

}

case class LabeledIF_ICMPLE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IF_ICMPLELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IF_ICMPLE = {
        IF_ICMPLE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIF_ICMPGT = {
        LabeledIF_ICMPGT(newJumpTargetLabel)
    }
}
