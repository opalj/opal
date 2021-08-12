/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that stores a value in an array.
 *
 * @author Michael Eichberg
 */
abstract class ArrayStoreInstruction extends ArrayAccessInstruction {

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 3

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def stackSlotsChange: Int = -2 - elementTypeComputationalType.operandSize

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def expressionResult: NoExpression.type = NoExpression
}
