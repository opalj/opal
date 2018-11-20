/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean XOR int.
 *
 * @author Michael Eichberg
 */
case object IXOR extends BitwiseInstruction {

    final val opcode = 130

    final val mnemonic = "ixor"

    final val operator = "^"

    final val computationalType = ComputationalTypeInt
}
