/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into byte or boolean array.
 *
 * @author Michael Eichberg
 */
case object BASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 84

    final val mnemonic = "bastore"

    final val elementTypeComputationalType = ComputationalTypeInt
}
