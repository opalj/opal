/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return reference from method.
 *
 * @author Michael Eichberg
 */
case object ARETURN extends ReturnValueInstruction {

    final val opcode = 176

    final val mnemonic = "areturn"

    final def returnValueComputationalType: ComputationalType = ComputationalTypeReference
}
