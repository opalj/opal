/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Super class of all bytecode instructions that always jump to a specific
 * target instruction.
 *
 * @author Michael Eichberg
 */
trait UnconditionalBranchInstructionLike extends SimpleBranchInstructionLike {

    final override def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final override def readsLocal: Boolean = false

    final override def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

}

trait UnconditionalBranchInstruction
    extends SimpleBranchInstruction
    with UnconditionalBranchInstructionLike {

    final override def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(currentPC + branchoffset)
    }

    override def toString(currentPC: Int): String = {
        val direction = if (branchoffset >= 0) "↓" else "↑"
        getClass.getSimpleName+" "+(currentPC + branchoffset) + direction
    }

}

/**
 * Extractor for [[UnconditionalBranchInstruction]]s.
 */
object UnconditionalBranchInstruction {

    /**
     * Extracts the instructions branchoffset.
     */
    def unapply(i: UnconditionalBranchInstruction): Some[Int] = Some(i.branchoffset)

}
