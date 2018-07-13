/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the long constant 1 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object LCONST_1 extends LConstInstruction {

    final val value = 1L

    final val opcode = 10

    final val mnemonic = "lconst_1"

}
