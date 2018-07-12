/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Trait that is mixed in if the local variable index of a load or store instruction
 * ((a,i,l,...)load/store_X) is predefined.
 *
 * @author Michael Eichberg
 */
trait ImplicitLocalVariableIndex extends ConstantLengthInstruction {

    final def length: Int = 1

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        (this eq code.instructions(otherPC))
    }

}
