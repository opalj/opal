/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object LSTORE_1 extends ConstantIndexLStoreInstruction {

    final val lvIndex = 1

    final val opcode = 64

    final val mnemonic = "lstore_1"

}
