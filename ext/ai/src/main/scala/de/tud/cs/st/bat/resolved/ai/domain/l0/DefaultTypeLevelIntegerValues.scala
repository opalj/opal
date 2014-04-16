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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l0

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
        with TypeLevelIntegerValues[I] { Domain ⇒

    case object ABooleanValue extends super.BooleanValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case ABooleanValue ⇒ NoUpdate
                case _             ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.ABooleanValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object AByteValue extends super.ByteValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case AByteValue ⇒ NoUpdate
                case _          ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.AByteValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object AShortValue extends super.ShortValue {

        override def doJoin(pc: PC, that: DomainValue): Update[DomainValue] =
            that match {
                case AShortValue ⇒ NoUpdate
                case _           ⇒ StructuralUpdate(AnIntegerValue)

            }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.AShortValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object ACharValue extends super.CharValue {

        override def doJoin(pc: PC, that: DomainValue): Update[DomainValue] =
            that match {
                case ACharValue ⇒ NoUpdate
                case _          ⇒ StructuralUpdate(AnIntegerValue)
            }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.ACharValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    case object AnIntegerValue extends super.IntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            NoUpdate

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            targetDomain match {
                case thatDomain: DefaultTypeLevelIntegerValues[ThatI] ⇒
                    thatDomain.AnIntegerValue.asInstanceOf[targetDomain.DomainValue]
                case _ ⇒ super.adapt(targetDomain, pc)
            }
    }

    override def BooleanValue(pc: PC): DomainValue = ABooleanValue
    override def BooleanValue(pc: PC, value: Boolean): DomainValue = ABooleanValue

    override def ByteValue(pc: PC): DomainValue = AByteValue
    override def ByteValue(pc: PC, value: Byte) = AByteValue

    override def ShortValue(pc: PC): DomainValue = AShortValue
    override def ShortValue(pc: PC, value: Short) = AShortValue

    override def CharValue(pc: PC): DomainValue = ACharValue
    override def CharValue(pc: PC, value: Char) = ACharValue

    override def IntegerValue(pc: PC): DomainValue = AnIntegerValue
    override def IntegerValue(pc: PC, value: Int) = AnIntegerValue
}

