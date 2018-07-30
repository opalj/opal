/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that subtracts two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class SubtractInstruction extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction {

    final override def isShiftInstruction: Boolean = false

    final override def operator: String = "-"

    final override def jvmExceptions: List[ObjectType] = Nil

    final override def mayThrowExceptions: Boolean = false

    final override def stackSlotsChange: Int = -computationalType.operandSize

}
