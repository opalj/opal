/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

import language.existentials

/**
 * A general, non-recoverable exception occured.
 *
 * @author Michael Eichberg
 */
class AIException(
    message: String,
    cause: Throwable) extends RuntimeException(message, cause)

/**
 * Exception that is thrown by BATAI when the abstract interpretation of a method
 * was not possible.
 *
 * @author Michael Eichberg
 */
case class InterpreterException[D <: SomeDomain](
    throwable: Throwable,
    domain: D,
    worklist: List[PC],
    evaluated: List[PC],
    operandsArray: Array[_ <: List[_ <: D#DomainValue]],
    localsArray: Array[_ <: Array[_ <: D#DomainValue]])
        // TODO [Design] using the cause's message as the message doesn't make sense when we also pass on the cause itself...
        extends AIException(
            throwable.getClass().getSimpleName()+": "+throwable.getLocalizedMessage(),
            throwable)

/**
 * An exception related to performing computations in a specific domain occured.
 * This exception is primarily intended to be used if the exception is most likely
 * due to a bug in the implementation of a `Domain`.
 *
 * @author Michael Eichberg
 */
case class DomainException(
        domain: SomeDomain,
        message: String) extends AIException(message, null) {

    def enrich(
        worklist: List[PC],
        evaluated: List[PC],
        operandsArray: Array[List[domain.DomainValue]],
        localsArray: Array[Array[domain.DomainValue]]) = {

        new InterpreterException(
            this,
            domain,
            worklist,
            evaluated,
            operandsArray,
            localsArray)
    }
}

