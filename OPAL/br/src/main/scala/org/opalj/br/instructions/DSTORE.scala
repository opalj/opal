/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store double into local variable.
 *
 * @author Michael Eichberg
 */
case class DSTORE(lvIndex: Int) extends DStoreInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = DSTORE.opcode

    final def mnemonic: String = "dstore"

}

object DSTORE extends InstructionMetaInformation {

    final val opcode = 57

    def canonicalRepresentation(lvIndex: Int): StoreLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => DSTORE_0
            case 1 => DSTORE_1
            case 2 => DSTORE_2
            case 3 => DSTORE_3
            case _ => new DSTORE(lvIndex)
        }
}
