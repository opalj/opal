/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push int constant value 5.
 *
 * @author Michael Eichberg
 */
case object ICONST_5 extends IConstInstruction {

    final val value = 5

    final val opcode = 8

    final val mnemonic = "iconst_5"
}
