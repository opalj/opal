/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 1.
 *
 * @author Michael Eichberg
 */
case object ICONST_1 extends IConstInstruction {

    final val value = 1

    final val opcode = 4

    final val mnemonic = "iconst_1"

}
