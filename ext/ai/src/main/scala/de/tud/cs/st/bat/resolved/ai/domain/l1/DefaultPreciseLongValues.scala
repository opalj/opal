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
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Basic implementation of the `PreciseLongValues` trait that requires that
 * `Domain`'s  `Value` trait is not extended.
 *
 * @author Michael Eichberg
 */
trait DefaultPreciseLongValues[+I]
        extends DefaultDomainValueBinding[I]
        with PreciseLongValues[I] {

    case class AnLongValue() extends super.AnLongValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = this

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultPreciseLongValues[ThatI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultPreciseLongValues[ThatI]]
                thatDomain.AnLongValue().asInstanceOf[targetDomain.DomainValue]
            } else {
                super.adapt(targetDomain, pc)
            }
    }

    case class LongValue(
        val initial: Long,
        val value: Long)
            extends super.LongValue {

        def this(value: Long) = this(value, value)

        def update(newValue: Long): DomainValue = LongValue(initial, newValue)

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case AnLongValue() ⇒ StructuralUpdate(value)
                case LongValue(otherInitial, otherValue) ⇒
                    // First check if they are growing in the same direction...
                    var increasing = (this.value - this.initial >= 0)
                    if (increasing != (otherValue - otherInitial) >= 0)
                        return StructuralUpdate(AnLongValue())

                    def result(newInitial: Long, newValue: Long) = {
                        if (spread(newValue, newInitial) > maxSpreadLong)
                            StructuralUpdate(AnLongValue())
                        else if (newValue != this.value)
                            StructuralUpdate(LongValue(newInitial, newValue))
                        else if (newInitial != this.initial)
                            MetaInformationUpdate(LongValue(newInitial, newValue))
                        else
                            NoUpdate
                    }

                    if (increasing)
                        result(
                            Math.min(this.initial, otherInitial),
                            Math.max(this.value, otherValue))
                    else
                        result(
                            Math.max(this.initial, otherInitial),
                            Math.min(this.value, otherValue))

            }

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue =
            doJoin(pc, value) match {
                case NoUpdate             ⇒ this
                case SomeUpdate(newValue) ⇒ newValue
            }

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultPreciseLongValues[ThatI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultPreciseLongValues[ThatI]]
                thatDomain.LongValue(this.initial, this.value).
                    asInstanceOf[targetDomain.DomainValue]
            } else {
                super.adapt(targetDomain, pc)
            }

        override def toString: String = "LongValue(initial="+initial+", value="+value+")"
    }
/*
    def newBooleanValue(): DomainValue = AnLongValue()
    def newBooleanValue(pc: PC): DomainValue = AnLongValue()
    def newBooleanValue(pc: PC, value: Boolean): DomainValue =
        if (value) newLongValue(pc, 1) else newLongValue(pc, 0)

    def newByteValue() = AnLongValue()
    def newByteValue(pc: PC): DomainValue = AnLongValue()
    def newByteValue(pc: PC, value: Byte) = new LongValue(value)

    def newShortValue() = AnLongValue()
    def newShortValue(pc: PC): DomainValue = AnLongValue()
    def newShortValue(pc: PC, value: Short) = new LongValue(value)

    def newCharValue() = AnLongValue()
    def newCharValue(pc: PC): DomainValue = AnLongValue()
    def newCharValue(pc: PC, value: Char) = new LongValue(value)
*/
    def newLongValue() = AnLongValue()
    def newLongValue(pc: PC): DomainValue = AnLongValue()
    def newLongValue(pc: PC, value: Long) = new LongValue(value)
    def newLongConstant0: DomainValue = new LongValue(0)

}

