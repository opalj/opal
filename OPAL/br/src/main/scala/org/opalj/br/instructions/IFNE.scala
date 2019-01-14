/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value ≠ 0.
 *
 * @author Michael Eichberg
 */
trait IFNELike extends IF0InstructionLike {

    final def opcode: Opcode = IFNE.opcode

    final def mnemonic: String = "ifne"

    final def operator: String = "!= 0"

    final def condition: RelationalOperator = RelationalOperators.NE
}

case class IFNE(branchoffset: Int) extends IF0Instruction[IFNE] with IFNELike {

    def copy(branchoffset: Int): IFNE = new IFNE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFEQ = {
        IFEQ(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFNE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFNE extends InstructionMetaInformation {

    final val opcode = 154

    /**
     * Creates [[LabeledIFNE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFNE = LabeledIFNE(branchTarget)

}

case class LabeledIFNE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFNELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFNE = {
        IFNE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFEQ = {
        LabeledIFEQ(newJumpTargetLabel)
    }
}
