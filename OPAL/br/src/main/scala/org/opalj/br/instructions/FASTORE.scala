/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into float array.
 *
 * @author Michael Eichberg
 */
case object FASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 81

    final val mnemonic = "fastore"

    final val elementTypeComputationalType = ComputationalTypeFloat

}
