/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Divide int.
 *
 * @author Michael Eichberg
 */
case object IDIV extends IntegerDivideInstruction {

    final val opcode = 108

    final val mnemonic = "idiv"

    final val computationalType = ComputationalTypeInt

}
