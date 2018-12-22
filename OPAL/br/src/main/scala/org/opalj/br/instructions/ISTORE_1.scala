/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store int into local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object ISTORE_1 extends ConstantIndexIStoreInstruction {

    final val lvIndex = 1

    final val opcode = 60

    final val mnemonic = "istore_1"

}
