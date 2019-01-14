/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An arithmetic instruction that takes all its operands from the stack and, hence,
 * the constant length "1"; i.e., only one byte is needed to encode the instruction.
 *
 * @author Michael Eichberg
 */
abstract class StackBasedArithmeticInstruction
    extends ArithmeticInstruction
    with ConstantLengthInstruction
    with InstructionMetaInformation {

    final def length: Int = 1

    final def expressionResult: Stack.type = Stack

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

}
