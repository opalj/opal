/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object ILOAD_1 extends ConstantIndexILoadInstruction {

    final val lvIndex = 1

    final val opcode = 27

    final val mnemonic = "iload_1"

}
