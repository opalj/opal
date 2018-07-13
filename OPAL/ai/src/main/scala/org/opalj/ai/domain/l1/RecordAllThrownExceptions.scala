/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.collection.Set

/**
 * Records '''all''' exception values thrown by a method. I.e., for each instruction that
 * throws an exception (or multiple exceptions) all exceptions are recorded.
 *
 * @author Michael Eichberg
 */
trait RecordAllThrownExceptions extends domain.RecordThrownExceptions {
    domain: ReferenceValues with Configuration with ExceptionsFactory ⇒

    override type ThrownException = Set[DomainSingleOriginReferenceValue]

    override protected[this] def recordThrownException(
        pc:    Int,
        value: ExceptionValue
    ): ThrownException = {
        value match {
            case MultipleReferenceValues(values)        ⇒ values
            case DomainSingleOriginReferenceValue(sorv) ⇒ Set.empty + sorv
        }
    }

    override protected[this] def joinThrownExceptions(
        pc:                        Int,
        previouslyThrownException: ThrownException,
        value:                     ExceptionValue
    ): ThrownException = {
        value match {
            case MultipleReferenceValues(values)        ⇒ previouslyThrownException ++ values
            case DomainSingleOriginReferenceValue(sorv) ⇒ previouslyThrownException + sorv
        }
    }
}

