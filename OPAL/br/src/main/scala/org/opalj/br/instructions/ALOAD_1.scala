/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable with index 1.
 *
 * @author Michael Eichberg
 */
case object ALOAD_1 extends ConstantALoadInstruction {

    final val lvIndex = 1

    final val opcode = 43

    final val mnemonic = "aload_1"

}
