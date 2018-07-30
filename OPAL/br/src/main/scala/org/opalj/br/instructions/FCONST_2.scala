/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the float constant 2.0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object FCONST_2 extends FConstInstruction {

    final val value = 2.0f

    final val opcode = 13

    final val mnemonic = "fconst_2"

}
