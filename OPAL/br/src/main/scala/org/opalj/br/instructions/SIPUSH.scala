/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push short.
 *
 * @author Michael Eichberg
 */
case class SIPUSH(value: Int) extends LoadConstantInstruction[Int] {

    final def opcode: Opcode = SIPUSH.opcode

    final def mnemonic: String = "sipush"

    final def length: Int = 3

    final def computationalType = ComputationalTypeInt

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            SIPUSH.opcode == other.opcode &&
            this.value == other.asInstanceOf[SIPUSH].value
        )
    }

}

object SIPUSH extends InstructionMetaInformation {

    final val opcode = 17

}
