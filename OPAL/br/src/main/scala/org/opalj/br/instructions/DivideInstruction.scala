/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that divides two primitive values.
 *
 * @author Michael Eichberg
 */
abstract class DivideInstruction extends StackBasedBinaryArithmeticInstruction {

    final def operator: String = "/"

    final def isShiftInstruction: Boolean = false

    final def stackSlotsChange: Int = -computationalType.operandSize

}
