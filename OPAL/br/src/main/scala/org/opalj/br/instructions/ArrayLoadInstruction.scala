/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that loads a value stored in an array.
 *
 * @author Michael Eichberg
 */
abstract class ArrayLoadInstruction extends ArrayAccessInstruction {

    final def jvmExceptions: List[ObjectType] = ArrayLoadInstruction.jvmExceptions

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 2

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -2 + elementTypeComputationalType.operandSize

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly) {
            List(indexOfNextInstruction(currentPC))
        } else {
            Instruction.nextInstructionOrExceptionHandlers(this, currentPC, jvmExceptions)
        }
    }

    final def expressionResult: Stack.type = Stack

}

/**
 * Defines common properties of instructions that load values stored in arrays.
 *
 * @author Michael Eichberg
 */
object ArrayLoadInstruction {

    def unapply(ali: ArrayLoadInstruction): Option[ComputationalType] = {
        Some(ali.elementTypeComputationalType)
    }

    /**
     * The exceptions that are potentially thrown by instructions that load values
     * stored in an array.
     */
    final val jvmExceptions: List[ObjectType] = {
        import ObjectType._
        List(ArrayIndexOutOfBoundsException, NullPointerException)
    }

}
