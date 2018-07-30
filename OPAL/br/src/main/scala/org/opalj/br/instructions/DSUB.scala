/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Subtract double.
 *
 * @author Michael Eichberg
 */
case object DSUB extends SubtractInstruction {

    final val opcode = 103

    final val mnemonic = "dsub"

    final val computationalType = ComputationalTypeDouble

}
