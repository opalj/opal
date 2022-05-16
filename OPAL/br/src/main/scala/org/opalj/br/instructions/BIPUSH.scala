/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Push byte.
 *
 * @note BIPUSH instructions are inherently cached; two BIPUSH instructions that
 *      push the same value are always reference identical.
 *
 * @author Michael Eichberg
 */
class BIPUSH private (val value: Int) extends LoadConstantInstruction[Int] {

    final def opcode: Opcode = BIPUSH.opcode

    final def mnemonic: String = "bipush"

    final def length: Int = 2

    final def computationalType = ComputationalTypeInt

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            BIPUSH.opcode == other.opcode &&
            this.value == other.asInstanceOf[BIPUSH].value
        )
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: BIPUSH => this eq that
            case _            => false
        }
    }

    override def hashCode = value

    override def toString = "BIPUSH("+value+")"
}

object BIPUSH extends InstructionMetaInformation {

    final val opcode = 16

    private[this] val bipushes = {
        val bipushes = new Array[BIPUSH](256)
        for (i <- -128 to 127) { bipushes(i + 128) = new BIPUSH(i) }
        bipushes
    }

    def apply(value: Int): BIPUSH = bipushes(value + 128)

    def unapply(bipush: BIPUSH): Some[Int] = Some(bipush.value)
}
