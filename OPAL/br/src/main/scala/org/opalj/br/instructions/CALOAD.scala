/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load char from array.
 *
 * @author Michael Eichberg
 */
case object CALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 52

    final val mnemonic = "caload"

    final val elementTypeComputationalType = ComputationalTypeInt
}
