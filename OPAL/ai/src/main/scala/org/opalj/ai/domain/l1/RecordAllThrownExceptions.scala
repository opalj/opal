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

