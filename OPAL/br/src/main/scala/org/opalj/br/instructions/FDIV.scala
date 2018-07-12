/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Divide float.
 *
 * @author Michael Eichberg
 */
case object FDIV extends FloatingPointDivideInstruction {

    final val opcode = 110

    final val mnemonic = "fdiv"

    final val computationalType = ComputationalTypeFloat

}
