/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store int into local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object ISTORE_0 extends ConstantIndexIStoreInstruction {

    final val lvIndex = 0

    final val opcode = 59

    final val mnemonic = "istore_0"

}
