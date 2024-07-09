/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that performs a manipulation of a value's bits.
 *
 * @author Michael Eichberg
 */
abstract class BitwiseInstruction
    extends AlwaysSucceedingStackBasedBinaryArithmeticInstruction
    with InstructionMetaInformation {

    override final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    override final def stackSlotsChange: Int = -computationalType.operandSize

    override final def isShiftInstruction: Boolean = false

}
