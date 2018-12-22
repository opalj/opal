/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Jump subroutine.
 *
 * @author Michael Eichberg
 */
trait JSRLike extends JSRInstructionLike {

    final def opcode: Opcode = JSR.opcode

    final def mnemonic: String = "jsr"

    final def length: Int = 3
}

case class JSR(branchoffset: Int) extends JSRInstruction with JSRLike {

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledJSR(InstructionLabel(currentPC + branchoffset))
    }
}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object JSR extends InstructionMetaInformation {

    final val opcode = 168

    /**
     * Creates [[LabeledJSR]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledJSR = LabeledJSR(branchTarget)
}

case class LabeledJSR(
        branchTarget: InstructionLabel
) extends LabeledUnconditionalBranchInstruction with JSRLike {

    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(
        currentPC: PC,
        pcs:       Map[InstructionLabel, PC]
    ): JSRInstruction = {
        JSR(asShortBranchoffset(pcs(branchTarget) - currentPC))
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }
}
