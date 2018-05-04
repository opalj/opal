/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import org.opalj.br.ComputationalTypeLong

import scala.collection.immutable.SortedSet

/**
 * This domain implements the tracking of long values at the level of sets.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait DefaultLongSetValues
    extends DefaultDomainValueBinding
    with CorrelationalDomain
    with LongSetValues {
    domain: IntegerRangeValuesFactory with Configuration with ExceptionsFactory ⇒

    class ALongValue() extends super.ALongValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = {
            // we are not joining the "same" value; the join stabilization trait
            // takes care of handling potential aliases
            MetaInformationUpdate(new ALongValue())
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other.computationalType == ComputationalTypeLong
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.LongValue(pc)
        }

        override def hashCode: Int = 929

        override def equals(other: Any): Boolean = other.isInstanceOf[LongValue]

        override def toString: String = "ALongValue"
    }

    class LongSet(val values: SortedSet[Long]) extends super.LongSet {

        assert(values.nonEmpty)

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case _: ALongValue ⇒ StructuralUpdate(LongValue(pc))
                case LongSet(thatValues) ⇒
                    val thisValues = this.values
                    val newValues = thisValues ++ thatValues
                    val newValuesSize = newValues.size
                    if (newValuesSize == 1) {
                        // This is a "singleton set" (a concrete value), hence there
                        // will be NO further constraints
                        NoUpdate
                    } else if (newValuesSize > maxCardinalityOfLongSets) {
                        StructuralUpdate(LongValue(pc))
                    } else if (newValuesSize == thisValues.size) {
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same values, but which can still be two different
                        // runtime values (they were not created at the same time!)
                        MetaInformationUpdate(LongSet(thisValues))
                    } else
                        StructuralUpdate(LongSet(newValues))
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (other match {
                case that: LongSet ⇒
                    that.values.subsetOf(this.values)
                case _ ⇒ false
            })
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue =
            if (target.isInstanceOf[LongSetValues]) {
                val thatDomain = target.asInstanceOf[DefaultLongSetValues]
                thatDomain.LongSet(this.values).asInstanceOf[target.DomainValue]
            } else if (values.size == 1) {
                target.LongValue(pc, values.head)
            } else {
                target.LongValue(pc)
            }

        override def hashCode: Int = this.values.hashCode * 13

        override def equals(other: Any): Boolean = {
            other match {
                case that: LongSet ⇒
                    (this eq that) || (this.values == that.values)
                case _ ⇒
                    false
            }
        }

        override def toString: String = values.mkString("LongSet(", ",", ")")
    }

    override def LongValue(origin: ValueOrigin): ALongValue = new ALongValue()

    override def LongSet(values: SortedSet[Long]): LongSet = new LongSet(values)

    override def LongValue(origin: ValueOrigin, value: Long): LongSet = {
        new LongSet(SortedSet(value))
    }

}
