/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Boolean AND long.
 *
 * @author Michael Eichberg
 */
case object LAND extends BitwiseInstruction {

    final val opcode = 127

    final val mnemonic = "land"

    final val operator = "&"

    final val computationalType = ComputationalTypeLong

}
