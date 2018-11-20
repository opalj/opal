/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant.
 *
 * @author Michael Eichberg
 */
case object ICONST_M1 extends IConstInstruction {

    final val value = -1

    final val opcode = 2

    final val mnemonic = "iconst_m1"

}
