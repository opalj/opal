/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Negate int.
 *
 * @author Michael Eichberg
 */
case object INEG extends NegateInstruction {

    final val opcode = 116

    final val mnemonic = "ineg"

    final val computationalType = ComputationalTypeInt

}
