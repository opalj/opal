/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load long from local variable.
 *
 * @author Michael Eichberg
 */
case class LLOAD(lvIndex: Int) extends LLoadInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = LLOAD.opcode

    final def mnemonic: String = "lload"

    override def equals(other: Any): Boolean =
        other match {
            case that: LLOAD => that.lvIndex == this.lvIndex
            case _           => false
        }

    override def hashCode: Int = LLOAD.opcode * 233 + lvIndex

    override def toString: String = s"LLOAD($lvIndex)"

}
object LLOAD extends InstructionMetaInformation {

    final val opcode = 22

    def canonicalRepresentation(lvIndex: Int): LoadLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => LLOAD_0
            case 1 => LLOAD_1
            case 2 => LLOAD_2
            case 3 => LLOAD_3
            case _ => new LLOAD(lvIndex)
        }

}
