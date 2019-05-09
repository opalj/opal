/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store reference into local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object ASTORE_2 extends ConstantIndexAStoreInstruction {

    final val lvIndex = 2

    final val opcode = 77

    final val mnemonic = "astore_2"

}
