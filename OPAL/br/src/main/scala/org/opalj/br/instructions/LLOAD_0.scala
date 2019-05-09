/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object LLOAD_0 extends ConstantIndexLLoadInstruction {

    final val lvIndex = 0

    final val opcode = 30

    final val mnemonic = "lload_0"

}
