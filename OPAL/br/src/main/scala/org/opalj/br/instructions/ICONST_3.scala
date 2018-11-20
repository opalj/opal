/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 3.
 *
 * @author Michael Eichberg
 */
case object ICONST_3 extends IConstInstruction {

    final val value = 3

    final val opcode = 6

    final val mnemonic = "iconst_3"

}
