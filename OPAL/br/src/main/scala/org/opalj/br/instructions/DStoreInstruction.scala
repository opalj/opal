/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Store long into local variable.
 *
 * @author Michael Eichberg
 */
trait DStoreInstruction extends StoreLocalVariableInstruction {

    def computationalType: ComputationalType = ComputationalTypeDouble

    final def stackSlotsChange: Int = -2
}

object DStoreInstruction {

    def unapply(dstore: DStoreInstruction): Option[Int] = Some(dstore.lvIndex)
}

trait ConstantIndexDStoreInstruction
    extends DStoreInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation