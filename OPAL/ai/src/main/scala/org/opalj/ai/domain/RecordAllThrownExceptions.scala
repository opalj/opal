/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
 * Records '''all''' exceptions thrown by a method. I.e., for each instruction that
 * throws an exception (or multiple exceptions) all exceptions are recorded.
 *
 * @note This domain requires that `DomainValue`s that represent thrown exceptions
 *      have meaningful `equals` and `hashCode` methods. (Depending on the purpose
 *      of the abstract interpretation, reference equality may be sufficient.)
 *
 * @author Michael Eichberg
 */
trait RecordAllThrownExceptions extends RecordThrownExceptions { domain: ValuesDomain ⇒

    type ThrownException = Set[ExceptionValue]

    override protected[this] def recordThrownException(
        pc: PC,
        value: ExceptionValue): ThrownException =
        Set.empty + value

    override protected[this] def joinThrownExceptions(
        pc: PC,
        previouslyThrownException: ThrownException,
        value: ExceptionValue): ThrownException =
        previouslyThrownException + value

}

