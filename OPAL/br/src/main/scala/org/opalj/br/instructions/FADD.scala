/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Add float.
 *
 * @author Michael Eichberg
 */
case object FADD extends AddInstruction {

    final val opcode = 98

    final val mnemonic = "fadd"

    final val computationalType = ComputationalTypeFloat

}
