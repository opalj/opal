/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object ILOAD_3 extends ConstantIndexILoadInstruction {

    final val lvIndex = 3

    final val opcode = 29

    final val mnemonic = "iload_3"

}
