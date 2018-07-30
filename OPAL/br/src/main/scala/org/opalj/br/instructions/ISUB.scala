/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Subtract int.
 *
 * @author Michael Eichberg
 */
case object ISUB extends SubtractInstruction {

    final val opcode = 100

    final val mnemonic = "isub"

    final val computationalType = ComputationalTypeInt

}
