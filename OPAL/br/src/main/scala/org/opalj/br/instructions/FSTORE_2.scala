/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object FSTORE_2 extends ConstantIndexFStoreInstruction {

    final val lvIndex = 2

    final val opcode = 69

    final val mnemonic = "fstore_2"

}
