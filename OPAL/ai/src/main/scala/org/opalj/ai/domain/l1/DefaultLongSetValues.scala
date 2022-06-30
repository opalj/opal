/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.collection.immutable.SortedSet

import org.opalj.br.ComputationalTypeLong

/**
 * This domain implements the tracking of long values at the level of sets.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait DefaultLongSetValues
    extends DefaultSpecialDomainValuesBinding
    with CorrelationalDomain
    with LongSetValues {
    domain: IntegerRangeValuesFactory with Configuration with ExceptionsFactory =>

    class ALongValue() extends super.ALongValueLike {

        override def constantValue: Option[Long] = None

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

    class LongSet(val values: SortedSet[Long]) extends super.LongSetLike {

        assert(values.nonEmpty)

        override def constantValue: Option[Long] = {
            if (values.size == 1) Some(values.head) else None
        }

        override def doJoin(pc: Int, other: DomainValue): Update[DomainValue] = {
            val result = other match {
                case _: ALongValue => StructuralUpdate(LongValue(pc))
                case LongSetLike(thatValues) =>
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
                case that: LongSet =>
                    that.values.subsetOf(this.values)
                case _ => false
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
                case that: LongSet =>
                    (this eq that) || (this.values == that.values)
                case _ =>
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
