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

import scala.collection.immutable.SortedSet

/**
 * This domain implements the tracking of integer values at the level of sets.
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerSetValues
        extends DefaultDomainValueBinding
        with JoinStabilization
        with IdentityBasedAliasBreakUpDetection
        with IntegerSetValues {
    domain: Configuration with VMLevelExceptionsFactory ⇒

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

        override def hashCode: Int = 107

        override def equals(other: Any): Boolean = other.isInstanceOf[AnIntegerValue]

        override def toString: String = "AnIntegerValue"
    }

    def AnIntegerValue() = new AnIntegerValue()

    class IntegerSet(val values: SortedSet[Int]) extends super.IntegerSet {

        require(values.size > 0)

        override def doJoin(pc: PC, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case that: AnIntegerValue ⇒ StructuralUpdate(AnIntegerValue())
                case IntegerSet(thatValues) ⇒
                    val newValues = this.values ++ thatValues

                    if (newValues.size == 1)
                        // This is a "point-range" (a concrete value), hence there
                        // will be NO further constraints
                        NoUpdate

                    else if (newValues.size > maxCardinalityOfIntegerValuesSet)
                        StructuralUpdate(AnIntegerValue())

                    else if (newValues.size == this.values.size)
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same range, but which can still be two different
                        // runtime values (they were not created at the same time!
                        MetaInformationUpdate(IntegerSet(this.values))
                    else
                        StructuralUpdate(IntegerSet(newValues))
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case that: IntegerSet ⇒
                    that.values.subsetOf(this.values)
                case _ ⇒ false
            })
        }

        override def summarize(pc: PC): DomainValue = this

        override def adapt(target: TargetDomain, pc: PC): target.DomainValue =
            if (target.isInstanceOf[IntegerSetValues]) {
                val thatDomain = target.asInstanceOf[DefaultIntegerSetValues]
                thatDomain.IntegerSet(this.values).asInstanceOf[target.DomainValue]
            } else {
                target.IntegerValue(pc)
            }

        override def hashCode = this.values.hashCode * 13

        override def equals(other: Any): Boolean = {
            val thisValue = this
            other match {
                case that: IntegerSet ⇒
                    (this eq that) || (this.values == that.values)
                case _ ⇒
                    false
            }
        }

        override def toString: String = "IntegerSet("+values.mkString(",")+")"
    }

    override def IntegerSet(values: SortedSet[Int]): IntegerSet = new IntegerSet(values)

    override def BooleanValue(pc: PC): DomainValue = AnIntegerValue()
    override def BooleanValue(pc: PC, value: Boolean): DomainValue =
        if (value) IntegerValue(pc, 1) else IntegerValue(pc, 0)

    override def ByteValue(pc: PC): DomainValue = AnIntegerValue()
    override def ByteValue(pc: PC, value: Byte) = IntegerSet(value)

    override def ShortValue(pc: PC): DomainValue = AnIntegerValue()
    override def ShortValue(pc: PC, value: Short) = IntegerSet(value)

    override def CharValue(pc: PC): DomainValue = AnIntegerValue()
    override def CharValue(pc: PC, value: Char) = IntegerSet(value)

    override def IntegerValue(pc: PC): DomainValue = AnIntegerValue()
    override def IntegerValue(pc: PC, value: Int) = IntegerSet(value)

}

