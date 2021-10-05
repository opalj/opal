/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load double from local variable.
 *
 * @author Michael Eichberg
 */
case class DLOAD(lvIndex: Int) extends DLoadInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = DLOAD.opcode

    final def mnemonic: String = "dload"

    override def equals(other: Any): Boolean =
        other match {
            case that: DLOAD => that.lvIndex == this.lvIndex
            case _           => false
        }

    override def hashCode: Int = DLOAD.opcode * 97 + lvIndex

    override def toString: String = s"DLOAD($lvIndex)"
}

object DLOAD extends InstructionMetaInformation {

    final val opcode = 24

    def canonicalRepresentation(lvIndex: Int): LoadLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => DLOAD_0
            case 1 => DLOAD_1
            case 2 => DLOAD_2
            case 3 => DLOAD_3
            case _ => new DLOAD(lvIndex)
        }

}
