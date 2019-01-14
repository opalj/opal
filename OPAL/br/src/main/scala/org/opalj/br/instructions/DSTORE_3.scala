/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store double into local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object DSTORE_3 extends ConstantIndexDStoreInstruction {

    final val lvIndex = 3

    final val opcode = 74

    final val mnemonic = "dstore_3"

}
