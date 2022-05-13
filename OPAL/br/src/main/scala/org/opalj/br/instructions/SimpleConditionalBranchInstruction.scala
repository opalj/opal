/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all instructions that perform a conditional jump.
 *
 * @author Michael Eichberg
 */
trait SimpleConditionalBranchInstructionLike
    extends ConditionalBranchInstructionLike
    with SimpleBranchInstructionLike {

    /**
     * The comparison operator (incl. the constant) underlying the if instruction.
     * E.g., `<`, `< 0` or `!= null`.
     */
    def operator: String

    final def length: Int = 3

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }
}

trait SimpleConditionalBranchInstruction[T <: SimpleConditionalBranchInstruction[T]]
    extends ConditionalBranchInstruction
    with SimpleBranchInstruction
    with SimpleConditionalBranchInstructionLike {

    def copy(branchoffset: Int): SimpleConditionalBranchInstruction[T]

    /**
     * Returns the IF instruction that - when compared with this if instruction -
     * performs a jump in case of a fall-through and vice-versa. I.e., given the
     * following condition: `(a < b)`, the negation is performend: `!(a < b)` which
     * is equivalent to `(a &geq; b)`. In other words,  if this IF instruction is an
     * IFGT instruction and IFLE instruction is returned.
     */
    def negate(newBranchoffset: Int = branchoffset): SimpleConditionalBranchInstruction[_]

    final override def isSimpleConditionalBranchInstruction: Boolean = true
    final override def asSimpleConditionalBranchInstruction: this.type = this

    /**
     * @inheritdoc
     *
     * A simple conditional branch instruction has two targets unless both targets point
     * to the same instruction. In that case the jump has only one target, because the state
     * - independent of the taken path - always has to be the same.
     */
    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        val nextInstruction = indexOfNextInstruction(currentPC)
        val jumpInstruction = currentPC + branchoffset
        if (nextInstruction == jumpInstruction)
            List(nextInstruction)
        else
            List(nextInstruction, jumpInstruction)
    }

    override def toString(currentPC: Int): String = {
        val jumpDirection = if (branchoffset >= 0) "↓" else "↑"
        s"${getClass.getSimpleName}(true=${currentPC + branchoffset}$jumpDirection, false=↓)"
    }

}
/**
 * Extractor for [[SimpleConditionalBranchInstruction]]s.
 */
object SimpleConditionalBranchInstruction {

    /**
     * Extracts the instructions branchoffset.
     */
    def unapply(i: SimpleConditionalBranchInstruction[_ <: SimpleConditionalBranchInstruction[_]]): Some[Int] = Some(i.branchoffset)

}
