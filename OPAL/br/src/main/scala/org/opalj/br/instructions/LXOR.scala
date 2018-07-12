/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean XOR long.
 *
 * @author Michael Eichberg
 */
case object LXOR extends BitwiseInstruction {

    final val opcode = 131

    final val mnemonic = "lxor"

    final val operator = "^"

    final val computationalType = ComputationalTypeLong

}
