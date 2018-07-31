/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Negate long.
 *
 * @author Michael Eichberg
 */
case object LNEG extends NegateInstruction {

    final val opcode = 117

    final val mnemonic = "lneg"

    final val computationalType = ComputationalTypeLong

}
