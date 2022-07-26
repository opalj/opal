/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable.
 *
 * @author Michael Eichberg
 */
case class LSTORE(lvIndex: Int) extends LStoreInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = LSTORE.opcode

    final def mnemonic: String = "lstore"

}
object LSTORE extends InstructionMetaInformation {

    final val opcode = 55

    def canonicalRepresentation(lvIndex: Int): StoreLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => LSTORE_0
            case 1 => LSTORE_1
            case 2 => LSTORE_2
            case 3 => LSTORE_3
            case _ => new LSTORE(lvIndex)
        }
}
