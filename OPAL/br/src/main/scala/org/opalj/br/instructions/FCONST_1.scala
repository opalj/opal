/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the float constant 1.0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object FCONST_1 extends FConstInstruction {

    final val value = 1.0f

    final val opcode = 12

    final val mnemonic = "fconst_1"

}
