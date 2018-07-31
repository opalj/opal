/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return double from method.
 *
 * @author Michael Eichberg
 */
case object DRETURN extends ReturnValueInstruction {

    final val opcode = 175

    final val mnemonic = "dreturn"

    final def returnValueComputationalType: ComputationalType = ComputationalTypeDouble
}
