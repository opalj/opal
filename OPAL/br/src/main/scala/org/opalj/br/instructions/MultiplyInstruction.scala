/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that multiplies two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class MultiplyInstruction extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction {

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def operator: String = "*"

    final def isShiftInstruction: Boolean = false

}
