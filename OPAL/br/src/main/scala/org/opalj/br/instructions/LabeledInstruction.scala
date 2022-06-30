/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction where the jump targets are identified using `Symbols` associated with the
 * instructions which should be executed in case of a jump.
 * The labels are `InstructionLabel`s; however, the eDSL provides implicits to faciliate the
 * usage of standard scala Symbols as labels.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
trait LabeledInstruction extends InstructionLike {

    // TODO Rename to jumpTargets to clearly state that the "fall through case" is NOT covered!
    def branchTargets: Iterator[InstructionLabel]

    /**
     * If this instruction uses `Symbol`s to mark jump targets then the targets are replaced
     * by the branchoffsets and an [[Instruction]] is returned. If this instruction already
     * has concrete branchoffsets nothing special will happen.
     *
     * If this instruction already has concrete jump targets nothing special will happen.
     *
     * @param   pc The final pc of this instruction in the code array.
     * @param   pcs The map which maps all symbols to their final pcs.
     */
    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    def resolveJumpTargets(pc: PC, pcs: Map[InstructionLabel, PC]): Instruction

    /**
     * Validates the branchoffset and returns it or throws an exception!
     */
    @throws[BranchoffsetOutOfBoundsException]("if the branchoffset is invalid")
    protected def asShortBranchoffset(branchoffset: Int): Int = {
        if (branchoffset < Short.MinValue || branchoffset > Short.MaxValue) {
            throw BranchoffsetOutOfBoundsException(this);
        }
        branchoffset
    }

}
