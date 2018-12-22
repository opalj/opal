/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable with index 3.
 *
 * @author Michael Eichberg
 */
case object ALOAD_3 extends ConstantALoadInstruction {

    final val lvIndex = 3

    final val opcode = 45

    final val mnemonic = "aload_3"

}
