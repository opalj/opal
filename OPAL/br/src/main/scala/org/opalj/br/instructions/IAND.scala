/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean AND int.
 *
 * @author Michael Eichberg
 */
case object IAND extends BitwiseInstruction {

    final val opcode = 126

    final val mnemonic = "iand"

    final def operator: String = "&"

    final val computationalType = ComputationalTypeInt
}
