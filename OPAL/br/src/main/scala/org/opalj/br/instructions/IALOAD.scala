/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from array.
 *
 * @author Michael Eichberg
 */
case object IALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 46

    final val mnemonic = "iaload"

    final val elementTypeComputationalType = ComputationalTypeInt
}
