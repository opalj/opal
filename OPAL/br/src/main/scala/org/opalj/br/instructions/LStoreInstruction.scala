/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable.
 *
 * @author Michael Eichberg
 */
trait LStoreInstruction extends StoreLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeLong

    final def stackSlotsChange: Int = -2
}

object LStoreInstruction {

    def unapply(lstore: LStoreInstruction): Some[Int] = Some(lstore.lvIndex)
}

trait ConstantIndexLStoreInstruction
    extends LStoreInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation
