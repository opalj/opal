/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.collection.immutable

/**
 * Records '''all''' exception values thrown by a method. I.e., for each instruction that
 * throws an exception (or multiple exceptions) all exceptions are recorded.
 *
 * @author Michael Eichberg
 */
trait RecordAllThrownExceptions extends domain.RecordThrownExceptions {
    domain: ReferenceValues with Configuration with ExceptionsFactory =>

    override type ThrownException = immutable.Set[DomainSingleOriginReferenceValue]

    override protected[this] def recordThrownException(
        pc:    Int,
        value: ExceptionValue
    ): ThrownException = {
        value match {
            case DomainMultipleReferenceValuesTag(value)   => value.values
            case DomainSingleOriginReferenceValueTag(sorv) => immutable.Set.empty + sorv
        }
    }

    override protected[this] def joinThrownExceptions(
        pc:                        Int,
        previouslyThrownException: ThrownException,
        value:                     ExceptionValue
    ): ThrownException = {
        value match {
            case DomainMultipleReferenceValuesTag(value) =>
                previouslyThrownException ++ value.values
            case DomainSingleOriginReferenceValueTag(sorv) =>
                previouslyThrownException + sorv
        }
    }
}

