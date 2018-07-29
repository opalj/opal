/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return int from method.
 *
 * @author Michael Eichberg
 */
case object IRETURN extends ReturnValueInstruction {

    final val opcode = 172

    final val mnemonic = "ireturn"

    final def returnValueComputationalType: ComputationalType = ComputationalTypeInt

}
