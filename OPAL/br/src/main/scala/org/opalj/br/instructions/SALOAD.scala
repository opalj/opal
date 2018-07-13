/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load short from array.
 *
 * @author Michael Eichberg
 */
case object SALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 53

    final val mnemonic = "saload"

    final val elementTypeComputationalType = ComputationalTypeInt
}
