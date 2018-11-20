/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Multiply double.
 *
 * @author Michael Eichberg
 */
case object DMUL extends MultiplyInstruction {

    final val opcode = 107

    final val mnemonic = "dmul"

    final val computationalType = ComputationalTypeDouble

    final def stackSlotsChange: Int = -2
}
