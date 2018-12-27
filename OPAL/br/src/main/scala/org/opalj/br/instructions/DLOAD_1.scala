/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object DLOAD_1 extends ConstantIndexDLoadInstruction {

    final val lvIndex = 1

    final val opcode = 39

    final val mnemonic = "dload_1"

}
