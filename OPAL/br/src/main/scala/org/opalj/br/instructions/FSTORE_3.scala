/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object FSTORE_3 extends ConstantIndexFStoreInstruction {

    final val lvIndex = 3

    final val opcode = 70

    final val mnemonic = "fstore_3"

}
