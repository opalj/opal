/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Negate float.
 *
 * @author Michael Eichberg
 */
case object FNEG extends NegateInstruction {

    final val opcode = 118

    final val mnemonic = "fneg"

    final val computationalType = ComputationalTypeFloat

}
