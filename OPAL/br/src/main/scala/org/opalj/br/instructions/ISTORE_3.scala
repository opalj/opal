/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store int into local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object ISTORE_3 extends ConstantIndexIStoreInstruction {

    final val lvIndex = 3

    final val opcode = 62

    final val mnemonic = "istore_3"

}
