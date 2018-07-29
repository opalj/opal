/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the float constant 0.0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object FCONST_0 extends FConstInstruction {

    final val value = 0.0f

    final val opcode = 11

    final val mnemonic = "fconst_0"

}
