/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from array.
 *
 * @author Michael Eichberg
 */
case object FALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 48

    final val mnemonic = "faload"

    final val elementTypeComputationalType = ComputationalTypeFloat

}
