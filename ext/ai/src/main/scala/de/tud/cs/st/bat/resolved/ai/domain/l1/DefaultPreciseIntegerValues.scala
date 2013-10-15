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
 * Basic implementation of the `PreciseIntegerValues` trait that requires that
 * `Domain`'s  `Value` trait is not extended.
 *
 * @author Michael Eichberg
 */
trait DefaultPreciseIntegerValues[+I]
        extends DefaultValueBinding[I]
        with PreciseIntegerValues[I] {

    case class AnIntegerValue() extends super.AnIntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = this

        override def adapt[ThatI >: I](
            targetDomain: Domain[ThatI],
            pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultPreciseIntegerValues[ThatI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultPreciseIntegerValues[ThatI]]
                thatDomain.AnIntegerValue().asInstanceOf[targetDomain.DomainValue]
            } else {
                super.adapt(targetDomain, pc)
            }
    }

    case class IntegerValue(
        val initial: Int,
        val value: Int)
            extends super.IntegerValue {

        def this(value: Int) = this(value, value)

        def update(newValue: Int): DomainValue = IntegerValue(initial, newValue)

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case AnIntegerValue() ⇒ StructuralUpdate(value)
                case IntegerValue(otherInitial, otherValue) ⇒
                    // First check if they are growing in the same direction...
                    var increasing = (this.value - this.initial >= 0)
                    if (increasing != (otherValue - otherInitial) >= 0)
                        return StructuralUpdate(AnIntegerValue())

                    def result(newInitial: Int, newValue: Int) = {
                        if (spread(newValue, newInitial) > maxSpread)
                            StructuralUpdate(AnIntegerValue())
                        else if (newValue != this.value)
                            StructuralUpdate(IntegerValue(newInitial, newValue))
                        else if (newInitial != this.initial)
                            MetaInformationUpdate(IntegerValue(newInitial, newValue))
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
            if (targetDomain.isInstanceOf[DefaultPreciseIntegerValues[ThatI]]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultPreciseIntegerValues[ThatI]]
                thatDomain.IntegerValue(this.initial, this.value).
                    asInstanceOf[targetDomain.DomainValue]
            } else {
                super.adapt(targetDomain, pc)
            }

        override def toString: String = "IntegerValue(initial="+initial+", value="+value+")"
    }

    def newBooleanValue(): DomainValue = AnIntegerValue()
    def newBooleanValue(pc: PC): DomainValue = AnIntegerValue()
    def newBooleanValue(pc: PC, value: Boolean): DomainValue =
        if (value) newIntegerValue(pc, 1) else newIntegerValue(pc, 0)

    def newByteValue() = AnIntegerValue()
    def newByteValue(pc: PC): DomainValue = AnIntegerValue()
    def newByteValue(pc: PC, value: Byte) = new IntegerValue(value)

    def newShortValue() = AnIntegerValue()
    def newShortValue(pc: PC): DomainValue = AnIntegerValue()
    def newShortValue(pc: PC, value: Short) = new IntegerValue(value)

    def newCharValue() = AnIntegerValue()
    def newCharValue(pc: PC): DomainValue = AnIntegerValue()
    def newCharValue(pc: PC, value: Char) = new IntegerValue(value)

    def newIntegerValue() = AnIntegerValue()
    def newIntegerValue(pc: PC): DomainValue = AnIntegerValue()
    def newIntegerValue(pc: PC, value: Int) = new IntegerValue(value)
    def newIntegerConstant0: DomainValue = new IntegerValue(0)

}

