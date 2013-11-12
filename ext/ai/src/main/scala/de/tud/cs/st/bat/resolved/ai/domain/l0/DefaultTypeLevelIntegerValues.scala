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
 * Base implementation of the `TypeLevelIntegerValues` trait that requires that
 * the domain`s `Value` trait is not extended. This implementation satisfies
 * the requirements of BATAI w.r.t. the domain's computational type but provides 
 * some additional information about a value's range if possible.
 *  
 * @author Michael Eichberg
 */
trait DefaultTypeLevelIntegerValues[+I]
        extends DefaultDomainValueBinding[I]
        with TypeLevelIntegerValues[I] {
    
    case object BooleanValue extends super.BooleanValue {
        
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = 
            value match {
            case BooleanValue         ⇒ NoUpdate
            case other @ IntegerValue ⇒ StructuralUpdate(other)
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.BooleanValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object ByteValue extends super.ByteValue {
        
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = value match {
            case ByteValue            ⇒ NoUpdate
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case other @ ShortValue   ⇒ StructuralUpdate(other)
            case other @ CharValue    ⇒ StructuralUpdate(other)
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.ByteValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object ShortValue extends super.ShortValue {
        
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = value match {
            case ShortValue           ⇒ NoUpdate
            case ByteValue            ⇒ NoUpdate
            case other @ IntegerValue ⇒ StructuralUpdate(other)
            case other @ CharValue    ⇒ StructuralUpdate(IntegerValue)
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.ShortValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object CharValue extends super.CharValue {
        
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = value match {
            case CharValue            ⇒ NoUpdate
            case ByteValue            ⇒ NoUpdate
            case ShortValue           ⇒ StructuralUpdate(IntegerValue)
            case other @ IntegerValue ⇒ StructuralUpdate(other)
        }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.CharValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object IntegerValue extends super.IntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.IntegerValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    def newBooleanValue(): DomainValue = BooleanValue
    def newBooleanValue(pc: PC): DomainValue = BooleanValue
    def newBooleanValue(pc: PC, value: Boolean): DomainValue = BooleanValue

    def newByteValue() = ByteValue
    def newByteValue(pc: PC): DomainValue = ByteValue
    def newByteValue(pc: PC, value: Byte) = ByteValue

    def newShortValue() = ShortValue
    def newShortValue(pc: PC): DomainValue = ShortValue
    def newShortValue(pc: PC, value: Short) = ShortValue

    def newCharValue() = CharValue
    def newCharValue(pc: PC): DomainValue = CharValue
    def newCharValue(pc: PC, value: Byte) = CharValue

    def newIntegerValue() = IntegerValue
    def newIntegerValue(pc: PC): DomainValue = IntegerValue
    def newIntegerValue(pc: PC, value: Int) = IntegerValue
    val newIntegerConstant0: DomainValue = newIntegerValue(Int.MinValue, 0)
}

