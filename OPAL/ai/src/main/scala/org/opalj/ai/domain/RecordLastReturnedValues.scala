/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Records the last value that is returned by a specific return instruction.
 *
 * Recording just the last value that is returned by an `(a|i|l|f|d)return` instruction
 * is often sufficient (e.g., in case of a domain that performs all computations at the type
 * level). The "last" value encodes all necessary information.
 *
 * @author Michael Eichberg
 */
trait RecordLastReturnedValues extends RecordReturnedValues {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    type ReturnedValue = DomainValue

    protected[this] def recordReturnedValue(
        pc:    Int,
        value: DomainValue
    ): ReturnedValue = value

    protected[this] def joinReturnedValues(
        pc:                      Int,
        previouslyReturnedValue: ReturnedValue,
        value:                   DomainValue
    ): ReturnedValue = value

}

