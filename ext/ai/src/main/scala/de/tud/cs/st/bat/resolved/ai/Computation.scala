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

/**
 * Encapsulates the result of a computation in a domain. In general, the
 * result is either some value V or some exception(s). In some cases, however,
 * when the domain cannot "precisely" determine the result, it may be both: some
 * exceptional value(s) and a value. In the latter case BATAI will follow all
 * possible paths.
 *
 * @tparam V The result of the computation. Typically a `DomainValue` or a
 *      `DomainTypedValue`. If the computation is primarily executed for its side
 *      effect (e.g., as in case of a `monitorenter` or `monitorexit` instruction)
 *      the type of V maybe nothing.
 * @tparam E The exception(s) that maybe thrown by the computation. Typically,
 *      a `DomainTypedValue[ObjectType]` or a set thereof.
 */
sealed trait Computation[+V, +E] {

    /**
     * The return value of the computation (if any); defined if and only if
     * `hasValue` returns true.
     */
    def result: V

    /**
     * `True` if this computation may have a return value, false otherwise.
     */
    def hasResult: Boolean

    /**
     * The exception or exceptions when the computation raised an exception.
     *
     * E.g., the invocation of a method may lead to
     * several (checked/unchecked) exceptions.
     */
    def exceptions: E

    /**
     * `True` if this computation may have raised an exception.
     */
    def throwsException: Boolean

    /**
     * `True` if this computation may have returned normally without
     * throwing an exception.
     */
    def returnsNormally: Boolean
}

/**
 * Encapsulates the result of a computation that returned normally and
 * that did not throw an exception.
 */
case class ComputedValue[+V](
    result: V)
        extends Computation[V, Nothing] {

    def hasResult: Boolean = true

    def exceptions = AIImplementationError("ValuesAnswer - the computation succeeded without an exception")

    def throwsException: Boolean = false

    def returnsNormally: Boolean = true
}

/**
 * Encapsulates the result of a computation that either returned normally
 * or threw an exception.
 */
case class ComputedValueAndException[+V, +E](
    result: V,
    exceptions: E)
        extends Computation[V, E] {

    def hasResult: Boolean = true

    def throwsException: Boolean = true

    def returnsNormally: Boolean = true

}

/**
 * Encapsulates the result of a computation that threw an exception.
 */
case class ThrowsException[+E](
    exceptions: E)
        extends Computation[Nothing, E] {

    def result = AIImplementationError("ValuesAnswer - the computation resulted in an exception")

    def hasResult: Boolean = false

    def throwsException: Boolean = true

    def returnsNormally: Boolean = false
}

/**
 * Encapsulates the result of a computation that returned normally (but which
 * did not return some value) or that threw an exception.
 */
case class ComputationWithSideEffectOrException[+E](
    exceptions: E)
        extends Computation[Nothing, E] {

    def result = AIImplementationError("ValuesAnswer - the computation was executed for its side effect only")

    def hasResult: Boolean = false

    def throwsException: Boolean = true

    def returnsNormally: Boolean = true
}

/**
 * Encapsulates the result of a computation that returned normally (but which
 * did not return some value).
 */
case object ComputationWithSideEffectOnly
        extends Computation[Nothing, Nothing] {

    def result = AIImplementationError("ValuesAnswer - the computation was executed for its side effect only")

    def hasResult: Boolean = false

    def exceptions = AIImplementationError("ValuesAnswer - the computation succeeded without an exception")

    def throwsException: Boolean = false

    def returnsNormally: Boolean = true
}
      


