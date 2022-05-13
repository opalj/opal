/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction to create a new array.
 *
 * @author Michael Eichberg
 */
abstract class CreateNewArrayInstruction
    extends Instruction
    with ConstantLengthInstruction
    with NoLabels {

    final override def asCreateNewArrayInstruction: this.type = this

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (this == other)
    }

    final override def jvmExceptions: List[ObjectType] = {
        CreateNewArrayInstruction.jvmExceptionsAndErrors
    }

    final def mayThrowExceptions: Boolean = true

    final override def nextInstructions(
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
            Instruction.nextInstructionOrExceptionHandlers(
                this, currentPC, CreateNewArrayInstruction.jvmExceptionsAndErrors
            )
    }

    final override def expressionResult: Stack.type = Stack

    final override def toString(currentPC: Int): String = toString()

    def arrayType: ArrayType
}

object CreateNewArrayInstruction {

    val jvmExceptions: List[ObjectType] = List(ObjectType.NegativeArraySizeException)

    val jvmExceptionsAndErrors: List[ObjectType] = ObjectType.OutOfMemoryError :: jvmExceptions

}
