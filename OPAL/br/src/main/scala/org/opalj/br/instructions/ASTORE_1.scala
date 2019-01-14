/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store reference into local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object ASTORE_1 extends ConstantIndexAStoreInstruction {

    final val lvIndex = 1

    final val opcode = 76

    final val mnemonic = "astore_1"

}
