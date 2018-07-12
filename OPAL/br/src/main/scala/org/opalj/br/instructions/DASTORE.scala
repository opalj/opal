/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into double array.
 *
 * @author Michael Eichberg
 */
case object DASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 82

    final val mnemonic = "dastore"

    final val elementTypeComputationalType = ComputationalTypeDouble
}
