/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object LSTORE_2 extends ConstantIndexLStoreInstruction {

    final val lvIndex = 2

    final val opcode = 65

    final val mnemonic = "lstore_2"
}
