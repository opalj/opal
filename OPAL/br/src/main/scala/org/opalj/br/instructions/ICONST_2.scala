/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 2.
 *
 * @author Michael Eichberg
 */
case object ICONST_2 extends IConstInstruction {

    final val value = 2

    final val opcode = 5

    final val mnemonic = "iconst_2"

}
