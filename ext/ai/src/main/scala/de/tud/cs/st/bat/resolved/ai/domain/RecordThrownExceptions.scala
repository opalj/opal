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
package de.tud.cs.st
package bat
package resolved
package ai
package domain

import language.implicitConversions

/**
 * Records the exceptions thrown by a method. This trait can be used to record
 * the thrown exceptions independently of the precision of the domain.
 *
 * ==Usage==
 * A domain that mixes in this trait should only be used to analyze a single method.
 *
 * ==Thread Safety==
 * This class is not thread safe. I.e., this domain can only be used if
 * an instance of this domain is not used by multiple threads.
 *
 * @author Michael Eichberg
 */
trait RecordThrownExceptions extends Domain {

    /**
     * This type determines in which way thrown exceptions are recorded.
     *
     * For example, if you may want to collect all thrown exceptions, then
     * the type of `ThrownException` could be `Set[ThrownException]`.
     *
     * @see The implementation of
     *      [[de.tud.cs.st.bat.resolved.ai.domain.RecordAllThrownExceptions]].
     * @see The implementation of
     *      [[de.tud.cs.st.bat.resolved.ai.domain.RecordLastReturnedValues]]. It
     *      follows the same pattern.
     */
    type ThrownException <: AnyRef

    /**
     * Wraps the given value into a `ThrownException`.
     *
     * @see For details study the documentation of the abstract type `ThrownException`
     *      and study the subclass(es) of `RecordThrownExceptions`.
     */
    protected[this] def thrownException(pc: PC, value: ExceptionValue): ThrownException

    /**
     * Joins the previously thrown exception and the newly thrown exception. Both
     * exceptions are thrown by the same instruction (same `pc`).
     *
     * @note The instruction might be an `athrow` instruction or some other instruction
     *      that throws an exception.
     *
     * @see For details study the documentation of the abstract type `ThrownException`
     *      and study the subclass(es) of `RecordThrownExceptions`.
     */
    protected[this] def joinThrownExceptions(
        pc: PC,
        previouslyThrownException: ThrownException,
        value: ExceptionValue): ThrownException

    @volatile private[this] var thrownExceptions: Map[PC, ThrownException] = Map.empty

    def allThrownExceptions: Map[PC, ThrownException] = thrownExceptions

    abstract override def abruptMethodExecution(pc: PC, exception: DomainValue) {
        thrownExceptions =
            thrownExceptions.updated(
                pc,
                thrownExceptions.get(pc) match {
                    case Some(previouslyThrownException) ⇒
                        joinThrownExceptions(pc, previouslyThrownException, exception)
                    case None ⇒
                        thrownException(pc, exception)
                }
            )
        super.abruptMethodExecution(pc, exception)
    }
}



