/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value > 0.
 *
 * @author Michael Eichberg
 */
trait IFGTLike extends IF0InstructionLike {

    final def opcode: Opcode = IFGT.opcode

    final def mnemonic: String = "ifgt"

    final def operator: String = "> 0"

    final def condition: RelationalOperator = RelationalOperators.GT
}

case class IFGT(branchoffset: Int) extends IF0Instruction[IFGT] with IFGTLike {

    def copy(branchoffset: Int): IFGT = new IFGT(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFLE = {
        IFLE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFGT(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFGT extends InstructionMetaInformation {

    final val opcode = 157

    /**
     * Creates [[LabeledIFGT]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFGT = LabeledIFGT(branchTarget)

}

case class LabeledIFGT(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFGTLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFGT = {
        IFGT(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFLE = {
        LabeledIFLE(newJumpTargetLabel)
    }
}
