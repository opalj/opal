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
package l1

import org.opalj.util.{ Answer, Yes, No, Unknown }

/**
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerRangeValues
        extends DefaultDomainValueBinding
        with IntegerRangeValues {
    this: Configuration ⇒

    /**
     * @note The functionality to propagate a constraint crucially depends on
     *      the fact two integer values created at two different places are represented
     *      by two different instances of "AnIntegerValue"; otherwise, propagating the
     *      constraint that some value (after some kind of check) has to have a special
     *      value may affect unrelated values!
     */
    class AnIntegerValue() extends super.AnIntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = NoUpdate

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: Domain, pc: PC): target.DomainValue =
            target.IntegerValue(pc)

        override def hashCode: Int = 101;

        override def equals(other: Any): Boolean = {
            other match {
                case that: AnIntegerValue ⇒ true
                case _                    ⇒ false
            }
        }

        override def toString: String = "AnIntegerValue"
    }

    def AnIntegerValue() = new AnIntegerValue()

    class IntegerRange(
        val lowerBound: Int,
        val upperBound: Int)
            extends super.IntegerRange {

        def update(newValue: Int): DomainValue = {
            val newLowerBound = if (lowerBound > newValue) newValue else lowerBound
            val newUpperBound = if (upperBound < newValue) newValue else upperBound

            new IntegerRange(newLowerBound, newUpperBound)
        }

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case that: AnIntegerValue ⇒ StructuralUpdate(that)
                case IntegerRange(otherLB, otherUB) ⇒
                    val newLowerBound = Math.min(this.lowerBound, otherLB)
                    val newUpperBound = Math.max(this.upperBound, otherUB)

                    if (newUpperBound.toLong - newLowerBound.toLong > maxSizeOfIntegerRanges)
                        StructuralUpdate(AnIntegerValue())

                    else if (newLowerBound == lowerBound && newUpperBound == upperBound)
                        NoUpdate
                    else if (newLowerBound == otherLB && newUpperBound == otherUB)
                        StructuralUpdate(other)
                    else
                        StructuralUpdate(IntegerRange(newLowerBound, newUpperBound))
            }
            result
        }

        override def summarize(pc: PC): DomainValue = this

        override def adapt(
            targetDomain: Domain,
            pc: PC): targetDomain.DomainValue =
            if (targetDomain.isInstanceOf[DefaultIntegerRangeValues]) {
                val thatDomain = targetDomain.asInstanceOf[DefaultIntegerRangeValues]
                thatDomain.IntegerRange(this.lowerBound, this.upperBound).
                    asInstanceOf[targetDomain.DomainValue]
            } else {
                targetDomain.IntegerValue(pc)
            }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true;

            other match {
                case IntegerRange(otherLB, otherUB) ⇒
                    this.lowerBound <= otherLB && this.upperBound >= otherUB
                case _ ⇒ false
            }
        }

        override def hashCode = this.lowerBound * 13 + this.upperBound

        override def equals(other: Any): Boolean = {
            val thisValue = this
            other match {
                case that: IntegerRange ⇒
                    (this eq that) || (
                        that.lowerBound == this.lowerBound && that.upperBound == this.upperBound
                    )
                case _ ⇒
                    false
            }
        }

        override def toString: String = "IntegerRange(lb="+lowerBound+", ub="+upperBound+")"
    }

    override def IntegerRange(lb: Int, ub: Int): DomainValue = new IntegerRange(lb, ub)

    override def BooleanValue(pc: PC): DomainValue = AnIntegerValue()
    override def BooleanValue(pc: PC, value: Boolean): DomainValue =
        if (value) IntegerValue(pc, 1) else IntegerValue(pc, 0)

    override def ByteValue(pc: PC): DomainValue = AnIntegerValue()
    override def ByteValue(pc: PC, value: Byte) = new IntegerRange(value, value)

    override def ShortValue(pc: PC): DomainValue = AnIntegerValue()
    override def ShortValue(pc: PC, value: Short) = new IntegerRange(value, value)

    override def CharValue(pc: PC): DomainValue = AnIntegerValue()
    override def CharValue(pc: PC, value: Char) = new IntegerRange(value, value)

    override def IntegerValue(pc: PC): DomainValue = AnIntegerValue()
    override def IntegerValue(pc: PC, value: Int) = new IntegerRange(value, value)

}

