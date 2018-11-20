/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Logical shift right long.
 *
 * @author Michael Eichberg
 */
case object LUSHR extends ShiftInstruction {

    final val opcode = 125

    final val mnemonic = "lushr"

    final val operator = ">>>"

    final val computationalType = ComputationalTypeLong

}
