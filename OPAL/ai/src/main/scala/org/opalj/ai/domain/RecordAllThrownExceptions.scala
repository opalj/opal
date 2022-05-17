/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.collection.immutable

/**
 * Records '''all''' exceptions thrown by a method. I.e., for each instruction that
 * throws an exception (or multiple exceptions) all exceptions are recorded.
 *
 * @note This domain requires that `DomainValue`s that represent thrown exceptions
 *      have meaningful `equals` and `hashCode` methods. (Depending on the purpose
 *      of the abstract interpretation, reference equality may be sufficient.)
 *
 * @note This domain is only effective if the calculation of joins is fast. Otherwise
 *      it can significantly hamper overall performance!
 *
 * @author Michael Eichberg
 */
trait RecordAllThrownExceptions extends RecordThrownExceptions {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    type ThrownException = immutable.Set[ExceptionValue]

    override protected[this] def recordThrownException(
        pc:    Int,
        value: ExceptionValue
    ): ThrownException = immutable.Set(value)

    override protected[this] def joinThrownExceptions(
        pc:                        Int,
        previouslyThrownException: ThrownException,
        value:                     ExceptionValue
    ): ThrownException = previouslyThrownException + value

}

