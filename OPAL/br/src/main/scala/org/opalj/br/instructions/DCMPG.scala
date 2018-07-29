/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Compare double.
 *
 * @author Michael Eichberg
 */
case object DCMPG extends ComparisonInstruction {

    final val opcode = 152

    final val mnemonic = "dcmpg"

    final val operator = "cmpg"

    final val computationalType = ComputationalTypeDouble

}
