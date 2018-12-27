/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if reference is null.
 *
 * @author Michael Eichberg
 */
trait IFNULLLike extends IFXNullInstructionLike {

    final def opcode: Opcode = IFNULL.opcode

    final def mnemonic: String = "ifnull"

    final def operator: String = "== null"

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IFNULL(branchoffset: Int) extends IFXNullInstruction[IFNULL] with IFNULLLike {

    def copy(branchoffset: Int): IFNULL = new IFNULL(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFNONNULL = {
        IFNONNULL(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFNULL(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFNULL extends InstructionMetaInformation {

    final val opcode = 198

    /**
     * Creates LabeledIFNULL instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFNULL = LabeledIFNULL(branchTarget)

}

case class LabeledIFNULL(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFNULLLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFNULL = {
        IFNULL(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFNONNULL = {
        LabeledIFNONNULL(newJumpTargetLabel)
    }
}
