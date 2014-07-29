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
 * Defines the primary factory methods to create `Integer` values.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait IntegerValuesFactory extends ValuesDomain { domain ⇒

    /**
     * Factory method to create a representation of the integer constant value 0.
     *
     * OPAL in particular uses this special value for performing subsequent
     * computations against the fixed value 0 (e.g., for if_XX instructions).
     *
     * (The origin ([[ValueOrigin]]) that should be used should be the
     * [[ConstantValueOrigin]] to signify that this value was not created by the program.)
     *
     * The domain may ignore the information about the value.
     */
    final def IntegerConstant0: DomainValue = IntegerValue(ConstantValueOrigin, 0)

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def IntegerValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given integer value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def IntegerValue(vo: ValueOrigin, value: Int): DomainValue

    /**
     * Factory method to create a representation of a boolean value if we know the
     * origin of the value.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def BooleanValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a representation of a boolean value with the given
     * initial value and origin.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def BooleanValue(vo: ValueOrigin, value: Boolean): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def ByteValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given byte value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     */
    def ByteValue(vo: ValueOrigin, value: Byte): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def ShortValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given short value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def ShortValue(pc: PC, value: Short): DomainValue

    /**
     * Factory method to create a `DomainValue` that was created (explicitly or
     * implicitly) by the instruction with the specified program counter.
     *
     * The domain may ignore the information about the origin (`vo`).
     */
    def CharValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given char value
     * and that was created (explicitly or implicitly) by the instruction with the
     * specified program counter.
     */
    def CharValue(vo: ValueOrigin, value: Char): DomainValue
}

