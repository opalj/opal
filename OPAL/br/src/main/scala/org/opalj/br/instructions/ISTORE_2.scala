/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store int into local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object ISTORE_2 extends ConstantIndexIStoreInstruction {

    final val lvIndex = 2

    final val opcode = 61

    final val mnemonic = "istore_2"

}
