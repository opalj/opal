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
 * Trait that can be mixed in if information is needed about all returned values and
 * the thrown exceptions. ''This information is, however, only available after the
 * evaluation of a method has completed.''
 *
 * @author Michael Eichberg
 */
trait MethodCallResults { domain: ValuesDomain ⇒

    /**
     * `true` if the method returned due to a `("void"|a|i|l|f|d)return` instruction.
     *
     * @note This method may only be called after the abstract interpretation of a
     * 		 method has completed.
     */
    def returnedNormally: Boolean

    /**
     * Adapts and returns the returned value.
     *
     * @note This method is only defined if the method returned normally. In this case
     * 		`None` is returned if the method's return type is `void`;
     *   	`Some(DomainValue)` is returned otherwise.
     *
     * @note This method may only be called after the abstract interpretation of a
     * 		 method has completed.
     */
    def returnedValue(target: TargetDomain, callerPC: PC): Option[target.DomainValue]

    /**
     * Adapts and returns the exceptions that are thrown by the called method.
     *
     * In general, for each type of exception there should be at most one
     * `ExceptionValue`.
     *
     * @note This method may only be called after the abstract interpretation of a
     * 		 method has completed.
     */
    def thrownExceptions(target: TargetDomain, callerPC: PC): target.ExceptionValues

}


