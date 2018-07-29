/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Remainder int.
 *
 * @author Michael Eichberg
 */
case object IREM extends IntegerRemainderInstruction {

    final val opcode = 112

    final val mnemonic = "irem"

    final val computationalType = ComputationalTypeInt
}
