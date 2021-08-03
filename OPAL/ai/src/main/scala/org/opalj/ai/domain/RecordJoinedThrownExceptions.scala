/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Records the exception that is thrown by an instruction. If an instruction throws
 * multiple exceptions. The exceptions are `join`ed using the [[Domain#DomainValue]]'s
 * `join` method.
 *
 * This trait can be used to record the thrown exceptions independently of the
 * precision of the domain.
 *
 * @author Michael Eichberg
 */
trait RecordJoinedThrownExceptions extends RecordThrownExceptions {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    type ThrownException = ExceptionValue

    override protected[this] def recordThrownException(
        pc:    Int,
        value: ExceptionValue
    ): ThrownException = {
        value
    }

    override protected[this] def joinThrownExceptions(
        pc:                        Int,
        previouslyThrownException: ThrownException,
        thrownException:           ExceptionValue
    ): ThrownException = {

        if (previouslyThrownException eq thrownException)
            return thrownException;

        previouslyThrownException.join(pc, thrownException) match {
            case NoUpdate                     => previouslyThrownException
            case StructuralUpdate(exVal)      => exVal.asInstanceOf[ExceptionValue]
            case MetaInformationUpdate(exVal) => exVal.asInstanceOf[ExceptionValue]
        }
    }
}

