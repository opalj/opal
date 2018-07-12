/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 0.
 *
 * @author Michael Eichberg
 */
case object ICONST_0 extends IConstInstruction {

    final val value = 0

    final val opcode = 3

    final val mnemonic = "iconst_0"

}
