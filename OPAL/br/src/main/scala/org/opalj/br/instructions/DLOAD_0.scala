/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object DLOAD_0 extends ConstantIndexDLoadInstruction {

    final val lvIndex = 0

    final val opcode = 38

    final val mnemonic = "dload_0"

}
