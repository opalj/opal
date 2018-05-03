/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package domain

/**
 * Provides a default implementation for the instructions related to synchronization.
 *
 * @author Michael Eichberg
 */
trait IgnoreSynchronization extends MonitorInstructionsDomain {
    this: ValuesDomain with ReferenceValuesDomain with ExceptionsFactory with Configuration ⇒

    protected[this] def sideEffectOnlyOrExceptions(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        refIsNull(pc, value) match {
            case Yes ⇒
                ThrowsException(NullPointerException(ValueOriginForVMLevelValue(pc)))
            case Unknown if throwNullPointerExceptionOnMonitorAccess ⇒
                val npe = NullPointerException(ValueOriginForVMLevelValue(pc))
                ComputationWithSideEffectOrException(npe)
            case _ /* No OR Unknown but throwNullPointerExceptionOnMonitorAccess is No */ ⇒
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

