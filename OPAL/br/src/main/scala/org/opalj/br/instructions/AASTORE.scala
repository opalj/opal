/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into reference array.
 *
 * @author Michael Eichberg
 */
case object AASTORE extends ArrayStoreInstruction with InstructionMetaInformation {

    final val opcode = 83

    final val mnemonic = "aastore"

    final val elementTypeComputationalType = ComputationalTypeReference

    final val jvmExceptions: List[ObjectType] = {
        import ObjectType._
        List(ArrayIndexOutOfBoundsException, NullPointerException, ArrayStoreException)
    }

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandlers(this, currentPC, jvmExceptions)
    }

}
