/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Trait that can be mixed in if the value of a instruction is implicitly defined.
 *
 * @author Michael Eichberg
 */
trait ImplicitValue extends ConstantLengthInstruction with InstructionMetaInformation {

    final def length: Int = 1

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (other.opcode == this.opcode)
    }

}
