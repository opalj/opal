/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store int into local variable.
 *
 * @author Michael Eichberg
 */
case class ISTORE(lvIndex: Int) extends IStoreInstruction with ExplicitLocalVariableIndex {

    final def opcode: Opcode = ISTORE.opcode

    final def mnemonic: String = "istore"

}
object ISTORE extends InstructionMetaInformation {

    final val opcode = 54

    def canonicalRepresentation(lvIndex: Int): StoreLocalVariableInstruction =
        (lvIndex: @scala.annotation.switch) match {
            case 0 => ISTORE_0
            case 1 => ISTORE_1
            case 2 => ISTORE_2
            case 3 => ISTORE_3
            case _ => new ISTORE(lvIndex)
        }

}
