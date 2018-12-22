/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store float into local variable.
 *
 * @author Michael Eichberg
 */
trait FStoreInstruction extends StoreLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeFloat

    final def stackSlotsChange: Int = -1
}

object FStoreInstruction {

    def unapply(fstore: FStoreInstruction): Some[Int] = Some(fstore.lvIndex)
}

trait ConstantIndexFStoreInstruction
    extends FStoreInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation