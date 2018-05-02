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

import java.lang.Math.min
import java.lang.Math.max

import org.opalj.br.CTIntType

import scala.collection.immutable.SortedSet
import scala.reflect.ClassTag

/**
 * This domain implements the tracking of integer values using sets.
 *
 * @author Michael Eichberg
 */
trait DefaultIntegerSetValues extends DefaultDomainValueBinding with IntegerSetValues {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory ⇒

    class AnIntegerValue extends super.AnIntegerValue {

        override def doJoin(pc: Int, value: DomainValue): Update[DomainValue] = {
            // - we are not joining the "same" value
            // - the join stabilization trait takes care of handling potential aliases
            MetaInformationUpdate(AnIntegerValue())
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            other.isInstanceOf[IsIntegerValue[_]]
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.IntegerValue(pc)
        }

        override def hashCode: Int = 107

        override def equals(other: Any): Boolean = other.isInstanceOf[AnIntegerValue]

        override def toString: String = "AnIntegerValue"
    }

    def AnIntegerValue(): AnIntegerValue = new AnIntegerValue()

    class IntegerSet(val values: SortedSet[Int]) extends super.IntegerSet {

        assert(values.size > 0)

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case _: AnIntegerValue ⇒ StructuralUpdate(AnIntegerValue())

                case that: BaseTypesBasedSet ⇒
                    // every base types based set is necessarily larger, because the
                    // absolute maximum cardinality is 254 (ByteSet.cardinality-1)
                    val thisLB = this.values.firstKey
                    val thisUB = this.values.lastKey
                    val newLB = min(thisLB, that.lb)
                    val newUB = max(thisUB, that.ub)
                    StructuralUpdate(approximateSet(pc, newLB, newUB))

                case IntegerSet(thatValues) ⇒
                    val newValues = this.values ++ thatValues

