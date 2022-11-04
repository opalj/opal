/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Throw exception or error.
 *
 * @author Michael Eichberg
 */
case object ATHROW extends Instruction with NoLabels {

    final val opcode = 191

    final val mnemonic = "athrow"

    final override def isAthrow: Boolean = true

    final override def asATHROW: this.type = this

    final def jvmExceptions: List[ObjectType] = Instruction.justNullPointerException

    final def mayThrowExceptions: Boolean = true

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = -1 // take the current exception or null

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean =
        this eq code.instructions(otherPC)

    final val readsLocal = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final val writesLocal = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code) = {
        indexOfNextInstruction(currentPC, false)
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = currentPC + 1

    final def nextInstructions(
        currentPC: PC, regularSuccessorsOnly: Boolean
    )(
        implicit
        code: Code, classHierarchy: ClassHierarchy
    ): List[PC] = {
        if (regularSuccessorsOnly)
            List.empty
        else
            code.handlerInstructionsFor(currentPC)
    }

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}
