/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Remainder float.
 *
 * @author Michael Eichberg
 */
case object FREM extends FloatingPointRemainderInstruction {

    final val opcode = 114

    final val mnemonic = "frem"

    final val computationalType = ComputationalTypeFloat

}
