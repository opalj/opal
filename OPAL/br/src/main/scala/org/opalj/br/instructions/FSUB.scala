/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Subtract float.
 *
 * @author Michael Eichberg
 */
case object FSUB extends SubtractInstruction {

    final val opcode = 102

    final val mnemonic = "fsub"

    final val computationalType = ComputationalTypeFloat

}