                    if (newValues.size == 1) {
                        // This set represents a single, concrete value, hence there
                        // will be NO further constraints that affect this set.
                        // Furthermore, since we have "value semantics" for int
                        // values we are no longer concerned about potential aliasing
                        // relations
                        NoUpdate

                    } else if (newValues.size > maxCardinalityOfIntegerSets) {
                        StructuralUpdate(approximateSet(pc, newValues.firstKey, newValues.lastKey))

                    } else if (newValues.size == this.values.size) {
                        // This is NOT a "NoUpdate" since we have two values that may
                        // have the same range, but which can still be two different
                        // runtime values (they were not created at the same time!
                        MetaInformationUpdate(IntegerSet(this.values))
                    } else {
                        StructuralUpdate(IntegerSet(newValues))
                    }
            }
            result
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case that: IntegerSet ⇒ that.values.subsetOf(this.values)
                    case _                ⇒ false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            if (target.isInstanceOf[IntegerSetValues]) {
                val thatDomain = target.asInstanceOf[IntegerSetValues]
                thatDomain.IntegerSet(this.values).asInstanceOf[target.DomainValue]
            } else if (target.isInstanceOf[IntegerRangeValues]) {
                val thatDomain = target.asInstanceOf[IntegerRangeValues]
                val value = thatDomain.IntegerRange(this.values.firstKey, this.values.lastKey)
                value.asInstanceOf[target.DomainValue]
            } else {
                target.IntegerValue(pc)
            }
        }

        override def hashCode = this.values.hashCode * 13

        override def equals(other: Any): Boolean = {
            other match {
                case that: IntegerSet ⇒ (this eq that) || (this.values == that.values)
                case _                ⇒ false
            }
        }

        override def toString: String = "IntegerSet("+values.mkString(",")+")"
    }

    // NOTE: IF WE HAVE TWO VALUES THAT ARE REFERENCE EQUAL, THEN THE
    // (UNKNOWN) UNDERLYING VALUE IS THE SAME!
    trait BaseTypesBasedSet extends super.BaseTypesBasedSet { this: DomainValue ⇒

        def name: String

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            other match {
                case _: AnIntegerValue ⇒ StructuralUpdate(AnIntegerValue())

                case that: BaseTypesBasedSet ⇒
                    val thisLB = this.lb
                    val thisUB = this.ub
                    val newLB = min(thisLB, that.lb)
                    val newUB = max(thisUB, that.ub)
                    if (thisLB == newLB && thisUB == newUB)
                        MetaInformationUpdate(newInstance)
                    else
                        StructuralUpdate(approximateSet(pc, newLB, newUB))

                case IntegerSet(thatValues) ⇒
                    val thisLB = this.lb
                    val thisUB = this.ub
                    val thatLB = thatValues.firstKey
                    val thatUB = thatValues.lastKey
                    if (thisLB <= thatLB && thisUB >= thatUB)
                        MetaInformationUpdate(newInstance)
                    else {
                        val newLB = min(thisLB, thatLB)
                        val newUB = max(thisUB, thatUB)
                        StructuralUpdate(approximateSet(pc, newLB, newUB))
                    }

            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            (this eq other) || (
                other match {
                    case other: BaseTypesBasedSet ⇒
                        lb <= other.lb && ub >= other.ub
                    case that: IntegerSet ⇒
                        lb <= that.values.firstKey && ub >= that.values.lastKey

                    case _ ⇒ false
                }
            )
        }

        override def summarize(pc: Int): DomainValue = this

        override def hashCode = ub

        override def equals(other: Any): Boolean = {
            other match {
                case that: BaseTypesBasedSet ⇒ this.lb == that.lb && this.ub == that.ub
                case _                       ⇒ false
            }
        }

        override def toString: String = s"$name=[$lb,$ub]"
    }

    type DomainBaseTypesBasedSet = BaseTypesBasedSet
    val DomainBaseTypesBasedSet: ClassTag[DomainBaseTypesBasedSet] = implicitly

    def U7BitSet(): DomainTypedValue[CTIntType] = new U7BitSet

    class U7BitSet extends super.U7BitSet with BaseTypesBasedSet { this: DomainValue ⇒
        def name = "Unsigned7BitValue"
        def newInstance = new U7BitSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            val result = target match {
                case isv: IntegerSetValues   ⇒ isv.U7BitSet()
                case irv: IntegerRangeValues ⇒ irv.IntegerRange(lb, ub)
                case _                       ⇒ target.ByteValue(pc)
            }
            result.asInstanceOf[target.DomainValue]
        }
    }

    def U15BitSet(): DomainTypedValue[CTIntType] = new U15BitSet()

    class U15BitSet extends super.U15BitSet with BaseTypesBasedSet { this: DomainValue ⇒
        def name = "Unsigned15BitValue"
        def newInstance = new U15BitSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            val result = target match {
                case isv: IntegerSetValues   ⇒ isv.U15BitSet()
                case irv: IntegerRangeValues ⇒ irv.IntegerRange(lb, ub)
                case _                       ⇒ target.ByteValue(pc)
            }
            result.asInstanceOf[target.DomainValue]

        }
    }

    class ByteSet extends super.ByteSet with BaseTypesBasedSet {
        def name = "ByteValue"
        def newInstance = new ByteSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.ByteValue(pc)
        }
    }

    class ShortSet extends super.ShortSet with BaseTypesBasedSet {
        def name = "ShortValue"
        def newInstance = new ShortSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.ShortValue(pc)
        }
    }

    class CharSet extends super.CharSet with BaseTypesBasedSet {
        def name = "CharCalue"
        def newInstance = new CharSet
        override def adapt(target: TargetDomain, pc: Int): target.DomainValue = {
            target.CharValue(pc)
        }
    }

    override def IntegerSet(values: SortedSet[Int]): IntegerSet = new IntegerSet(values)

    override def BooleanValue(pc: Int): IntegerLikeValue = {
        if (maxCardinalityOfIntegerSets > 1)
            IntegerSet(SortedSet(0, 1))
        else
            new ByteSet()
    }

    override def BooleanValue(pc: Int, value: Boolean): DomainTypedValue[CTIntType] = {
        if (value) IntegerValue(pc, 1) else IntegerValue(pc, 0)
    }

    override def ByteValue(pc: Int): ByteSet = new ByteSet()
    override def ByteValue(pc: Int, value: Byte): DomainTypedValue[CTIntType] = IntegerSet(value.toInt)

    override def ShortValue(pc: Int): ShortSet = new ShortSet()
    override def ShortValue(pc: Int, value: Short): DomainTypedValue[CTIntType] = IntegerSet(value.toInt)

    override def CharValue(pc: Int): CharSet = new CharSet()
    override def CharValue(pc: Int, value: Char): DomainTypedValue[CTIntType] = IntegerSet(value.toInt)

    override def IntegerValue(pc: Int): AnIntegerValue = AnIntegerValue()
    override def IntegerValue(pc: Int, value: Int): DomainTypedValue[CTIntType] = IntegerSet(value)

}
