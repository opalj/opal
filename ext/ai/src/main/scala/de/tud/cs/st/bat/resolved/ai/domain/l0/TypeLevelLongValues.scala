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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Support the computation with long values at the type level.
 *
 * @author Michael Eichberg
 */
trait TypeLevelLongValues[+I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    trait LongValue extends Value { this: DomainValue ⇒

        override final def computationalType: ComputationalType = ComputationalTypeLong

    }

    protected def newLongValue(): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    abstract override def types(value: DomainValue): TypesAnswer =
        value match {
            case r: LongValue ⇒ TypeLevelLongValues.typesAnswer
            case _            ⇒ super.types(value)
        }

    //
    // RELATIONAL OPERATORS
    //
    override def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue(pc)

    //
    // UNARY EXPRESSIONS
    //
    override def lneg(pc: PC, value: DomainValue) = newLongValue()

    //
    // BINARY EXPRESSIONS
    //

    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def ldiv(pc: PC, value1: DomainValue, value2: DomainValue) =
        ComputedValue(newLongValue())

    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lrem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newLongValue()

    //
    // CONVERSION INSTRUCTIONS
    //

    override def l2d(pc: PC, value: DomainValue): DomainValue = newDoubleValue(pc)
    override def l2f(pc: PC, value: DomainValue): DomainValue = newFloatValue(pc)
    override def l2i(pc: PC, value: DomainValue): DomainValue = newIntegerValue(pc)
}
private object TypeLevelLongValues {
    private final val typesAnswer: IsPrimitiveType = IsPrimitiveType(LongType)
}



