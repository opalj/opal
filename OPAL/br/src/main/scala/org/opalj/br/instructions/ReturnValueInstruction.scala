/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * An instruction that returns a value calculated by the method.
 *
 * @author Michael Eichberg
 */
abstract class ReturnValueInstruction extends ReturnInstruction {

    final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -returnValueComputationalType.operandSize

    def returnValueComputationalType: ComputationalType

}
