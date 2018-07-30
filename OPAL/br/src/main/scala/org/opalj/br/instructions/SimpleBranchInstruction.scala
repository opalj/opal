/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

trait SimpleBranchInstructionLike
    extends ControlTransferInstructionLike
    with ConstantLengthInstructionLike

trait SimpleBranchInstruction
    extends SimpleBranchInstructionLike
    with ConstantLengthInstruction
    with ControlTransferInstruction {

    final override def isSimpleBranchInstruction: Boolean = true

    final override def asSimpleBranchInstruction: SimpleBranchInstruction = this

    def branchoffset: Int

    final override def jumpTargets(
        currentPC: PC
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        Iterator(branchoffset + currentPC)
    }

}
