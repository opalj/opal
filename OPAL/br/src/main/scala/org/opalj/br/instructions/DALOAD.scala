/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from array.
 *
 * @author Michael Eichberg
 */
case object DALOAD extends ArrayLoadInstruction with PrimitiveArrayAccess {

    final val opcode = 49

    final val mnemonic = "daload"

    final val elementTypeComputationalType = ComputationalTypeDouble

}
