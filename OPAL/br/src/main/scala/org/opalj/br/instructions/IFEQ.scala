/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value = 0.
 *
 * @author Michael Eichberg
 */
trait IFEQLike extends IF0InstructionLike {

    final def opcode: Opcode = IFEQ.opcode

    final def mnemonic: String = "ifeq"

    final def operator: String = "== 0"

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IFEQ(branchoffset: Int) extends IF0Instruction[IFEQ] with IFEQLike {

    def copy(branchoffset: Int): IFEQ = new IFEQ(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFNE = {
        IFNE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFEQ(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFEQ extends InstructionMetaInformation {

    final val opcode = 153

    /**
     * Creates [[LabeledIFEQ]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFEQ = LabeledIFEQ(branchTarget)

}

case class LabeledIFEQ(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFEQLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFEQ = {
        IFEQ(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFNE = {
        LabeledIFNE(newJumpTargetLabel)
    }
}
