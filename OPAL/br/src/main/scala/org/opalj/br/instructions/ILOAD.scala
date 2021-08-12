/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load int from local variable.
 *
 * @author Michael Eichberg
 */
case class ILOAD(lvIndex: Int) extends ILoadInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = ILOAD.opcode

    final def mnemonic: String = "iload"

    override def equals(other: Any): Boolean =
        other match {
            case that: ILOAD => that.lvIndex == this.lvIndex
            case _           => false
        }

    override def hashCode: Int = ILOAD.opcode * 71 + lvIndex

    override def toString: String = s"ILOAD($lvIndex)"
}

object ILOAD extends InstructionMetaInformation {

    final val opcode = 21

    def canonicalRepresentation(lvIndex: Int): LoadLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => ILOAD_0
            case 1 => ILOAD_1
            case 2 => ILOAD_2
            case 3 => ILOAD_3
            case _ => new ILOAD(lvIndex)
        }

}
