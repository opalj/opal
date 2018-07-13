/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Compare float.
 *
 * @author Michael Eichberg
 */
case object FCMPL extends ComparisonInstruction {

    final val opcode = 149

    final val mnemonic = "fcmpl"

    final val operator = "cmpl"

    final val computationalType = ComputationalTypeFloat

}
