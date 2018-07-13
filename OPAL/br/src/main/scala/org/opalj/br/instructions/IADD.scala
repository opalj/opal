/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Add int.
 *
 * @author Michael Eichberg
 */
case object IADD extends AddInstruction {

    final val opcode = 96

    final val mnemonic = "iadd"

    final val computationalType = ComputationalTypeInt

}
