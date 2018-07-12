/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Arithmetic shift right int.
 *
 * @author Michael Eichberg
 */
case object ISHR extends ShiftInstruction {

    final val opcode = 122

    final val mnemonic = "ishr"

    final val operator = ">>"

    final val computationalType = ComputationalTypeInt
}
