/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Negate double.
 *
 * @author Michael Eichberg
 */
case object DNEG extends NegateInstruction {

    final val opcode = 119

    final val mnemonic = "dneg"

    final val computationalType = ComputationalTypeDouble

}
