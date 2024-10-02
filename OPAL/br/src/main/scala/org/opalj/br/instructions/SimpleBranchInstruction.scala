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

    override final def isSimpleBranchInstruction: Boolean = true

    override final def asSimpleBranchInstruction: SimpleBranchInstruction = this

    def branchoffset: Int

    override final def jumpTargets(
        currentPC: PC
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        Iterator(branchoffset + currentPC)
    }

}
