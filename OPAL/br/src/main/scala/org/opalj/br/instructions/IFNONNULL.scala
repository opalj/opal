/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if reference is not null.
 *
 * @author Michael Eichberg
 */
trait IFNONNULLLike extends IFXNullInstructionLike {

    final def opcode: Opcode = IFNONNULL.opcode

    final def mnemonic: String = "ifnonnull"

    final def operator: String = "!= null"

    final def condition: RelationalOperator = RelationalOperators.NE
}

case class IFNONNULL(branchoffset: Int) extends IFXNullInstruction[IFNONNULL] with IFNONNULLLike {

    def copy(branchoffset: Int): IFNONNULL = new IFNONNULL(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFNULL = {
        IFNULL(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFNONNULL(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFNONNULL extends InstructionMetaInformation {

    final val opcode = 199

    /**
     * Creates [[LabeledIFNONNULL]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFNONNULL = LabeledIFNONNULL(branchTarget)

}

case class LabeledIFNONNULL(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFNONNULLLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFNONNULL = {
        IFNONNULL(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFNULL = {
        LabeledIFNULL(newJumpTargetLabel)
    }
}
