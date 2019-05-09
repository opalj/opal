/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that stores the top-most stack value with computational type
 * reference value OR return address in a local variable.
 *
 * @author Michael Eichberg
 */
trait AStoreInstruction extends StoreLocalVariableInstruction {

    def computationalType: ComputationalType = ComputationalTypeReference

    final def stackSlotsChange: Int = -1
}

object AStoreInstruction {

    def unapply(astore: AStoreInstruction): Some[Int] = Some(astore.lvIndex)

}

trait ConstantIndexAStoreInstruction
    extends AStoreInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation