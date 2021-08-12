/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Provides a default implementation for the instructions related to synchronization.
 *
 * @author Michael Eichberg
 */
trait IgnoreSynchronization extends MonitorInstructionsDomain {
    this: ValuesDomain with ReferenceValuesDomain with ExceptionsFactory with Configuration =>

    protected[this] def sideEffectOnlyOrExceptions(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        refIsNull(pc, value) match {
            case Yes =>
                ThrowsException(VMNullPointerException(pc))
            case Unknown if throwNullPointerExceptionOnMonitorAccess =>
                ComputationWithSideEffectOrException(VMNullPointerException(pc))
            case _ /* No OR Unknown but throwNullPointerExceptionOnMonitorAccess is No */ =>
                ComputationWithSideEffectOnly
        }
    }

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     *      an exception if it is `null` or maybe `null`.
     */
    /*override*/ def monitorenter(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        sideEffectOnlyOrExceptions(pc, value)
    }

    /**
     * Handles a `monitorexit` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     *      an exception if it is `null` or maybe `null`.
     */
    /*override*/ def monitorexit(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValues] = {
        val result = sideEffectOnlyOrExceptions(pc, value)
        if (result.returnsNormally /* <=> the value maybe non-null*/ &&
            throwIllegalMonitorStateException) {

            val imsException = VMIllegalMonitorStateException(pc)
            if (result.throwsException) {
                ComputationWithSideEffectOrException(Set(result.exceptions, imsException))
            } else {
                ComputationWithSideEffectOrException(Set(imsException))
            }
        } else { // the receiver is null
            ComputationWithSideEffectOrException(Set(result.exceptions))
        }
    }
}

