/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.ForeachRefIterator

/**
 * An instruction where the jump target is identified using a `Symbol` associated with the
 * instruction which should be executed in case of a jump.
 * The label is a standard Scala `Symbol`.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
trait LabeledSingleJumpTargetInstruction extends LabeledInstruction {

    final def branchTargets: ForeachRefIterator[InstructionLabel] = {
        ForeachRefIterator.single(branchTarget)
    }

    def branchTarget: InstructionLabel
}
