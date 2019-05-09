/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value ≤ 0.
 *
 * @author Michael Eichberg
 */
trait IFLELike extends IF0InstructionLike {

    final def opcode: Opcode = IFLE.opcode

    final def mnemonic: String = "ifle"

    final def operator: String = "<= 0"

    final def condition: RelationalOperator = RelationalOperators.LE

}

case class IFLE(branchoffset: Int) extends IF0Instruction[IFLE] with IFLELike {

    def copy(branchoffset: Int): IFLE = new IFLE(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFGT = {
        IFGT(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFLE(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFLE extends InstructionMetaInformation {

    final val opcode = 158

    /**
     * Creates [[LabeledIFLE]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFLE = LabeledIFLE(branchTarget)

}

case class LabeledIFLE(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFLELike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFLE = {
        IFLE(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFGT = {
        LabeledIFGT(newJumpTargetLabel)
    }
}
