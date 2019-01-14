/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object FSTORE_0 extends ConstantIndexFStoreInstruction {

    final val lvIndex = 0

    final val opcode = 67

    final val mnemonic = "fstore_0"
}
