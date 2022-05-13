/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that negates a primitive value.
 *
 * @author Michael Eichberg
 */
abstract class NegateInstruction
    extends StackBasedArithmeticInstruction
    with UnaryArithmeticInstruction {

    final def operator: String = "-"

    final def isPrefixOperator: Boolean = true

    final def isShiftInstruction: Boolean = false

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = 0

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }
}
