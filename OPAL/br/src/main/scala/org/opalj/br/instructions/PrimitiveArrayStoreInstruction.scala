/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that stores a primitive value in an array of primitive values.
 *
 * @author Michael Eichberg
 */
abstract class PrimitiveArrayStoreInstruction
    extends ArrayStoreInstruction
    with InstructionMetaInformation {

    final def jvmExceptions: List[ObjectType] =
        PrimitiveArrayAccess.jvmExceptions

    final def nextInstructions(
        currentPC: PC, regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandlers(
                this, currentPC, PrimitiveArrayAccess.jvmExceptions
            )
    }

}
