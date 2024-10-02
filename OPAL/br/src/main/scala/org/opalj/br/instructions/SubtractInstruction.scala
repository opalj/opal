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

    override final def isShiftInstruction: Boolean = false

    override final def operator: String = "-"

    override final def jvmExceptions: List[ObjectType] = Nil

    override final def mayThrowExceptions: Boolean = false

    override final def stackSlotsChange: Int = -computationalType.operandSize

}
