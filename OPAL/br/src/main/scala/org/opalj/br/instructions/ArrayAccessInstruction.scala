/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that loads or stores a value in an array.
 *
 * @author Michael Eichberg
 */
abstract class ArrayAccessInstruction extends ConstantLengthInstruction with NoLabels {

    final def mayThrowExceptions: Boolean = true

    final def length: Int = 1

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    def elementTypeComputationalType: ComputationalType

    final override def toString(currentPC: Int): String = toString()
}
