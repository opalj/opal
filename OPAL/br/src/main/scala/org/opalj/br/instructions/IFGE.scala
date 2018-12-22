/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value ≥ 0.
 *
 * @author Michael Eichberg
 */
trait IFGELike extends IF0InstructionLike {

    final def opcode: Opcode = IFGE.opcode

    final def mnemonic: String = "ifge"

    final def operator: String = ">= 0"

    final def condition: RelationalOperator = RelationalOperators.GE

}

case class IFGE(branchoffset: Int) extends IF0Instruction[IFGE] with IFGELike {

    def copy(branchoffset: Int): IFGE = new IFGE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFLT = {
        IFLT(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFGE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFGE extends InstructionMetaInformation {

    final val opcode = 156

    /**
     * Creates [[LabeledIFGE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFGE = LabeledIFGE(branchTarget)

}

case class LabeledIFGE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFGELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFGE = {
        IFGE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFLT = {
        LabeledIFLT(newJumpTargetLabel)
    }
}
