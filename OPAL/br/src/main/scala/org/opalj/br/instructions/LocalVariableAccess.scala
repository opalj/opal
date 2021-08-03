/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Defines an extractor to determine the local variable index accessed by the instruction.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object LocalVariableAccess {

    def unapply(i: Instruction): Option[(Int, Boolean)] = {
        i match {
            case i: LoadLocalVariableInstruction  => Some((i.indexOfReadLocal, true))
            case i: StoreLocalVariableInstruction => Some((i.indexOfWrittenLocal, false))
            case _                                => None
        }
    }

}
