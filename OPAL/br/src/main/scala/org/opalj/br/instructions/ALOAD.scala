/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable.
 *
 * @author Michael Eichberg
 */
case class ALOAD(lvIndex: Int) extends ALoadInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = ALOAD.opcode

    final def mnemonic: String = ALOAD.mnemonic

    override def equals(other: Any): Boolean = other match {
        case that: ALOAD => this.lvIndex == that.lvIndex
        case _           => false
    }

    override def hashCode: Int = ALOAD.opcode * 449 + lvIndex

    override def toString: String = s"ALOAD($lvIndex)"
}
object ALOAD extends InstructionMetaInformation {

    final val opcode = 25

    final val mnemonic = "aload"

    def canonicalRepresentation(lvIndex: Int): LoadLocalVariableInstruction = {
        lvIndex match {
            case 0 => ALOAD_0
            case 1 => ALOAD_1
            case 2 => ALOAD_2
            case 3 => ALOAD_3
            case _ => new ALOAD(lvIndex)
        }
    }

}
