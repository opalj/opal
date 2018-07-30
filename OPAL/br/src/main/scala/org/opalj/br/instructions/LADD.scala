/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Add long.
 *
 * @author Michael Eichberg
 */
case object LADD extends AddInstruction {

    final val opcode = 97

    final val mnemonic = "ladd"

    final val computationalType = ComputationalTypeLong

}
