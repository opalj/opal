/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object LLOAD_3 extends ConstantIndexLLoadInstruction {

    final val lvIndex = 3

    final val opcode = 33

    final val mnemonic = "lload_3"

}
