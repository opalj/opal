/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean OR long.
 *
 * @author Michael Eichberg
 */
case object LOR extends BitwiseInstruction {

    final val opcode = 129

    final val mnemonic = "lor"

    final val operator = "|"

    final val computationalType = ComputationalTypeLong

}
