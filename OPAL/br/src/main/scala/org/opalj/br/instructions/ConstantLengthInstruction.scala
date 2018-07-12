/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common interface of all instructions that have a fixed length (including operands!).
 *
 * Hence, instructions that may be modified by wide or where the length depends on
 * the position in the code array are never `ConstantLengthInstruction`s.
 *
 * @author Michael Eichberg
 */
trait ConstantLengthInstructionLike extends InstructionLike {

    final override def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        currentPC + length
    }

    /**
     * The number of bytes (in the [[Code]] array) used by the instruction.
     */
    def length: Int
}

trait ConstantLengthInstruction extends ConstantLengthInstructionLike with Instruction {

    final override def indexOfNextInstruction(currentPC: PC)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, false /* or true - doesn't matter at all */ )
    }
}
