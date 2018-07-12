/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that adds two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class ShiftInstruction extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def isShiftInstruction: Boolean = true

    final def stackSlotsChange: Int = -1

}

object ShiftInstruction {

    def unapply(instruction: ShiftInstruction): Option[ComputationalType] = {
        Some(instruction.computationalType)
    }
}
