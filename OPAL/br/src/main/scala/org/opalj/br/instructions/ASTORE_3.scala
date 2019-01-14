/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store reference into local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object ASTORE_3 extends ConstantIndexAStoreInstruction {

    final val lvIndex = 3

    final val opcode = 78

    final val mnemonic = "astore_3"

}
