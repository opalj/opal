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

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.Code

/**
 * Generic infrastructure to record the values returned by the method.
 * (Note that the computational type of the value(s) is not recorded.
 * It is directly determined by the signature of the method that is analyzed or
 * can be extracted using the respective method.)
 *
 * ==Usage==
 * This domain can be stacked on top of other traits that handle
 * return instructions that return some value.
 *
 * ==Usage==
 * A domain that mixes in this trait should only be used to analyze a single method.
 *
 * @author Michael Eichberg
 */
trait RecordReturnedValues extends RecordReturnedValuesInfrastructure with CustomInitialization {
    domain: ValuesDomain with Configuration with ExceptionsFactory ⇒

    /**
     * Wraps the given value into a `ReturnedValue`.
     *
     * @param pc The program counter of the return instruction. The returned
     *      values are automatically associated with the pc of the instruction. Hence,
     *      it is not strictly required to store it in the `ReturnedValue`.
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    protected[this] def recordReturnedValue(pc: Int, value: DomainValue): ReturnedValue

    /**
     * Joins the previously returned value and the newly given `value`. Both values
     * are returned by the same return instruction (same `pc`).
     *
     * @param pc The program counter of the return instruction. The returned
     *      values are automatically associated with the pc of the instruction. Hence,
     *      it is not strictly required to store it in the `ReturnedValue`.
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    protected[this] def joinReturnedValues(
        pc:                      Int,
        previouslyReturnedValue: ReturnedValue,
        value:                   DomainValue
    ): ReturnedValue

    private[this] var returnedValues: Map[Int /*PC*/ , ReturnedValue] = _ // IMPROVE Use Int2ObjectMap

    abstract override def initProperties(
        code:          Code,
        cfJoins:       IntTrieSet,
        initialLocals: Locals
    ): Unit = {
        returnedValues = Map.empty
        super.initProperties(code, cfJoins, initialLocals)
    }

    /**
     * Returns the set of all returned values.
     */
    def allReturnedValues: Map[Int /*PC */ , ReturnedValue] = returnedValues // IMPROVE Use Int2ObjectMap

    protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Unit = {
        returnedValues =
            returnedValues.updated(
                pc,
                returnedValues.get(pc) match {
                    case Some(returnedValue) ⇒
                        joinReturnedValues(pc, returnedValue, value)
                    case None ⇒
                        recordReturnedValue(pc, value)
                }
            )
    }
}
