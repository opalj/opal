/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push the double constant 0.0 onto the operand stack.
 *
 * @author Michael Eichberg
 */
case object DCONST_0 extends DConstInstruction {

    final val value = 0.0d

    final val opcode = 14

    final val mnemonic = "dconst_0"

}
