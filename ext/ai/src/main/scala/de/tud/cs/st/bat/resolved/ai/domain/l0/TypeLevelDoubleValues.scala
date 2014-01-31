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
package domain
package l0

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Domain that handles computations related to `Double` values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelDoubleValues[+I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF DOUBLE VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over double values at the type level.
     */
    trait DoubleValue
            extends Value
            with IsDoubleValue { this: DomainValue ⇒

        override final def computationalType: ComputationalType = ComputationalTypeDouble

    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    override def dcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    override def dcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        IntegerValue(pc)

    //
    // UNARY EXPRESSIONS
    //
    override def dneg(pc: PC, value: DomainValue) =
        DoubleValue(pc)

    //
    // BINARY EXPRESSIONS
    //
    override def dadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        DoubleValue(pc)

    override def ddiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        DoubleValue(pc)

    override def dmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        DoubleValue(pc)

    override def drem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        DoubleValue(pc)

    override def dsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        DoubleValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //
    override def d2f(pc: PC, value: DomainValue): DomainValue =
        FloatValue(pc)

    override def d2i(pc: PC, value: DomainValue): DomainValue =
        IntegerValue(pc)

    override def d2l(pc: PC, value: DomainValue): DomainValue =
        LongValue(pc)
}



