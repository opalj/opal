/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object LSTORE_0 extends ConstantIndexLStoreInstruction {

    final val lvIndex = 0

    final val opcode = 63

    final val mnemonic = "lstore_0"

}
