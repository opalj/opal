/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Branch if int comparison with zero succeeds; succeeds if and only if value < 0.
 *
 * @author Michael Eichberg
 */
trait IFLTLike extends IF0InstructionLike {

    final def opcode: Opcode = IFLT.opcode

    final def mnemonic: String = "iflt"

    final def operator: String = "< 0"

    final def condition: RelationalOperator = RelationalOperators.LT
}

case class IFLT(branchoffset: Int) extends IF0Instruction[IFLT] with IFLTLike {

    def copy(branchoffset: Int): IFLT = new IFLT(branchoffset)

    def negate(newBranchoffset: Int = branchoffset): IFGE = {
        IFGE(newBranchoffset)
    }

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledIFLT(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFLT extends InstructionMetaInformation {

    final val opcode = 155

    /**
     * Creates LabeledIFLT instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledIFLT = LabeledIFLT(branchTarget)
}

case class LabeledIFLT(
        branchTarget: InstructionLabel
) extends LabeledSimpleConditionalBranchInstruction
    with IFLTLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): IFLT = {
        IFLT(asShortBranchoffset(pcs(branchTarget) - pc))
    }

    override def negate(newJumpTargetLabel: InstructionLabel): LabeledIFGE = {
        LabeledIFGE(newJumpTargetLabel)
    }
}
