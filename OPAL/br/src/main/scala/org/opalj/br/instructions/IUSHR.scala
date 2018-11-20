/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Logical shift right int.
 *
 * @author Michael Eichberg
 */
case object IUSHR extends ShiftInstruction {

    final val opcode = 124

    final val mnemonic = "iushr"

    final val operator = ">>>"

    final val computationalType = ComputationalTypeInt
}
