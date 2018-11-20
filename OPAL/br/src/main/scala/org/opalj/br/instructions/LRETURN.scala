/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return long from method.
 *
 * @author Michael Eichberg
 */
case object LRETURN extends ReturnValueInstruction {

    final val opcode = 173

    final val mnemonic = "lreturn"

    final def returnValueComputationalType: ComputationalType = ComputationalTypeLong
}
