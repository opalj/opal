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

/**
 * Defines the methods that performs type conversions between primitive values with different
 * computational types.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait PrimitiveValuesConversionsDomain { domain: ValuesDomain ⇒

    def i2d(pc: Int, value: DomainValue): DomainValue
    def i2f(pc: Int, value: DomainValue): DomainValue
    def i2l(pc: Int, value: DomainValue): DomainValue

    /** Conversion of the given long value to a double value. */
    def l2d(pc: Int, value: DomainValue): DomainValue
    /** Conversion of the given long value to a float value. */
    def l2f(pc: Int, value: DomainValue): DomainValue
    /** Conversion of the given long value to an integer value. */
    def l2i(pc: Int, value: DomainValue): DomainValue

    def f2d(pc: Int, value: DomainValue): DomainValue
    def f2i(pc: Int, value: DomainValue): DomainValue
    def f2l(pc: Int, value: DomainValue): DomainValue

    def d2f(pc: Int, value: DomainValue): DomainValue
    def d2i(pc: Int, value: DomainValue): DomainValue
    def d2l(pc: Int, value: DomainValue): DomainValue

}
