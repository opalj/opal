/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Divide double.
 *
 * @author Michael Eichberg
 */
case object DDIV extends FloatingPointDivideInstruction {

    final val opcode = 111

    final val mnemonic = "ddiv"

    final val computationalType = ComputationalTypeDouble

}
