/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.immutable.Chain

/**
 * An instruction that stores a primitive value in an array of primitive values.
 *
 * @author Michael Eichberg
 */
abstract class PrimitiveArrayStoreInstruction extends ArrayStoreInstruction {

    final def jvmExceptions: List[ObjectType] =
        PrimitiveArrayAccess.jvmExceptions

    final def nextInstructions(
        currentPC: PC, regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        if (regularSuccessorsOnly)
            Chain.singleton(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandlers(
                this, currentPC, PrimitiveArrayAccess.jvmExceptions
            )
    }

}
