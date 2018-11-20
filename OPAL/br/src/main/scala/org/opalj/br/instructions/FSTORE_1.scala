/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object FSTORE_1 extends FStoreInstruction with ImplicitLocalVariableIndex {

    final val lvIndex = 1

    final val opcode = 68

    final val mnemonic = "fstore_1"

}
