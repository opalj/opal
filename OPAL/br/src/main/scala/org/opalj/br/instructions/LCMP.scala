/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Compare long.
 *
 * @author Michael Eichberg
 */
case object LCMP extends ComparisonInstruction {

    final val opcode = 148

    final val mnemonic = "lcmp"

    final val operator = "cmp"

    final val computationalType = ComputationalTypeLong
}
