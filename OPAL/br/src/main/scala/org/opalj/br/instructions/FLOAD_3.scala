/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object FLOAD_3 extends ConstantIndexFLoadInstruction {

    final val lvIndex = 3

    final val opcode = 37

    final val mnemonic = "fload_3"

}
