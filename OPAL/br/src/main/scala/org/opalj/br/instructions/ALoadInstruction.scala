/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Load reference from local variable.
 *
 * @author Michael Eichberg
 */
trait ALoadInstruction extends LoadLocalVariableInstruction {

    final def computationalType: ComputationalType = ComputationalTypeReference

    final def stackSlotsChange: Int = +1

}
object ALoadInstruction {

    /**
     * Extracts the index of the accessed local variable.
     */
    def unapply(li: ALoadInstruction): Some[Int] = Some(li.lvIndex)

}

trait ConstantALoadInstruction
    extends ALoadInstruction
    with ImplicitLocalVariableIndex
    with InstructionMetaInformation
