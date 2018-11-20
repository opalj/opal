/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all control transfer instructions.
 *
 * @author Michael Eichberg
 */
trait ControlTransferInstructionLike extends InstructionLike {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def expressionResult: NoExpression.type = NoExpression

    final override def isControlTransferInstruction: Boolean = true

}

trait ControlTransferInstruction extends Instruction with ControlTransferInstructionLike {

    /**
     * Iterator over all (absolute) pcs to which this instruction will jump to.
     *
     * @note Computing the jump targets is particularly expensive in case of [[RET]] instructions,
     *       and should be avoided if only "all jump targets" of a method's body should be
     *       identified. In that case, collecting the PCs following the JSRs is sufficient.
     *
     * @return All instructions to which this instruction explicitly jumps to. (The instruction to
     *         which an if-instruction i potentially falls through, is not a jump target
     *         w.r.t. i; it may still be a jump target w.r.t. some other control transfer
     *         instruction.)
     */
    def jumpTargets(
        currentPC: Int
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[Int /*PC*/ ] // IMPROVE Use IntIterator!

    final override def asControlTransferInstruction: ControlTransferInstruction = this

}
