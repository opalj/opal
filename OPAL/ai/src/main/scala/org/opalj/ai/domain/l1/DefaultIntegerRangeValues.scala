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

/**
 * This domain implements the tracking of integer values at the level of ranges.
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerRangeValues
        extends DefaultDomainValueBinding
        with JoinStabilization
        with IdentityBasedAliasBreakUpDetection
        with IntegerRangeValues {
    domain: Configuration with VMLevelExceptionsFactory ⇒

    /**
     * @note The functionality to propagate a constraint crucially depends on
     *      the fact that two integer values created at two different places / at
     *      one place but at two different points in time
     *      are represented by two different instances
     *      of "AnIntegerValue"; otherwise, propagating the
     *      constraint that some value (after some kind of check) has to have a special
     *      value may affect unrelated values!
     */
    class AnIntegerValue() extends super.AnIntegerValue {

        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = {
            // we are not joining the "same" value; the join stabilization trait
            // takes care of handling potential aliases
            MetaInformationUpdate(AnIntegerValue())
        }

        override def abstractsOver(other: DomainValue): Boolean =
            other.isInstanceOf[IsIntegerValue]

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            target.IntegerValue(pc)

        override def newInstance: AnIntegerValue = AnIntegerValue()

        override def hashCode: Int = 101

        override def equals(other: Any): Boolean = {
            other match {
                case that: AnIntegerValue ⇒ true
                case _                    ⇒ false
            }
        }

        override def toString: String = "AnIntegerValue"
    }

    def AnIntegerValue() = new AnIntegerValue()

    class IntegerRange(val lowerBound: Int, val upperBound: Int) extends super.IntegerRange {

        def update(newValue: Int): DomainValue = {
            val newLowerBound = if (lowerBound > newValue) newValue else lowerBound
            val newUpperBound = if (upperBound < newValue) newValue else upperBound

            new IntegerRange(newLowerBound, newUpperBound)
        }

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case that: AnIntegerValue ⇒ StructuralUpdate(AnIntegerValue())
                case IntegerRange(otherLB, otherUB) ⇒
                    val newLowerBound = Math.min(this.lowerBound, otherLB)
                    val newUpperBound = Math.max(this.upperBound, otherUB)

                    if (newLowerBound == newUpperBound)
                        // This is a "point-range" (a concrete value), hence there
                        // will be NO further constraints
                        NoUpdate

                    else if (newUpperBound.toLong - newLowerBound.toLong > maxSizeOfIntegerRanges)
                        StructuralUpdate(AnIntegerValue())

                    else if (newLowerBound == lowerBound && newUpperBound == upperBound)
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same range, but which can still be two different
                        // runtime values (they were not created at the same time!
                        MetaInformationUpdate(IntegerRange(newLowerBound, newUpperBound))
                    else
                        StructuralUpdate(IntegerRange(newLowerBound, newUpperBound))
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case that: IntegerRange ⇒
                    this.lowerBound <= that.lowerBound &&
                        this.upperBound >= that.upperBound
                case _ ⇒ false
            })
        }

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            if (target.isInstanceOf[IntegerRangeValues]) {
                val thatDomain = target.asInstanceOf[DefaultIntegerRangeValues]
                thatDomain.IntegerRange(this.lowerBound, this.upperBound).
                    asInstanceOf[target.DomainValue]
            } else {
                target.IntegerValue(pc)
            }

        override def newInstance: IntegerRange = IntegerRange(lowerBound, upperBound)

        override def hashCode = this.lowerBound * 13 + this.upperBound

        override def equals(other: Any): Boolean = {
            val thisValue = this
            other match {
                case that: IntegerRange ⇒
                    (this eq that) || (
                        that.lowerBound == this.lowerBound &&
                        that.upperBound == this.upperBound
                    )
                case _ ⇒
                    false
            }
        }

        override def toString: String = "IntegerRange(lb="+lowerBound+", ub="+upperBound+")"
    }

    override def IntegerRange(lb: Int, ub: Int): IntegerRange = new IntegerRange(lb, ub)

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

