/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object FLOAD_1 extends ConstantIndexFLoadInstruction {

    final val lvIndex = 1

    final val opcode = 35

    final val mnemonic = "fload_1"

}
