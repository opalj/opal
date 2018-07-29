/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 4.
 *
 * @author Michael Eichberg
 */
case object ICONST_4 extends IConstInstruction {

    final val value = 4

    final val opcode = 7

    final val mnemonic = "iconst_4"

}
