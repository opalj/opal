/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object DLOAD_3 extends ConstantIndexDLoadInstruction {

    final val lvIndex = 3

    final val opcode = 41

    final val mnemonic = "dload_3"

}
