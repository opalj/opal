/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the double constant 1.0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object DCONST_1 extends DConstInstruction {

    final val value = 1.0d

    final val opcode = 15

    final val mnemonic = "dconst_1"

}
