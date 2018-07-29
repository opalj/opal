/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from array.
 *
 * @author Michael Eichberg
 */
case object LALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 47

    final val mnemonic = "laload"

    final val elementTypeComputationalType = ComputationalTypeLong
}
