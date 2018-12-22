/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

trait IStoreInstruction extends StoreLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeInt

    final def stackSlotsChange: Int = -1
}

object IStoreInstruction {

    def unapply(istore: IStoreInstruction): Some[Int] = Some(istore.lvIndex)

}

trait ConstantIndexIStoreInstruction
    extends IStoreInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation
