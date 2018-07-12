/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Implemented by all arithmetic instructions that have one (runtime-dependent) operand;
 * i.e., the [[NegateInstruction]]s.
 *
 * @author Michael Eichberg
 */
trait UnaryArithmeticInstruction extends ArithmeticInstruction {

    def isPrefixOperator: Boolean

    def isPostfixOperator: Boolean = !isPrefixOperator

}

object UnaryArithmeticInstruction {

    def unapply(instruction: UnaryArithmeticInstruction): Option[ComputationalType] = {
        Some(instruction.computationalType)
    }
}
