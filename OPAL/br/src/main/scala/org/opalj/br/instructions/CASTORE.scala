/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into char array.
 *
 * @author Michael Eichberg
 */
case object CASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 85

    final val mnemonic = "castore"

    final val elementTypeComputationalType = ComputationalTypeInt
}
