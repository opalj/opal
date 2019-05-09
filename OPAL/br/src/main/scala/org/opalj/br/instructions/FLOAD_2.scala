/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object FLOAD_2 extends ConstantIndexFLoadInstruction {

    final val lvIndex = 2

    final val opcode = 36

    final val mnemonic = "fload_2"

}
