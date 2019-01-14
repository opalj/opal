/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store reference into local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object ASTORE_0 extends ConstantIndexAStoreInstruction {

    final val lvIndex = 0

    final val opcode = 75

    final val mnemonic = "astore_0"

}
