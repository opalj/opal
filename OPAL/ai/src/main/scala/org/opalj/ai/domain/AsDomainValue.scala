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
 * Mixed in by domains that support the conversation of a Java Object into a `DomainValue`.
 *
 * @see [[AsJavaObject]] for further information on limitations.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait AsDomainValue { domain: ReferenceValuesDomain ⇒

    /**
     * Converts the given Java object to a corresponding `DomainValue`. The conversion may be lossy.
     *
     * @note   To convert primitive values to `DomainValue`s use the domain's
     *         respective factory methods. I.e., this method deliberately does not perform any
     *         (Un-)Boxing as it does not have the necessary information. For more
     *         information study the implementation of the [[l1.ReflectiveInvoker]].
     *
     * @param  pc The program counter of the instruction that was responsible for
     *         creating the respective value. (This is in – in general – not the
     *         instruction where the transformation is performed.)
     * @param  value The object.
     *
     * @return A `DomainReferenceValue`.
     */
    def toDomainValue(pc: Int, value: Object): DomainReferenceValue
}
