/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Subtract long.
 *
 * @author Michael Eichberg
 */
case object LSUB extends SubtractInstruction {

    final val opcode = 101

    final val mnemonic = "lsub"

    final val computationalType = ComputationalTypeLong

}
