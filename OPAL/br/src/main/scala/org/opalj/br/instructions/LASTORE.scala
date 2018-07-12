/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store into long array.
 *
 * @author Michael Eichberg
 */
case object LASTORE extends PrimitiveArrayStoreInstruction with PrimitiveArrayAccess {

    final val opcode = 80

    final val mnemonic = "lastore"

    final val elementTypeComputationalType = ComputationalTypeLong
}
