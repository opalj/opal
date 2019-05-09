/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object ILOAD_0 extends ConstantIndexILoadInstruction {

    final val lvIndex = 0

    final val opcode = 26

    final val mnemonic = "iload_0"

}
