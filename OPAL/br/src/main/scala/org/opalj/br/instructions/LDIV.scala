/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Divide long.
 *
 * @author Michael Eichberg
 */
case object LDIV extends IntegerDivideInstruction {

    final val opcode = 109

    final val mnemonic = "ldiv"

    final val computationalType = ComputationalTypeLong

}
