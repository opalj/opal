/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push null.
 *
 * @author Michael Eichberg
 */
case object ACONST_NULL
    extends LoadConstantInstruction[Null]
    with ImplicitValue
    with InstructionMetaInformation {

    final def computationalType = ComputationalTypeReference

    final val value: Null = null

    final val opcode = 1

    final val mnemonic = "aconst_null"

}
