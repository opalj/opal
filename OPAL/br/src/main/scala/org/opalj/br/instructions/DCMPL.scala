/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Compare double.
 *
 * @author Michael Eichberg
 */
case object DCMPL extends ComparisonInstruction {

    final val opcode = 151

    final val mnemonic = "dcmpl"

    final val operator = "cmpl"

    final val computationalType = ComputationalTypeDouble

}
