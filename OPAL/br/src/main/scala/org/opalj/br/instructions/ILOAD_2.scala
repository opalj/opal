/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object ILOAD_2 extends ConstantIndexILoadInstruction {

    final val lvIndex = 2

    final val opcode = 28

    final val mnemonic = "iload_2"

}
