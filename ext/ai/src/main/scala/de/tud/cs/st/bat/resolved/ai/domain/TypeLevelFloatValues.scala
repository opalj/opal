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

trait TypeLevelFloatValues[I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF FLOAT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `float`.
     */
    trait FloatValue extends Value {
        final def computationalType: ComputationalType = ComputationalTypeFloat
    }

    private val typesAnswer: IsPrimitiveType = IsPrimitiveType(FloatType)

    abstract override def types(value: DomainValue): TypesAnswer[_] = {
        value match {
            case r: FloatValue ⇒ typesAnswer
            case _             ⇒ super.types(value)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue
    def fcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def fneg(pc: Int, value: DomainValue) = newFloatValue

    //
    // BINARY EXPRESSIONS
    //

    def fadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newFloatValue
    def fdiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newFloatValue
    def fmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newFloatValue
    def frem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newFloatValue
    def fsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newFloatValue

    //
    // TYPE CONVERSIONS
    //

    def f2d(pc: Int, value: DomainValue): DomainValue = newDoubleValue
    def f2i(pc: Int, value: DomainValue): DomainValue = newIntegerValue
    def f2l(pc: Int, value: DomainValue): DomainValue = newLongValue

}

trait DefaultTypeLevelFloatValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelFloatValues[I] {

    case object FloatValue extends super.FloatValue {

        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case FloatValue ⇒ NoUpdate
            case _          ⇒ MetaInformationUpdateIllegalValue
        }

        override def adapt(domain: Domain[_ >: I]): domain.DomainValue = domain match {
            case d: DefaultTypeLevelFloatValues[I] ⇒
                // "this" value does not have a dependency on this domain instance  
                this.asInstanceOf[domain.DomainValue]
            case _ ⇒ super.adapt(domain)
        }

        def onCopyToRegister = this

    }

    def newFloatValue(): FloatValue = FloatValue

    def newFloatValue(pc: Int, value: Float): FloatValue = FloatValue
}


