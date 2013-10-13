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
 * @author Michael Eichberg
 */
trait DefaultPreciseIntegerValues[+I]
        extends DefaultValueBinding[I]
        with PreciseIntegerValues[I] {

    case class AnIntegerValue() extends super.AnIntegerValue {

        override def join(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case _: IntegerLikeValue ⇒ NoUpdate
                case other               ⇒ MetaInformationUpdateIllegalValue
            }

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

    case class IntegerValue private (
        val initial: Int,
        val value: Int)
            extends super.IntegerValue {

        def this(value: Int) = this(value, value)

        def update(newValue: Int): DomainValue = IntegerValue(initial, newValue)

        override def join(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case AnIntegerValue() ⇒ StructuralUpdate(value)
                case IntegerValue(otherInitial, otherValue) ⇒
                    if (this.value == otherValue) {
                        if (spread(this.value, this.initial) >= spread(otherValue, otherInitial))
                            NoUpdate
                        else {
                            MetaInformationUpdate(IntegerValue(otherInitial, this.value))
                        }
                    } else {
                        // the value is only allowed to grow in one direction!
                        val newInitial =
                            if (Math.abs(otherValue - this.initial) > Math.abs(otherValue - otherInitial))
                                this.initial
                            else
                                otherInitial
                        val spread = Math.abs(otherValue - newInitial)
                        if (spread > maxSpread || // test for the boundary condition
                            // test if the value is no longer growing in one direction
                            spread < Math.abs(this.value - this.initial)) {
                            StructuralUpdate(AnIntegerValue())
                        } else {
                            StructuralUpdate(IntegerValue(newInitial, otherValue))
                        }
                    }
                case other ⇒ MetaInformationUpdateIllegalValue
            }

        override def summarize(pc: PC): DomainValue = this

        override def summarize(pc: PC, value: DomainValue): DomainValue = {
            value match {
                case AnIntegerValue() ⇒ value
                case IntegerValue(otherInitial, otherValue) ⇒
                    if (otherValue == this.value)
                        IntegerValue(Math.min(this.initial, otherInitial), this.value)
                    else
                        AnIntegerValue()
            }
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

        override def toString: String = "IntegerValue("+value+",initial="+initial+")"
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

