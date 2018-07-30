/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the long constant 0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object LCONST_0 extends LConstInstruction {

    final val value = 0L

    final val opcode = 9

    final val mnemonic = "lconst_0"

}
