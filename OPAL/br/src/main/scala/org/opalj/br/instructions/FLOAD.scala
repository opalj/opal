/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load float from local variable.
 *
 * @author Michael Eichberg
 */
case class FLOAD(lvIndex: Int) extends FLoadInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = FLOAD.opcode

    final def mnemonic: String = "fload"

    override def equals(other: Any): Boolean =
        other match {
            case that: FLOAD => that.lvIndex == this.lvIndex
            case _           => false
        }

    override def hashCode: Int = FLOAD.opcode * 53 + lvIndex

    override def toString: String = s"FLOAD($lvIndex)"
}

object FLOAD extends InstructionMetaInformation {

    final val opcode = 23

    def canonicalRepresentation(lvIndex: Int): LoadLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => FLOAD_0
            case 1 => FLOAD_1
            case 2 => FLOAD_2
            case 3 => FLOAD_3
            case _ => new FLOAD(lvIndex)
        }
}
