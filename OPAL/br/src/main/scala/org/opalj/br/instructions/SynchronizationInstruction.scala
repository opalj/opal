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

    final override def length: Int = 1

    final override def isMonitorInstruction: Boolean = true

    final override def mayThrowExceptions: Boolean = true

    final override def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final override def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    final override def readsLocal: Boolean = false

    final override def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final override def writesLocal: Boolean = false

    final override def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def expressionResult: NoExpression.type = NoExpression

    final override def toString(currentPC: Int): String = toString()
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
