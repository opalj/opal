/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object FLOAD_0 extends ConstantIndexFLoadInstruction {

    final val lvIndex = 0

    final val opcode = 34

    final val mnemonic = "fload_0"

}
