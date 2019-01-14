/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store double into local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object DSTORE_1 extends ConstantIndexDStoreInstruction {

    final val lvIndex = 1

    final val opcode = 72

    final val mnemonic = "dstore_1"

}
