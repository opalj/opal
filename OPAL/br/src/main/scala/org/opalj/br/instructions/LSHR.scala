/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Arithmetic shift right long.
 *
 * @author Michael Eichberg
 */
case object LSHR extends ShiftInstruction {

    final val opcode = 123

    final val mnemonic = "lshr"

    final val operator = ">>"

    final val computationalType = ComputationalTypeLong

}
