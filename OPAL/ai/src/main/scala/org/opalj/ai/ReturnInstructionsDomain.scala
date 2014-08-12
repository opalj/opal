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

/**
 *
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait ReturnInstructionsDomain { domain: ValuesDomain ⇒

    //
    // RETURN FROM METHOD
    //
    /**
     * Called by OPAL-AI when a return instruction with the given `pc` is reached.
     * In other words, when the method returns normally.
     */
    def returnVoid(pc: PC): Unit

    /**
     * The given `value`, which is a value with ''computational type integer'', is returned
     * by the return instruction with the given `pc`.
     */
    def ireturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type long'', is returned
     * by the return instruction with the given `pc`.
     */
    def lreturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type float'', is returned
     * by the return instruction with the given `pc`.
     */
    def freturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type double'', is returned
     * by the return instruction with the given `pc`.
     */
    def dreturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type reference'', is returned
     * by the return instruction with the given `pc`.
     */
    def areturn(pc: PC, value: DomainValue): Unit

    /**
     * Called by the abstract interpreter when an exception is thrown that is not
     * (guaranteed to be) handled within the same method.
     *
     * @note If the original exception value is `null` (`/*E.g.*/throw null;`), then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by OPAL and the exception value
     *      is hence never `null`.
     */
    def abruptMethodExecution(pc: PC, exceptionValue: ExceptionValue): Unit

}
