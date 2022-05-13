/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Extend local variable index by additional bytes.
 *
 * @author Michael Eichberg
 */
case object WIDE extends Instruction with ConstantLengthInstruction with NoLabels {

    final val opcode = 196

    final val mnemonic = "wide"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def length: Int = 1

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = 0

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this eq code.instructions(otherPC)
    }

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
        List(indexOfNextInstruction(currentPC))
    }

    final def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
}
