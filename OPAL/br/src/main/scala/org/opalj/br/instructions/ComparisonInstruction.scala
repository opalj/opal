/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that compares two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class ComparisonInstruction extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def stackSlotsChange: Int = {
        // take two 2 values and push one int value
        -2 * computationalType.operandSize + 1
    }

    final def isShiftInstruction: Boolean = false

}
