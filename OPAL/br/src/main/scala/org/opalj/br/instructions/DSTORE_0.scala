/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store double into local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object DSTORE_0 extends ConstantIndexDStoreInstruction {

    final val lvIndex = 0

    final val opcode = 71

    final val mnemonic = "dstore_0"

}
