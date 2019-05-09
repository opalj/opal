/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from array.
 *
 * @author Michael Eicberg
 */
case object AALOAD extends ArrayLoadInstruction with InstructionMetaInformation {

    final val opcode = 50

    final val mnemonic = "aaload"

    final val elementTypeComputationalType = ComputationalTypeReference

}
