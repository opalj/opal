/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into short array.
 *
 * @author Michael Eichberg
 */
case object SASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 86

    final val mnemonic = "sastore"

    final val elementTypeComputationalType = ComputationalTypeInt
}
