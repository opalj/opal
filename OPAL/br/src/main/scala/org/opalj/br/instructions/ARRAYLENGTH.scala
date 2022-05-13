/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Get length of array.
 *
 * @author Michael Eichberg
 */
case object ARRAYLENGTH extends Instruction with ConstantLengthInstruction with NoLabels {

    final val opcode = 190

    final val mnemonic = "arraylength"

    final def jvmExceptions: List[ObjectType] = Instruction.justNullPointerException

    final def mayThrowExceptions: Boolean = true

    final val length = 1

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this eq code.instructions(otherPC)
    }

    final val readsLocal = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final val writesLocal = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

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
            Instruction.nextInstructionOrExceptionHandler(
                this, currentPC, ObjectType.NullPointerException
            )
    }

    final def expressionResult: Stack.type = Stack

    final override def toString(currentPC: Int): String = toString()
}
