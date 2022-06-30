/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction where the jump target is identified using a `Symbol` associated with the
 * instruction which should be executed in case of a jump.
 * The label is a standard Scala `Symbol`.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
trait LabeledSingleJumpTargetInstruction extends LabeledInstruction {

    final def branchTargets: Iterator[InstructionLabel] =
        Iterator(branchTarget)

    def branchTarget: InstructionLabel
}
