/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object LLOAD_2 extends ConstantIndexLLoadInstruction {

    final val lvIndex = 2

    final val opcode = 32

    final val mnemonic = "lload_2"

}
