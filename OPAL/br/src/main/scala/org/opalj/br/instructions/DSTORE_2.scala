/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store double into local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object DSTORE_2 extends ConstantIndexDStoreInstruction {

    final val lvIndex = 2

    final val opcode = 73

    final val mnemonic = "dstore_2"

}
