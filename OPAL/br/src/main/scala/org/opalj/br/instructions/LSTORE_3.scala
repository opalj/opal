/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object LSTORE_3 extends ConstantIndexLStoreInstruction {

    final val lvIndex = 3

    final val opcode = 66

    final val mnemonic = "lstore_3"

}
