/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable with index 0.
 *
 * @author Michael Eichberg
 */
case object ALOAD_0 extends ConstantALoadInstruction {

    final val lvIndex = 0

    final val opcode = 42

    final val mnemonic = "aload_0"

}
