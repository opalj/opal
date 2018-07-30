/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Add double.
 *
 * @author Michael Eichberg
 */
case object DADD extends AddInstruction {

    final val opcode = 99

    final val mnemonic = "dadd"

    final val computationalType = ComputationalTypeDouble

}
