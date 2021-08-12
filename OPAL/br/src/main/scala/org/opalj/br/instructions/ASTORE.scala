/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store reference into local variable.
 *
 * @author Michael Eichberg
 */
case class ASTORE(lvIndex: Int) extends AStoreInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = ASTORE.opcode

    final def mnemonic: String = "astore"

}

object ASTORE extends InstructionMetaInformation {

    final val opcode = 58

    def canonicalRepresentation(lvIndex: Int): StoreLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => ASTORE_0
            case 1 => ASTORE_1
            case 2 => ASTORE_2
            case 3 => ASTORE_3
            case _ => new ASTORE(lvIndex)
        }
}