/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object DLOAD_2 extends ConstantIndexDLoadInstruction {

    final val lvIndex = 2

    final val opcode = 40

    final val mnemonic = "dload_2"

}
