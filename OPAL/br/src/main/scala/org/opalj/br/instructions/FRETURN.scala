/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Return float from method.
 *
 * @author Michael Eichberg
 */
case object FRETURN extends ReturnValueInstruction {

    final val opcode = 174

    final val mnemonic = "freturn"

    final def returnValueComputationalType: ComputationalType = ComputationalTypeFloat
}
