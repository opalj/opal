/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Compare float.
 *
 * @author Michael Eichberg
 */
case object FCMPG extends ComparisonInstruction {

    final val opcode = 150

    final val mnemonic = "fcmpg"

    final val operator = "cmpg"

    final val computationalType = ComputationalTypeFloat

}
