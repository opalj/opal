/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

private[instructions] trait NoLabels extends LabeledInstruction { this: Instruction =>

    override final def branchTargets: Iterator[InstructionLabel] =
        Iterator.empty

    override final def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): this.type = this

    final def toLabeledInstruction(currentPC: PC): LabeledInstruction = this

}
