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
package org.opalj
package ai
package domain
package li

import org.opalj.util.{ Answer, Yes, No, Unknown }

/**
 * Basic implementation of the `PreciseIntegerValues` trait that requires that
 * `Domain`'s  `Value` trait is not extended.
 *
 * @author Michael Eichberg
 */
trait DefaultPreciseIntegerValues
        extends DefaultDomainValueBinding
        with PreciseIntegerValues {
    this: Configuration ⇒

    /**
     * @note The functionality to propagate a constraint crucially depends on
     *      the fact two integer values created at two different places are represented
     *      by two different instances of "AnIntegerValue"; otherwise, propagating the
     *      constraint that some value (after some kind of check) has to have a special
     *      value may affect unrelated values!
     */
    case class AnIntegerValue() extends super.AnIntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            target.IntegerValue(pc)
    }

    case class TheIntegerValue(
        val value: Int,
        val updateCount: Int = 0)
            extends super.IntegerValue {

        def update(newValue: Int): DomainValue = TheIntegerValue(newValue, updateCount + 1)

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] =
            other match {
                case AnIntegerValue() ⇒ StructuralUpdate(other)
                case TheIntegerValue(otherValue, otherUpdateCount) ⇒
                    if (this.value == otherValue) {
                        if (this.updateCount == otherUpdateCount)
                            NoUpdate
                        else
                            MetaInformationUpdate(TheIntegerValue(otherValue, Math.max(this.updateCount, otherUpdateCount)))
                    } else {
                        val newUpdateCount = Math.max(this.updateCount, otherUpdateCount)+1
                        if (newUpdateCount < maxUpdatesForIntegerValues)
                            StructuralUpdate(TheIntegerValue(otherValue, newUpdateCount))
                        else
                            StructuralUpdate(AnIntegerValue())
                    }
            }

        override def summarize(pc: PC): DomainValue = this

        override def adapt(
            target: TargetDomain,
            pc: PC): target.DomainValue =
            if (target.isInstanceOf[DefaultPreciseIntegerValues]) {
                val thatDomain = target.asInstanceOf[DefaultPreciseIntegerValues]
                thatDomain.TheIntegerValue(this.value, this.updateCount).
                    asInstanceOf[target.DomainValue]
            } else {
                super.adapt(target, pc)
            }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;
            val thisValue = this.value
            other match {
                case TheIntegerValue(`thisValue`, _) ⇒ true
                case _                               ⇒ false
            }
        }

        override def toString: String = "IntegerValue(value="+value+", updates="+updateCount+")"
    }

    override def BooleanValue(pc: PC): DomainValue = AnIntegerValue()
    override def BooleanValue(pc: PC, value: Boolean): DomainValue =
        if (value) new TheIntegerValue(1) else new TheIntegerValue(0)

    override def ByteValue(pc: PC): DomainValue = AnIntegerValue()
    override def ByteValue(pc: PC, value: Byte) = TheIntegerValue(value)

    override def ShortValue(pc: PC): DomainValue = AnIntegerValue()
    override def ShortValue(pc: PC, value: Short) = TheIntegerValue(value)

    override def CharValue(pc: PC): DomainValue = AnIntegerValue()
    override def CharValue(pc: PC, value: Char) = TheIntegerValue(value)

    override def IntegerValue(pc: PC): DomainValue = AnIntegerValue()
    override def IntegerValue(pc: PC, value: Int) = TheIntegerValue(value)

}

