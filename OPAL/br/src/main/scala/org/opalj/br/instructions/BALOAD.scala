/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load byte or boolean from array.
 *
 * @author Michael Eichberg
 */
case object BALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 51

    final val mnemonic = "baload"

    final val elementTypeComputationalType = ComputationalTypeInt
}
