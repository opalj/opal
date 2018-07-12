/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that adds two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class AddInstruction extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction {

    final def isShiftInstruction: Boolean = false

    final def operator: String = "+"

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def stackSlotsChange: Int = -computationalType.operandSize

}
