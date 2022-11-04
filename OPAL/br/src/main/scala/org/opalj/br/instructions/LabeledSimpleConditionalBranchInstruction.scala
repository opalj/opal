/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An conditional branch instruction where the jump target is identified using a `Symbol`.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
trait LabeledSimpleConditionalBranchInstruction
    extends LabeledInstruction
    with SimpleConditionalBranchInstructionLike {

    final def branchTargets: Iterator[InstructionLabel] =
        Iterator(branchTarget)

    def branchTarget: InstructionLabel

    /**
     * @inheritdoc
     *
     * @return A `SimpleConditionalBranchInstruction`.
     */
    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    override def resolveJumpTargets(
        pc:  PC,
        pcs: Map[InstructionLabel, PC]
    ): SimpleConditionalBranchInstruction[_]

    override def toString(currentPC: Int): String = {
        s"${getClass.getSimpleName}(true=$branchTarget, false=↓)"
    }

    /**
     * Returns the negated if instruction. That is, an if_&lt;cond&GT; instruction is translated to
     * if_!&lt;cond%gt;.
     *
     * @param newJumpTargetLabel The new jump target label.
     * @return The negated if instruction with the given
     */
    def negate(newJumpTargetLabel: InstructionLabel): LabeledSimpleConditionalBranchInstruction

}
