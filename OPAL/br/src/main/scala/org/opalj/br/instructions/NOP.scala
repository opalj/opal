/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Do nothing.
 *
 * @author Michael Eichberg
 */
case object NOP extends ConstantLengthInstruction with NoLabels with InstructionMetaInformation {

    final val opcode = 0

    final val mnemonic = "nop"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final val length = 1

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

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
        List(indexOfNextInstruction(currentPC))
    }

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}
