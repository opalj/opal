/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object LLOAD_1 extends ConstantIndexLLoadInstruction {

    final val lvIndex = 1

    final val opcode = 31

    final val mnemonic = "lload_1"

}
