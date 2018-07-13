/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean OR int.
 *
 * @author Michael Eichberg
 */
case object IOR extends BitwiseInstruction {

    final val opcode = 128

    final val mnemonic = "ior"

    final val operator = "|"

    final val computationalType = ComputationalTypeInt
}
