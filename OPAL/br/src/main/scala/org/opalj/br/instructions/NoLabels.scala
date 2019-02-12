/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.ForeachRefIterator

private[instructions] trait NoLabels extends LabeledInstruction { this: Instruction ⇒

    final override def branchTargets: ForeachRefIterator[InstructionLabel] = {
        ForeachRefIterator.empty
    }

    final override def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): this.type = this

    final def toLabeledInstruction(currentPC: PC): LabeledInstruction = this

}
