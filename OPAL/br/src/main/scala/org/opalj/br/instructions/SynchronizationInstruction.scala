/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * A synchronization related instruction (MonitorEnter|MonitorExit).
 *
 * @author Michael Eichberg
 */
abstract class SynchronizationInstruction
    extends Instruction
    with ConstantLengthInstruction
    with NoLabels {

    override final def length: Int = 1

    override final def isMonitorInstruction: Boolean = true

    override final def mayThrowExceptions: Boolean = true

    override final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    override final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    override final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    override final def readsLocal: Boolean = false

    override final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    override final def writesLocal: Boolean = false

    override final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    override final def expressionResult: NoExpression.type = NoExpression

    override final def toString(currentPC: Int): String = toString()
}

object SynchronizationInstruction {

    /**
     * Extractor to match SynchronizationInstructions.
     *
     * ==Example==
     * To use this matcher, do not forget the parentheses. E.g.,
     * {{{
     * case SynchronizationInstruction() => ...
     * }}}
     */
    def unapply(si: SynchronizationInstruction): Boolean = si ne null
}
