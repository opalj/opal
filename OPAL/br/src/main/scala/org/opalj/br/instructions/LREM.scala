/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Remainder long.
 *
 * @author Michael Eichberg
 */
case object LREM extends IntegerRemainderInstruction {

    final val opcode = 113

    final val mnemonic = "lrem"

    final val computationalType = ComputationalTypeLong

}
