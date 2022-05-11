/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable.
 *
 * @author Michael Eichberg
 */
case class FSTORE(lvIndex: Int) extends FStoreInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = FSTORE.opcode

    final def mnemonic: String = "fstore"

}

object FSTORE extends InstructionMetaInformation {

    final val opcode = 56

    def canonicalRepresentation(lvIndex: Int): StoreLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => FSTORE_0
            case 1 => FSTORE_1
            case 2 => FSTORE_2
            case 3 => FSTORE_3
            case _ => new FSTORE(lvIndex)
        }

}
