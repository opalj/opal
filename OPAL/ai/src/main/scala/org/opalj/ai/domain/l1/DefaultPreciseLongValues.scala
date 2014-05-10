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
package l1

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * Basic implementation of the `PreciseLongValues` trait that requires that
 * `Domain`'s  `Value` trait is not extended.
 *
 * @author Riadh Chtara
 */
trait DefaultPreciseLongValues
        extends DefaultDomainValueBinding
        with PreciseLongValues {

    // ATTENTION: The functionality to propagate a constraint crucially depends on
    // the fact two long values created at two different places are represented
    // by two different instances of "ALongValue"; otherwise, propagating the
    // constraint that some value (after some kind of check) has to have a special
    // value may affect unrelated values!
    case class ALongValue() extends super.ALongValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def adapt(targetDomain: Domain, pc: PC): targetDomain.DomainValue =
            targetDomain.LongValue(pc)
    }

    case class LongRange(
        val initial: Long,
        val value: Long)
            extends super.LongValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] =
            value match {
                case ALongValue() ⇒ StructuralUpdate(value)
                case LongRange(otherInitial, otherValue) ⇒
                    // First check if they are growing in the same direction...
                    var increasing = (this.value - this.initial >= 0)
                    if (increasing != (otherValue - otherInitial) >= 0)
                        return StructuralUpdate(ALongValue())

                    def result(newInitial: Long, newValue: Long) = {
                        if (spread(newValue, newInitial) > maxSpreadLong)
                            StructuralUpdate(ALongValue())
                        else if (newValue != this.value)
                            StructuralUpdate(LongRange(newInitial, newValue))
                        else if (newInitial != this.initial)
                            MetaInformationUpdate(LongRange(newInitial, newValue))
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

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            if (target.isInstanceOf[DefaultPreciseLongValues]) {
                val thatDomain = target.asInstanceOf[DefaultPreciseLongValues]
                thatDomain.LongRange(this.initial, this.value).
                    asInstanceOf[target.DomainValue]
            } else {
                super.adapt(target, pc)
            }

        override def toString: String = "LongRange(initial="+initial+", value="+value+")"
    }

    override def LongValue(pc: PC): DomainValue = ALongValue()

    override def LongValue(pc: PC, value: Long) = new LongRange(value, value)

}

