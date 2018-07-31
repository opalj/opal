/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Remainder double.
 *
 * @author Michael Eichberg
 */
case object DREM extends FloatingPointRemainderInstruction {

    final val opcode = 115

    final val mnemonic = "drem"

    final val computationalType = ComputationalTypeDouble

}
