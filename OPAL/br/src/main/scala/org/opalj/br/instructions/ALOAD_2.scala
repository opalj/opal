/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable with index 2.
 *
 * @author Michael Eichberg
 */
case object ALOAD_2 extends ConstantALoadInstruction {

    final val lvIndex = 2

    final val opcode = 44

    final val mnemonic = "aload_2"

}
