/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Exit monitor for object.
 *
 * @author Michael Eichberg
 */
case object MONITOREXIT extends SynchronizationInstruction with InstructionMetaInformation {

    final val opcode = 195

    final val mnemonic = "monitorexit"

    final val jvmExceptions: List[ObjectType] = {
        List(ObjectType.NullPointerException, ObjectType.IllegalMonitorStateException)
    }

    final def stackSlotsChange: Int = -1

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
