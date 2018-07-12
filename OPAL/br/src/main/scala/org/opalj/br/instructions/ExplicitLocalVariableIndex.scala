/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Trait that can be mixed in if the local variable index of a load or store instruction
 * ((a,i,l,...)load/store_X) is not predefined as part of the instruction.
 *
 * @author Michael Eichberg
 */
trait ExplicitLocalVariableIndex extends Instruction {

    def lvIndex: Int

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)

        (this eq other) || (
            other.opcode == this.opcode &&
            other.asInstanceOf[ExplicitLocalVariableIndex].lvIndex == this.lvIndex
        )
    }

    final def indexOfNextInstruction(currentPC: Int)(implicit code: Code): Int = {
        indexOfNextInstruction(currentPC, code.isModifiedByWide(currentPC))
    }

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean): Int = {
        if (modifiedByWide)
            currentPC + 3
        else
            currentPC + 2
    }

}

object ExplicitLocalVariableIndex {

    def unapply(i: ExplicitLocalVariableIndex): Some[Int] = Some(i.lvIndex)

}
