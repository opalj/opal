/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Jump subroutine.
 *
 * @author Michael Eichberg
 */
trait JSR_WLike extends JSRInstructionLike {

    final def opcode: Opcode = JSR_W.opcode

    final def mnemonic: String = "jsr_w"

    final def length: Int = 5
}

case class JSR_W(branchoffset: Int) extends JSRInstruction with JSR_WLike {

    def toLabeledInstruction(currentPC: PC): LabeledInstruction = {
        LabeledJSR_W(InstructionLabel(currentPC + branchoffset))
    }

}

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object JSR_W extends InstructionMetaInformation {

    final val opcode = 201

    /**
     * Creates [[LabeledJSR_W]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: InstructionLabel): LabeledJSR_W = LabeledJSR_W(branchTarget)
}

case class LabeledJSR_W(
        branchTarget: InstructionLabel
) extends LabeledUnconditionalBranchInstruction with JSR_WLike {

    override def resolveJumpTargets(currentPC: PC, pcs: Map[InstructionLabel, PC]): JSR_W = {
        JSR_W(pcs(branchTarget) - currentPC)
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }
}
