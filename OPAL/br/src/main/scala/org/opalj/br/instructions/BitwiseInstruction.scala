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

    final override def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final override def stackSlotsChange: Int = -computationalType.operandSize

    final override def isShiftInstruction: Boolean = false

}
