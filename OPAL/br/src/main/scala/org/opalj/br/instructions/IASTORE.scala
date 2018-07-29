/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into int array.
 *
 * @author Michael Eichberg
 */
case object IASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 79

    final val mnemonic = "iastore"

    final val elementTypeComputationalType = ComputationalTypeInt
}
