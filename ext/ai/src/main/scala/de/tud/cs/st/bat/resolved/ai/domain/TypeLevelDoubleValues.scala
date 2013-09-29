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

trait TypeLevelDoubleValues[I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF DOUBLE VALUES
    //
    // -----------------------------------------------------------------------------------

    trait DoubleValue extends Value {
        final def computationalType: ComputationalType = ComputationalTypeDouble
    }

    private val typesAnswer: IsPrimitiveType = IsPrimitiveType(DoubleType)

    abstract override def types(value: DomainValue): TypesAnswer[_] =
        value match {
            case r: DoubleValue ⇒ typesAnswer
            case _              ⇒ super.types(value)
        }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    def dcmpg(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue(pc)
    def dcmpl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue(pc)

    //
    // UNARY EXPRESSIONS
    //
    def dneg(pc: Int, value: DomainValue) = newDoubleValue(pc)

    //
    // BINARY EXPRESSIONS
    //
    def dadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newDoubleValue(pc)
    def ddiv(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newDoubleValue(pc)
    def dmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newDoubleValue(pc)
    def drem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newDoubleValue(pc)
    def dsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newDoubleValue(pc)

    //
    // TYPE CONVERSION INSTRUCTIONS
    //
    def d2f(pc: Int, value: DomainValue): DomainValue = newFloatValue(pc)
    def d2i(pc: Int, value: DomainValue): DomainValue = newIntegerValue(pc)
    def d2l(pc: Int, value: DomainValue): DomainValue = newLongValue(pc)
}

trait DefaultTypeLevelDoubleValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelDoubleValues[I] {

    case object DoubleValue extends super.DoubleValue {

        override def merge(pc: Int, value: DomainValue): Update[DomainValue] =
            value match {
                case DoubleValue ⇒ NoUpdate
                case _           ⇒ MetaInformationUpdateIllegalValue
            }

        override def adapt(domain: Domain[_ >: I]): domain.DomainValue =
            domain match {
                case d: DefaultTypeLevelDoubleValues[I] ⇒
                    // "this" value does not have a dependency on this domain instance  
                    this.asInstanceOf[domain.DomainValue]
                case _ ⇒ super.adapt(domain)
            }

        // REMOVE? def onCopyToRegister = this
    }

    def newDoubleValue(): DoubleValue = DoubleValue

    def newDoubleValue(pc: Int): DomainValue = DoubleValue

    def newDoubleValue(pc: Int, value: Double): DoubleValue = DoubleValue
}



